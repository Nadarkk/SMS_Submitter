package com.edafa.sms_submitter.repository;

import com.edafa.sms_submitter.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message, Integer> {

    @Query("SELECT DISTINCT m FROM Message m " +
            "LEFT JOIN FETCH m.user " +
            "LEFT JOIN FETCH m.template " +
            "LEFT JOIN Sms s ON s.message.messageId = m.messageId " +
            "WHERE m.user.company.companyId = :companyId " +
            "OR s.contact.company.companyId = :companyId " +
            "ORDER BY m.sendDate DESC")
    List<Message> findAllByCompanyIdOrderBySendDateDesc(@Param("companyId") Integer companyId);

    Long countByUser_Company_CompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT DISTINCT m FROM Message m " +
            "LEFT JOIN FETCH m.user " +
            "LEFT JOIN FETCH m.template " +
            "ORDER BY m.sendDate DESC")
    List<Message> findAllByOrderBySendDateDesc();
}