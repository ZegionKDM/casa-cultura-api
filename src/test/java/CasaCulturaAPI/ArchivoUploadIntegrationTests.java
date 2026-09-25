package CasaCulturaAPI;

import CasaCulturaAPI.feature.alumnos.repository.AlumnoRepository;
import CasaCulturaAPI.feature.auth.repository.RolRepository;
import CasaCulturaAPI.feature.auth.repository.UsuarioRepository;
import CasaCulturaAPI.shared.entity.Alumno;
import CasaCulturaAPI.shared.entity.EstadoRegistro;
import CasaCulturaAPI.shared.entity.Persona;
import CasaCulturaAPI.shared.entity.Rol;
import CasaCulturaAPI.shared.entity.Usuario;
import CasaCulturaAPI.shared.repository.PersonaRepository;
import CasaCulturaAPI.util.CredentialHash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArchivoUploadIntegrationTests {

    private static final String PASSWORD = "AdminPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private AlumnoRepository alumnoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void subirFotoAlumnoConExito() throws Exception {
        String token = crearUsuarioSupervisorYObtenerToken();

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "alumno-rostro.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10}
        );

        mockMvc.perform(multipart("/api/v1/archivos/fotos/alumno")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url", containsString("/uploads/fotos/alumnos/foto_")))
                .andExpect(jsonPath("$.data.url", containsString(".jpg")));
    }

    @Test
    void subirFotoFormatoInvalidoRetornaBadRequest() throws Exception {
        String token = crearUsuarioSupervisorYObtenerToken();

        MockMultipartFile badFile = new MockMultipartFile(
                "archivo",
                "documento.txt",
                "text/plain",
                "este es un archivo de texto".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/archivos/fotos/alumno")
                        .file(badFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Solo se admiten imágenes")));
    }

    @Test
    void actualizarFotoAlumnoDirectamente() throws Exception {
        String token = crearUsuarioSupervisorYObtenerToken();
        String suffix = String.valueOf(System.nanoTime());

        Persona persona = personaRepository.save(Persona.builder()
                .nombre("AlumnoFoto")
                .apellidoPaterno("Prueba" + suffix)
                .fechaNacimiento(LocalDate.of(2005, 5, 15))
                .correo("alumno." + suffix + "@test.com")
                .telefono("9531234567")
                .direccion("Domicilio Test")
                .fotoUrl(null)
                .build());

        String rawCred = "cred-" + suffix;
        Alumno alumno = alumnoRepository.save(Alumno.builder()
                .persona(persona)
                .matricula("ALU-FOTO-" + suffix)
                .qrCredentialHash(CredentialHash.sha256(rawCred))
                .estado(EstadoRegistro.ACTIVO)
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "nueva-foto.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        mockMvc.perform(multipart("/api/v1/alumnos/{id}/foto", alumno.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url", containsString("/uploads/fotos/alumnos/foto_")))
                .andExpect(jsonPath("$.data.url", containsString(".png")));

        Persona actualizada = personaRepository.findById(persona.getId()).orElseThrow();
        assertNotNull(actualizada.getFotoUrl());
        assertTrue(actualizada.getFotoUrl().startsWith("/uploads/fotos/alumnos/"));
        assertTrue(actualizada.getFotoUrl().endsWith(".png"));
    }

    private String crearUsuarioSupervisorYObtenerToken() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Rol rol = rolRepository.findByNombre("SUPERVISOR")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("SUPERVISOR").descripcion("Supervisor").build()));

        Persona persona = personaRepository.save(Persona.builder()
                .nombre("Supervisor")
                .apellidoPaterno("Foto" + suffix)
                .correo("supervisor." + suffix + "@test.com")
                .telefono("9530001122")
                .direccion("Calle Test")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build());

        String username = "sup_foto_" + suffix;
        usuarioRepository.save(Usuario.builder()
                .persona(persona)
                .rol(rol)
                .nombreUsuario(username)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .estado(EstadoRegistro.ACTIVO)
                .debeCambiarPassword(false)
                .build());

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
}
