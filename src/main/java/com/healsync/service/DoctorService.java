package com.healsync.service;

import com.healsync.dto.DoctorPatientDTO;
import com.healsync.dto.AdminDashboardSummaryDTO;
import com.healsync.dto.AdminPatientAppointmentDTO;
import com.healsync.dto.AdminPatientsByDoctorDTO;
import com.healsync.dto.DoctorDashboardSummaryDTO;
import com.healsync.entity.DoctorAvailability;
import com.healsync.entity.DoctorLeave;
import com.healsync.entity.Appointment;
import com.healsync.entity.PatientProfile;
import com.healsync.entity.User;
import com.healsync.dto.AdminDoctorDTO;
import com.healsync.dto.DoctorSummaryDTO;
import com.healsync.dto.UpdateDoctorRequest;
import com.healsync.entity.Clinic;
import com.healsync.entity.DoctorProfile;
import com.healsync.enums.UserRole;
import com.healsync.enums.UserStatus;
import com.healsync.enums.AppointmentStatus;
import com.healsync.repository.AppointmentRepository;
import com.healsync.repository.ClinicRepository;
import com.healsync.repository.DoctorAvailabilityRepository;
import com.healsync.repository.DoctorLeaveRepository;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.repository.PrescriptionItemRepository;
import com.healsync.repository.PrescriptionRepository;
import com.healsync.repository.MedicalReportRepository;
import com.healsync.repository.ReportAttachmentRepository;
import com.healsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ClinicRepository clinicRepository;
    private final FileStorageService fileStorageService;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final ReportAttachmentRepository reportAttachmentRepository;

    public List<AdminDoctorDTO> getAllDoctorsAdmin() {
        List<AdminDoctorDTO> result = new ArrayList<>();
        List<Appointment> appointments = appointmentRepository.findAll();
        Map<Long, List<Appointment>> appointmentsByDoctorProfileId = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getDoctorId));
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        List<DoctorProfile> profiles = doctorProfileRepository.findAll();
        for (DoctorProfile profile : profiles) {
            Optional<User> userOpt = userRepository.findById(profile.getUserId());
            if (userOpt.isEmpty())
                continue;

            User user = userOpt.get();
            String clinicName = clinicRepository.findById(profile.getClinicId())
                    .map(Clinic::getName).orElse("Unknown");
            List<Appointment> doctorAppointments = appointmentsByDoctorProfileId.getOrDefault(profile.getId(), List.of());
            int patientCount = (int) doctorAppointments.stream()
                    .map(Appointment::getPatientId)
                    .distinct()
                    .count();
            LocalDateTime lastAppointment = doctorAppointments.stream()
                    .map(Appointment::getStartDateTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            List<DoctorAvailability> todaysAvailability = doctorAvailabilityRepository.findByDoctorId(profile.getUserId()).stream()
                    .filter(slot -> slot.getDayOfWeek() != null && slot.getDayOfWeek().equalsIgnoreCase(today.name()))
                    .sorted(Comparator.comparing(DoctorAvailability::getStartTime))
                    .toList();

            String availabilityStatus = determineAvailabilityStatus(todaysAvailability, doctorAppointments);
            String availabilitySummary = buildAvailabilitySummary(todaysAvailability, availabilityStatus);

            result.add(buildAdminDoctorDTO(profile, user, clinicName, patientCount, lastAppointment, availabilityStatus, availabilitySummary));
        }
        return result;
    }

    public AdminDashboardSummaryDTO getAdminDashboardSummary() {
        List<DoctorProfile> doctorProfiles = doctorProfileRepository.findAll();
        List<Appointment> appointments = appointmentRepository.findAll();
        LocalDate today = LocalDate.now();

        long appointmentsToday = appointments.stream()
                .filter(app -> app.getStartDateTime() != null && app.getStartDateTime().toLocalDate().isEqual(today))
                .count();

        long completedAppointmentsToday = appointments.stream()
                .filter(app -> app.getStartDateTime() != null && app.getStartDateTime().toLocalDate().isEqual(today))
                .filter(app -> app.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        long doctorsAvailableToday = doctorProfiles.stream()
                .filter(profile -> userRepository.findById(profile.getUserId())
                        .map(user -> user.getStatus() == UserStatus.ACTIVE)
                        .orElse(false))
                .filter(profile -> {
                    List<Appointment> doctorAppointments = appointments.stream()
                            .filter(app -> profile.getId().equals(app.getDoctorId()))
                            .toList();
                    List<DoctorAvailability> todaysAvailability = doctorAvailabilityRepository.findByDoctorId(profile.getUserId()).stream()
                            .filter(slot -> slot.getDayOfWeek() != null && slot.getDayOfWeek().equalsIgnoreCase(dayOfWeek.name()))
                            .toList();
                    return "AVAILABLE_TODAY".equals(determineAvailabilityStatus(todaysAvailability, doctorAppointments));
                })
                .count();

        return AdminDashboardSummaryDTO.builder()
                .totalDoctors(doctorProfiles.size())
                .totalPatients(patientProfileRepository.count())
                .appointmentsToday(appointmentsToday)
                .completedAppointmentsToday(completedAppointmentsToday)
                .doctorsAvailableToday(doctorsAvailableToday)
                .build();
    }

    public List<DoctorSummaryDTO> getAllDoctorsForPatient() {
        List<User> doctors = userRepository.findByRole(UserRole.DOCTOR);
        List<DoctorSummaryDTO> dtos = new ArrayList<>();

        for (User user : doctors) {
            Optional<DoctorProfile> profileOpt = doctorProfileRepository.findByUserId(user.getId());
            dtos.add(buildDoctorSummaryDTO(user, profileOpt.orElse(null)));
        }

        return dtos;
    }

    public List<AdminPatientsByDoctorDTO> getPatientsGroupedByDoctorForAdmin() {
        List<DoctorProfile> doctorProfiles = doctorProfileRepository.findAll().stream()
                .sorted(Comparator.comparing(DoctorProfile::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<Long, User> usersById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Appointment> appointments = appointmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Appointment::getStartDateTime).reversed())
                .toList();

        Map<Long, PatientProfile> patientProfilesById = patientProfileRepository.findAll().stream()
                .collect(Collectors.toMap(PatientProfile::getId, profile -> profile));

        List<AdminPatientsByDoctorDTO> grouped = new ArrayList<>();

        for (DoctorProfile doctorProfile : doctorProfiles) {
            User doctorUser = usersById.get(doctorProfile.getUserId());
            Map<Long, AdminPatientAppointmentDTO> latestPatientsByProfileId = new LinkedHashMap<>();

            for (Appointment appointment : appointments) {
                if (!doctorProfile.getId().equals(appointment.getDoctorId())) {
                    continue;
                }

                Long patientProfileId = appointment.getPatientId();
                if (latestPatientsByProfileId.containsKey(patientProfileId)) {
                    continue;
                }

                PatientProfile patientProfile = patientProfilesById.get(patientProfileId);
                if (patientProfile == null) {
                    continue;
                }

                User patientUser = usersById.get(patientProfile.getUserId());
                latestPatientsByProfileId.put(patientProfileId, AdminPatientAppointmentDTO.builder()
                        .patientId(patientProfile.getUserId())
                        .patientName(patientProfile.getFullName())
                        .email(patientUser != null ? patientUser.getEmail() : "")
                        .lastAppointmentDate(appointment.getStartDateTime())
                        .appointmentStatus(appointment.getStatus() != null ? appointment.getStatus().name() : null)
                        .build());
            }

            grouped.add(AdminPatientsByDoctorDTO.builder()
                    .doctorId(doctorProfile.getUserId())
                    .doctorName(doctorProfile.getFullName())
                    .doctorEmail(doctorUser != null ? doctorUser.getEmail() : "")
                    .specialization(doctorProfile.getSpecialization())
                    .profilePhotoUrl(resolveDoctorPhotoUrl(doctorProfile))
                    .patients(new ArrayList<>(latestPatientsByProfileId.values()))
                    .build());
        }

        return grouped;
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
        if (request.getClinicId() != null)
            profile.setClinicId(request.getClinicId());

        doctorProfileRepository.save(profile);
    }

    public Map<String, Object> getDoctorProfileForAdmin(Long doctorUserId) {
        User user = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Doctor user not found"));
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        String clinicName = clinicRepository.findById(profile.getClinicId())
                .map(Clinic::getName).orElse("Unknown");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("doctorId", doctorUserId);
        payload.put("profileId", profile.getId());
        payload.put("fullName", profile.getFullName());
        payload.put("specialization", profile.getSpecialization());
        payload.put("email", user.getEmail());
        payload.put("licenseNumber", profile.getLicenseNumber());
        payload.put("experienceYears", profile.getExperienceYears());
        payload.put("bio", profile.getBio());
        payload.put("clinicId", profile.getClinicId());
        payload.put("clinicName", clinicName);
        payload.put("profilePhotoUrl", resolveDoctorPhotoUrl(profile));
        return payload;
    }

    public Map<String, Object> updateDoctorProfileForAdmin(Long doctorUserId, UpdateDoctorRequest request) {
        updateDoctorAdmin(doctorUserId, request);
        return getDoctorProfileForAdmin(doctorUserId);
    }

    public void deleteDoctorAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    public void updateDoctorStatusAdmin(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status);
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

    public List<DoctorLeave> getLeaves(Long doctorId) {
        return doctorLeaveRepository.findByDoctorIdOrderByFromDateAsc(doctorId);
    }

    public DoctorAvailability addAvailability(DoctorAvailability availability) {
        if (availability == null || availability.getDoctorId() == null || availability.getDayOfWeek() == null
                || availability.getStartTime() == null || availability.getEndTime() == null) {
            throw new RuntimeException("Invalid availability payload");
        }
        boolean overlapExists = doctorAvailabilityRepository.existsByDoctorIdAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                availability.getDoctorId(),
                availability.getDayOfWeek(),
                availability.getEndTime(),
                availability.getStartTime());
        if (overlapExists) {
            throw new RuntimeException("Availability already exists for this day and time.");
        }
        return doctorAvailabilityRepository.save(availability);
    }

    public DoctorLeave addLeave(Long doctorId, LocalDate fromDate, LocalDate toDate, String reason) {
        if (fromDate == null || toDate == null) {
            throw new RuntimeException("From date and to date are required.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new RuntimeException("To date must be on or after from date.");
        }
        boolean overlapLeaveExists = doctorLeaveRepository.existsByDoctorIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                doctorId, toDate, fromDate);
        if (overlapLeaveExists) {
            throw new RuntimeException("Leave has already been applied for the selected dates.");
        }

        DoctorLeave leave = new DoctorLeave();
        leave.setDoctorId(doctorId);
        leave.setFromDate(fromDate);
        leave.setToDate(toDate);
        leave.setReason(reason);
        return doctorLeaveRepository.save(leave);
    }

    public void deleteAvailability(Long id) {
        doctorAvailabilityRepository.deleteById(id);
    }

    public void deleteAvailabilityForDoctor(Long id, Long doctorUserId) {
        DoctorAvailability slot = doctorAvailabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Availability slot not found"));
        if (slot.getDoctorId() == null || !slot.getDoctorId().equals(doctorUserId)) {
            throw new RuntimeException("Unauthorized: availability slot does not belong to this doctor");
        }
        doctorAvailabilityRepository.deleteById(id);
    }

    public void deleteLeave(Long id) {
        doctorLeaveRepository.deleteById(id);
    }

    public void deleteLeaveForDoctor(Long id, Long doctorUserId) {
        DoctorLeave leave = doctorLeaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave entry not found"));
        if (leave.getDoctorId() == null || !leave.getDoctorId().equals(doctorUserId)) {
            throw new RuntimeException("Unauthorized: leave entry does not belong to this doctor");
        }
        doctorLeaveRepository.deleteById(id);
    }

    public boolean isDoctorOnLeave(Long doctorId, LocalDate date) {
        return doctorLeaveRepository.existsByDoctorIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(doctorId, date, date);
    }

    public DoctorDashboardSummaryDTO getDoctorDashboardSummary(Long doctorUserId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        List<Appointment> appointments = appointmentRepository.findByDoctorId(profile.getId());
        long todaysAppointments = appointments.stream()
                .filter(a -> a.getStartDateTime() != null && a.getStartDateTime().toLocalDate().isEqual(today))
                .count();
        long pendingAppointments = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.REQUESTED)
                .count();
        long completedToday = appointments.stream()
                .filter(a -> a.getStartDateTime() != null && a.getStartDateTime().toLocalDate().isEqual(today))
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();
        long upcomingAppointments = appointments.stream()
                .filter(a -> a.getStartDateTime() != null && a.getStartDateTime().isAfter(now))
                .filter(a -> a.getStatus() == AppointmentStatus.REQUESTED || a.getStatus() == AppointmentStatus.CONFIRMED)
                .count();

        Appointment nextAppointment = appointments.stream()
                .filter(a -> a.getStartDateTime() != null && a.getStartDateTime().isAfter(now))
                .filter(a -> a.getStatus() == AppointmentStatus.REQUESTED || a.getStatus() == AppointmentStatus.CONFIRMED)
                .min(Comparator.comparing(Appointment::getStartDateTime))
                .orElse(null);

        String nextAppointmentSummary = "No upcoming appointments today.";
        if (nextAppointment != null) {
            String patientName = patientProfileRepository.findById(nextAppointment.getPatientId())
                    .map(PatientProfile::getFullName)
                    .orElse("Patient");
            nextAppointmentSummary = "Next: " + patientName + " at " + nextAppointment.getStartDateTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
        }

        List<String> missingDates = new ArrayList<>();
        List<DoctorAvailability> weeklyAvailability = doctorAvailabilityRepository.findByDoctorId(doctorUserId);
        for (int i = 0; i < 3; i++) {
            LocalDate date = today.plusDays(i);
            boolean onLeave = isDoctorOnLeave(doctorUserId, date);
            boolean hasAvailability = weeklyAvailability.stream()
                    .anyMatch(slot -> slot.getDayOfWeek() != null && slot.getDayOfWeek().equalsIgnoreCase(date.getDayOfWeek().name()));
            if (!onLeave && !hasAvailability) {
                missingDates.add(date.toString());
            }
        }

        return DoctorDashboardSummaryDTO.builder()
                .todaysAppointments(todaysAppointments)
                .pendingAppointments(pendingAppointments)
                .completedToday(completedToday)
                .upcomingAppointments(upcomingAppointments)
                .missingAvailabilityNext3Days(!missingDates.isEmpty())
                .missingAvailabilityDates(missingDates)
                .nextAppointmentSummary(nextAppointmentSummary)
                .build();
    }

    public Map<String, Object> getPatientHistoryForDoctor(Long doctorUserId, Long patientProfileId) {
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        PatientProfile patientProfile = patientProfileRepository.findById(patientProfileId)
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
        User patientUser = userRepository.findById(patientProfile.getUserId())
                .orElse(null);

        List<Appointment> allPatientAppointments = appointmentRepository.findByPatientId(patientProfileId).stream()
                .filter(a -> a.getDoctorId() != null && a.getDoctorId().equals(doctorProfile.getId()))
                .sorted(Comparator.comparing(Appointment::getStartDateTime).reversed())
                .toList();

        LocalDateTime now = LocalDateTime.now();
        List<Appointment> pastAppointments = allPatientAppointments.stream()
                .filter(a -> a.getStartDateTime() != null && a.getStartDateTime().isBefore(now))
                .toList();
        List<Appointment> completedAppointments = pastAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        List<Map<String, Object>> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientProfileId).stream()
                .filter(p -> p.getDoctorId() != null && p.getDoctorId().equals(doctorProfile.getId()))
                .map(p -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", p.getId());
                    row.put("notes", p.getNotes());
                    row.put("createdAt", p.getCreatedAt());
                    row.put("items", prescriptionItemRepository.findByPrescriptionId(p.getId()));
                    return row;
                })
                .toList();

        List<Map<String, Object>> reports = medicalReportRepository.findByPatientIdOrderByCreatedAtDesc(patientProfileId).stream()
                .filter(r -> r.getDoctorId() != null && r.getDoctorId().equals(doctorProfile.getId()))
                .map(r -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", r.getId());
                    row.put("title", r.getTitle());
                    row.put("description", r.getDescription());
                    row.put("reportType", r.getReportType());
                    row.put("createdAt", r.getCreatedAt());
                    row.put("attachments", reportAttachmentRepository.findByReportId(r.getId()));
                    return row;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("patient", Map.of(
                "patientProfileId", patientProfile.getId(),
                "patientUserId", patientProfile.getUserId(),
                "fullName", patientProfile.getFullName(),
                "email", patientUser != null ? patientUser.getEmail() : "",
                "dob", patientProfile.getDob(),
                "gender", patientProfile.getGender(),
                "bloodGroup", patientProfile.getBloodGroup() != null ? patientProfile.getBloodGroup() : "",
                "phone", patientProfile.getPhone() != null ? patientProfile.getPhone() : ""));
        response.put("pastAppointments", pastAppointments);
        response.put("completedAppointments", completedAppointments);
        response.put("prescriptions", prescriptions);
        response.put("reports", reports);
        return response;
    }

    public String resolveDoctorPhotoUrl(DoctorProfile profile) {
        if (profile == null) {
            return null;
        }

        return fileStorageService.withCacheBusting(profile.getProfilePhotoUrl(), profile.getUpdatedAt());
    }

    private AdminDoctorDTO buildAdminDoctorDTO(
            DoctorProfile profile,
            User user,
            String clinicName,
            Integer patientCount,
            LocalDateTime lastAppointment,
            String availabilityStatus,
            String availabilitySummary) {
        return AdminDoctorDTO.builder()
                .id(user.getId())
                .fullName(profile.getFullName())
                .specialization(profile.getSpecialization())
                .email(user != null ? user.getEmail() : null)
                .profilePhotoUrl(resolveDoctorPhotoUrl(profile))
                .experienceYears(profile.getExperienceYears())
                .clinicName(clinicName)
                .bio(profile.getBio())
                .status(user != null && user.getStatus() != null ? user.getStatus().name() : null)
                .active(user != null && user.getStatus() == UserStatus.ACTIVE)
                .patientCount(patientCount)
                .lastAppointment(lastAppointment)
                .availabilityStatus(availabilityStatus)
                .availabilitySummary(availabilitySummary)
                .build();
    }

    private String determineAvailabilityStatus(List<DoctorAvailability> todaysAvailability, List<Appointment> doctorAppointments) {
        if (todaysAvailability == null || todaysAvailability.isEmpty()) {
            return "NOT_SCHEDULED";
        }

        long todaysBookingCount = doctorAppointments.stream()
                .filter(app -> app.getStartDateTime() != null && app.getStartDateTime().toLocalDate().isEqual(LocalDate.now()))
                .filter(app -> app.getStatus() == AppointmentStatus.REQUESTED || app.getStatus() == AppointmentStatus.CONFIRMED)
                .count();

        return todaysBookingCount >= todaysAvailability.size() ? "FULLY_BOOKED" : "AVAILABLE_TODAY";
    }

    private String buildAvailabilitySummary(List<DoctorAvailability> todaysAvailability, String availabilityStatus) {
        if (todaysAvailability == null || todaysAvailability.isEmpty()) {
            return "Not Scheduled";
        }

        DoctorAvailability first = todaysAvailability.get(0);
        DoctorAvailability last = todaysAvailability.get(todaysAvailability.size() - 1);
        String timeWindow = first.getStartTime() + " - " + last.getEndTime();

        if ("FULLY_BOOKED".equals(availabilityStatus)) {
            return "Fully booked today • " + timeWindow;
        }

        return "Available today • " + timeWindow;
    }

    private DoctorSummaryDTO buildDoctorSummaryDTO(User user, DoctorProfile profile) {
        DoctorSummaryDTO dto = new DoctorSummaryDTO();
        dto.setDoctorId(user.getId());
        dto.setEmail(user.getEmail());

        if (profile != null) {
            dto.setName(profile.getFullName());
            dto.setSpecialization(profile.getSpecialization());
            dto.setProfilePhotoUrl(resolveDoctorPhotoUrl(profile));
        } else {
            dto.setName("Unknown Doctor");
            dto.setSpecialization("General");
            dto.setProfilePhotoUrl(null);
        }

        return dto;
    }
}
