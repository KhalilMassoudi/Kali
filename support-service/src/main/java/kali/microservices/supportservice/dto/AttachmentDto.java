package kali.microservices.supportservice.dto;

import kali.microservices.supportservice.entities.TicketAttachment;

import java.time.LocalDateTime;

/** Metadata only — never carries the file bytes, so listing a ticket's attachments stays cheap. */
public record AttachmentDto(
        Long id,
        Long ticketId,
        Long commentId,
        String filename,
        String contentType,
        long size,
        Long uploadedByUserId,
        LocalDateTime createdAt
) {
    public static AttachmentDto from(TicketAttachment a) {
        return new AttachmentDto(a.getId(), a.getTicketId(), a.getCommentId(), a.getFilename(),
                a.getContentType(), a.getSize(), a.getUploadedByUserId(), a.getCreatedAt());
    }
}
