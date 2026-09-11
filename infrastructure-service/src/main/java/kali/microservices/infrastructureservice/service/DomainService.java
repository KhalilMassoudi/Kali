package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateDomainRequest;
import kali.microservices.infrastructureservice.entities.Domain;
import kali.microservices.infrastructureservice.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DomainService {

    private final DomainRepository domainRepository;

    public Domain registerDomain(CreateDomainRequest request) {
        if (domainRepository.existsByName(request.getName())) {
            throw new RuntimeException("Le domaine est déjà enregistré: " + request.getName());
        }

        Domain domain = new Domain();
        domain.setUserId(request.getUserId());
        domain.setName(request.getName());
        domain.setTld(extractTld(request.getName()));
        domain.setStatus(Domain.DomainStatus.ACTIVE);
        domain.setRegisteredAt(LocalDate.now());
        domain.setExpiresAt(LocalDate.now().plusYears(1));
        domain.setNameserver1(request.getNameserver1() != null ? request.getNameserver1() : "ns1.kali-cloud.tn");
        domain.setNameserver2(request.getNameserver2() != null ? request.getNameserver2() : "ns2.kali-cloud.tn");

        return domainRepository.save(domain);
    }

    public List<Domain> getDomainsByUser(Long userId) {
        return domainRepository.findByUserId(userId);
    }

    public List<Domain> getAllDomains() {
        return domainRepository.findAll();
    }

    public Domain getDomainById(Long id) {
        return domainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Domaine non trouvé: " + id));
    }

    public void deleteDomain(Long id) {
        Domain domain = getDomainById(id);
        domain.setStatus(Domain.DomainStatus.DELETED);
        domainRepository.save(domain);
    }

    private String extractTld(String domainName) {
        int lastDot = domainName.lastIndexOf(".");
        return lastDot >= 0 ? domainName.substring(lastDot) : "";
    }
}