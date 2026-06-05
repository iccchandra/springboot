package com.example.studentfees.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CCAvenue merchant configuration, bound from {@code ccavenue.*} in application.properties
 * (override with environment variables in production).
 */
@ConfigurationProperties(prefix = "ccavenue")
public class CcavenueProperties {

    /** Merchant id issued by CCAvenue (KOTAK_MERCHANT_ID in the Node backend). */
    private String merchantId;

    /** Access code issued by CCAvenue (KOTAK_ACCESS_CODE). */
    private String accessCode;

    /** Working key used for AES encryption (KOTAK_WORKING_KEY). KEEP SECRET. */
    private String workingKey;

    /** CCAvenue transaction endpoint (test vs secure). */
    private String transactionUrl;

    /** URL CCAvenue redirects to after payment (our /payment/response). */
    private String redirectUrl;

    /** URL CCAvenue redirects to on cancel. */
    private String cancelUrl;

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }

    public String getWorkingKey() { return workingKey; }
    public void setWorkingKey(String workingKey) { this.workingKey = workingKey; }

    public String getTransactionUrl() { return transactionUrl; }
    public void setTransactionUrl(String transactionUrl) { this.transactionUrl = transactionUrl; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
}
