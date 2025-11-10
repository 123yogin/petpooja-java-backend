package com.example.restrosuite.repository;

import com.example.restrosuite.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, UUID> {
    Optional<Payroll> findByEmployeeIdAndMonthAndYear(UUID employeeId, Integer month, Integer year);
    List<Payroll> findByEmployeeIdOrderByYearDescMonthDesc(UUID employeeId);
    List<Payroll> findByMonthAndYear(Integer month, Integer year);
    List<Payroll> findByStatus(String status);
    List<Payroll> findByYear(Integer year);
}

