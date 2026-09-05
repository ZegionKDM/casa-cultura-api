package CasaCulturaAPI.controller;

import CasaCulturaAPI.dto.request.UsuarioRequest;
import CasaCulturaAPI.dto.request.LoginRequest;
import CasaCulturaAPI.dto.request.PasswordChangeRequest;
import CasaCulturaAPI.dto.response.ApiResponse;
import CasaCulturaAPI.service.interfaces.UsuarioAccountService;
import CasaCulturaAPI.service.interfaces.AuthService;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API + "/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsuarioAccountService service;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Autenticación correcta.",
                authService.login(request)));
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Usuario creado correctamente.", service.crear(request)));
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> listUsers() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.", service.listar()));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> deactivateUser(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Usuario desactivado.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> currentUser(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Usuario autenticado.",
                service.obtenerPorNombre(authentication.getName())));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<?>> changePassword(
            Authentication authentication, @Valid @RequestBody PasswordChangeRequest request) {
        service.cambiarPassword(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña actualizada.", null));
    }
}
