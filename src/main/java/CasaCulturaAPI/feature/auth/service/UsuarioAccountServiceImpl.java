package CasaCulturaAPI.feature.auth.service;

import CasaCulturaAPI.feature.auth.dto.request.UsuarioRequest;
import CasaCulturaAPI.feature.auth.dto.request.PasswordChangeRequest;
import CasaCulturaAPI.feature.auth.dto.response.UsuarioResponse;
import CasaCulturaAPI.shared.entity.*;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.shared.repository.*;
import CasaCulturaAPI.feature.alumnos.repository.*;
import CasaCulturaAPI.feature.asistencias.repository.*;
import CasaCulturaAPI.feature.auth.repository.*;
import CasaCulturaAPI.feature.catalogo.repository.*;
import CasaCulturaAPI.feature.docentes.repository.*;
import CasaCulturaAPI.feature.inscripciones.repository.*;
import CasaCulturaAPI.feature.pagos.repository.*;
import CasaCulturaAPI.feature.alumnos.repository.*;
import CasaCulturaAPI.feature.asistencias.repository.*;
import CasaCulturaAPI.feature.auth.repository.*;
import CasaCulturaAPI.feature.catalogo.repository.*;
import CasaCulturaAPI.feature.docentes.repository.*;
import CasaCulturaAPI.feature.inscripciones.repository.*;
import CasaCulturaAPI.feature.pagos.repository.*;
import CasaCulturaAPI.feature.auth.service.UsuarioAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioAccountServiceImpl implements UsuarioAccountService {
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }
        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));

        Persona persona;
        if (request.getPersonaId() != null) {
            persona = personaRepository.findById(request.getPersonaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada."));
        } else {
            if (request.getNombre() == null || request.getNombre().isBlank()) {
                throw new IllegalArgumentException("El nombre de la persona es obligatorio.");
            }
            if (request.getApellidoPaterno() == null || request.getApellidoPaterno().isBlank()) {
                throw new IllegalArgumentException("El apellido paterno es obligatorio.");
            }
            persona = personaRepository.save(Persona.builder()
                    .nombre(request.getNombre().trim())
                    .apellidoPaterno(request.getApellidoPaterno().trim())
                    .apellidoMaterno(request.getApellidoMaterno() != null && !request.getApellidoMaterno().isBlank()
                            ? request.getApellidoMaterno().trim() : null)
                    .correo(request.getCorreo() != null && !request.getCorreo().isBlank()
                            ? request.getCorreo().trim() : null)
                    .telefono(request.getTelefono() != null && !request.getTelefono().isBlank()
                            ? request.getTelefono().trim() : null)
                    .build());
        }

        boolean mustChange = request.getDebeCambiarPassword() != null
                ? request.getDebeCambiarPassword()
                : ("ALUMNO".equalsIgnoreCase(rol.getNombre()) || "DOCENTE".equalsIgnoreCase(rol.getNombre()));

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .persona(persona)
                .rol(rol)
                .nombreUsuario(request.getNombreUsuario().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .debeCambiarPassword(mustChange)
                .build());
        return toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorNombre(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        usuario.setEstado(EstadoRegistro.INACTIVO);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void cambiarPassword(String nombreUsuario, PasswordChangeRequest request) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es válida.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse toResponse(Usuario x) {
        return UsuarioResponse.builder().id(x.getId()).personaId(x.getPersona().getId())
                .rolId(x.getRol().getId()).rol(x.getRol().getNombre())
                .nombreUsuario(x.getNombreUsuario()).estado(x.getEstado())
                .debeCambiarPassword(Boolean.TRUE.equals(x.getDebeCambiarPassword())).build();
    }
}
