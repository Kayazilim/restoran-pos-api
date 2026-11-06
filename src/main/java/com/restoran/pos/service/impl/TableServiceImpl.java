package com.restoran.pos.service.impl;

import com.restoran.pos.dto.table.QrCredentialsResponse;
import com.restoran.pos.dto.table.TableCreateRequest;
import com.restoran.pos.dto.table.TableCreatedResponse;
import com.restoran.pos.dto.table.TablePatchRequest;
import com.restoran.pos.dto.table.TableResponse;
import com.restoran.pos.dto.table.TableUpdateRequest;
import com.restoran.pos.entity.RestaurantTable;
import com.restoran.pos.entity.Role;
import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.exception.BadRequestException;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.repository.BillRepository;
import com.restoran.pos.repository.RestaurantTableRepository;
import com.restoran.pos.repository.RoleRepository;
import com.restoran.pos.repository.UserAccountRepository;
import com.restoran.pos.security.AccessPolicy;
import com.restoran.pos.security.CurrentUser;
import com.restoran.pos.security.SystemRoles;
import com.restoran.pos.service.TableService;
import com.restoran.pos.util.PatchGuard;
import com.restoran.pos.util.Texts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Masa yönetimi. Her masa için "masa-{numara}" adlı, MUSTERI rolünde ve masaya bağlı bir müşteri hesabı tutulur;
 * masadaki QR kod bu hesabın kimlik bilgilerini taşır.
 */
@Service
public class TableServiceImpl implements TableService {

    private static final String CUSTOMER_USERNAME_PREFIX = "masa-";
    private static final int QR_SECRET_BYTES = 18;

    private final RestaurantTableRepository tableRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final BillRepository billRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessPolicy accessPolicy;
    private final SecureRandom secureRandom;
    private final String qrBaseUrl;

    public TableServiceImpl(RestaurantTableRepository tableRepository,
                            UserAccountRepository userAccountRepository,
                            RoleRepository roleRepository,
                            BillRepository billRepository,
                            PasswordEncoder passwordEncoder,
                            AccessPolicy accessPolicy,
                            SecureRandom secureRandom,
                            @Value("${pos.qr.base-url}") String qrBaseUrl) {
        this.tableRepository = tableRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.billRepository = billRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessPolicy = accessPolicy;
        this.secureRandom = secureRandom;
        this.qrBaseUrl = qrBaseUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableResponse> findAll(CurrentUser user) {
        List<RestaurantTable> tables;
        if (user.isTableBound()) {
            tables = tableRepository.findById(user.tableId()).map(List::of).orElse(List.of());
        } else if (accessPolicy.hasAllTablesAccess(user)) {
            tables = tableRepository.findAll();
        } else {
            tables = tableRepository.findByWaiterId(user.id());
        }
        Set<Long> occupiedTableIds = billRepository.findTableIdsWithOpenBill();
        return tables.stream()
                .map(table -> TableResponse.from(table, occupiedTableIds.contains(table.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TableResponse findById(Long id, CurrentUser user) {
        RestaurantTable table = getTable(id);
        accessPolicy.checkTableAccess(user, table);
        return toResponse(table);
    }

    @Override
    @Transactional
    public TableCreatedResponse create(TableCreateRequest request) {
        ensureTableNumberIsUnique(request.tableNumber(), null);
        UserAccount waiter = resolveWaiter(request.waiterId());
        RestaurantTable table = tableRepository.save(new RestaurantTable(request.tableNumber(), request.capacity(),
                Texts.trimToNull(request.location()), waiter, request.active() == null || request.active()));

        String secret = newQrSecret();
        UserAccount customer = createCustomerAccount(table, secret);
        return new TableCreatedResponse(TableResponse.from(table, false),
                QrCredentialsResponse.of(table.getTableNumber(), customer.getUsername(), secret, qrBaseUrl));
    }

    @Override
    @Transactional
    public TableResponse update(Long id, TableUpdateRequest request) {
        RestaurantTable table = getTable(id);
        changeTableNumber(table, request.tableNumber());
        table.setCapacity(request.capacity());
        table.setLocation(Texts.trimToNull(request.location()));
        table.setWaiter(resolveWaiter(request.waiterId()));
        table.setActive(request.active());
        return toResponse(tableRepository.update(table));
    }

    @Override
    @Transactional
    public TableResponse patch(Long id, TablePatchRequest request) {
        PatchGuard.ensureIdUnchanged(id, request.id());
        RestaurantTable table = getTable(id);
        if (request.tableNumber() != null) {
            changeTableNumber(table, request.tableNumber());
        }
        if (request.capacity() != null) {
            table.setCapacity(request.capacity());
        }
        if (request.location() != null) {
            table.setLocation(Texts.trimToNull(request.location()));
        }
        if (request.waiterId() != null) {
            table.setWaiter(resolveWaiter(request.waiterId()));
        }
        if (request.active() != null) {
            table.setActive(request.active());
        }
        return toResponse(tableRepository.update(table));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RestaurantTable table = getTable(id);
        if (billRepository.existsByTableId(id)) {
            throw new ConflictException("Sipariş/adisyon geçmişi olan masa silinemez; kullanımdan kaldırmak için "
                    + "'active' alanını false yapın.");
        }
        userAccountRepository.findByTableId(id).forEach(userAccountRepository::delete);
        tableRepository.delete(table);
    }

    @Override
    @Transactional
    public QrCredentialsResponse regenerateQrCode(Long id) {
        RestaurantTable table = getTable(id);
        String secret = newQrSecret();
        List<UserAccount> customers = userAccountRepository.findByTableId(id);
        UserAccount customer;
        if (customers.isEmpty()) {
            customer = createCustomerAccount(table, secret);
        } else {
            // Eski QR anahtarı bu andan itibaren geçersizdir.
            customer = customers.getFirst();
            customer.setPasswordHash(passwordEncoder.encode(secret));
            userAccountRepository.update(customer);
        }
        return QrCredentialsResponse.of(table.getTableNumber(), customer.getUsername(), secret, qrBaseUrl);
    }

    private UserAccount createCustomerAccount(RestaurantTable table, String secret) {
        String username = customerUsername(table.getTableNumber());
        if (userAccountRepository.existsByUsername(username, null)) {
            throw new ConflictException("'" + username + "' kullanıcı adı zaten kullanımda; masa hesabı oluşturulamadı.");
        }
        Role customerRole = roleRepository.findByName(SystemRoles.MUSTERI)
                .orElseThrow(() -> new IllegalStateException("MUSTERI rolü veritabanında tanımlı değil"));
        UserAccount customer = new UserAccount(username, passwordEncoder.encode(secret),
                "Masa " + table.getTableNumber() + " müşterisi", true);
        customer.replaceRoles(List.of(customerRole));
        customer.setTable(table);
        return userAccountRepository.save(customer);
    }

    private void changeTableNumber(RestaurantTable table, Integer newNumber) {
        if (newNumber.equals(table.getTableNumber())) {
            return;
        }
        ensureTableNumberIsUnique(newNumber, table.getId());
        String oldUsername = customerUsername(table.getTableNumber());
        String newUsername = customerUsername(newNumber);
        for (UserAccount customer : userAccountRepository.findByTableId(table.getId())) {
            if (customer.getUsername().equals(oldUsername)) {
                if (userAccountRepository.existsByUsername(newUsername, customer.getId())) {
                    throw new ConflictException("'" + newUsername + "' kullanıcı adı zaten kullanımda.");
                }
                customer.setUsername(newUsername);
                customer.setFullName("Masa " + newNumber + " müşterisi");
                userAccountRepository.update(customer);
            }
        }
        table.setTableNumber(newNumber);
    }

    private UserAccount resolveWaiter(Long waiterId) {
        if (waiterId == null) {
            return null;
        }
        UserAccount waiter = userAccountRepository.findById(waiterId)
                .orElseThrow(() -> ResourceNotFoundException.of("Garson", waiterId));
        if (!waiter.hasRole(SystemRoles.GARSON)) {
            throw new BadRequestException("Kullanıcı (id=" + waiterId + ") GARSON rolünde değil; masaya atanamaz.");
        }
        if (!waiter.isEnabled()) {
            throw new BadRequestException("Pasif kullanıcı (id=" + waiterId + ") masaya atanamaz.");
        }
        return waiter;
    }

    private void ensureTableNumberIsUnique(Integer tableNumber, Long excludeId) {
        if (tableRepository.existsByTableNumber(tableNumber, excludeId)) {
            throw new ConflictException("Bu numarada bir masa zaten var: " + tableNumber);
        }
    }

    private RestaurantTable getTable(Long id) {
        return tableRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Masa", id));
    }

    private TableResponse toResponse(RestaurantTable table) {
        return TableResponse.from(table, billRepository.findTableIdsWithOpenBill().contains(table.getId()));
    }

    private String newQrSecret() {
        byte[] bytes = new byte[QR_SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String customerUsername(Integer tableNumber) {
        return CUSTOMER_USERNAME_PREFIX + tableNumber;
    }
}
