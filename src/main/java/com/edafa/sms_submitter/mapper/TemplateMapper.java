package com.edafa.sms_submitter.mapper;
import com.edafa.sms_submitter.dto.TemplateReqDto;
import com.edafa.sms_submitter.dto.TemplateResDto;
import com.edafa.sms_submitter.entity.Company;
import com.edafa.sms_submitter.entity.Template;
import com.edafa.sms_submitter.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {
    public Template toEntity(TemplateReqDto dto, Company company) {
        if (dto == null) return null;
        return new Template(dto.getName(), dto.getBody(), company);
    }
    public TemplateResDto toDto(Template template) {
        if (template == null || template.getCompany() == null) return null;
        return new TemplateResDto(template.getTemplateId(), template.getName(), template.getBody(),
                template.getCompany().getCompanyId(), template.getCompany().getCompanyName());
    }

};
