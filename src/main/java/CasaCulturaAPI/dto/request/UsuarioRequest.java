package CasaCulturaAPI.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UsuarioRequest {
    @NotNull private Long personaId;
    @NotNull private Long rolId;
    @NotBlank @Size(max = 80) private String nombreUsuario;
    @NotBlank @Size(min = 8, max = 100) private String password;
}
