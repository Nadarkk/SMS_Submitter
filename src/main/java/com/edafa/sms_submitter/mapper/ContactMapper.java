package com.edafa.sms_submitter.mapper;

import com.edafa.sms_submitter.dto.AddContactReqDto;
import com.edafa.sms_submitter.dto.ContactResDto;
import com.edafa.sms_submitter.entity.Company;
import com.edafa.sms_submitter.entity.Contact;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {
    public Contact toEntity(AddContactReqDto dto, Company company){
        if (dto == null) return null;
        return new Contact(dto.getName(), dto.getPhoneNo() ,company );
    }

    public ContactResDto toDto(Contact contact){
        if (contact == null || contact.getCompany()==null) return null;
        return new ContactResDto(
                contact.getContactId(), contact.getName(), contact.getPhoneNo(),
                contact.getCompany().getCompanyId(),
                contact.getCompany().getCompanyName()
        );
    }

    public ContactResDto updateEntityFromDto(AddContactReqDto dto, Contact contact) {
        if (dto == null || contact == null) return null;
        contact.setName(dto.getName());
        contact.setPhoneNo(dto.getPhoneNo());
        return  toDto(contact);
    }


}
