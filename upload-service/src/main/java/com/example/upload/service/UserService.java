package com.example.upload.service;

import com.example.upload.model.User;
import com.example.upload.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public com.example.upload.model.User register(String username, String rawPassword) {
        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("username_taken");
        }
        com.example.upload.model.User u = new com.example.upload.model.User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRoles("ROLE_USER");
        return userRepo.save(u);
    }

    public java.util.Optional<com.example.upload.model.User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.example.upload.model.User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user_not_found"));
        List<org.springframework.security.core.GrantedAuthority> authorities = org.springframework.util.StringUtils.hasText(u.getRoles())
                ? java.util.Arrays.stream(u.getRoles().split(","))
                    .map(String::trim)
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                    .toList()
                : List.of();
        return User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .authorities(authorities)
                .accountLocked(false)
                .disabled(!u.isEnabled())
                .build();
    }
}