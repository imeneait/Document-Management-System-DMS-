package com.example.auth.controller;

import com.example.auth.entity.RoleEntity;
import com.example.auth.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles") 
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;  

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleEntity createRole(@RequestBody RoleEntity role) {  
        return roleRepository.save(role);
    }

    @PutMapping("/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public RoleEntity editRole(@RequestBody RoleEntity role) {  
        return roleRepository.save(role);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public List<RoleEntity> listRoles() { 
        return roleRepository.findAll();
    }
   
    // @PostMapping("/create-user")
    // @PreAuthorize("hasRole('ADMIN')")
    // public RoleEntity createRole(@RequestBody RoleEntity role) {  
    //     return roleRepository.save(role);
    


}
