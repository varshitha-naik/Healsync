package com.healsync.repository;

import com.healsync.entity.MedicalSummaryReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalSummaryReportRepository extends JpaRepository<MedicalSummaryReport, Long> {
    Optional<MedicalSummaryReport> findByAppointmentId(Long appointmentId);

    List<MedicalSummaryReport> findByEmailedAtIsNullOrderByGeneratedAtDesc();
}
