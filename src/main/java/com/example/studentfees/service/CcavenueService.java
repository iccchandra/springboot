package com.example.studentfees.service;

import com.example.studentfees.config.CcavenueProperties;
import com.example.studentfees.model.StudentFeeForm;
import com.example.studentfees.util.AesCryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mirrors the logic of the NestJS KotakService / CryptoService:
 *   - builds a CCAvenue request query string
 *   - encrypts it with the working key
 *   - decrypts + parses the callback response
 */
@Service
public class CcavenueService {

    private static final Logger log = LoggerFactory.getLogger(CcavenueService.class);

    private final CcavenueProperties props;
    private final AesCryptUtil crypto;

    public CcavenueService(CcavenueProperties props) {
        this.props = props;
        if (props.getWorkingKey() == null || props.getWorkingKey().trim().isEmpty()) {
            throw new IllegalStateException("ccavenue.working-key is not configured");
        }
        this.crypto = new AesCryptUtil(props.getWorkingKey());
    }

    /** Unique fee order id, e.g. FEE1730000000001234 */
    public String generateOrderId() {
        long ts = System.currentTimeMillis();
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "FEE" + ts + rand;
    }

    /**
     * Build the CCAvenue parameter string for a student fee payment and return
     * the encrypted request, ready to POST to the gateway.
     */
    public PaymentRequest buildPaymentRequest(StudentFeeForm form, String orderId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", props.getMerchantId());
        params.put("order_id", orderId);
        params.put("amount", String.format("%.2f", form.getAmount()));
        params.put("currency", "INR");
        params.put("redirect_url", props.getRedirectUrl());
        params.put("cancel_url", props.getCancelUrl());
        params.put("language", "EN");

        // Billing details (student)
        params.put("billing_name", safe(form.getStudentName()));
        params.put("billing_email", safe(form.getEmail()));
        params.put("billing_tel", safe(form.getMobile()));

        // Student-specific data carried through CCAvenue and echoed back on callback
        params.put("merchant_param1", safe(form.getRollNumber()));
        params.put("merchant_param2", safe(form.getCourse()));
        params.put("merchant_param3", safe(form.getFeeType()));

        String queryString = toQueryString(params);
        String encRequest = crypto.encrypt(queryString);

        log.info("Built CCAvenue request for order {} (student {})", orderId, form.getRollNumber());

        return new PaymentRequest(encRequest, props.getAccessCode(), props.getTransactionUrl());
    }

    /**
     * Decrypt and parse the CCAvenue callback (encResp) into a map of fields.
     */
    public Map<String, String> handleResponse(String encResp) {
        String decrypted = crypto.decrypt(encResp);
        Map<String, String> data = parseQueryString(decrypted);
        log.info("CCAvenue response for order {} -> status {}",
                data.get("order_id"), data.get("order_status"));
        return data;
    }

    // ----- helpers (mirror crypto.service.ts objectToQueryString / queryStringToObject) -----

    private String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private Map<String, String> parseQueryString(String qs) {
        Map<String, String> map = new LinkedHashMap<>();
        if (qs == null || qs.isEmpty()) return map;
        for (String pair : qs.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                map.put(decode(pair), "");
            } else {
                map.put(decode(pair.substring(0, idx)), decode(pair.substring(idx + 1)));
            }
        }
        return map;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always available on a standard JVM
            throw new IllegalStateException("UTF-8 decoding not supported", e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Encrypted request payload to hand to the auto-submit redirect form. */
    public static final class PaymentRequest {
        private final String encRequest;
        private final String accessCode;
        private final String transactionUrl;

        public PaymentRequest(String encRequest, String accessCode, String transactionUrl) {
            this.encRequest = encRequest;
            this.accessCode = accessCode;
            this.transactionUrl = transactionUrl;
        }

        public String encRequest() { return encRequest; }
        public String accessCode() { return accessCode; }
        public String transactionUrl() { return transactionUrl; }
    }
}
