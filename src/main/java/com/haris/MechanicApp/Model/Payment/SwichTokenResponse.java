package com.haris.MechanicApp.Model.Payment;



import lombok.Data;

@Data
public class SwichTokenResponse {

    private String access_token;
    private String token_type;
    private Integer expires_in;
}
