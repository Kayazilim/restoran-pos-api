package com.restoran.pos;

import com.restoran.pos.dto.bill.PaymentCreateRequest;
import com.restoran.pos.dto.bill.PaymentResult;
import com.restoran.pos.dto.order.OrderCreateRequest;
import com.restoran.pos.dto.order.OrderItemRequest;
import com.restoran.pos.dto.order.OrderPlacementResult;
import com.restoran.pos.dto.product.ProductCreateRequest;
import com.restoran.pos.dto.product.ProductResponse;
import com.restoran.pos.dto.table.TableCreateRequest;
import com.restoran.pos.dto.table.TableCreatedResponse;
import com.restoran.pos.entity.PaymentMethod;
import com.restoran.pos.exception.InsufficientStockException;
import com.restoran.pos.repository.UserAccountRepository;
import com.restoran.pos.security.CurrentUser;
import com.restoran.pos.security.PosUserDetails;
import com.restoran.pos.service.BillService;
import com.restoran.pos.service.OrderService;
import com.restoran.pos.service.ProductService;
import com.restoran.pos.service.TableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Projenin ana problemi: eşzamanlı işlemler. Aynı anda gelen istekler gerçek iş parçacıklarıyla
 * (aynı anda serbest bırakılarak) simüle edilir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrencyTest {

    private static final AtomicInteger TABLE_NUMBERS = new AtomicInteger(1000);

    private final OrderService orderService;
    private final BillService billService;
    private final ProductService productService;
    private final TableService tableService;
    private final UserDetailsService userDetailsService;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    ConcurrencyTest(OrderService orderService, BillService billService, ProductService productService,
                    TableService tableService, UserDetailsService userDetailsService,
                    UserAccountRepository userAccountRepository) {
        this.orderService = orderService;
        this.billService = billService;
        this.productService = productService;
        this.tableService = tableService;
        this.userDetailsService = userDetailsService;
        this.userAccountRepository = userAccountRepository;
    }

    @Test
    void lastPortionIsSoldOnlyOnceWhenOrderedAtTheSameMomentFromDifferentTables() throws Exception {
        ProductResponse lastPortion = createProduct(1);
        Long waiterId = userAccountRepository.findByUsername(TestUsers.WAITER_1).orElseThrow().getId();
        CurrentUser waiter = currentUser(TestUsers.WAITER_1);

        int parallelRequests = 8;
        List<Long> tableIds = new ArrayList<>();
        for (int i = 0; i < parallelRequests; i++) {
            tableIds.add(createTable(waiterId).table().id());
        }

        List<Callable<Boolean>> attempts = tableIds.stream().<Callable<Boolean>>map(tableId -> () -> {
            try {
                orderService.placeOrder(orderFor(tableId, lastPortion.id(), 1), UUID.randomUUID().toString(), waiter);
                return true;
            } catch (InsufficientStockException ex) {
                return false;
            }
        }).toList();

        List<Boolean> results = runSimultaneously(attempts);

        assertThat(results.stream().filter(Boolean::booleanValue).count())
                .as("son porsiyonu yalnızca bir sipariş alabilmeli").isEqualTo(1);
        assertThat(productService.findById(lastPortion.id()).stockQuantity())
                .as("stok asla eksiye düşmemeli").isZero();
    }

    @Test
    void repeatedSubmitWithSameIdempotencyKeyCreatesSingleOrderAndDecreasesStockOnce() throws Exception {
        ProductResponse product = createProduct(10);
        TableCreatedResponse table = createTable(null);
        CurrentUser customer = currentUser(table.qrCredentials().username());
        String sameKey = UUID.randomUUID().toString();
        OrderCreateRequest request = orderFor(null, product.id(), 2);

        List<Callable<OrderPlacementResult>> clicks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clicks.add(() -> orderService.placeOrder(request, sameKey, customer));
        }
        List<OrderPlacementResult> results = runSimultaneously(clicks);

        Set<Long> orderIds = results.stream().map(result -> result.order().id()).collect(Collectors.toSet());
        assertThat(orderIds).as("tüm tıklamalar aynı siparişi döndürmeli").hasSize(1);
        assertThat(results.stream().filter(result -> !result.replayed()).count()).isEqualTo(1);
        assertThat(productService.findById(product.id()).stockQuantity()).isEqualTo(8);
    }

    @Test
    void repeatedPaymentWithSameIdempotencyKeyChargesOnlyOnce() throws Exception {
        ProductResponse product = createProduct(5);
        TableCreatedResponse table = createTable(null);
        CurrentUser customer = currentUser(table.qrCredentials().username());
        OrderPlacementResult order = orderService.placeOrder(orderFor(null, product.id(), 2),
                UUID.randomUUID().toString(), customer);
        Long billId = order.order().billId();
        CurrentUser cashier = currentUser(TestUsers.CASHIER);
        String paymentKey = UUID.randomUUID().toString();
        PaymentCreateRequest payment = new PaymentCreateRequest(new BigDecimal("200.00"), PaymentMethod.CARD);

        List<Callable<PaymentResult>> clicks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clicks.add(() -> billService.addPayment(billId, payment, paymentKey, cashier));
        }
        List<PaymentResult> results = runSimultaneously(clicks);

        assertThat(results.stream().filter(result -> !result.replayed()).count()).isEqualTo(1);
        assertThat(billService.findById(billId, cashier).paidAmount()).isEqualByComparingTo("200.00");
        assertThat(billService.findPayments(billId, cashier)).hasSize(1);
    }

    /** Tüm görevleri hazır bekletip aynı anda serbest bırakır. */
    private static <T> List<T> runSimultaneously(List<Callable<T>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private ProductResponse createProduct(int stock) {
        return productService.create(new ProductCreateRequest("Eşzamanlılık Test Ürünü " + UUID.randomUUID(), null,
                "Test", new BigDecimal("100.00"), stock, List.of(), true));
    }

    private TableCreatedResponse createTable(Long waiterId) {
        return tableService.create(new TableCreateRequest(TABLE_NUMBERS.incrementAndGet(), 4, "Test", waiterId, true));
    }

    private static OrderCreateRequest orderFor(Long tableId, Long productId, int quantity) {
        return new OrderCreateRequest(tableId, List.of(new OrderItemRequest(productId, quantity, null)), null);
    }

    private CurrentUser currentUser(String username) {
        return ((PosUserDetails) userDetailsService.loadUserByUsername(username)).currentUser();
    }
}
