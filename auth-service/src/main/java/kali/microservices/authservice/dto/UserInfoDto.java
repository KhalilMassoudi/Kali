package kali.microservices.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfoDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}