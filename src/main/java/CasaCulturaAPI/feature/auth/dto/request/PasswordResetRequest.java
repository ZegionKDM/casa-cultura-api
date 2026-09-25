package CasaCulturaAPI.feature.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank
    @Size(min = 8, max = 100)
    private String passwordNueva;

    private Boolean debeCambiarPassword;
}

