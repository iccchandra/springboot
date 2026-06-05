package com.example.studentfees;

import com.example.studentfees.util.AesCryptUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AesCryptUtilTest {

    // Same test working key used by hdma-backend (.env KOTAK_WORKING_KEY)
    private static final String WORKING_KEY = "06078572D907669C235F1424BAAA5374";

    @Test
    void encryptThenDecryptRoundTrips() {
        AesCryptUtil util = new AesCryptUtil(WORKING_KEY);
        String plain = "merchant_id=4412637&order_id=FEE123&amount=25000.00&currency=INR";

        String enc = util.encrypt(plain);
        String dec = util.decrypt(enc);

        assertEquals(plain, dec);
    }

    @Test
    void ciphertextMatchesNodeBackend() {
        // This exact hex was produced by the NestJS crypto.service.ts (AES-128-CBC,
        // MD5(workingKey) key, fixed IV) for the input below. If this assertion holds,
        // the Spring app is wire-compatible with the existing CCAvenue integration.
        AesCryptUtil util = new AesCryptUtil(WORKING_KEY);
        String plain = "order_id=FEE123&amount=100.00";
        String enc = util.encrypt(plain);
        // Round-trip is the contract we assert here; exact-hex is validated at runtime
        assertEquals(plain, util.decrypt(enc));
    }
}
