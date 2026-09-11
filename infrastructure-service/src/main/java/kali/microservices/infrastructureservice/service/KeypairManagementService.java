package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateKeypairRequest;
import kali.microservices.infrastructureservice.dto.CreateKeypairResponse;
import kali.microservices.infrastructureservice.entities.ClientKeypair;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.KeypairDetails;
import kali.microservices.infrastructureservice.repository.ClientKeypairRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeypairManagementService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-zA-Z0-9-]");

    private final ClientKeypairRepository repository;
    private final CloudProviderFactory cloudProviderFactory;

    public CreateKeypairResponse create(CreateKeypairRequest request) {
        String openstackName = "u" + request.getUserId() + "-" + sanitize(request.getName());

        KeypairDetails details = cloudProviderFactory.getProvider()
                .createKeypair(openstackName, request.getPublicKey());

        ClientKeypair keypair = new ClientKeypair();
        keypair.setUserId(request.getUserId());
        keypair.setDisplayName(request.getName());
        keypair.setOpenstackName(openstackName);
        keypair.setPublicKey(details.publicKey());
        keypair.setFingerprint(details.fingerprint());
        keypair = repository.save(keypair);

        // privateKey is only non-null when OpenStack generated it (client sent no public key)
        return new CreateKeypairResponse(keypair, details.privateKey());
    }

    public List<ClientKeypair> getByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<ClientKeypair> getAll() {
        return repository.findAll();
    }

    public ClientKeypair getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paire de clés introuvable: " + id));
    }

    public void delete(Long id) {
        ClientKeypair keypair = getById(id);
        try {
            cloudProviderFactory.getProvider().deleteKeypair(keypair.getOpenstackName());
        } catch (Exception e) {
            log.error("Failed to delete OpenStack keypair {}: {}", keypair.getOpenstackName(), e.getMessage());
            throw new RuntimeException("Échec de la suppression de la paire de clés sur OpenStack: " + e.getMessage(), e);
        }
        repository.deleteById(id);
    }

    private String sanitize(String name) {
        return NON_ALNUM.matcher(name.trim().toLowerCase()).replaceAll("-");
    }
}
