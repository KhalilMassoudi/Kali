package kali.microservices.supportservice.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Stored as a DB blob (no filesystem/object-storage dependency for this app). */
@Entity
@Table(name = "ticket_attachments")
@Data
@NoArgsConstructor
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    /** Null when attached at ticket-creation time rather than to a specific reply. */
    private Long commentId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private Long uploadedByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
