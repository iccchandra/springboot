package com.example.studentfees.service;

import com.example.studentfees.entity.PaymentStatus;
import com.example.studentfees.entity.StudentFeePayment;
import com.example.studentfees.model.StudentFeeForm;
import com.example.studentfees.repository.StudentFeePaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Persists student fee payments to MySQL across the CCAvenue flow.
 */
@Service
public class PaymentRecordService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRecordService.class);

    private final StudentFeePaymentRepository repository;

    public PaymentRecordService(StudentFeePaymentRepository repository) {
        this.repository = repository;
    }

    /** Create a PENDING record when the student is redirected to CCAvenue. */
    @Transactional
    public StudentFeePayment createPending(StudentFeeForm form, String orderId) {
        StudentFeePayment p = new StudentFeePayment();
        p.setOrderId(orderId);
        p.setStudentName(form.getStudentName());
        p.setRollNumber(form.getRollNumber());
        p.setCourse(form.getCourse());
        p.setFeeType(form.getFeeType());
        p.setEmail(form.getEmail());
        p.setMobile(form.getMobile());
        p.setAmount(form.getAmount());
        p.setCurrency("INR");
        p.setStatus(PaymentStatus.PENDING);
        StudentFeePayment saved = repository.save(p);
        log.info("Saved PENDING payment for order {}", orderId);
        return saved;
    }

    /**
     * Update the record from the decrypted CCAvenue callback. Idempotent: an
     * already-SUCCESS record is left untouched.
     */
    @Transactional
    public void updateFromCallback(Map<String, String> response) {
        String orderId = response.get("order_id");
        if (orderId == null) {
            log.warn("Callback had no order_id; nothing to update");
            return;
        }

        StudentFeePayment p = repository.findByOrderId(orderId).orElse(null);
        if (p == null) {
            log.warn("No payment record found for order {} (callback for an unknown order)", orderId);
            return;
        }

        if (p.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Order {} already SUCCESS; skipping duplicate callback", orderId);
            return;
        }

        p.setTrackingId(response.get("tracking_id"));
        p.setBankRefNo(response.get("bank_ref_no"));
        p.setPaymentMode(response.get("payment_mode"));
        p.setStatusMessage(response.get("status_message"));
        p.setFailureMessage(response.get("failure_message"));

        String orderStatus = response.getOrDefault("order_status", "").toLowerCase();
        switch (orderStatus) {
            case "success":
            case "successful":
                p.setStatus(PaymentStatus.SUCCESS);
                break;
            case "aborted":
                p.setStatus(PaymentStatus.ABORTED);
                break;
            default:
                p.setStatus(PaymentStatus.FAILED);
                break;
        }

        repository.save(p);
        log.info("Updated payment for order {} -> {}", orderId, p.getStatus());
    }

    @Transactional(readOnly = true)
    public List<StudentFeePayment> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
