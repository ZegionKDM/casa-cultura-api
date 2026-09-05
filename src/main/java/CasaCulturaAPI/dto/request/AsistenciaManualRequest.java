package CasaCulturaAPI.dto.request;

import CasaCulturaAPI.entity.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AsistenciaManualRequest {
    @NotNull
    private Long inscripcionId;

    @NotNull
    private Long horarioId;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private EstadoAsistencia estado;
}
