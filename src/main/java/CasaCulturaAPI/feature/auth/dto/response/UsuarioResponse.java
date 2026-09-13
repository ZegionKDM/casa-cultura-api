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
}
