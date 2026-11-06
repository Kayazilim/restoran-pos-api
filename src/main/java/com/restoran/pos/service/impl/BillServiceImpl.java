package com.restoran.pos.service.impl;

import com.restoran.pos.dto.bill.BillPatchRequest;
import com.restoran.pos.dto.bill.BillResponse;
import com.restoran.pos.dto.bill.PaymentCreateRequest;
import com.restoran.pos.dto.bill.PaymentReceiptResponse;
import com.restoran.pos.dto.bill.PaymentResponse;
import com.restoran.pos.dto.bill.PaymentResult;
import com.restoran.pos.entity.Bill;
import com.restoran.pos.entity.BillStatus;
import com.restoran.pos.entity.Payment;
import com.restoran.pos.entity.RestaurantTable;
import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.exception.BadRequestException;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ForbiddenOperationException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.repository.BillRepository;
import com.restoran.pos.repository.PaymentRepository;
import com.restoran.pos.repository.RestaurantTableRepository;
import com.restoran.pos.repository.UserAccountRepository;
import com.restoran.pos.security.AccessPolicy;
import com.restoran.pos.security.CurrentUser;
import com.restoran.pos.service.BillService;
import com.restoran.pos.util.IdempotencyKeys;
import com.restoran.pos.util.PatchGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adisyon ve ödeme iş kuralları. Ödemeler Idempotency-Key ve adisyon satırı kilidiyle çifte tahsilata karşı korunur.
 */
@Service
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public BillServiceImpl(BillRepository billRepository,
                           PaymentRepository paymentRepository,
                           RestaurantTableRepository tableRepository,
                           UserAccountRepository userAccountRepository,
                           AccessPolicy accessPolicy,
                           Clock clock) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.tableRepository = tableRepository;
        this.userAccountRepository = userAccountRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillResponse> findAll(BillStatus status, CurrentUser user) {
        List<Bill> bills;
        if (user.isTableBound()) {
            // Müşteri yalnızca kendi masasının güncel (açık) adisyonunu görür.
            if (status == BillStatus.CLOSED) {
                throw new ForbiddenOperationException("Masanın geçmiş adisyonlarına erişemezsiniz.");
            }
            bills = billRepository.search(BillStatus.OPEN, user.tableId(), null);
        } else if (accessPolicy.hasAllTablesAccess(user)) {
            bills = billRepository.search(status, null, null);
        } else {
            bills = billRepository.search(status, null, user.id());
        }
        return bills.stream().map(BillResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse findById(Long id, CurrentUser user) {
        Bill bill = getBill(id);
        checkReadAccess(user, bill);
        return BillResponse.from(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> findPayments(Long billId, CurrentUser user) {
        checkReadAccess(user, getBill(billId));
        return paymentRepository.findByBillId(billId).stream().map(PaymentResponse::from).toList();
    }

    @Override
    @Transactional
    public PaymentResult addPayment(Long billId, PaymentCreateRequest request, String idempotencyKey,
                                    CurrentUser user) {
        String key = IdempotencyKeys.validate(idempotencyKey);

        // Adisyon satırı kilitlenir: aynı adisyona eşzamanlı ödeme denemeleri (çift tıklama, iki kasa) sıraya girer.
        Bill bill = billRepository.findByIdForUpdate(billId)
                .orElseThrow(() -> ResourceNotFoundException.of("Adisyon", billId));
        accessPolicy.checkTableAccess(user, bill.getTable());

        // Aynı anahtarla alınmış ödeme varsa tekrar tahsil edilmez; önceki makbuz döner.
        Optional<Payment> previous = paymentRepository.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            Payment existing = previous.get();
            if (!existing.getBill().getId().equals(billId)) {
                throw new ConflictException("Bu Idempotency-Key başka bir adisyonun ödemesi için kullanılmış.");
            }
            return new PaymentResult(PaymentReceiptResponse.of(existing, bill), true);
        }

        if (bill.getStatus() != BillStatus.OPEN) {
            throw new ConflictException("Adisyon kapalı; ödeme alınamaz.");
        }
        BigDecimal remaining = bill.getRemainingAmount();
        if (remaining.signum() <= 0) {
            throw new ConflictException("Bu adisyonun ödenecek tutarı kalmadı.");
        }
        if (request.amount().compareTo(remaining) > 0) {
            throw new ConflictException("Ödeme tutarı kalan tutarı aşamaz (kalan: " + remaining + " TL).");
        }

        UserAccount cashier = userAccountRepository.findById(user.id())
                .orElseThrow(() -> ResourceNotFoundException.of("Kullanıcı", user.id()));
        Payment payment = paymentRepository.save(
                new Payment(bill, request.amount(), request.method(), cashier, key, Instant.now(clock)));
        bill.setPaidAmount(bill.getPaidAmount().add(request.amount()));
        Bill updated = billRepository.update(bill);
        return new PaymentResult(PaymentReceiptResponse.of(payment, updated), false);
    }

    @Override
    @Transactional
    public BillResponse patch(Long billId, BillPatchRequest request, CurrentUser user) {
        PatchGuard.ensureIdUnchanged(billId, request.id());
        if (request.status() != BillStatus.CLOSED) {
            throw new BadRequestException("Adisyon yalnızca CLOSED durumuna geçirilerek kapatılabilir.");
        }

        // Kilit sırası masa → adisyon: kapanış anında aynı masaya yeni sipariş eklenmesiyle çakışmaz.
        Long tableId = billRepository.findTableIdByBillId(billId)
                .orElseThrow(() -> ResourceNotFoundException.of("Adisyon", billId));
        RestaurantTable table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> ResourceNotFoundException.of("Masa", tableId));
        accessPolicy.checkTableAccess(user, table);
        Bill bill = billRepository.findByIdForUpdate(billId)
                .orElseThrow(() -> ResourceNotFoundException.of("Adisyon", billId));

        if (bill.getStatus() == BillStatus.CLOSED) {
            throw new ConflictException("Adisyon zaten kapalı.");
        }
        if (bill.getRemainingAmount().signum() > 0) {
            throw new ConflictException("Adisyon tamamen ödenmeden kapatılamaz (kalan: " + bill.getRemainingAmount() + " TL).");
        }

        UserAccount cashier = userAccountRepository.findById(user.id())
                .orElseThrow(() -> ResourceNotFoundException.of("Kullanıcı", user.id()));
        bill.close(cashier, Instant.now(clock));
        return BillResponse.from(billRepository.update(bill));
    }

    private void checkReadAccess(CurrentUser user, Bill bill) {
        accessPolicy.checkTableAccess(user, bill.getTable());
        if (user.isTableBound() && bill.getStatus() != BillStatus.OPEN) {
            throw new ForbiddenOperationException("Masanın geçmiş adisyonlarına erişemezsiniz.");
        }
    }

    private Bill getBill(Long id) {
        return billRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Adisyon", id));
    }
}
