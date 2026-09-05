package CasaCulturaAPI.feature.pagos.dto.response;

import CasaCulturaAPI.shared.entity.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class PagoResponse {
    private Long id;
    private Long inscripcionId;
    private TipoPago tipoPago;
    private String periodo;
    private LocalDate fechaVencimiento;
    private LocalDate fechaPago;
    private EstadoPago estado;
}
