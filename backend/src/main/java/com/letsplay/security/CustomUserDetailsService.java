package com.letsplay.security;

import com.letsplay.model.User;
import com.letsplay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String trimmedIdentifier = identifier != null ? identifier.trim() : "";
        User user = userRepository.findById(trimmedIdentifier)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(trimmedIdentifier)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + trimmedIdentifier)));
        return new CustomUserDetails(user);
    }
}
