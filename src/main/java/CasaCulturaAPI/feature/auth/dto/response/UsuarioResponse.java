package CasaCulturaAPI.feature.auth.dto.response;

import CasaCulturaAPI.shared.entity.EstadoRegistro;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UsuarioResponse {
    private Long id;
    private Long personaId;
    private Long rolId;
    private String rol;
    private String nombreUsuario;
    private EstadoRegistro estado;
    private Boolean debeCambiarPassword;

    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String correo;
    private String telefono;
    private String fotoUrl;
    private PersonaDto persona;

    @Data @Builder
    public static class PersonaDto {
        private Long id;
        private String nombre;
        private String apellidoPaterno;
        private String apellidoMaterno;
        private String correo;
        private String telefono;
        private String fotoUrl;
    }
}
