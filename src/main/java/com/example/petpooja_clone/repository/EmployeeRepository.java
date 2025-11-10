package com.example.petpooja_clone.repository;

import com.example.petpooja_clone.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByEmail(String email);
    List<Employee> findByIsActiveTrue();
    List<Employee> findByDepartment(String department);
    List<Employee> findByIsActive(Boolean isActive);
}

