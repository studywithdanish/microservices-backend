package com.danish.blog.identity.security;

import com.danish.blog.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class IdentityUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public IdentityUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User account was not found"));
    }
}
