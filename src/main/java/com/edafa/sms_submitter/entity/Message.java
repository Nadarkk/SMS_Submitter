package com.edafa.sms_submitter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "Message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Message_ID")
    private Integer messageId;

    @Column(name = "Send_Date", nullable = false)
    private Instant sendDate;

    @Column(name = "Sender", nullable = false, length = 50)
    private String sender;

    @Column(name = "Body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name="Segment_Count", nullable = false)
    @Positive
    private int segmentCount=1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Template_ID", nullable = true)
    private Template template;

    public Message(Instant sendDate, String sender, String body, User user, Template template) {
        this.sendDate = sendDate;
        this.sender = sender;
        this.body = body;
        this.user = user;
        this.template=template;
    }
}