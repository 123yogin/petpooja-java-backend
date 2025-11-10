package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Bill;
import com.example.petpooja_clone.entity.Customer;
import com.example.petpooja_clone.entity.Payment;
import com.example.petpooja_clone.repository.BillRepository;
import com.example.petpooja_clone.repository.CustomerRepository;
import com.example.petpooja_clone.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @GetMapping("/bill/{billId}")
    public List<Payment> getPaymentsByBill(@PathVariable UUID billId) {
        return paymentRepository.findByBillId(billId);
    }

    @GetMapping("/customer/{customerId}")
    public List<Payment> getPaymentsByCustomer(@PathVariable UUID customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    @GetMapping("/status/{status}")
    public List<Payment> getPaymentsByStatus(@PathVariable String status) {
        return paymentRepository.findByStatus(status);
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));
    }

    @PostMapping
    public Payment createPayment(@RequestBody Payment payment) {
        Bill bill = billRepository.findById(payment.getBill().getId())
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        payment.setBill(bill);

        if (payment.getCustomer() != null && payment.getCustomer().getId() != null) {
            Customer customer = customerRepository.findById(payment.getCustomer().getId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            payment.setCustomer(customer);
        }

        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        if (payment.getStatus() == null) {
            payment.setStatus("COMPLETED");
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Update bill payment status
        updateBillPaymentStatus(bill);

        return savedPayment;
    }

    @PutMapping("/{id}")
    public Payment updatePayment(@PathVariable UUID id, @RequestBody Payment paymentDetails) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setAmount(paymentDetails.getAmount());
        payment.setPaymentDate(paymentDetails.getPaymentDate());
        payment.setPaymentMode(paymentDetails.getPaymentMode());
        payment.setTransactionId(paymentDetails.getTransactionId());
        payment.setChequeNumber(paymentDetails.getChequeNumber());
        payment.setBankName(paymentDetails.getBankName());
        payment.setStatus(paymentDetails.getStatus());
        payment.setRemarks(paymentDetails.getRemarks());

        if (paymentDetails.getCustomer() != null && paymentDetails.getCustomer().getId() != null) {
            Customer customer = customerRepository.findById(paymentDetails.getCustomer().getId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            payment.setCustomer(customer);
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Update bill payment status
        updateBillPaymentStatus(payment.getBill());

        return savedPayment;
    }

    @DeleteMapping("/{id}")
    public String deletePayment(@PathVariable UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Bill bill = payment.getBill();
        paymentRepository.deleteById(id);

        // Update bill payment status after deletion
        updateBillPaymentStatus(bill);

        return "Payment deleted successfully!";
    }

    private void updateBillPaymentStatus(Bill bill) {
        List<Payment> payments = paymentRepository.findByBillId(bill.getId());
        double totalPaid = payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        bill.setPaidAmount(totalPaid);
        bill.setPendingAmount(bill.getGrandTotal() - totalPaid);

        if (totalPaid == 0) {
            bill.setPaymentStatus("PENDING");
        } else if (totalPaid >= bill.getGrandTotal()) {
            bill.setPaymentStatus("PAID");
        } else {
            bill.setPaymentStatus("PARTIAL");
        }

        billRepository.save(bill);
    }
}

