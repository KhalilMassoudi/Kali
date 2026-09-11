package kali.microservices.infrastructureservice.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Plain, stable JSON shape for a page of results — deliberately not returning Spring Data's
 * Page/PageImpl directly from a controller, since its Pageable/Sort fields don't serialize
 * cleanly by default and can throw mid-response.
 */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
