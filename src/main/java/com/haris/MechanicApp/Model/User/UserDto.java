package com.haris.MechanicApp.Model.User;

import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String password;
    private String email;
    private String userimgurl;
    private String username;
    private String phonenumber;

}
