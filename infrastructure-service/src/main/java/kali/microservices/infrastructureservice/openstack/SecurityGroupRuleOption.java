package kali.microservices.infrastructureservice.openstack;

public record SecurityGroupRuleOption(String id, String direction, String protocol,
                                       Integer portMin, Integer portMax, String remoteCidr) {
}
