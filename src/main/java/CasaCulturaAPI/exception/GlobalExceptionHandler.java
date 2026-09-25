package CasaCulturaAPI.exception;

import CasaCulturaAPI.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> resourceNotFound(ResourceNotFoundException ex){

        ApiResponse<?> response =
                new ApiResponse<>(false, ex.getMessage(), null);

        return new ResponseEntity<>(response,HttpStatus.NOT_FOUND);

    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> illegalArgument(IllegalArgumentException ex){

        ApiResponse<?> response =
                new ApiResponse<>(false, ex.getMessage(), null);

        return ResponseEntity.badRequest().body(response);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("La solicitud contiene datos inválidos.");
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> accessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, "No tienes permisos para realizar esta operación.", null));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> dataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        String message = "Conflicto con los datos enviados: ya existe un registro asociado o un dato duplicado.";
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        if (rootMsg != null) {
            if (rootMsg.contains("persona_id")) {
                message = "La persona seleccionada ya tiene una cuenta de usuario asignada.";
            } else if (rootMsg.contains("nombre_usuario")) {
                message = "El nombre de usuario ya está en uso.";
            } else if (rootMsg.contains("matricula")) {
                message = "La matrícula ya está registrada.";
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(false, message, null));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> maxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "La imagen seleccionada supera el tamaño máximo permitido (5 MB).", null));
    }
}
