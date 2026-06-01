package com.fpt.swp.security;

import com.fpt.swp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDetails loadUserByMail(String mail) throws UsernameNotFoundException {
        var user = userRepository.findByMail(mail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + mail));

        if (user.getStatus() == com.fpt.swp.model.UserStatus.INACTIVE) {
            throw new UsernameNotFoundException("Account is inactive: " + mail);
        }
        if (user.getStatus() == com.fpt.swp.model.UserStatus.SUSPENDED) {
            throw new UsernameNotFoundException("Account is suspended: " + mail);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getMail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return loadUserByMail(identifier);
    }
}
