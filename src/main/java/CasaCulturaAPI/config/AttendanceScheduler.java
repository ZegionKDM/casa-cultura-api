package CasaCulturaAPI.config;

import CasaCulturaAPI.service.interfaces.AsistenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AttendanceScheduler {
    private final AsistenciaService asistenciaService;

    @Scheduled(fixedDelayString = "${attendance.absence-check-ms:60000}")
    public void generateAbsences() {
        asistenciaService.generarFaltas(LocalDate.now());
    }
}
