package com.edafa.sms_submitter.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class    Company {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Company_ID")
    private Integer companyId;

    @Column(name = "Company_Name", nullable = false, length = 100)
    private String companyName;

    public Company(String companyName) {
        this.companyName = companyName;
    }
}