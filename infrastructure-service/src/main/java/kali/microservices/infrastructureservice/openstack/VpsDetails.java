package kali.microservices.infrastructureservice.openstack;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VpsDetails {
    private String externalId;
    private String name;
    private String status;
    private String ipAddress;
    private Integer ram;
    private String os;
    private String region;
}
