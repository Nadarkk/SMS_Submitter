package com.edafa.sms_submitter.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Contact")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Contact_ID")
    private Integer contactId;

    @Column(name = "Contact_Name", nullable = false, length = 50)
    private String name;

    @Column(name = "Contact_No", nullable = false, length = 20)
    private String phoneNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Company_ID", nullable = false)
    private Company company;

    public Contact(String name, String phoneNo, Company company) {
        this.name = name;
        this.phoneNo = phoneNo;
        this.company = company;
    }
}