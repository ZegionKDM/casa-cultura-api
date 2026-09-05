package CasaCulturaAPI.service.impl;

import CasaCulturaAPI.dto.response.AttendanceReportResponse;
import CasaCulturaAPI.entity.EstadoAsistencia;
import CasaCulturaAPI.repository.AsistenciaRepository;
import CasaCulturaAPI.service.interfaces.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final AsistenciaRepository asistenciaRepository;

    @Override
    @Transactional(readOnly = true)
    public AttendanceReportResponse attendanceSummary(LocalDate desde, LocalDate hasta) {
        LocalDate end = hasta == null ? LocalDate.now() : hasta;
        LocalDate start = desde == null ? end : desde;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        List<EstadoAsistencia> states = asistenciaRepository.findByFechaBetween(start, end)
                .stream().map(x -> x.getEstado()).toList();
        return AttendanceReportResponse.builder()
                .desde(start).hasta(end).total(states.size())
                .presentes(states.stream().filter(x -> x == EstadoAsistencia.PRESENTE).count())
                .retardos(states.stream().filter(x -> x == EstadoAsistencia.RETARDO).count())
                .faltas(states.stream().filter(x -> x == EstadoAsistencia.FALTA).count())
                .build();
    }
}
