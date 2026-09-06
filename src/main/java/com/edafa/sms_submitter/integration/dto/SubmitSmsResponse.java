package com.edafa.sms_submitter.integration.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SubmitSmsResponse {

    @JsonProperty("SMSResponseBySmsId")
    public List<SmsResponseItem> smsResponseList;

    @JsonProperty("ResultStatus")
    public String resultStatus;

    @JsonProperty("Description")
    public String description;
}