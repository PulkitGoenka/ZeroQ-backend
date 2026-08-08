package com.example.Qpay.Security;

import com.example.Qpay.Entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String phone;
    private final boolean active;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.phone = user.getPhone();
        this.active = Boolean.TRUE.equals(user.getIsActive());
    }

    @Override public String getUsername() { return phone; }
    @Override public String getPassword() { return null; }
    @Override public boolean isEnabled() { return active; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return active; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
