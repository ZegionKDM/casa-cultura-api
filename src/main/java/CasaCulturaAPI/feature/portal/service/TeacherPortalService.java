package CasaCulturaAPI.feature.portal.service;

import CasaCulturaAPI.feature.asistencias.dto.response.AsistenciaResponse;
import CasaCulturaAPI.feature.catalogo.dto.response.CatalogResponse;
import CasaCulturaAPI.feature.docentes.dto.response.DocenteResponse;
import CasaCulturaAPI.feature.portal.dto.request.BatchAttendanceRequest;
import CasaCulturaAPI.feature.portal.dto.response.BatchAttendanceResponse;
import CasaCulturaAPI.feature.portal.dto.response.TeacherDashboardResponse;
import CasaCulturaAPI.feature.portal.dto.response.TeacherGroupResponse;
import CasaCulturaAPI.feature.portal.dto.response.TeacherStudentResponse;

import java.time.LocalDate;
import java.util.List;

public interface TeacherPortalService {
    TeacherDashboardResponse dashboard(String nombreUsuario);
    DocenteResponse perfil(String nombreUsuario);
    List<TeacherGroupResponse> misGrupos(String nombreUsuario);
    List<TeacherStudentResponse> alumnosDeGrupo(Long grupoId, String nombreUsuario);
    List<TeacherStudentResponse> todosMisAlumnos(String nombreUsuario);
    List<AsistenciaResponse> asistenciasDeGrupo(Long grupoId, LocalDate fecha, String nombreUsuario);
    BatchAttendanceResponse registrarPaseListaBatch(BatchAttendanceRequest request, String nombreUsuario);
    List<CatalogResponse> horarios(String nombreUsuario);
}
