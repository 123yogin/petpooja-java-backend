package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Bill;
import com.example.petpooja_clone.entity.Customer;
import com.example.petpooja_clone.entity.Payment;
import com.example.petpooja_clone.repository.BillRepository;
import com.example.petpooja_clone.repository.CustomerRepository;
import com.example.petpooja_clone.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts-receivable")
public class AccountsReceivableController {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/summary")
    public Map<String, Object> getAccountsReceivableSummary() {
        List<Bill> allBills = billRepository.findAll();
        
        double totalReceivables = allBills.stream()
                .filter(b -> b.getPendingAmount() != null && b.getPendingAmount() > 0)
                .mapToDouble(b -> b.getPendingAmount())
                .sum();

        long pendingBills = allBills.stream()
                .filter(b -> "PENDING".equals(b.getPaymentStatus()) || "PARTIAL".equals(b.getPaymentStatus()))
                .count();

        long overdueBills = allBills.stream()
                .filter(b -> "OVERDUE".equals(b.getPaymentStatus()))
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalReceivables", totalReceivables);
        result.put("pendingBills", pendingBills);
        result.put("overdueBills", overdueBills);
        result.put("totalBills", allBills.size());

        return result;
    }

    @GetMapping("/customer/{customerId}")
    public Map<String, Object> getCustomerReceivables(@PathVariable UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<Bill> customerBills = billRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getId().equals(customerId))
                .collect(Collectors.toList());

        double totalReceivables = customerBills.stream()
                .filter(b -> b.getPendingAmount() != null && b.getPendingAmount() > 0)
                .mapToDouble(b -> b.getPendingAmount())
                .sum();

        List<Payment> customerPayments = paymentRepository.findByCustomerId(customerId);

        Map<String, Object> result = new HashMap<>();
        result.put("customer", customer);
        result.put("totalReceivables", totalReceivables);
        result.put("totalBills", customerBills.size());
        result.put("bills", customerBills);
        result.put("payments", customerPayments);

        return result;
    }

    @GetMapping("/pending")
    public List<Bill> getPendingBills() {
        return billRepository.findAll().stream()
                .filter(b -> "PENDING".equals(b.getPaymentStatus()) || 
                           "PARTIAL".equals(b.getPaymentStatus()) ||
                           "OVERDUE".equals(b.getPaymentStatus()))
                .collect(Collectors.toList());
    }

    @GetMapping("/overdue")
    public List<Bill> getOverdueBills() {
        return billRepository.findAll().stream()
                .filter(b -> "OVERDUE".equals(b.getPaymentStatus()))
                .collect(Collectors.toList());
    }
}

