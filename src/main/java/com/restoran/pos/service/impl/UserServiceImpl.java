package com.restoran.pos.service.impl;

import com.restoran.pos.dto.user.PasswordChangeRequest;
import com.restoran.pos.dto.user.UserCreateRequest;
import com.restoran.pos.dto.user.UserPatchRequest;
import com.restoran.pos.dto.user.UserResponse;
import com.restoran.pos.dto.user.UserUpdateRequest;
import com.restoran.pos.entity.Role;
import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.exception.BadRequestException;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.repository.BillRepository;
import com.restoran.pos.repository.OrderRepository;
import com.restoran.pos.repository.PaymentRepository;
import com.restoran.pos.repository.RestaurantTableRepository;
import com.restoran.pos.repository.RoleRepository;
import com.restoran.pos.repository.UserAccountRepository;
import com.restoran.pos.security.CurrentUser;
import com.restoran.pos.security.SystemRoles;
import com.restoran.pos.service.UserService;
import com.restoran.pos.util.PatchGuard;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Personel hesapları ve rol atamaları (yalnızca yönetici). Masa (QR) hesapları /api/tables üzerinden yönetilir.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final RestaurantTableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserAccountRepository userAccountRepository,
                           RoleRepository roleRepository,
                           RestaurantTableRepository tableRepository,
                           OrderRepository orderRepository,
                           PaymentRepository paymentRepository,
                           BillRepository billRepository,
                           PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll(String role, Boolean enabled) {
        return userAccountRepository.search(role, enabled).stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(getUser(id));
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        ensureUsernameIsUnique(username, null);
        UserAccount user = new UserAccount(username, passwordEncoder.encode(request.password()),
                request.fullName().trim(), request.enabled() == null || request.enabled());
        user.replaceRoles(resolveRoles(request.roles()));
        return UserResponse.from(userAccountRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, CurrentUser actor) {
        UserAccount user = getUser(id);
        if (user.getTable() != null) {
            throw new ConflictException("Masa (QR) hesapları /api/tables uç noktalarından yönetilir; "
                    + "burada yalnızca PATCH ile aktiflik değiştirilebilir.");
        }
        List<Role> roles = resolveRoles(request.roles());
        guardSelfModification(actor, user, request.enabled(), roles);

        String username = request.username().trim();
        ensureUsernameIsUnique(username, id);
        user.setUsername(username);
        user.setFullName(request.fullName().trim());
        user.setEnabled(request.enabled());
        user.replaceRoles(roles);
        return UserResponse.from(userAccountRepository.update(user));
    }

    @Override
    @Transactional
    public UserResponse patch(Long id, UserPatchRequest request, CurrentUser actor) {
        PatchGuard.ensureIdUnchanged(id, request.id());
        UserAccount user = getUser(id);
        if (user.getTable() != null
                && (request.username() != null || request.fullName() != null || request.roles() != null)) {
            throw new ConflictException("Masa (QR) hesaplarında yalnızca 'enabled' alanı değiştirilebilir.");
        }
        List<Role> roles = request.roles() != null ? resolveRoles(request.roles()) : null;
        guardSelfModification(actor, user, request.enabled(), roles);

        if (request.username() != null) {
            String username = request.username().trim();
            ensureUsernameIsUnique(username, id);
            user.setUsername(username);
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (roles != null) {
            user.replaceRoles(roles);
        }
        return UserResponse.from(userAccountRepository.update(user));
    }

    @Override
    @Transactional
    public void changePassword(Long id, PasswordChangeRequest request) {
        UserAccount user = getUser(id);
        if (user.getTable() != null) {
            throw new ConflictException("Masa hesaplarının anahtarı POST /api/tables/{id}/qr-code ile yenilenir.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.update(user);
    }

    @Override
    @Transactional
    public void delete(Long id, CurrentUser actor) {
        if (id.equals(actor.id())) {
            throw new ConflictException("Kendi hesabınızı silemezsiniz.");
        }
        UserAccount user = getUser(id);
        if (user.getTable() != null) {
            throw new ConflictException("Masa (QR) hesapları, masa silindiğinde otomatik silinir.");
        }
        if (tableRepository.countByWaiterId(id) > 0) {
            throw new ConflictException("Bu garsona atanmış masalar var; önce masaları başka bir garsona atayın.");
        }
        if (orderRepository.countByCreatedById(id) > 0 || paymentRepository.countByReceivedById(id) > 0
                || billRepository.countByClosedById(id) > 0) {
            throw new ConflictException("İşlem geçmişi olan kullanıcı silinemez; bunun yerine hesabı pasif hale "
                    + "getirin (enabled=false).");
        }
        userAccountRepository.delete(user);
    }

    private List<Role> resolveRoles(Set<String> roleNames) {
        Set<String> normalized = roleNames.stream()
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (normalized.contains(SystemRoles.MUSTERI)) {
            throw new BadRequestException("MUSTERI rolü yalnızca masa hesaplarına atanır; masa oluşturulurken otomatik verilir.");
        }
        List<Role> roles = roleRepository.findByNames(normalized);
        if (roles.size() != normalized.size()) {
            Set<String> found = roles.stream().map(Role::getName).collect(Collectors.toSet());
            List<String> unknown = normalized.stream().filter(name -> !found.contains(name)).sorted().toList();
            throw new BadRequestException("Tanımsız rol: " + unknown);
        }
        return roles;
    }

    /** Yöneticinin kendi hesabını pasifleştirerek veya yönetici rolünü kaldırarak sistemi kilitlemesini engeller. */
    private static void guardSelfModification(CurrentUser actor, UserAccount user, Boolean enabled, List<Role> roles) {
        if (!user.getId().equals(actor.id())) {
            return;
        }
        if (Boolean.FALSE.equals(enabled)) {
            throw new ConflictException("Kendi hesabınızı pasifleştiremezsiniz.");
        }
        if (roles != null && roles.stream().noneMatch(role -> role.getName().equals(SystemRoles.YONETICI))) {
            throw new ConflictException("Kendi hesabınızdan YONETICI rolünü kaldıramazsınız.");
        }
    }

    private void ensureUsernameIsUnique(String username, Long excludeId) {
        if (userAccountRepository.existsByUsername(username, excludeId)) {
            throw new ConflictException("Bu kullanıcı adı zaten kullanımda: " + username);
        }
    }

    private UserAccount getUser(Long id) {
        return userAccountRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Kullanıcı", id));
    }
}
