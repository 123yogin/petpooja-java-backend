package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Employee;
import com.example.petpooja_clone.repository.EmployeeRepository;
import com.example.petpooja_clone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @GetMapping("/active")
    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByIsActiveTrue();
    }

    @GetMapping("/department/{department}")
    public List<Employee> getEmployeesByDepartment(@PathVariable String department) {
        return employeeRepository.findByDepartment(department);
    }

    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        // Check if employeeId already exists
        if (employee.getEmployeeId() != null && employeeRepository.findByEmployeeId(employee.getEmployeeId()).isPresent()) {
            throw new RuntimeException("Employee ID already exists: " + employee.getEmployeeId());
        }
        // Check if email already exists
        if (employee.getEmail() != null && employeeRepository.findByEmail(employee.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + employee.getEmail());
        }
        if (employee.getIsActive() == null) {
            employee.setIsActive(true);
        }
        if (employee.getBasicSalary() == null) {
            employee.setBasicSalary(0.0);
        }
        if (employee.getAllowances() == null) {
            employee.setAllowances(0.0);
        }
        if (employee.getDeductions() == null) {
            employee.setDeductions(0.0);
        }
        if (employee.getJoinDate() == null) {
            employee.setJoinDate(LocalDate.now());
        }
        return employeeRepository.save(employee);
    }

    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable UUID id, @RequestBody Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPhone(employeeDetails.getPhone());
        employee.setAddress(employeeDetails.getAddress());
        employee.setCity(employeeDetails.getCity());
        employee.setState(employeeDetails.getState());
        employee.setPincode(employeeDetails.getPincode());
        employee.setDepartment(employeeDetails.getDepartment());
        employee.setDesignation(employeeDetails.getDesignation());
        employee.setShift(employeeDetails.getShift());
        employee.setBasicSalary(employeeDetails.getBasicSalary());
        employee.setAllowances(employeeDetails.getAllowances());
        employee.setDeductions(employeeDetails.getDeductions());
        employee.setDateOfBirth(employeeDetails.getDateOfBirth());
        employee.setGender(employeeDetails.getGender());
        employee.setEmergencyContact(employeeDetails.getEmergencyContact());
        employee.setEmergencyPhone(employeeDetails.getEmergencyPhone());
        employee.setIsActive(employeeDetails.getIsActive());

        if (employeeDetails.getUser() != null && employeeDetails.getUser().getId() != null) {
            employee.setUser(userRepository.findById(employeeDetails.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found")));
        }

        return employeeRepository.save(employee);
    }

    @DeleteMapping("/{id}")
    public String deleteEmployee(@PathVariable UUID id) {
        employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));
        employeeRepository.deleteById(id);
        return "Employee deleted successfully!";
    }
}

