package com.example.studentfees.controller;

import com.example.studentfees.model.StudentFeeForm;
import com.example.studentfees.service.CcavenueService;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final CcavenueService ccavenue;
    private final com.example.studentfees.service.PaymentRecordService paymentRecords;

    public PaymentController(CcavenueService ccavenue,
                             com.example.studentfees.service.PaymentRecordService paymentRecords) {
        this.ccavenue = ccavenue;
        this.paymentRecords = paymentRecords;
    }

    /** Fee payment form. */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("feeForm", new StudentFeeForm());
        return "index";
    }

    /**
     * Step 1: take the student fee form, encrypt the CCAvenue request and render
     * an auto-submitting HTML form that POSTs to CCAvenue.
     */
    @PostMapping("/pay")
    public String pay(@Valid @ModelAttribute("feeForm") StudentFeeForm feeForm,
                      BindingResult binding,
                      Model model) {
        if (binding.hasErrors()) {
            return "index";
        }

        String orderId = ccavenue.generateOrderId();
        CcavenueService.PaymentRequest req = ccavenue.buildPaymentRequest(feeForm, orderId);

        // Persist a PENDING record before redirecting to the gateway
        paymentRecords.createPending(feeForm, orderId);

        log.info("Redirecting student {} to CCAvenue for order {}", feeForm.getRollNumber(), orderId);

        model.addAttribute("encRequest", req.encRequest());
        model.addAttribute("accessCode", req.accessCode());
        model.addAttribute("transactionUrl", req.transactionUrl());
        model.addAttribute("orderId", orderId);
        model.addAttribute("studentName", feeForm.getStudentName());
        model.addAttribute("amount", String.format("%.2f", feeForm.getAmount()));
        return "redirect-to-ccavenue";
    }

    /**
     * Step 2: CCAvenue posts the encrypted response (encResp) back here after payment.
     * Both redirect_url and cancel_url point to this endpoint.
     */
    @PostMapping("/payment/response")
    public String paymentResponse(@RequestParam(value = "encResp", required = false) String encResp,
                                  Model model) {
        if (encResp == null || encResp.trim().isEmpty()) {
            log.error("No encResp received from CCAvenue");
            model.addAttribute("error", "No encrypted response received from the payment gateway.");
            return "payment-result";
        }

        Map<String, String> response = ccavenue.handleResponse(encResp);

        // Persist the outcome
        paymentRecords.updateFromCallback(response);

        String orderStatus = response.getOrDefault("order_status", "Unknown");
        boolean success = "Success".equalsIgnoreCase(orderStatus)
                || "Successful".equalsIgnoreCase(orderStatus);

        model.addAttribute("success", success);
        model.addAttribute("orderStatus", orderStatus);
        model.addAttribute("orderId", response.get("order_id"));
        model.addAttribute("trackingId", response.get("tracking_id"));
        model.addAttribute("bankRefNo", response.get("bank_ref_no"));
        model.addAttribute("amount", response.get("amount"));
        model.addAttribute("paymentMode", response.get("payment_mode"));
        model.addAttribute("statusMessage", response.get("status_message"));
        model.addAttribute("failureMessage", response.get("failure_message"));

        // Student-specific echoed params
        model.addAttribute("rollNumber", response.get("merchant_param1"));
        model.addAttribute("course", response.get("merchant_param2"));
        model.addAttribute("feeType", response.get("merchant_param3"));
        model.addAttribute("billingName", response.get("billing_name"));

        return "payment-result";
    }

    /** Some CCAvenue setups issue a GET on cancel; accept it too. */
    @GetMapping("/payment/response")
    public String paymentResponseGet(@RequestParam(value = "encResp", required = false) String encResp,
                                     Model model) {
        return paymentResponse(encResp, model);
    }

    /** Payments list (all student fee payments, newest first). */
    @GetMapping("/payments")
    public String payments(Model model) {
        java.util.List<com.example.studentfees.entity.StudentFeePayment> all = paymentRecords.findAll();
        model.addAttribute("payments", all);
        model.addAttribute("total", all.size());
        return "payments";
    }
}
