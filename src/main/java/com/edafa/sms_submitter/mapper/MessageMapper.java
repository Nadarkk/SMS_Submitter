package com.edafa.sms_submitter.mapper;

import com.edafa.sms_submitter.dto.MessageReqDto;
import com.edafa.sms_submitter.dto.MessageResDto;
import com.edafa.sms_submitter.dto.SmsResDto;
import com.edafa.sms_submitter.entity.Message;
import com.edafa.sms_submitter.entity.Template;
import com.edafa.sms_submitter.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class MessageMapper {

    public Message toEntity(MessageReqDto dto, User user, Template template, Instant sendDate) {
        if (dto == null || user == null) return null;
        return new Message(sendDate, dto.getSender(), dto.getBody(), user, template);
    }

    public MessageResDto toDto(Message message, List<SmsResDto> recipients) {
        if (message == null || recipients == null) return null;

        Integer userId = null;
        String firstName = "Unknown";
        String lastName = "User";

        if (message.getUser() != null) {
            try {
                userId = message.getUser().getUserId();
                firstName = message.getUser().getFirstName();
                lastName = message.getUser().getLastName();
            } catch (EntityNotFoundException e) {
                // Triggered when foreign key user_id exists in message table but record is missing in users table
                firstName = "Deleted";
                lastName = "User";
            }
        }

        Integer templateId = message.getTemplate() != null ? message.getTemplate().getTemplateId() : null;
        String templateName = message.getTemplate() != null ? message.getTemplate().getName() : null;

        return new MessageResDto(
                message.getMessageId(),
                message.getSendDate(),
                message.getSender(),
                message.getBody(),
                message.getSegmentCount(),
                userId,
                firstName,
                lastName,
                templateId,
                templateName,
                recipients
        );
    }
}