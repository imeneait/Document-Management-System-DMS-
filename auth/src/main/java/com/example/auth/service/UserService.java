package com.example.auth.service;

import com.example.auth.entity.RoleEntity;
import com.example.auth.entity.UserEntity;
import com.example.auth.repository.RoleRepository;
import com.example.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;


    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    
    public RoleEntity getRoleByName(String roleName) {
        return roleRepository.findByName(roleName).orElse(null);
    }
}