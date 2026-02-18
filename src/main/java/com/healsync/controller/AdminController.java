package com.healsync.controller;

import com.healsync.service.AuthService;
import com.healsync.dto.CreateDoctorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @PostMapping("/doctors")
    public ResponseEntity<?> createDoctor(@RequestBody CreateDoctorRequest request) {
        authService.createDoctor(request);
        return ResponseEntity.ok().body("{\"message\": \"Doctor created successfully\"}");
    }
}
