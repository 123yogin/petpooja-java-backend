package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Employee;
import com.example.restrosuite.entity.Leave;
import com.example.restrosuite.entity.User;
import com.example.restrosuite.repository.EmployeeRepository;
import com.example.restrosuite.repository.LeaveRepository;
import com.example.restrosuite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Leave> getAllLeaves() {
        return leaveRepository.findAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Leave> getLeavesByEmployee(@PathVariable UUID employeeId) {
        return leaveRepository.findByEmployeeIdOrderByAppliedAtDesc(employeeId);
    }

    @GetMapping("/status/{status}")
    public List<Leave> getLeavesByStatus(@PathVariable String status) {
        return leaveRepository.findByStatus(status);
    }

    @GetMapping("/pending")
    public List<Leave> getPendingLeaves() {
        return leaveRepository.findByStatus("PENDING");
    }

    @GetMapping("/{id}")
    public Leave getLeaveById(@PathVariable UUID id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found with ID: " + id));
    }

    @PostMapping
    public Leave createLeave(@RequestBody Leave leave) {
        Employee employee = employeeRepository.findById(leave.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        leave.setEmployee(employee);
        leave.setStatus("PENDING");
        leave.setAppliedAt(LocalDateTime.now());

        // Calculate number of days
        if (leave.getStartDate() != null && leave.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            leave.setNumberOfDays((int) days);
        }

        return leaveRepository.save(leave);
    }

    @PutMapping("/{id}")
    public Leave updateLeave(@PathVariable UUID id, @RequestBody Leave leaveDetails) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setLeaveType(leaveDetails.getLeaveType());
        leave.setStartDate(leaveDetails.getStartDate());
        leave.setEndDate(leaveDetails.getEndDate());
        leave.setReason(leaveDetails.getReason());
        leave.setRemarks(leaveDetails.getRemarks());

        // Recalculate number of days
        if (leave.getStartDate() != null && leave.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            leave.setNumberOfDays((int) days);
        }

        return leaveRepository.save(leave);
    }

    @PutMapping("/{id}/approve")
    public Leave approveLeave(@PathVariable UUID id, @RequestParam UUID approvedById) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        User approvedBy = userRepository.findById(approvedById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        leave.setStatus("APPROVED");
        leave.setApprovedBy(approvedBy);
        leave.setApprovedAt(LocalDateTime.now());

        return leaveRepository.save(leave);
    }

    @PutMapping("/{id}/reject")
    public Leave rejectLeave(@PathVariable UUID id, @RequestParam UUID approvedById, @RequestParam String rejectionReason) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        User approvedBy = userRepository.findById(approvedById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        leave.setStatus("REJECTED");
        leave.setApprovedBy(approvedBy);
        leave.setApprovedAt(LocalDateTime.now());
        leave.setRejectionReason(rejectionReason);

        return leaveRepository.save(leave);
    }

    @PutMapping("/{id}/cancel")
    public Leave cancelLeave(@PathVariable UUID id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!"PENDING".equals(leave.getStatus())) {
            throw new RuntimeException("Only pending leaves can be cancelled");
        }

        leave.setStatus("CANCELLED");
        return leaveRepository.save(leave);
    }

    @DeleteMapping("/{id}")
    public String deleteLeave(@PathVariable UUID id) {
        leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        leaveRepository.deleteById(id);
        return "Leave deleted successfully!";
    }
}

