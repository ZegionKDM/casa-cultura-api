package CasaCulturaAPI.config;

import CasaCulturaAPI.entity.*;
import CasaCulturaAPI.repository.*;
import CasaCulturaAPI.util.CredentialHash;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DemoDataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String ADMIN_USERNAME = "demo-superadmin";
    private static final String SUPERVISOR_USERNAME = "demo-supervisor";

    private final AlumnoRepository alumnoRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final CategoriaEdadRepository categoriaRepository;
    private final CursoRepository cursoRepository;
    private final DocenteRepository docenteRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final OfertaCursoRepository ofertaRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner createDemoData(
            @Value("${demo-data.enabled:${DEMO_DATA_ENABLED:false}}") boolean enabled) {
        return args -> {
            if (enabled) {
                seed();
            }
        };
    }

    private void seed() {
        if (usuarioRepository.existsByNombreUsuario(ADMIN_USERNAME)) {
            log.info("Datos de demostración ya existen; no se insertaron duplicados.");
            log.info("QR de prueba: QR-ALUMNO-001, QR-ALUMNO-002, QR-ALUMNO-003, QR-ALUMNO-004, QR-ALUMNO-005");
            return;
        }

        Rol superAdmin = role("SUPER_ADMIN", "Administrador total del sistema");
        Rol supervisor = role("SUPERVISOR", "Supervisor de actividades");
        usuario(ADMIN_USERNAME, "Demo", "Superadmin", superAdmin, "DemoAdmin123!");
        usuario(SUPERVISOR_USERNAME, "Demo", "Supervisor", supervisor, "DemoSupervisor123!");

        List<Docente> docentes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Persona persona = persona("Profesor " + i, "Demo", "profesor" + i + "@example.com");
            docentes.add(docenteRepository.save(Docente.builder()
                    .persona(persona)
                    .especialidad(i % 2 == 0 ? "Música" : "Artes")
                    .build()));
        }

        List<Alumno> alumnos = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String credential = "QR-ALUMNO-00" + i;
            Persona persona = persona("Alumno " + i, "Demo", null);
            alumnos.add(alumnoRepository.save(Alumno.builder()
                    .persona(persona)
                    .matricula("DEMO-" + String.format("%03d", i))
                    .qrCredentialHash(CredentialHash.sha256(credential))
                    .build()));
        }

        CategoriaEdad categoria = categoriaRepository.save(CategoriaEdad.builder()
                .nombre("Todas las edades").build());
        Curso musica = cursoRepository.save(Curso.builder().nombre("Música").build());
        Curso pintura = cursoRepository.save(Curso.builder().nombre("Pintura").build());
        LocalDate today = LocalDate.now();
        OfertaCurso ofertaMusica = ofertaRepository.save(OfertaCurso.builder()
                .curso(musica).tipo("Regular").fechaInicio(today).fechaFin(today.plusMonths(6)).build());
        OfertaCurso ofertaPintura = ofertaRepository.save(OfertaCurso.builder()
                .curso(pintura).tipo("Regular").fechaInicio(today).fechaFin(today.plusMonths(6)).build());
        Grupo grupoMusica = grupoRepository.save(Grupo.builder()
                .oferta(ofertaMusica).categoria(categoria).nombreGrupo("Música - Demo").build());
        Grupo grupoPintura = grupoRepository.save(Grupo.builder()
                .oferta(ofertaPintura).categoria(categoria).nombreGrupo("Pintura - Demo").build());

        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
        LocalTime start = LocalTime.now().minusMinutes(30);
        LocalTime end = LocalTime.now().plusMinutes(90);
        horarioRepository.save(Horario.builder().grupo(grupoMusica).dia(currentDay)
                .horaInicio(start).horaFin(end).build());
        horarioRepository.save(Horario.builder().grupo(grupoPintura).dia(currentDay)
                .horaInicio(start).horaFin(end).build());
        horarioRepository.save(Horario.builder().grupo(grupoMusica).dia(currentDay.plus(1))
                .horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(12, 0)).build());
        horarioRepository.save(Horario.builder().grupo(grupoPintura).dia(currentDay.plus(1))
                .horaInicio(LocalTime.of(12, 0)).horaFin(LocalTime.of(14, 0)).build());

        asignacionRepository.save(AsignacionDocente.builder().docente(docentes.get(0))
                .grupo(grupoMusica).fechaInicio(today).build());
        asignacionRepository.save(AsignacionDocente.builder().docente(docentes.get(1))
                .grupo(grupoPintura).fechaInicio(today).build());

        for (int i = 0; i < alumnos.size(); i++) {
            inscripcionRepository.save(Inscripcion.builder()
                    .alumno(alumnos.get(i))
                    .grupo(i < 3 ? grupoMusica : grupoPintura)
                    .fechaInscripcion(today)
                    .build());
        }

        log.info("Datos de demostración creados correctamente.");
        log.info("Usuarios: {} / DemoAdmin123! y {} / DemoSupervisor123!", ADMIN_USERNAME, SUPERVISOR_USERNAME);
        log.info("QR de prueba: QR-ALUMNO-001, QR-ALUMNO-002, QR-ALUMNO-003, QR-ALUMNO-004, QR-ALUMNO-005");
        log.info("Los horarios de hoy están activos durante la ventana actual para probar asistencia.");
    }

    private Rol role(String name, String description) {
        return rolRepository.findByNombre(name)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(name).descripcion(description).build()));
    }

    private Persona persona(String nombre, String apellido, String correo) {
        return personaRepository.save(Persona.builder()
                .nombre(nombre)
                .apellidoPaterno(apellido)
                .correo(correo)
                .build());
    }

    private void usuario(String username, String nombre, String apellido, Rol role, String password) {
        Persona persona = persona(nombre, apellido, username + "@example.com");
        usuarioRepository.save(Usuario.builder()
                .persona(persona)
                .rol(role)
                .nombreUsuario(username)
                .passwordHash(passwordEncoder.encode(password))
                .estado(EstadoRegistro.ACTIVO)
                .build());
    }
}
