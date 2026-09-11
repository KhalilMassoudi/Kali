package kali.microservices.infrastructureservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddSecurityGroupRuleRequest {
    @NotBlank private String direction;   // ingress | egress
    @NotBlank private String protocol;    // tcp | udp | icmp
    private Integer portMin;
    private Integer portMax;
    private String cidr = "0.0.0.0/0";
}
