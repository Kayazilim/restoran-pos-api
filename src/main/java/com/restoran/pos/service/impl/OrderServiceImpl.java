package com.restoran.pos.service.impl;

import com.restoran.pos.dto.kitchen.KitchenOrderResponse;
import com.restoran.pos.dto.order.OrderCreateRequest;
import com.restoran.pos.dto.order.OrderItemRequest;
import com.restoran.pos.dto.order.OrderPatchRequest;
import com.restoran.pos.dto.order.OrderPlacementResult;
import com.restoran.pos.dto.order.OrderResponse;
import com.restoran.pos.dto.order.OrderUpdateRequest;
import com.restoran.pos.entity.Bill;
import com.restoran.pos.entity.BillStatus;
import com.restoran.pos.entity.CustomerOrder;
import com.restoran.pos.entity.OrderChannel;
import com.restoran.pos.entity.OrderItem;
import com.restoran.pos.entity.OrderStatus;
import com.restoran.pos.entity.Product;
import com.restoran.pos.entity.RestaurantTable;
import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.exception.BadRequestException;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ForbiddenOperationException;
import com.restoran.pos.exception.InsufficientStockException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.notification.KitchenEventType;
import com.restoran.pos.notification.KitchenOrderEvent;
import com.restoran.pos.repository.BillRepository;
import com.restoran.pos.repository.OrderRepository;
import com.restoran.pos.repository.OrderSearchCriteria;
import com.restoran.pos.repository.ProductRepository;
import com.restoran.pos.repository.RestaurantTableRepository;
import com.restoran.pos.repository.UserAccountRepository;
import com.restoran.pos.security.AccessPolicy;
import com.restoran.pos.security.CurrentUser;
import com.restoran.pos.service.OrderService;
import com.restoran.pos.util.IdempotencyKeys;
import com.restoran.pos.util.PatchGuard;
import com.restoran.pos.util.Texts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Sipariş iş kuralları ve eşzamanlılık güvenliği.
 * <p>
 * Kilit sırası her işlemde aynıdır: <b>masa → sipariş → ürünler (id sırasıyla) → adisyon</b>.
 * Bu sıra kilitlenmeyi (deadlock) önler; kilit bekleme süresi sınırlı olduğundan yoğun anlarda sistem
 * kilitlenip kalmaz, istek anlaşılır bir 409 hatasıyla sonlanır.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestaurantTableRepository tableRepository;
    private final BillRepository billRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccessPolicy accessPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final int maxQuantityPerItem;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            RestaurantTableRepository tableRepository,
                            BillRepository billRepository,
                            UserAccountRepository userAccountRepository,
                            AccessPolicy accessPolicy,
                            ApplicationEventPublisher eventPublisher,
                            Clock clock,
                            @Value("${pos.order.max-quantity-per-item}") int maxQuantityPerItem) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.tableRepository = tableRepository;
        this.billRepository = billRepository;
        this.userAccountRepository = userAccountRepository;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.maxQuantityPerItem = maxQuantityPerItem;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(OrderStatus status, Long tableId, CurrentUser user) {
        Collection<OrderStatus> statuses = status != null ? List.of(status) : null;
        OrderSearchCriteria criteria;
        if (user.isTableBound()) {
            // Müşteri yalnızca kendi masasının güncel (açık adisyondaki) siparişlerini görür.
            if (tableId != null && !tableId.equals(user.tableId())) {
                throw new ForbiddenOperationException("Yalnızca kendi masanızın siparişlerini görebilirsiniz.");
            }
            criteria = new OrderSearchCriteria(user.tableId(), null, statuses, true, false);
        } else if (accessPolicy.hasAllTablesAccess(user)) {
            criteria = new OrderSearchCriteria(tableId, null, statuses, false, false);
        } else {
            // Garson yalnızca kendisine atanmış masaların siparişlerini görür.
            if (tableId != null) {
                accessPolicy.checkTableAccess(user, getTable(tableId));
            }
            criteria = new OrderSearchCriteria(tableId, user.id(), statuses, false, false);
        }
        return orderRepository.search(criteria).stream().map(OrderResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findById(Long id, CurrentUser user) {
        CustomerOrder order = orderRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Sipariş", id));
        accessPolicy.checkTableAccess(user, order.getTable());
        if (user.isTableBound() && order.getBill().getStatus() != BillStatus.OPEN) {
            throw new ForbiddenOperationException("Masanın önceki oturumlarına ait siparişlere erişemezsiniz.");
        }
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderPlacementResult placeOrder(OrderCreateRequest request, String idempotencyKey, CurrentUser user) {
        String key = IdempotencyKeys.validate(idempotencyKey);
        Map<Long, Integer> requestedQuantities = sumQuantities(request.items());
        Long tableId = resolveTargetTableId(request.tableId(), user);

        // 1) Masa satırı kilitlenir: aynı masaya aynı anda gelen istekler (çift tıklama dahil) sıraya girer.
        RestaurantTable table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> ResourceNotFoundException.of("Masa", tableId));
        accessPolicy.checkTableAccess(user, table);
        if (!table.isActive()) {
            throw new ConflictException("Masa " + table.getTableNumber() + " kullanımda değil; sipariş verilemez.");
        }

        // 2) Idempotency: bu anahtarla daha önce sipariş oluşturulduysa yeniden işlenmez, aynı sipariş döner.
        Optional<CustomerOrder> previous = orderRepository.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            CustomerOrder existing = previous.get();
            if (!existing.getTable().getId().equals(tableId)) {
                throw new ConflictException("Bu Idempotency-Key başka bir masanın siparişi için kullanılmış.");
            }
            return new OrderPlacementResult(OrderResponse.from(existing), true);
        }

        // 3) Ürün satırları kilitlenir; stok yeterliyse düşülür, değilse işlemin tamamı geri alınır.
        Map<Long, Product> products = applyStockChange(Map.of(), requestedQuantities);

        // 4) Masanın açık adisyonu bulunur veya açılır (masa kilidi altında: masa başına tek açık adisyon).
        Bill bill = billRepository.findOpenByTableIdForUpdate(tableId)
                .orElseGet(() -> billRepository.save(new Bill(table, Instant.now(clock))));

        // 5) Sipariş kaydedilir. idempotency_key üzerindeki UNIQUE kısıt, çift kaydı veritabanında da engeller.
        UserAccount creator = userAccountRepository.findById(user.id())
                .orElseThrow(() -> ResourceNotFoundException.of("Kullanıcı", user.id()));
        OrderChannel channel = user.isTableBound() ? OrderChannel.QR_MENU : OrderChannel.WAITER;
        CustomerOrder order = new CustomerOrder(table, bill, creator, channel, Texts.trimToNull(request.note()), key);
        addItems(order, request.items(), products);
        orderRepository.save(order);
        adjustBillTotal(bill, order.getTotalAmount());

        // 6) Commit sonrası mutfak ekranlarına anlık bildirim (SSE).
        publish(KitchenEventType.ORDER_CREATED, order);
        return new OrderPlacementResult(OrderResponse.from(order), false);
    }

    @Override
    @Transactional
    public OrderResponse update(Long id, OrderUpdateRequest request, CurrentUser user) {
        CustomerOrder order = lockOrderForModification(id, user);
        replaceItems(order, request.items());
        order.setNote(Texts.trimToNull(request.note()));
        CustomerOrder saved = orderRepository.update(order);
        publish(KitchenEventType.ORDER_UPDATED, saved);
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional
    public OrderResponse patch(Long id, OrderPatchRequest request, CurrentUser user) {
        PatchGuard.ensureIdUnchanged(id, request.id());
        CustomerOrder order = lockOrderForModification(id, user);
        if (request.items() != null) {
            replaceItems(order, request.items());
        }
        if (request.note() != null) {
            order.setNote(Texts.trimToNull(request.note()));
        }
        CustomerOrder saved = orderRepository.update(order);
        publish(KitchenEventType.ORDER_UPDATED, saved);
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional
    public void cancel(Long id, CurrentUser user) {
        CustomerOrder order = lockOrderForModification(id, user);
        // İptal edilen siparişin ürünleri stoğa iade edilir.
        applyStockChange(quantitiesOf(order), Map.of());
        Bill bill = lockOpenBill(order.getBill().getId());
        adjustBillTotal(bill, order.getTotalAmount().negate());
        order.setStatus(OrderStatus.CANCELLED);
        CustomerOrder saved = orderRepository.update(order);
        publish(KitchenEventType.ORDER_CANCELLED, saved);
    }

    /**
     * Değişiklik yapılacak siparişin masasını ve kendisini (bu sırayla) kilitler, sahiplik ve durum kurallarını denetler.
     */
    private CustomerOrder lockOrderForModification(Long orderId, CurrentUser user) {
        Long tableId = orderRepository.findTableIdByOrderId(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sipariş", orderId));
        RestaurantTable table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> ResourceNotFoundException.of("Masa", tableId));
        accessPolicy.checkTableAccess(user, table);
        if (user.isTableBound()) {
            throw new ForbiddenOperationException(
                    "Müşteri hesapları verilen siparişi değiştiremez veya iptal edemez; lütfen garsonunuzdan yardım isteyin.");
        }
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sipariş", orderId));
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ConflictException("Yalnızca 'Onaylandı' durumundaki (mutfağın henüz başlamadığı) siparişler "
                    + "değiştirilebilir veya iptal edilebilir; siparişin durumu: " + order.getStatus().getLabel());
        }
        return order;
    }

    private void replaceItems(CustomerOrder order, List<OrderItemRequest> items) {
        BigDecimal oldTotal = order.getTotalAmount();
        Map<Long, Product> products = applyStockChange(quantitiesOf(order), sumQuantities(items));
        order.clearItems();
        addItems(order, items, products);
        Bill bill = lockOpenBill(order.getBill().getId());
        adjustBillTotal(bill, order.getTotalAmount().subtract(oldTotal));
    }

    /**
     * Eski ve yeni adetler arasındaki farkı stoğa uygular. Etkilenen ürün satırları id sırasıyla
     * PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) ile kilitlenir: aynı ürünü aynı anda sipariş eden ikinci işlem,
     * birincisi bitene kadar bekler ve güncel stoğu görür. Böylece son ürün iki kez satılamaz, stok eksiye düşmez.
     */
    private Map<Long, Product> applyStockChange(Map<Long, Integer> oldQuantities, Map<Long, Integer> newQuantities) {
        Set<Long> productIds = new TreeSet<>(oldQuantities.keySet());
        productIds.addAll(newQuantities.keySet());

        Map<Long, Product> products = new HashMap<>();
        for (Product product : productRepository.findAllByIdsForUpdate(productIds)) {
            products.put(product.getId(), product);
        }
        List<Long> missing = productIds.stream().filter(productId -> !products.containsKey(productId)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Ürün bulunamadı: id=" + missing);
        }

        List<String> problems = new ArrayList<>();
        for (Long productId : productIds) {
            int delta = newQuantities.getOrDefault(productId, 0) - oldQuantities.getOrDefault(productId, 0);
            Product product = products.get(productId);
            if (delta > 0 && !product.isActive()) {
                problems.add(product.getName() + ": şu anda satışta değil");
            } else if (delta > 0 && product.getStockQuantity() < delta) {
                problems.add(product.getName() + ": istenen " + delta + " adet, stokta kalan " + product.getStockQuantity());
            }
        }
        if (!problems.isEmpty()) {
            throw new InsufficientStockException(problems);
        }

        for (Long productId : productIds) {
            int delta = newQuantities.getOrDefault(productId, 0) - oldQuantities.getOrDefault(productId, 0);
            if (delta != 0) {
                Product product = products.get(productId);
                product.setStockQuantity(product.getStockQuantity() - delta);
            }
        }
        return products;
    }

    private Bill lockOpenBill(Long billId) {
        Bill bill = billRepository.findByIdForUpdate(billId)
                .orElseThrow(() -> ResourceNotFoundException.of("Adisyon", billId));
        if (bill.getStatus() != BillStatus.OPEN) {
            throw new ConflictException("Adisyon kapatılmış; bu siparişte değişiklik yapılamaz.");
        }
        return bill;
    }

    private static void adjustBillTotal(Bill bill, BigDecimal delta) {
        BigDecimal newTotal = bill.getTotalAmount().add(delta);
        if (newTotal.compareTo(bill.getPaidAmount()) < 0) {
            throw new ConflictException("Bu değişiklikle adisyon tutarı alınmış ödemelerin altına düşüyor; "
                    + "önce kasiyerle görüşün.");
        }
        bill.setTotalAmount(newTotal);
    }

    private Long resolveTargetTableId(Long requestedTableId, CurrentUser user) {
        if (user.isTableBound()) {
            // Müşteri her zaman yalnızca kendi masasına sipariş verebilir.
            if (requestedTableId != null && !requestedTableId.equals(user.tableId())) {
                throw new ForbiddenOperationException("Yalnızca kendi masanız için sipariş verebilirsiniz.");
            }
            return user.tableId();
        }
        if (requestedTableId == null) {
            throw new BadRequestException("tableId alanı zorunludur.");
        }
        return requestedTableId;
    }

    private Map<Long, Integer> sumQuantities(List<OrderItemRequest> items) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        quantities.forEach((productId, quantity) -> {
            if (quantity > maxQuantityPerItem) {
                throw new BadRequestException("Bir üründen tek siparişte en fazla " + maxQuantityPerItem
                        + " adet istenebilir (ürün id=" + productId + ").");
            }
        });
        return quantities;
    }

    private static Map<Long, Integer> quantitiesOf(CustomerOrder order) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItem item : order.getItems()) {
            quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private static void addItems(CustomerOrder order, List<OrderItemRequest> items, Map<Long, Product> products) {
        for (OrderItemRequest item : items) {
            order.addItem(new OrderItem(products.get(item.productId()), item.quantity(), Texts.trimToNull(item.note())));
        }
        order.recalculateTotal();
    }

    private RestaurantTable getTable(Long tableId) {
        return tableRepository.findById(tableId).orElseThrow(() -> ResourceNotFoundException.of("Masa", tableId));
    }

    private void publish(KitchenEventType type, CustomerOrder order) {
        eventPublisher.publishEvent(new KitchenOrderEvent(type, KitchenOrderResponse.from(order)));
    }
}
