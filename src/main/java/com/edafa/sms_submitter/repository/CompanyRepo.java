package com.edafa.sms_submitter.repository;
import com.edafa.sms_submitter.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepo extends JpaRepository<Company, Integer> {
    boolean existsByCompanyName(String name);
    Company findByCompanyName(String name);
    Company findByCompanyId(Integer companyId);
}

