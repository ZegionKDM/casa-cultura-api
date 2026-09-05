package CasaCulturaAPI.feature.catalogo.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class HorarioRequest {
    @NotNull private Long grupoId;
    @NotNull private DayOfWeek dia;
    @NotNull private LocalTime horaInicio;
    @NotNull private LocalTime horaFin;
}
