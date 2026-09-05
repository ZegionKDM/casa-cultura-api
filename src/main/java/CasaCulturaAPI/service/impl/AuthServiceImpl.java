package CasaCulturaAPI.service.impl;

import CasaCulturaAPI.dto.request.LoginRequest;
import CasaCulturaAPI.dto.response.TokenResponse;
import CasaCulturaAPI.entity.EstadoRegistro;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.repository.UsuarioRepository;
import CasaCulturaAPI.security.JwtService;
import CasaCulturaAPI.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        var usuario = usuarioRepository.findByNombreUsuario(request.getNombreUsuario())
                .filter(x -> x.getEstado() == EstadoRegistro.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Credenciales inválidas."));
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new ResourceNotFoundException("Credenciales inválidas.");
        }
        return TokenResponse.builder().token(jwtService.generateToken(usuario))
                .type("Bearer").expiresIn(jwtService.getExpirationMillis()).build();
    }
}
