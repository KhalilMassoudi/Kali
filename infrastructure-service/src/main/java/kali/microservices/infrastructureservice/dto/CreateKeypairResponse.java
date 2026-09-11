package kali.microservices.infrastructureservice.dto;

import kali.microservices.infrastructureservice.entities.ClientKeypair;

/**
 * privateKey is set ONLY when OpenStack generated the key server-side (client omitted its own
 * public key) and is never persisted or returned again after this one response.
 */
public record CreateKeypairResponse(ClientKeypair keypair, String privateKey) {
}
