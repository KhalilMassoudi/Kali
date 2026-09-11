package kali.microservices.infrastructureservice.dto;

import lombok.Data;

/**
 * projectId may be null to unassign a resource back to "ungrouped".
 */
@Data
public class AssignProjectRequest {
    private Long projectId;
}
