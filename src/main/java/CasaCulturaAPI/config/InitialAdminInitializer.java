package CasaCulturaAPI.config;

import CasaCulturaAPI.shared.entity.*;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class InitialAdminInitializer {
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner createInitialAdmin(
            @Value("${initial-admin.username:${INITIAL_ADMIN_USERNAME:}}") String username,
            @Value("${initial-admin.password:${INITIAL_ADMIN_PASSWORD:}}") String password) {
        return args -> {
            if (username.isBlank() || password.isBlank() || usuarioRepository.existsByNombreUsuario(username)) {
                return;
            }
            if (password.length() < 8) {
                throw new IllegalArgumentException("initial-admin.password debe tener al menos 8 caracteres.");
            }
            Rol role = rolRepository.findByNombre("SUPER_ADMIN")
                    .orElseGet(() -> rolRepository.save(Rol.builder().nombre("SUPER_ADMIN")
                            .descripcion("Administrador del sistema").build()));
            Persona person = personaRepository.save(Persona.builder()
                    .nombre("Administrador")
                    .apellidoPaterno("Inicial")
                    .build());
            usuarioRepository.save(Usuario.builder()
                    .persona(person)
                    .rol(role)
                    .nombreUsuario(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .build());
        };
    }
}
