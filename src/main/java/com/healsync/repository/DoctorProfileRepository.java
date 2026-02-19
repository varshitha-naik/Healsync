package com.healsync.repository;

import com.healsync.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUserId(Long userId);

    List<DoctorProfile> findByClinicId(Long clinicId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<DoctorProfile> findBySpecialization(String specialization);
}
