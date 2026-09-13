package CasaCulturaAPI.feature.portal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAttendanceResponse {
    private int totalProcesadas;
    private int presentes;
    private int retardos;
    private int faltas;
    private String mensaje;
}
