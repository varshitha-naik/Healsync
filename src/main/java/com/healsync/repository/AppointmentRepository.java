package com.healsync.repository;

import com.healsync.entity.Appointment;
import com.healsync.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
        List<Appointment> findByDoctorId(Long doctorId);

        List<Appointment> findByPatientId(Long patientId);

        List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

        List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

        @Query("SELECT DISTINCT a.patientId FROM Appointment a WHERE a.doctorId = :doctorId")
        List<Long> findDistinctPatientIdsByDoctorId(@Param("doctorId") Long doctorId);

        @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctorId = :doctorId " +
                        "AND a.status NOT IN ('CANCELLED') " +
                        "AND ((a.startDateTime < :endDateTime AND a.endDateTime > :startDateTime))")
        boolean existsOverlappingAppointment(
                        @Param("doctorId") Long doctorId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime);

        Optional<Appointment> findTopByPatientIdAndDoctorIdOrderByStartDateTimeDesc(Long patientId, Long doctorId);
}
