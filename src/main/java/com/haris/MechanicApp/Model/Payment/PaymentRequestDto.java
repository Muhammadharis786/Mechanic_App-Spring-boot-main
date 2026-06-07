package com.haris.MechanicApp.Model.Payment;

import lombok.Data;


// PaymentRequestDto.java
@Data
public class PaymentRequestDto {
    private String payeeName;
    private String email;
    private String msisdn;
    private String item;
    private String amount;
}