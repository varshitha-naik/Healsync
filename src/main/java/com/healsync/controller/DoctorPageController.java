package com.healsync.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/doctor")
public class DoctorPageController {

    @GetMapping("/patients")
    public String doctorPatientsPage() {
        return "doctor/patients";
    }

    @GetMapping("/availability")
    public String doctorAvailabilityPage() {
        return "doctor/availability";
    }
}
