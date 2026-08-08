package com.example.Qpay.Service.impl;

import com.example.Qpay.Service.SmsService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.from-number}")
    private String fromNumber;

    @Value("${twilio.sms-enabled:false}")
    private boolean smsEnabled;

    @Override
    public void sendOtp(String phone, String otp) {
        if (!smsEnabled) {
            // Dev mode — OTP console pe dikhega
            log.warn("DEV MODE — OTP for {}: {}", phone, otp);
            return;
        }

        try {
            String toNumber = formatIndianNumber(phone);

            Message message = Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(fromNumber),
                    buildOtpMessage(otp)
            ).create();

            log.info("OTP SMS sent to {} — SID: {}", phone, message.getSid());

        } catch (Exception e) {
            log.error("SMS failed for {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to send OTP SMS. Please try again.");
        }
    }

    // 9876543210 → +919876543210
    private String formatIndianNumber(String phone) {
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("91")) return "+" + phone;
        return "+91" + phone;
    }

    private String buildOtpMessage(String otp) {
        return String.format(
                "Your ZeroQ verification code is: %s\n" +
                        "Valid for 5 minutes.\n" +
                        "Do not share this OTP with anyone.",
                otp
        );
    }
}