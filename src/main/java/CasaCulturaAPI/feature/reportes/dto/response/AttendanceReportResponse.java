package CasaCulturaAPI.feature.reportes.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AttendanceReportResponse {
    private LocalDate desde;
    private LocalDate hasta;
    private long total;
    private long presentes;
    private long retardos;
    private long faltas;
}
