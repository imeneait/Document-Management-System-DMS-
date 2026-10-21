package com.example.auth.model;

import lombok.Data;


@Data
public class User {
    private String email;
    private String password;
    private String role;
}