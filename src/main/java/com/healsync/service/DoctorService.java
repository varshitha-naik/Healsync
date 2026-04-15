package com.healsync.service;

import com.healsync.dto.DoctorPatientDTO;
import com.healsync.entity.DoctorAvailability;
import com.healsync.entity.PatientProfile;
import com.healsync.entity.User;
import com.healsync.dto.AdminDoctorDTO;
import com.healsync.dto.UpdateDoctorRequest;
import com.healsync.entity.Clinic;
import com.healsync.entity.DoctorProfile;
import com.healsync.enums.UserStatus;
import com.healsync.repository.AppointmentRepository;
import com.healsync.repository.ClinicRepository;
import com.healsync.repository.DoctorAvailabilityRepository;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ClinicRepository clinicRepository;

    public List<AdminDoctorDTO> getAllDoctorsAdmin() {
        List<AdminDoctorDTO> result = new ArrayList<>();
        List<DoctorProfile> profiles = doctorProfileRepository.findAll();
        for (DoctorProfile profile : profiles) {
            Optional<User> userOpt = userRepository.findById(profile.getUserId());
            if (userOpt.isEmpty())
                continue;

            User user = userOpt.get();
            String clinicName = clinicRepository.findById(profile.getClinicId())
                    .map(Clinic::getName).orElse("Unknown");

            result.add(AdminDoctorDTO.builder()
                    .id(user.getId())
                    .fullName(profile.getFullName())
                    .specialization(profile.getSpecialization())
                    .email(user.getEmail())
                    .experienceYears(profile.getExperienceYears())
                    .clinicName(clinicName)
                    .bio(profile.getBio())
                    .status(user.getStatus().name())
                    .build());
        }
        return result;
    }

    public void updateDoctorAdmin(Long userId, UpdateDoctorRequest request) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        if (request.getFullName() != null)
            profile.setFullName(request.getFullName());
        if (request.getSpecialization() != null)
            profile.setSpecialization(request.getSpecialization());
        if (request.getExperienceYears() != null)
            profile.setExperienceYears(request.getExperienceYears());
        if (request.getBio() != null)
            profile.setBio(request.getBio());

        doctorProfileRepository.save(profile);
    }

    public void deleteDoctorAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    public List<DoctorPatientDTO> getPatientsForDoctor(Long doctorId) {
        // doctorId passed from controller is likely User ID.
        // Appointment table stores doctorID as User ID (based on earlier findings).

        List<Long> patientProfileIds = appointmentRepository.findDistinctPatientIdsByDoctorId(doctorId);
        List<DoctorPatientDTO> result = new ArrayList<>();

        for (Long profileId : patientProfileIds) {
            Optional<PatientProfile> profileOpt = patientProfileRepository.findById(profileId);
            if (profileOpt.isEmpty())
                continue;

            PatientProfile profile = profileOpt.get();
            Optional<User> userOpt = userRepository.findById(profile.getUserId());

            DoctorPatientDTO dto = new DoctorPatientDTO();
            dto.setPatientId(profile.getId()); // Profile ID
            dto.setUserId(profile.getUserId()); // User ID
            dto.setPatientName(profile.getFullName());
            dto.setEmail(userOpt.map(User::getEmail).orElse(""));

            appointmentRepository.findTopByPatientIdAndDoctorIdOrderByStartDateTimeDesc(profileId, doctorId)
                    .ifPresent(app -> dto.setLastVisitDate(app.getStartDateTime()));

            result.add(dto);
        }
        return result;
    }

    public List<DoctorAvailability> getAvailability(Long doctorId) {
        return doctorAvailabilityRepository.findByDoctorId(doctorId);
    }

    public DoctorAvailability addAvailability(DoctorAvailability availability) {
        return doctorAvailabilityRepository.save(availability);
    }

    public void deleteAvailability(Long id) {
        doctorAvailabilityRepository.deleteById(id);
    }
}
