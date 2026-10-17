package com.newtonite.ops.security;

import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    private final UserRepository userRepository;

    public AuthHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Unauthenticated request");
        }
        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUser();
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found: " + email));
    }
}
