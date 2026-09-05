package CasaCulturaAPI.feature.asistencias.controller;

import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaQrRequest;
import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaManualRequest;
import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.feature.asistencias.service.AsistenciaService;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping(Constants.API + "/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {
    private final AsistenciaService service;

    @PostMapping("/qr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> registrar(@Valid @RequestBody AsistenciaQrRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Asistencia registrada correctamente.",
                service.registrarPorQr(request)));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR', 'DOCENTE')")
    public ResponseEntity<ApiResponse<?>> registrarManual(
            @Valid @RequestBody AsistenciaManualRequest request,
            Authentication authentication) {
        boolean docente = authentication.getAuthorities().stream()
                .anyMatch(x -> x.getAuthority().equals("ROLE_DOCENTE"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Asistencia manual registrada.",
                service.registrarManual(request, authentication.getName(), docente)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listar(
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) Long alumnoId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.",
                service.listar(fecha, alumnoId)));
    }

    @GetMapping("/mis-grupos")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<ApiResponse<?>> listarDeMisGrupos(
            @RequestParam(required = false) LocalDate fecha,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.",
                service.listarPorDocente(fecha, authentication.getName())));
    }

    @PostMapping("/faltas")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> generarFaltas(
            @RequestParam(required = false) LocalDate fecha) {
        LocalDate targetDate = fecha == null ? LocalDate.now() : fecha;
        int generated = service.generarFaltas(targetDate);
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Faltas generadas correctamente.", generated));
    }
}
