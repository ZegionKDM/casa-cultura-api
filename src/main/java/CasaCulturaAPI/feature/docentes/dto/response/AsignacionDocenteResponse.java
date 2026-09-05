package CasaCulturaAPI.feature.docentes.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class AsignacionDocenteResponse {
    private Long id;
    private Long docenteId;
    private Long grupoId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
