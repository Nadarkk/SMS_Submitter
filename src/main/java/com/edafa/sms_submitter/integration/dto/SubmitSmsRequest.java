package com.edafa.sms_submitter.integration.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SubmitSmsRequest {

    @JsonProperty("AccountId")
    public String accountId;

    @JsonProperty("Password")
    public String password;

    @JsonProperty("SecureHash")
    public String secureHash;

    @JsonProperty("SMSList")
    public List<SmsDetails> smsList;
}