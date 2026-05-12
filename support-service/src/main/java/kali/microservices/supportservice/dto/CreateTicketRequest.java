package kali.microservices.supportservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kali.microservices.supportservice.entities.Ticket;
import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private Ticket.Priority priority = Ticket.Priority.MEDIUM;
    private Ticket.Category category = Ticket.Category.OTHER;
}