package com.healsync.service;

import com.healsync.entity.Appointment;
import com.healsync.entity.PatientProfile;
import com.healsync.entity.User;
import com.healsync.enums.AppointmentStatus;
import com.healsync.enums.UserRole;
import com.healsync.repository.AppointmentRepository;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void getCompletedAppointmentsByPatientUserId_returnsCompletedAppointmentsForValidPatientUserId() {
        User patientUser = new User();
        patientUser.setId(101L);
        patientUser.setRole(UserRole.PATIENT);

        PatientProfile profile = new PatientProfile();
        profile.setId(501L);
        profile.setUserId(101L);
        profile.setFullName("Patient One");

        Appointment completedAppointment = new Appointment();
        completedAppointment.setId(9001L);
        completedAppointment.setPatientId(501L);
        completedAppointment.setDoctorId(700L);
        completedAppointment.setStatus(AppointmentStatus.COMPLETED);
        completedAppointment.setStartDateTime(LocalDateTime.of(2026, 4, 10, 9, 0));

        when(userRepository.findById(101L)).thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findByUserId(101L)).thenReturn(Optional.of(profile));
        when(appointmentRepository.findByPatientIdAndStatus(501L, AppointmentStatus.COMPLETED))
                .thenReturn(List.of(completedAppointment));

        List<Appointment> result = appointmentService.getCompletedAppointmentsByPatientUserId(101L);

        assertEquals(1, result.size());
        assertEquals(AppointmentStatus.COMPLETED, result.get(0).getStatus());
        verify(appointmentRepository).findByPatientIdAndStatus(501L, AppointmentStatus.COMPLETED);
    }

    @Test
    void getCompletedAppointmentsByPatientUserId_returnsEmptyListForWrongId() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        List<Appointment> result = appointmentService.getCompletedAppointmentsByPatientUserId(999L);

        assertTrue(result.isEmpty());
        verify(patientProfileRepository, never()).findByUserId(999L);
        verify(appointmentRepository, never()).findByPatientIdAndStatus(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }
}
