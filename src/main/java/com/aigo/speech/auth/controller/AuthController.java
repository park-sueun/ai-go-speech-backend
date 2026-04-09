package com.aigo.speech.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/login/success")
    public String loginSuccess() {
        return "OAuth Login Success!";
    }
}
