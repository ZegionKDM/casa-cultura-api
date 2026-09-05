package CasaCulturaAPI.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OfertaCursoRequest {
    @NotNull private Long cursoId;
    @NotBlank @Size(max = 80) private String tipo;
    @NotNull private LocalDate fechaInicio;
    @NotNull private LocalDate fechaFin;
}
