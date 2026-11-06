package com.restoran.pos.security;

import com.restoran.pos.entity.Permission;
import com.restoran.pos.entity.Role;
import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * HTTP Basic kimlik doğrulamasında kullanıcıyı, aktiflik bilgisini, rollerini ve yetkilerini veritabanından yükler.
 * Parola karşılaştırmasını Spring Security, Argon2id uyumlu PasswordEncoder ile yapar.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        Long tableId = user.getTable() != null ? user.getTable().getId() : null;

        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), roles, permissions, tableId);
        return new PosUserDetails(currentUser, user.getPasswordHash(), user.isEnabled());
    }
}
