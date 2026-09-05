package CasaCulturaAPI;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleScopeIntegrationTests {
    private static final String PASSWORD = "Teacher123!";

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PersonaRepository personaRepository;
    @Autowired RolRepository rolRepository;
    @Autowired DocenteRepository docenteRepository;
    @Autowired AlumnoRepository alumnoRepository;
    @Autowired CursoRepository cursoRepository;
    @Autowired CategoriaEdadRepository categoriaRepository;
    @Autowired OfertaCursoRepository ofertaRepository;
    @Autowired GrupoRepository grupoRepository;
    @Autowired AsignacionDocenteRepository asignacionRepository;
    @Autowired InscripcionRepository inscripcionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void teacherOnlySeesStudentsFromAssignedGroups() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Rol role = rolRepository.findByNombre("DOCENTE")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("DOCENTE").build()));
        Persona teacherPerson = personaRepository.save(Persona.builder()
                .nombre("Teacher").apellidoPaterno(suffix).build());
        Docente teacher = docenteRepository.save(Docente.builder().persona(teacherPerson).build());
        String username = "teacher-" + suffix;
        usuarioRepository.save(Usuario.builder()
                .persona(teacherPerson).rol(role).nombreUsuario(username)
                .passwordHash(passwordEncoder.encode(PASSWORD)).build());

        Grupo assignedGroup = createGroup("ASSIGNED-" + suffix);
        Grupo otherGroup = createGroup("OTHER-" + suffix);
        asignacionRepository.save(AsignacionDocente.builder()
                .docente(teacher).grupo(assignedGroup).fechaInicio(LocalDate.now()).build());

        Alumno assignedStudent = createStudent("ASSIGNED-" + suffix);
        Alumno otherStudent = createStudent("OTHER-" + suffix);
        inscripcionRepository.save(Inscripcion.builder().alumno(assignedStudent).grupo(assignedGroup).build());
        inscripcionRepository.save(Inscripcion.builder().alumno(otherStudent).grupo(otherGroup).build());

        String token = login(username);
        mockMvc.perform(get("/api/v1/alumnos/mis-grupos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].matricula").value("ASSIGNED-" + suffix));
    }

    private String login(String username) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String marker = "\"token\":\"";
        int start = response.indexOf(marker) + marker.length();
        return response.substring(start, response.indexOf('"', start));
    }

    private Alumno createStudent(String matricula) {
        Persona person = personaRepository.save(Persona.builder()
                .nombre("Student").apellidoPaterno(matricula).build());
        return alumnoRepository.save(Alumno.builder()
                .persona(person).matricula(matricula)
                .qrCredentialHash(CasaCulturaAPI.util.CredentialHash.sha256(matricula))
                .build());
    }

    private Grupo createGroup(String name) {
        Curso course = cursoRepository.save(Curso.builder().nombre("COURSE-" + name).build());
        CategoriaEdad category = categoriaRepository.save(CategoriaEdad.builder()
                .nombre("CATEGORY-" + name).build());
        OfertaCurso offer = ofertaRepository.save(OfertaCurso.builder()
                .curso(course).tipo("Test").fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusDays(30)).build());
        return grupoRepository.save(Grupo.builder()
                .oferta(offer).categoria(category).nombreGrupo(name).build());
    }
}
