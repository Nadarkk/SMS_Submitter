package com.edafa.sms_submitter;

import com.edafa.sms_submitter.entity.Company;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.repository.CompanyRepo;
import com.edafa.sms_submitter.repository.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class SmsSubmitterApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmsSubmitterApplication.class, args);
    }

}
