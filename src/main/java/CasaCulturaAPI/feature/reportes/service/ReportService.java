package CasaCulturaAPI.feature.reportes.service;

import CasaCulturaAPI.feature.reportes.dto.response.AttendanceReportResponse;

import java.time.LocalDate;

public interface ReportService {
    AttendanceReportResponse attendanceSummary(LocalDate desde, LocalDate hasta);
}
