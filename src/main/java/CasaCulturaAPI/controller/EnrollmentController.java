package CasaCulturaAPI.controller;

import CasaCulturaAPI.dto.request.*;
import CasaCulturaAPI.dto.response.ApiResponse;
import CasaCulturaAPI.service.interfaces.EnrollmentService;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API)
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService service;

    @PostMapping("/inscripciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> inscribir(@Valid @RequestBody InscripcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Inscripción creada correctamente.", service.inscribir(request)));
    }
    @GetMapping("/inscripciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listarInscripciones() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.", service.listarInscripciones()));
    }
    @PostMapping("/pagos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> registrarPago(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Pago registrado correctamente.", service.registrarPago(request)));
    }
    @GetMapping("/pagos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> listarPagos() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.", service.listarPagos()));
    }
}
