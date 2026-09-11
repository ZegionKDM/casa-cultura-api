package CasaCulturaAPI.feature.alumnos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlumnoUserCredentialResponse {
    private Long alumnoId;
    private String matricula;
    private String nombreUsuario;
    private String passwordTemporal;
}
