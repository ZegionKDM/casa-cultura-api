package CasaCulturaAPI.feature.portal.dto.request;

import CasaCulturaAPI.shared.entity.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAttendanceItemRequest {
    @NotNull(message = "El id de inscripción es obligatorio.")
    private Long inscripcionId;

    @NotNull(message = "El estado de asistencia es obligatorio.")
    private EstadoAsistencia estado;
}
