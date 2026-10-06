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

    public boolean isSupervisor(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getJobTittle() != null && user.getJobTittle().trim().equalsIgnoreCase("SUPERVISOR");
    }

    // ACHADO C2 (crítico) da auditoria de 05/10/2026: isAdmin() era um
    // deny-list ("não é RH, nem FINANCEIRO, nem SUPERVISOR"), então qualquer
    // jobTittle nulo, vazio ou desconhecido virava admin por acidente.
    // Agora é um allow-list explícito: só é admin quem tem jobTittle "ADMIN".
    public boolean isAdmin(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getJobTittle() != null && user.getJobTittle().trim().equalsIgnoreCase("ADMIN");
    }

    // Usado como checagem mínima (defesa em profundidade) em endpoints de
    // leitura que precisam estar abertos a qualquer papel reconhecido do
    // sistema, mas não a um jobTittle desconhecido/corrompido — ver
    // PromoterController (achado C5).
    public boolean isKnownRole(Authentication authentication) {
        return isRh(authentication) || isFinance(authentication)
                || isSupervisor(authentication) || isAdmin(authentication);
    }
}
