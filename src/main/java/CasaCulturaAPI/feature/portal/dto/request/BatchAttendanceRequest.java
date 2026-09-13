package CasaCulturaAPI.feature.portal.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAttendanceRequest {
    @NotNull(message = "El id del grupo es obligatorio.")
    private Long grupoId;

    @NotNull(message = "El id del horario es obligatorio.")
    private Long horarioId;

    @NotNull(message = "La fecha es obligatoria.")
    private LocalDate fecha;

    @NotEmpty(message = "La lista de asistencias no puede estar vacía.")
    @Valid
    private List<BatchAttendanceItemRequest> asistencias;
}
