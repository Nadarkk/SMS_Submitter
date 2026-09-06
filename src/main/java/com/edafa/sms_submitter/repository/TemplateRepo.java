package com.edafa.sms_submitter.repository;

import com.edafa.sms_submitter.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface TemplateRepo extends JpaRepository<Template, Integer> {
    boolean existsTemplateByNameAndCompanyCompanyId(String name, Integer companyId);
    List<Template> findByCompanyCompanyId(Integer companyId);
    long countByCompany_CompanyId(Integer companyId);
}
