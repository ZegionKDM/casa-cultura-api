package CasaCulturaAPI.feature.docentes.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AsignacionDocenteRequest {
    @NotNull private Long docenteId;
    @NotNull private Long grupoId;
    @NotNull private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
