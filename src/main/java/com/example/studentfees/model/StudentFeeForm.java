package com.example.studentfees.model;

import jakarta.validation.constraints.*;

/**
 * Backing form for the "Pay Student Fees" page.
 */
public class StudentFeeForm {

    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Roll number is required")
    private String rollNumber;

    @NotBlank(message = "Course is required")
    private String course;

    @NotBlank(message = "Fee type is required")
    private String feeType;          // e.g. Tuition / Exam / Hostel

    @Email(message = "A valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(regexp = "\\d{10}", message = "Mobile must be a 10-digit number")
    private String mobile;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    private Double amount;

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
