package com.healsync.controller;

import com.healsync.dto.AvailabilityRequest;
import com.healsync.dto.DoctorPatientDTO;
import com.healsync.entity.DoctorAvailability;
import com.healsync.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/{doctorId}/patients")
    public ResponseEntity<?> getPatients(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getPatientsForDoctor(doctorId));
    }

    @GetMapping("/availability/{doctorId}")
    public ResponseEntity<?> getAvailability(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getAvailability(doctorId));
    }

    @PostMapping("/availability")
    public ResponseEntity<?> addAvailability(@RequestBody AvailabilityRequest req) {
        for (String day : req.getDays()) {
            DoctorAvailability slot = new DoctorAvailability();
            slot.setDoctorId(req.getDoctorId());
            slot.setDayOfWeek(day);
            slot.setStartTime(req.getStartTime());
            slot.setEndTime(req.getEndTime());
            doctorService.addAvailability(slot);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<?> deleteAvailability(@PathVariable Long id) {
        doctorService.deleteAvailability(id);
        return ResponseEntity.ok().build();
    }
}
