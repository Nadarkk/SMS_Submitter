package com.edafa.sms_submitter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "sms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sms {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SMS_ID")
    private Integer smsId;

    @Column(name ="Sim_ID" , nullable = true, unique = true)
    private String simId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private SmsStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Message_ID", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Contact_ID", nullable = false)
    private Contact contact;

    public Sms(SmsStatus status, Message message, Contact contact) {
        this.status = status;
        this.message = message;
        this.contact = contact;
    }
}