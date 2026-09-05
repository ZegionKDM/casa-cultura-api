package CasaCulturaAPI.feature.reportes.controller;

import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.feature.reportes.service.ReportService;
import CasaCulturaAPI.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping(Constants.API + "/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
public class ReportController {
    private final ReportService service;

    @GetMapping("/asistencias/resumen")
    public ResponseEntity<ApiResponse<?>> attendanceSummary(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.",
                service.attendanceSummary(desde, hasta)));
    }
}
