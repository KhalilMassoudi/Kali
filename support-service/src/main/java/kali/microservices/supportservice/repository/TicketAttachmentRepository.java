package kali.microservices.supportservice.repository;

import kali.microservices.supportservice.dto.AttachmentDto;
import kali.microservices.supportservice.entities.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    /** Constructor-expression projection — never pulls the blob bytes into memory for a listing. */
    @Query("SELECT new kali.microservices.supportservice.dto.AttachmentDto(" +
            "a.id, a.ticketId, a.commentId, a.filename, a.contentType, a.size, a.uploadedByUserId, a.createdAt) " +
            "FROM TicketAttachment a WHERE a.ticketId = :ticketId ORDER BY a.createdAt ASC")
    List<AttachmentDto> findSummariesByTicketId(@Param("ticketId") Long ticketId);
}
