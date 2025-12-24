package com.haris.MechanicApp.Model.Questions;


import com.haris.MechanicApp.Model.Verification.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table (name = "questions")
public class Questions {
    @Id
    private int id;
    private String text;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private Questions parentQuestion;
    @Column(name = "service_charge")
    private BigDecimal servicecharges;

    public Questions(String text, Questions parentQuestion, BigDecimal servicecharges) {
        this.text = text;
        this.parentQuestion = parentQuestion;
        this.servicecharges = servicecharges;
    }

    public BigDecimal getServicecharges() {
        return servicecharges;
    }

    public void setServicecharges(BigDecimal servicecharges) {
        this.servicecharges = servicecharges;
    }

    public Questions getParentQuestion() {
        return parentQuestion;
    }

    public void setParentQuestion(Questions parentQuestion) {
        this.parentQuestion = parentQuestion;
    }

    public Questions() {

    }

    public Questions(String text, int id) {
        this.text = text;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


}
