package com.example.Qpay.Config;


import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TwilioConfig {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    // App start hone pe Twilio initialize hoga
    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio initialized successfully ✅");
    }
}