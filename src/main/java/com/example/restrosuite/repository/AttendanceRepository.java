package com.example.restrosuite.repository;

import com.example.restrosuite.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);
    List<Attendance> findByEmployeeIdOrderByDateDesc(UUID employeeId);
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByEmployeeIdAndDateBetween(UUID employeeId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByStatus(String status);
}

