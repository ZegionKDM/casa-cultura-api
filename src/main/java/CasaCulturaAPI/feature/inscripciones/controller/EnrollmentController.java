package CasaCulturaAPI.feature.inscripciones.controller;

import CasaCulturaAPI.feature.alumnos.dto.request.*;
import CasaCulturaAPI.feature.asistencias.dto.request.*;
import CasaCulturaAPI.feature.auth.dto.request.*;
import CasaCulturaAPI.feature.catalogo.dto.request.*;
import CasaCulturaAPI.feature.docentes.dto.request.*;
import CasaCulturaAPI.feature.inscripciones.dto.request.*;
import CasaCulturaAPI.feature.pagos.dto.request.*;
import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.feature.inscripciones.service.EnrollmentService;
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
    @PatchMapping("/inscripciones/{id}/baja")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Inscripción dada de baja correctamente.", service.darDeBaja(id)));
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
    @PatchMapping("/pagos/{id}/estado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> actualizarEstadoPago(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        CasaCulturaAPI.shared.entity.EstadoPago nuevoEstado = CasaCulturaAPI.shared.entity.EstadoPago.valueOf(body.get("estado"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de pago actualizado correctamente.", service.actualizarEstadoPago(id, nuevoEstado)));
    }
}
