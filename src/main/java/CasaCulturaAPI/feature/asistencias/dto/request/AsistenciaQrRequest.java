package CasaCulturaAPI.feature.asistencias.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AsistenciaQrRequest {
    @NotBlank(message = "La credencial QR es obligatoria")
    private String credential;
}
