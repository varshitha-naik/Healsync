package com.healsync.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/create-doctor")
    public String createDoctor() {
        return "admin/create-doctor";
    }

    @GetMapping("/admin/appointments")
    public String adminAppointments() {
        return "admin/appointments";
    }

    @GetMapping("/admin/audit-logs")
    public String auditLogs() {
        return "admin/audit-logs";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/admin/tenants")
    public String adminTenants() {
        return "admin/tenants";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/doctor/dashboard")
    public String doctorDashboard() {
        return "doctor/dashboard";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard() {
        return "patient/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments() {
        return "appointments";
    }
}
