package CasaCulturaAPI;

import CasaCulturaAPI.feature.alumnos.repository.AlumnoRepository;
import CasaCulturaAPI.feature.asistencias.repository.AsistenciaRepository;
import CasaCulturaAPI.feature.auth.repository.RolRepository;
import CasaCulturaAPI.feature.auth.repository.UsuarioRepository;
import CasaCulturaAPI.feature.catalogo.repository.CategoriaEdadRepository;
import CasaCulturaAPI.feature.catalogo.repository.CursoRepository;
import CasaCulturaAPI.feature.catalogo.repository.GrupoRepository;
import CasaCulturaAPI.feature.catalogo.repository.HorarioRepository;
import CasaCulturaAPI.feature.catalogo.repository.OfertaCursoRepository;
import CasaCulturaAPI.feature.docentes.repository.AsignacionDocenteRepository;
import CasaCulturaAPI.feature.docentes.repository.DocenteRepository;
import CasaCulturaAPI.feature.inscripciones.repository.InscripcionRepository;
import CasaCulturaAPI.feature.portal.dto.request.BatchAttendanceItemRequest;
import CasaCulturaAPI.feature.portal.dto.request.BatchAttendanceRequest;
import CasaCulturaAPI.shared.entity.*;
import CasaCulturaAPI.shared.repository.PersonaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherPortalIntegrationTests {
    private static final String DEFAULT_PASS = "TempPass123!";

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PersonaRepository personaRepository;
    @Autowired RolRepository rolRepository;
    @Autowired DocenteRepository docenteRepository;
    @Autowired AlumnoRepository alumnoRepository;
    @Autowired CursoRepository cursoRepository;
    @Autowired CategoriaEdadRepository categoriaRepository;
    @Autowired OfertaCursoRepository ofertaRepository;
    @Autowired GrupoRepository grupoRepository;
    @Autowired HorarioRepository horarioRepository;
    @Autowired AsignacionDocenteRepository asignacionRepository;
    @Autowired InscripcionRepository inscripcionRepository;
    @Autowired AsistenciaRepository asistenciaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void teacherPortalCompleteFlowTest() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        // 1. Create Role DOCENTE and Teacher User
        Rol rolDocente = rolRepository.findByNombre("DOCENTE")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("DOCENTE").build()));

        Persona teacherPerson = personaRepository.save(Persona.builder()
                .nombre("Profesor").apellidoPaterno("Tlaxiaco " + suffix)
                .correo("profe" + suffix + "@cultura.mx").telefono("9531112233")
                .build());

        Docente teacher = docenteRepository.save(Docente.builder()
                .persona(teacherPerson)
                .especialidad("Música Tradicional")
                .build());

        String teacherUsername = "docente-" + suffix;
        Usuario teacherUser = usuarioRepository.save(Usuario.builder()
                .persona(teacherPerson)
                .rol(rolDocente)
                .nombreUsuario(teacherUsername)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASS))
                .debeCambiarPassword(true) // User must change password on first login
                .build());

        assertTrue(teacherUser.getDebeCambiarPassword());

        // 2. Setup Course, Group and Schedule
        Curso curso = cursoRepository.save(Curso.builder().nombre("Guitarra Clásica " + suffix).build());
        CategoriaEdad categoria = categoriaRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoriaRepository.save(CategoriaEdad.builder().nombre("Adultos").build()));
        OfertaCurso oferta = ofertaRepository.save(OfertaCurso.builder()
                .curso(curso).tipo("Semestral 2026").fechaInicio(LocalDate.now().minusMonths(1))
                .fechaFin(LocalDate.now().plusMonths(5)).build());
        Grupo grupoAsignado = grupoRepository.save(Grupo.builder()
                .oferta(oferta).categoria(categoria).nombreGrupo("Guitarra A - " + suffix).build());
        Grupo otroGrupo = grupoRepository.save(Grupo.builder()
                .oferta(oferta).categoria(categoria).nombreGrupo("Guitarra B - " + suffix).build());

        Horario horario = horarioRepository.save(Horario.builder()
                .grupo(grupoAsignado)
                .dia(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(16, 0))
                .horaFin(LocalTime.of(18, 0))
                .build());

        // Assign teacher to grupoAsignado only
        asignacionRepository.save(AsignacionDocente.builder()
                .docente(teacher).grupo(grupoAsignado).fechaInicio(LocalDate.now().minusMonths(1)).build());

        // 3. Create Students and Enrollments
        Persona alumnoPerson1 = personaRepository.save(Persona.builder().nombre("Juan").apellidoPaterno("Pérez " + suffix).build());
        Alumno alumno1 = alumnoRepository.save(Alumno.builder()
                .persona(alumnoPerson1)
                .matricula("ALU-1-" + suffix)
                .qrCredentialHash(CasaCulturaAPI.util.CredentialHash.sha256("ALU-1-" + suffix))
                .build());
        Inscripcion inscripcion1 = inscripcionRepository.save(Inscripcion.builder().alumno(alumno1).grupo(grupoAsignado).estado(EstadoInscripcion.ACTIVA).build());

        Persona alumnoPerson2 = personaRepository.save(Persona.builder().nombre("María").apellidoPaterno("López " + suffix).build());
        Alumno alumno2 = alumnoRepository.save(Alumno.builder()
                .persona(alumnoPerson2)
                .matricula("ALU-2-" + suffix)
                .qrCredentialHash(CasaCulturaAPI.util.CredentialHash.sha256("ALU-2-" + suffix))
                .build());
        Inscripcion inscripcion2 = inscripcionRepository.save(Inscripcion.builder().alumno(alumno2).grupo(grupoAsignado).estado(EstadoInscripcion.ACTIVA).build());

        // 4. Authenticate as Teacher
        String token = login(teacherUsername, DEFAULT_PASS);

        // 5. Test GET /dashboard
        mockMvc.perform(get("/api/v1/portal/docente/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.docente.especialidad").value("Música Tradicional"))
                .andExpect(jsonPath("$.data.estadisticas.totalGrupos").value(1))
                .andExpect(jsonPath("$.data.estadisticas.totalAlumnos").value(2))
                .andExpect(jsonPath("$.data.grupos[0].id").value(grupoAsignado.getId()));

        // 6. Test GET /grupos/{grupoId}/alumnos
        mockMvc.perform(get("/api/v1/portal/docente/grupos/" + grupoAsignado.getId() + "/alumnos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 7. Test Batch Attendance taking: POST /grupos/{grupoId}/asistencias/batch
        LocalDate today = LocalDate.now();
        String batchJson = """
                {
                    "grupoId": %d,
                    "horarioId": %d,
                    "fecha": "%s",
                    "asistencias": [
                        {"inscripcionId": %d, "estado": "PRESENTE"},
                        {"inscripcionId": %d, "estado": "RETARDO"}
                    ]
                }
                """.formatted(grupoAsignado.getId(), horario.getId(), today, inscripcion1.getId(), inscripcion2.getId());

        mockMvc.perform(post("/api/v1/portal/docente/grupos/" + grupoAsignado.getId() + "/asistencias/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProcesadas").value(2))
                .andExpect(jsonPath("$.data.presentes").value(1))
                .andExpect(jsonPath("$.data.retardos").value(1));

        // 8. Test GET /grupos/{grupoId}/asistencias
        mockMvc.perform(get("/api/v1/portal/docente/grupos/" + grupoAsignado.getId() + "/asistencias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 9. Security restriction: Teacher cannot access an unassigned group
        mockMvc.perform(get("/api/v1/portal/docente/grupos/" + otroGrupo.getId() + "/alumnos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String marker = "\"token\":\"";
        int start = response.indexOf(marker) + marker.length();
        return response.substring(start, response.indexOf('"', start));
    }
}
