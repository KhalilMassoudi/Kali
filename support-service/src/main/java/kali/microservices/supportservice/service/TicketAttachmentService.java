package kali.microservices.supportservice.service;

import kali.microservices.supportservice.dto.AttachmentDto;
import kali.microservices.supportservice.entities.TicketAttachment;
import kali.microservices.supportservice.repository.TicketAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TicketAttachmentService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf", "text/plain"
    );

    private final TicketAttachmentRepository attachmentRepository;

    public TicketAttachment upload(Long ticketId, Long commentId, Long uploaderId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier trop volumineux (5 Mo max)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de fichier non autorisé (images, PDF ou texte uniquement)");
        }

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(ticketId);
        attachment.setCommentId(commentId);
        attachment.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier");
        attachment.setContentType(contentType);
        attachment.setSize(file.getSize());
        attachment.setUploadedByUserId(uploaderId);
        try {
            attachment.setData(file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erreur de lecture du fichier");
        }

        return attachmentRepository.save(attachment);
    }

    public List<AttachmentDto> listForTicket(Long ticketId) {
        return attachmentRepository.findSummariesByTicketId(ticketId);
    }

    public TicketAttachment get(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable"));
    }
}
