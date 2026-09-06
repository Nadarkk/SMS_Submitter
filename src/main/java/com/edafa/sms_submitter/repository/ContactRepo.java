package com.edafa.sms_submitter.repository;

import com.edafa.sms_submitter.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepo extends JpaRepository<Contact, Integer> {
    boolean existsByPhoneNo(String number);
    boolean existsContactByPhoneNoAndCompanyCompanyId(String phoneNo, Integer companyId);
    boolean existsContactByPhoneNoAndCompanyCompanyIdAndContactIdNot(String phoneNo, Integer companyId, Integer contactId);

    Contact findContactByName(String name);

    long countByCompany_CompanyId(Integer companyId);
    List<Contact> findAllByCompanyCompanyId(Integer companyId);
}
