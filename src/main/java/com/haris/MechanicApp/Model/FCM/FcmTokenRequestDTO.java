package com.haris.MechanicApp.Model.FCM;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FcmTokenRequestDTO {

    private String fcmToken;
    private String userType;
    private Long userId;
    private String platform;
}
