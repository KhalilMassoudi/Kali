package kali.microservices.infrastructureservice.openstack;

import java.util.List;

public record ServerGroupDetails(String id, String name, List<String> policies, List<String> members) {
}
