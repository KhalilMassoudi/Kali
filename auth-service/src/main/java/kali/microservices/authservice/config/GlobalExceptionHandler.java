package kali.microservices.authservice.config;

import kali.microservices.authservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", message, 400));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Erreur inattendue";
        HttpStatus status = resolveStatus(msg);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(toCode(status), msg, status.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "Une erreur interne s'est produite", 500));
    }

    private HttpStatus resolveStatus(String msg) {
        if (msg.contains("Email déjà utilisé")) return HttpStatus.CONFLICT;
        if (msg.contains("incorrect") || msg.contains("invalide") || msg.contains("expiré"))
            return HttpStatus.UNAUTHORIZED;
        if (msg.contains("désactivé") || msg.contains("Accès refusé")) return HttpStatus.FORBIDDEN;
        if (msg.contains("non trouvé")) return HttpStatus.NOT_FOUND;
        return HttpStatus.BAD_REQUEST;
    }

    private String toCode(HttpStatus status) {
        return switch (status) {
            case CONFLICT -> "EMAIL_EXISTS";
            case UNAUTHORIZED -> "AUTH_ERROR";
            case FORBIDDEN -> "ACCESS_DENIED";
            case NOT_FOUND -> "NOT_FOUND";
            default -> "BAD_REQUEST";
        };
    }
}