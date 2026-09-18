package com.atpromo.systematpromo.security;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AccessControl {

    private final UserRepository userRepository;

    public AccessControl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User currentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    public boolean isRh(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getJobTittle() != null && user.getJobTittle().trim().equalsIgnoreCase("RH");
    }

    public boolean isFinance(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getJobTittle() != null && user.getJobTittle().trim().equalsIgnoreCase("FINANCEIRO");
    }

    public boolean isAdmin(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && !isRh(authentication) && !isFinance(authentication);
    }
}