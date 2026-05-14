package com.opspilot.interfaceadapters.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        return ResponseEntity.ok(Map.of(
                "token", "dev-jwt-token-" + email,
                "role", "ADMIN"
        ));
    }
}
