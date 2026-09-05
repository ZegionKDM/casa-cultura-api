package CasaCulturaAPI.dto.request;

import CasaCulturaAPI.entity.EstadoPago;
import CasaCulturaAPI.entity.TipoPago;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PagoRequest {
    @NotNull private Long inscripcionId;
    @NotNull private TipoPago tipoPago;
    private String periodo;
    private LocalDate fechaVencimiento;
    private LocalDate fechaPago;
    @NotNull private EstadoPago estado;
}
