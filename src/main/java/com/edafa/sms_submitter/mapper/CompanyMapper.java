package com.edafa.sms_submitter.mapper;

import com.edafa.sms_submitter.dto.CompanyReqDto;
import com.edafa.sms_submitter.dto.CompanyResDto;
import com.edafa.sms_submitter.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResDto toDto(Company entity) {
        if (entity == null) {
            return null;
        }
        CompanyResDto dto = new CompanyResDto();
        dto.setCompanyId(entity.getCompanyId());
        dto.setCompanyName(entity.getCompanyName());
        return dto;
    }

    public Company toEntity(CompanyReqDto dto) {
        if (dto == null) {
            return null;
        }
        Company entity = new Company();
        entity.setCompanyName(dto.getCompanyName());
        return entity;
    }
}