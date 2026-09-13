package CasaCulturaAPI.feature.portal.controller;

import CasaCulturaAPI.feature.portal.dto.request.BatchAttendanceRequest;
import CasaCulturaAPI.feature.portal.service.TeacherPortalService;
import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping(Constants.API + "/portal/docente")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCENTE', 'SUPER_ADMIN', 'ADMIN')")
public class TeacherPortalController {
    private final TeacherPortalService service;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<?>> dashboard(Authentication authentication) {
        return ok(service.dashboard(authentication.getName()));
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<?>> perfil(Authentication authentication) {
        return ok(service.perfil(authentication.getName()));
    }

    @GetMapping("/grupos")
    public ResponseEntity<ApiResponse<?>> misGrupos(Authentication authentication) {
        return ok(service.misGrupos(authentication.getName()));
    }

    @GetMapping("/grupos/{grupoId}/alumnos")
    public ResponseEntity<ApiResponse<?>> alumnosDeGrupo(
            @PathVariable Long grupoId, Authentication authentication) {
        return ok(service.alumnosDeGrupo(grupoId, authentication.getName()));
    }

    @GetMapping("/alumnos")
    public ResponseEntity<ApiResponse<?>> todosMisAlumnos(Authentication authentication) {
        return ok(service.todosMisAlumnos(authentication.getName()));
    }

    @GetMapping("/grupos/{grupoId}/asistencias")
    public ResponseEntity<ApiResponse<?>> asistenciasDeGrupo(
            @PathVariable Long grupoId,
            @RequestParam(required = false) LocalDate fecha,
            Authentication authentication) {
        return ok(service.asistenciasDeGrupo(grupoId, fecha, authentication.getName()));
    }

    @PostMapping("/grupos/{grupoId}/asistencias/batch")
    public ResponseEntity<ApiResponse<?>> registrarPaseListaBatch(
            @PathVariable Long grupoId,
            @Valid @RequestBody BatchAttendanceRequest request,
            Authentication authentication) {
        request.setGrupoId(grupoId);
        return ok(service.registrarPaseListaBatch(request, authentication.getName()));
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiResponse<?>> horarios(Authentication authentication) {
        return ok(service.horarios(authentication.getName()));
    }

    private ResponseEntity<ApiResponse<?>> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Operación realizada correctamente.", data));
    }
}
