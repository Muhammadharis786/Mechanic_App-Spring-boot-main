package com.haris.MechanicApp.Model.Payment;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String appointmentId;
    private Double amount;

    // getters & setters
}