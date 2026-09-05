package CasaCulturaAPI.feature.portal.controller;

import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.feature.portal.service.StudentPortalService;
import CasaCulturaAPI.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API + "/portal/alumno")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ALUMNO')")
public class StudentPortalController {
    private final StudentPortalService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> dashboard(Authentication authentication) {
        return ok(service.dashboard(authentication.getName()));
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<?>> perfil(Authentication authentication) {
        return ok(service.perfil(authentication.getName()));
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiResponse<?>> horarios(Authentication authentication) {
        return ok(service.horarios(authentication.getName()));
    }

    @GetMapping("/pagos")
    public ResponseEntity<ApiResponse<?>> pagos(Authentication authentication) {
        return ok(service.pagos(authentication.getName()));
    }

    @GetMapping("/asistencias")
    public ResponseEntity<ApiResponse<?>> asistencias(Authentication authentication) {
        return ok(service.asistencias(authentication.getName()));
    }

    private ResponseEntity<ApiResponse<?>> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.", data));
    }
}
