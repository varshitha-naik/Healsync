package com.healsync.repository;

import com.healsync.entity.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {
    List<DoctorLeave> findByDoctorIdOrderByFromDateAsc(Long doctorId);

    boolean existsByDoctorIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(Long doctorId, LocalDate date1, LocalDate date2);
}
