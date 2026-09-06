package com.edafa.sms_submitter.integration.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class InquireSmsResponse {
    @JsonProperty("EnquireSMSByIdResponseList")
    public List<InquiryResponseItem> responseList;
    @JsonProperty("ResultStatus")
    public String resultStatus;
    @JsonProperty("Description")
    public String description;
}