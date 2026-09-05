package CasaCulturaAPI.feature.alumnos.controller;

import CasaCulturaAPI.feature.alumnos.dto.request.AlumnoRequest;
import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.feature.alumnos.service.AlumnoService;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API + "/alumnos")
@RequiredArgsConstructor
public class AlumnoController {
    private final AlumnoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> crear(@Valid @RequestBody AlumnoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Alumno registrado correctamente.", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listar() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.", service.listar()));
    }

    @GetMapping("/mis-grupos")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<ApiResponse<?>> listarAlumnosDeMisGrupos(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.",
                service.listarPorDocente(authentication.getName())));
    }

    @PostMapping("/{id}/qr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> regenerarQr(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Credencial QR regenerada.",
                service.regenerarCredential(id)));
    }

    @PostMapping("/{id}/credencial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> generarCredencial(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Credencial digital generada.",
                service.regenerarCredential(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Alumno encontrado.", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> actualizar(
            @PathVariable Long id, @Valid @RequestBody AlumnoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Alumno actualizado.", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alumno desactivado.", null));
    }
}
