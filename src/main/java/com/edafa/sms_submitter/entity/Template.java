package com.edafa.sms_submitter.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Template {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Template_ID")
    private Integer templateId;

    @Column(name = "Template_Name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "Template_Body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public Template(String name, String body, Company company) {
        this.name = name;
        this.body = body;
        this.company = company;
    }
}