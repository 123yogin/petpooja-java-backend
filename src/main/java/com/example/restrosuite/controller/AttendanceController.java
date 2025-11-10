package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Attendance;
import com.example.restrosuite.entity.Employee;
import com.example.restrosuite.repository.AttendanceRepository;
import com.example.restrosuite.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Attendance> getAttendanceByEmployee(@PathVariable UUID employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByDateDesc(employeeId);
    }

    @GetMapping("/date/{date}")
    public List<Attendance> getAttendanceByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return attendanceRepository.findByDate(localDate);
    }

    @GetMapping("/employee/{employeeId}/range")
    public List<Attendance> getAttendanceByRange(
            @PathVariable UUID employeeId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return attendanceRepository.findByEmployeeIdAndDateBetween(
                employeeId, LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    @GetMapping("/status/{status}")
    public List<Attendance> getAttendanceByStatus(@PathVariable String status) {
        return attendanceRepository.findByStatus(status);
    }

    @PostMapping("/check-in")
    public Attendance checkIn(@RequestBody Attendance attendance) {
        Employee employee = employeeRepository.findById(attendance.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LocalDate today = LocalDate.now();
        Attendance existing = attendanceRepository.findByEmployeeIdAndDate(employee.getId(), today)
                .orElse(null);

        if (existing != null && existing.getCheckIn() != null) {
            throw new RuntimeException("Employee already checked in today");
        }

        if (existing == null) {
            attendance.setEmployee(employee);
            attendance.setDate(today);
            attendance.setCheckIn(LocalDateTime.now());
            attendance.setStatus("PRESENT");
        } else {
            existing.setCheckIn(LocalDateTime.now());
            existing.setStatus("PRESENT");
            attendance = existing;
        }

        return attendanceRepository.save(attendance);
    }

    @PostMapping("/check-out")
    public Attendance checkOut(@RequestBody Attendance attendance) {
        Employee employee = employeeRepository.findById(attendance.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LocalDate today = LocalDate.now();
        Attendance existing = attendanceRepository.findByEmployeeIdAndDate(employee.getId(), today)
                .orElseThrow(() -> new RuntimeException("No check-in found for today"));

        if (existing.getCheckOut() != null) {
            throw new RuntimeException("Employee already checked out today");
        }

        existing.setCheckOut(LocalDateTime.now());

        // Calculate working hours
        if (existing.getCheckIn() != null && existing.getCheckOut() != null) {
            Duration duration = Duration.between(existing.getCheckIn(), existing.getCheckOut());
            long minutes = duration.toMinutes();
            existing.setWorkingHours(minutes);

            // Calculate overtime (assuming 8 hours = 480 minutes is standard)
            if (minutes > 480) {
                existing.setOvertimeHours(minutes - 480);
            }
        }

        return attendanceRepository.save(existing);
    }

    @PostMapping
    public Attendance createAttendance(@RequestBody Attendance attendance) {
        Employee employee = employeeRepository.findById(attendance.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        attendance.setEmployee(employee);
        if (attendance.getDate() == null) {
            attendance.setDate(LocalDate.now());
        }

        // Calculate working hours if both check-in and check-out are provided
        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            Duration duration = Duration.between(attendance.getCheckIn(), attendance.getCheckOut());
            long minutes = duration.toMinutes();
            attendance.setWorkingHours(minutes);
            if (minutes > 480) {
                attendance.setOvertimeHours(minutes - 480);
            }
        }

        return attendanceRepository.save(attendance);
    }

    @PutMapping("/{id}")
    public Attendance updateAttendance(@PathVariable UUID id, @RequestBody Attendance attendanceDetails) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        attendance.setDate(attendanceDetails.getDate());
        attendance.setCheckIn(attendanceDetails.getCheckIn());
        attendance.setCheckOut(attendanceDetails.getCheckOut());
        attendance.setStatus(attendanceDetails.getStatus());
        attendance.setShift(attendanceDetails.getShift());
        attendance.setRemarks(attendanceDetails.getRemarks());

        // Recalculate working hours
        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            Duration duration = Duration.between(attendance.getCheckIn(), attendance.getCheckOut());
            long minutes = duration.toMinutes();
            attendance.setWorkingHours(minutes);
            if (minutes > 480) {
                attendance.setOvertimeHours(minutes - 480);
            }
        }

        return attendanceRepository.save(attendance);
    }

    @DeleteMapping("/{id}")
    public String deleteAttendance(@PathVariable UUID id) {
        attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));
        attendanceRepository.deleteById(id);
        return "Attendance deleted successfully!";
    }
}

