package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.response.AttendanceReportResponse;

import java.time.LocalDate;

public interface ReportService {
    AttendanceReportResponse attendanceSummary(LocalDate desde, LocalDate hasta);
}
