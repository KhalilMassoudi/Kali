package kali.microservices.infrastructureservice.openstack;

/**
 * privateKey is populated ONLY on the response of a create call that triggered server-side
 * generation — Nova never stores it and never returns it again. Never persist this field.
 */
public record KeypairDetails(String name, String publicKey, String privateKey, String fingerprint) {
}
