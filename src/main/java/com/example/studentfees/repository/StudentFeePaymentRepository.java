package com.example.studentfees.repository;

import com.example.studentfees.entity.StudentFeePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentFeePaymentRepository extends JpaRepository<StudentFeePayment, Long> {

    Optional<StudentFeePayment> findByOrderId(String orderId);

    List<StudentFeePayment> findAllByOrderByCreatedAtDesc();
}
