package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Attendance;
import com.example.petpooja_clone.entity.Employee;
import com.example.petpooja_clone.entity.Payroll;
import com.example.petpooja_clone.repository.AttendanceRepository;
import com.example.petpooja_clone.repository.EmployeeRepository;
import com.example.petpooja_clone.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @GetMapping
    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Payroll> getPayrollsByEmployee(@PathVariable UUID employeeId) {
        return payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId);
    }

    @GetMapping("/month/{month}/year/{year}")
    public List<Payroll> getPayrollsByMonthAndYear(@PathVariable Integer month, @PathVariable Integer year) {
        return payrollRepository.findByMonthAndYear(month, year);
    }

    @GetMapping("/year/{year}")
    public List<Payroll> getPayrollsByYear(@PathVariable Integer year) {
        return payrollRepository.findByYear(year);
    }

    @GetMapping("/status/{status}")
    public List<Payroll> getPayrollsByStatus(@PathVariable String status) {
        return payrollRepository.findByStatus(status);
    }

    @GetMapping("/{id}")
    public Payroll getPayrollById(@PathVariable UUID id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found with ID: " + id));
    }

    @PostMapping("/generate")
    public Payroll generatePayroll(@RequestParam UUID employeeId, @RequestParam Integer month, @RequestParam Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check if payroll already exists
        if (payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year).isPresent()) {
            throw new RuntimeException("Payroll already exists for this employee, month, and year");
        }

        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setStatus("PENDING");

        // Set basic salary
        payroll.setBasicSalary(employee.getBasicSalary() != null ? employee.getBasicSalary() : 0.0);

        // Set allowances (from employee or defaults)
        payroll.setHouseRentAllowance(employee.getAllowances() != null ? employee.getAllowances() * 0.4 : 0.0); // 40% of allowances as HRA
        payroll.setTransportAllowance(employee.getAllowances() != null ? employee.getAllowances() * 0.2 : 0.0); // 20% as transport
        payroll.setMedicalAllowance(employee.getAllowances() != null ? employee.getAllowances() * 0.2 : 0.0); // 20% as medical
        payroll.setOtherAllowances(employee.getAllowances() != null ? employee.getAllowances() * 0.2 : 0.0); // 20% as other

        // Calculate attendance summary
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);

        int daysPresent = 0;
        int daysAbsent = 0;
        int daysLeave = 0;
        int daysOvertime = 0;
        long totalOvertimeMinutes = 0;

        for (Attendance att : attendances) {
            if ("PRESENT".equals(att.getStatus())) {
                daysPresent++;
                if (att.getOvertimeHours() != null && att.getOvertimeHours() > 0) {
                    daysOvertime++;
                    totalOvertimeMinutes += att.getOvertimeHours();
                }
            } else if ("ABSENT".equals(att.getStatus())) {
                daysAbsent++;
            } else if ("LEAVE".equals(att.getStatus()) || att.getStatus().contains("LEAVE")) {
                daysLeave++;
            }
        }

        payroll.setDaysPresent(daysPresent);
        payroll.setDaysAbsent(daysAbsent);
        payroll.setDaysLeave(daysLeave);
        payroll.setDaysOvertime(daysOvertime);

        // Calculate overtime pay (assuming 1.5x hourly rate)
        double hourlyRate = payroll.getBasicSalary() / (30 * 8); // Assuming 30 days, 8 hours per day
        double overtimePay = (totalOvertimeMinutes / 60.0) * hourlyRate * 1.5;
        payroll.setOvertimePay(overtimePay);

        // Calculate deductions
        double pfRate = 0.12; // 12% PF
        payroll.setProvidentFund(payroll.getBasicSalary() * pfRate);
        payroll.setProfessionalTax(200.0); // Fixed professional tax
        payroll.setIncomeTax(calculateIncomeTax(payroll.getBasicSalary() + payroll.getHouseRentAllowance()));
        payroll.setOtherDeductions(employee.getDeductions() != null ? employee.getDeductions() : 0.0);

        // Calculate totals
        double totalEarnings = payroll.getBasicSalary() + payroll.getHouseRentAllowance() +
                payroll.getTransportAllowance() + payroll.getMedicalAllowance() +
                payroll.getOtherAllowances() + payroll.getOvertimePay() + (payroll.getBonus() != null ? payroll.getBonus() : 0.0);
        payroll.setTotalEarnings(totalEarnings);

        double totalDeductions = payroll.getProvidentFund() + payroll.getProfessionalTax() +
                payroll.getIncomeTax() + payroll.getOtherDeductions();
        payroll.setTotalDeductions(totalDeductions);

        payroll.setNetSalary(totalEarnings - totalDeductions);
        payroll.setPaymentDate(LocalDate.of(year, month, 1).plusMonths(1)); // Pay on 1st of next month

        return payrollRepository.save(payroll);
    }

    @PostMapping
    public Payroll createPayroll(@RequestBody Payroll payroll) {
        Employee employee = employeeRepository.findById(payroll.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        payroll.setEmployee(employee);

        // Calculate totals if not provided
        if (payroll.getTotalEarnings() == null) {
            double totalEarnings = payroll.getBasicSalary() +
                    (payroll.getHouseRentAllowance() != null ? payroll.getHouseRentAllowance() : 0.0) +
                    (payroll.getTransportAllowance() != null ? payroll.getTransportAllowance() : 0.0) +
                    (payroll.getMedicalAllowance() != null ? payroll.getMedicalAllowance() : 0.0) +
                    (payroll.getOtherAllowances() != null ? payroll.getOtherAllowances() : 0.0) +
                    (payroll.getOvertimePay() != null ? payroll.getOvertimePay() : 0.0) +
                    (payroll.getBonus() != null ? payroll.getBonus() : 0.0);
            payroll.setTotalEarnings(totalEarnings);
        }

        if (payroll.getTotalDeductions() == null) {
            double totalDeductions = (payroll.getProvidentFund() != null ? payroll.getProvidentFund() : 0.0) +
                    (payroll.getProfessionalTax() != null ? payroll.getProfessionalTax() : 0.0) +
                    (payroll.getIncomeTax() != null ? payroll.getIncomeTax() : 0.0) +
                    (payroll.getOtherDeductions() != null ? payroll.getOtherDeductions() : 0.0);
            payroll.setTotalDeductions(totalDeductions);
        }

        if (payroll.getNetSalary() == null) {
            payroll.setNetSalary(payroll.getTotalEarnings() - payroll.getTotalDeductions());
        }

        if (payroll.getStatus() == null) {
            payroll.setStatus("PENDING");
        }

        return payrollRepository.save(payroll);
    }

    @PutMapping("/{id}")
    public Payroll updatePayroll(@PathVariable UUID id, @RequestBody Payroll payrollDetails) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        payroll.setMonth(payrollDetails.getMonth());
        payroll.setYear(payrollDetails.getYear());
        payroll.setBasicSalary(payrollDetails.getBasicSalary());
        payroll.setHouseRentAllowance(payrollDetails.getHouseRentAllowance());
        payroll.setTransportAllowance(payrollDetails.getTransportAllowance());
        payroll.setMedicalAllowance(payrollDetails.getMedicalAllowance());
        payroll.setOtherAllowances(payrollDetails.getOtherAllowances());
        payroll.setOvertimePay(payrollDetails.getOvertimePay());
        payroll.setBonus(payrollDetails.getBonus());
        payroll.setProvidentFund(payrollDetails.getProvidentFund());
        payroll.setProfessionalTax(payrollDetails.getProfessionalTax());
        payroll.setIncomeTax(payrollDetails.getIncomeTax());
        payroll.setOtherDeductions(payrollDetails.getOtherDeductions());
        payroll.setPaymentDate(payrollDetails.getPaymentDate());
        payroll.setPaymentMode(payrollDetails.getPaymentMode());
        payroll.setStatus(payrollDetails.getStatus());
        payroll.setRemarks(payrollDetails.getRemarks());

        // Recalculate totals
        double totalEarnings = payroll.getBasicSalary() +
                (payroll.getHouseRentAllowance() != null ? payroll.getHouseRentAllowance() : 0.0) +
                (payroll.getTransportAllowance() != null ? payroll.getTransportAllowance() : 0.0) +
                (payroll.getMedicalAllowance() != null ? payroll.getMedicalAllowance() : 0.0) +
                (payroll.getOtherAllowances() != null ? payroll.getOtherAllowances() : 0.0) +
                (payroll.getOvertimePay() != null ? payroll.getOvertimePay() : 0.0) +
                (payroll.getBonus() != null ? payroll.getBonus() : 0.0);
        payroll.setTotalEarnings(totalEarnings);

        double totalDeductions = (payroll.getProvidentFund() != null ? payroll.getProvidentFund() : 0.0) +
                (payroll.getProfessionalTax() != null ? payroll.getProfessionalTax() : 0.0) +
                (payroll.getIncomeTax() != null ? payroll.getIncomeTax() : 0.0) +
                (payroll.getOtherDeductions() != null ? payroll.getOtherDeductions() : 0.0);
        payroll.setTotalDeductions(totalDeductions);

        payroll.setNetSalary(payroll.getTotalEarnings() - payroll.getTotalDeductions());

        return payrollRepository.save(payroll);
    }

    @PutMapping("/{id}/mark-paid")
    public Payroll markAsPaid(@PathVariable UUID id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        payroll.setStatus("PAID");
        if (payroll.getPaymentDate() == null) {
            payroll.setPaymentDate(LocalDate.now());
        }

        return payrollRepository.save(payroll);
    }

    @DeleteMapping("/{id}")
    public String deletePayroll(@PathVariable UUID id) {
        payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));
        payrollRepository.deleteById(id);
        return "Payroll deleted successfully!";
    }

    private Double calculateIncomeTax(Double annualSalary) {
        // Simplified income tax calculation (Indian tax slabs)
        if (annualSalary <= 250000) {
            return 0.0;
        } else if (annualSalary <= 500000) {
            return (annualSalary - 250000) * 0.05 / 12; // 5% on amount above 2.5L, monthly
        } else if (annualSalary <= 1000000) {
            return (12500 + (annualSalary - 500000) * 0.20) / 12; // 20% on amount above 5L, monthly
        } else {
            return (112500 + (annualSalary - 1000000) * 0.30) / 12; // 30% on amount above 10L, monthly
        }
    }
}

