package com.example.auth.config;

import com.example.auth.entity.RoleEntity;
import com.example.auth.entity.UserEntity;
import com.example.auth.repository.RoleRepository;
import com.example.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // Create or fetch ROLE_ADMIN
            RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        RoleEntity role = new RoleEntity();
                        role.setName("ROLE_ADMIN");
                        return roleRepository.save(role);
                    });

            // Create or fetch ROLE_USER
            RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        RoleEntity role = new RoleEntity();
                        role.setName("ROLE_USER");
                        return roleRepository.save(role);
                    });

            UserEntity admin = new UserEntity();
            admin.setEmail("soumia@gmail.com");
            admin.setPassword("soumia"); 
            admin.setSalt("randomSalt");
            admin.setRole(adminRole);
            userRepository.save(admin);

            
            UserEntity user = new UserEntity();
            user.setEmail("user@example.com");
            user.setPassword("user123"); 
            user.setSalt("randomSalt");
            user.setRole(userRole);
            userRepository.save(user);

        
             UserEntity user2 = new UserEntity();
             user2.setEmail("user2@example.com"); 
             user2.setPassword("user123"); 
             user2.setSalt("randomSalt");
             user2.setRole(userRole);
             userRepository.save(user2);
        }
    }
}
