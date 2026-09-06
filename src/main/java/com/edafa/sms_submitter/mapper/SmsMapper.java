package com.edafa.sms_submitter.mapper;

import com.edafa.sms_submitter.dto.SmsDetailResDto;
import com.edafa.sms_submitter.dto.SmsResDto;
import com.edafa.sms_submitter.entity.Contact;
import com.edafa.sms_submitter.entity.Message;
import com.edafa.sms_submitter.entity.Sms;
import org.springframework.stereotype.Component;

@Component
public class SmsMapper {
        public SmsDetailResDto toDto(Sms sms){
            if(sms==null || sms.getMessage()==null || sms.getContact()==null)return null;
            Message message=sms.getMessage();
            Contact contact = sms.getContact();
            return new SmsDetailResDto(sms.getSmsId(), sms.getSimId(), sms.getStatus(),message.getMessageId(), message.getBody(),message.getSendDate(),contact.getContactId(), contact.getName(), contact.getPhoneNo());
        }
        public SmsResDto toDto(Sms sms, Contact contact){
            if(sms==null || contact==null)return null;
            return new SmsResDto(sms.getSmsId(), sms.getSimId(), contact.getContactId(), contact.getName(), contact.getPhoneNo(), sms.getStatus());
        }
}
