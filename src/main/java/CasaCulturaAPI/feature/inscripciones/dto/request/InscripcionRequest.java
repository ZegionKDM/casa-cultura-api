package CasaCulturaAPI.feature.inscripciones.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InscripcionRequest {
    @NotNull private Long alumnoId;
    @NotNull private Long grupoId;
}
