package CasaCulturaAPI;

import CasaCulturaAPI.shared.entity.Persona;
import CasaCulturaAPI.shared.entity.Rol;
import CasaCulturaAPI.shared.entity.Usuario;
import CasaCulturaAPI.shared.repository.PersonaRepository;
import CasaCulturaAPI.feature.auth.repository.RolRepository;
import CasaCulturaAPI.feature.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {
    private static final String USERNAME = "integration-admin";
    private static final String PASSWORD = "Integration123!";

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PersonaRepository personaRepository;
    @Autowired RolRepository rolRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void createUser() {
        if (usuarioRepository.existsByNombreUsuario(USERNAME)) {
            return;
        }
        Rol role = rolRepository.findByNombre("SUPER_ADMIN")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("SUPER_ADMIN").build()));
        Persona person = personaRepository.save(Persona.builder()
                .nombre("Integration")
                .apellidoPaterno("Admin")
                .build());
        usuarioRepository.save(Usuario.builder()
                .persona(person)
                .rol(role)
                .nombreUsuario(USERNAME)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .build());
    }

    @Test
    void loginReturnsJwtToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"integration-admin","password":"Integration123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.type").value("Bearer"));
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void superAdminCanListUsers() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/v1/auth/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void authenticatedUserCanRegisterStudent() throws Exception {
        String token = loginToken();
        mockMvc.perform(post("/api/v1/alumnos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "apellidoPaterno":"Pérez",
                                  "apellidoMaterno":"López",
                                  "fechaNacimiento":"2012-05-10",
                                  "telefono":"5551234567",
                                  "direccion":"Av. Principal 10",
                                  "correo":"ana@example.com",
                                  "matricula":"INT-ALU-001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.alumno.matricula").value("INT-ALU-001"))
                .andExpect(jsonPath("$.data.qrCredential").isNotEmpty());
    }

    @Test
    void invalidStudentRequestReturnsBadRequest() throws Exception {
        String token = loginToken();
        mockMvc.perform(post("/api/v1/alumnos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","apellidoPaterno":"","matricula":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registerStudentWithoutAddressFailsValidation() throws Exception {
        String token = loginToken();
        mockMvc.perform(post("/api/v1/alumnos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Carlos",
                                  "apellidoPaterno":"Fuentes",
                                  "fechaNacimiento":"2011-03-15",
                                  "telefono":"5559876543",
                                  "correo":"carlos@example.com",
                                  "matricula":"INT-ALU-002"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("direccion")));
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"integration-admin","password":"Integration123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String marker = "\"token\":\"";
        int start = response.indexOf(marker) + marker.length();
        int end = response.indexOf('"', start);
        return response.substring(start, end);
    }
}
