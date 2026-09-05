package CasaCulturaAPI.feature.pagos.dto.request;

import CasaCulturaAPI.shared.entity.EstadoPago;
import CasaCulturaAPI.shared.entity.TipoPago;
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
