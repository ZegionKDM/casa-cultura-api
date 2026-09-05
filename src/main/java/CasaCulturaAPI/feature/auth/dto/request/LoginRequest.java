package CasaCulturaAPI.feature.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank private String nombreUsuario;
    @NotBlank private String password;
}
