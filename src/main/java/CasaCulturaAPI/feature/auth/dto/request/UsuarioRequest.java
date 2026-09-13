package CasaCulturaAPI.feature.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UsuarioRequest {
    private Long personaId;

    @Size(max = 80)
    private String nombre;

    @Size(max = 80)
    private String apellidoPaterno;

    @Size(max = 80)
    private String apellidoMaterno;

    @Email
    @Size(max = 160)
    private String correo;

    @Size(max = 20)
    private String telefono;

    @NotNull private Long rolId;
    @NotBlank @Size(max = 80) private String nombreUsuario;
    @NotBlank @Size(min = 8, max = 100) private String password;
    private Boolean debeCambiarPassword;
}
