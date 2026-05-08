package com.gymplanner.auth;

import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return GymPrincipal.from(user);
    }

    public record GymPrincipal(
            Long id,
            String email,
            String password,
            String fullName,
            UserRole role,
            Long gymId,
            boolean active) implements UserDetails {

        static GymPrincipal from(User user) {
            return new GymPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getFullName(),
                    user.getRole(),
                    user.getGym().getId(),
                    user.isActive());
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return email;
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
            return active;
        }
    }
}
