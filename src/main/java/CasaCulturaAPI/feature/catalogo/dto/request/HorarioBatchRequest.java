package CasaCulturaAPI.feature.catalogo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioBatchRequest {
    @NotNull(message = "El identificador del grupo es obligatorio.")
    private Long grupoId;

    private List<DayOfWeek> dias;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    private List<HorarioSlotRequest> slots;
}
