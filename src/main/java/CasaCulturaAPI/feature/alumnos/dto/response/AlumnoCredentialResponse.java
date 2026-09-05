package CasaCulturaAPI.feature.alumnos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlumnoCredentialResponse {
    private AlumnoResponse alumno;
    private String qrCredential;
}
