package CasaCulturaAPI.controller;

import CasaCulturaAPI.dto.request.AsignacionDocenteRequest;
import CasaCulturaAPI.dto.response.ApiResponse;
import CasaCulturaAPI.service.interfaces.AsignacionDocenteService;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API + "/asignaciones-docentes")
@RequiredArgsConstructor
public class AsignacionDocenteController {
    private final AsignacionDocenteService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> asignar(@Valid @RequestBody AsignacionDocenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,
                "Docente asignado correctamente.", service.asignar(request)));
    }

    @GetMapping("/grupo/{grupoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listar(@PathVariable Long grupoId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Consulta realizada correctamente.", service.listarPorGrupo(grupoId)));
    }

    @GetMapping("/mis-grupos")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<ApiResponse<?>> listarMisGrupos(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Consulta realizada correctamente.",
                service.listarPorUsuario(authentication.getName())));
    }
}
