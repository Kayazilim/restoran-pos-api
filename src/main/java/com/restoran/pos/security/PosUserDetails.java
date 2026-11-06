package com.restoran.pos.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security'nin kullandığı kullanıcı modeli. Roller "ROLE_" önekiyle, yetkiler kendi adlarıyla
 * authority olarak eklenir (rol/yetki eşlemesi).
 */
public class PosUserDetails implements UserDetails {

    private final CurrentUser currentUser;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public PosUserDetails(CurrentUser currentUser, String passwordHash, boolean enabled) {
        this.currentUser = currentUser;
        this.passwordHash = passwordHash;
        this.enabled = enabled;

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        currentUser.roles().forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        currentUser.permissions().forEach(permission -> grantedAuthorities.add(new SimpleGrantedAuthority(permission)));
        this.authorities = List.copyOf(grantedAuthorities);
    }

    public CurrentUser currentUser() {
        return currentUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return currentUser.username();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
