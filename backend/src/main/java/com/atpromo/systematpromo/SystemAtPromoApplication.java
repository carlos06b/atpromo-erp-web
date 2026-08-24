package com.atpromo.systematpromo;

import com.atpromo.systematpromo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SystemAtPromoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemAtPromoApplication.class, args);
    }

    @Bean
    CommandLineRunner testConnection(UserRepository userRepository) {
        return args -> {
            long total = userRepository.count();
            System.out.println("Conexão OK! Total de usuários no banco: " + total);
        };
    }
}