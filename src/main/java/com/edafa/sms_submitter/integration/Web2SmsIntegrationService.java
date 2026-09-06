package com.edafa.sms_submitter.integration;

import com.edafa.sms_submitter.integration.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class Web2SmsIntegrationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${web2sms.api.url:http://localhost:8080/web2sms/api}")
    private String baseUrl;

    @Value("${web2sms.account.id:test-account}")
    private String accountId;

    @Value("${web2sms.password:test-pass}")
    private String password;

    @Value("${web2sms.secure.hash:test-hash}")
    private String secureHash;

    /**
     * Sends a batch of messages to the Web2SMS Simulator.
     */
    public SubmitSmsResponse sendSmsBatch(List<SmsDetails> messages) {
        String url = baseUrl + "/submit/submitAndgetSmsId";

        SubmitSmsRequest request = new SubmitSmsRequest();
        request.accountId = accountId;
        request.password = password;
        request.secureHash = secureHash;
        request.smsList = messages;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<SubmitSmsRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(url, entity, SubmitSmsResponse.class);
    }

    /**
     * Inquires about the status of a batch of tracking IDs.
     */
    public InquireSmsResponse inquireStatus(List<String> smsIds) {
        String url = baseUrl + "/submit/inquireBySmsId";

        InquireSmsRequest request = new InquireSmsRequest();
        request.accountId = accountId;
        request.password = password;
        request.secureHash = secureHash;
        request.smsIdList = smsIds;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<InquireSmsRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(url, entity, InquireSmsResponse.class);
    }
}