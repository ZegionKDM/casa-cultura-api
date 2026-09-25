package CasaCulturaAPI.feature.portal.service;

import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.feature.asistencias.dto.response.AsistenciaResponse;
import CasaCulturaAPI.feature.asistencias.repository.AsistenciaRepository;
import CasaCulturaAPI.feature.auth.repository.UsuarioRepository;
import CasaCulturaAPI.feature.catalogo.dto.response.CatalogResponse;
import CasaCulturaAPI.feature.catalogo.repository.HorarioRepository;
import CasaCulturaAPI.feature.docentes.dto.response.DocenteResponse;
import CasaCulturaAPI.feature.docentes.repository.AsignacionDocenteRepository;
import CasaCulturaAPI.feature.docentes.repository.DocenteRepository;
import CasaCulturaAPI.feature.inscripciones.repository.InscripcionRepository;
import CasaCulturaAPI.feature.portal.dto.request.BatchAttendanceItemRequest;
import CasaCulturaAPI.feature.portal.dto.request.BatchAttendanceRequest;
import CasaCulturaAPI.feature.portal.dto.response.*;
import CasaCulturaAPI.shared.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherPortalServiceImpl implements TeacherPortalService {
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final HorarioRepository horarioRepository;
    private final AsistenciaRepository asistenciaRepository;

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardResponse dashboard(String nombreUsuario) {
        Docente docente = findDocenteByUsername(nombreUsuario);
        LocalDate today = LocalDate.now();
        List<Grupo> activeGroups = getActiveAssignedGroups(docente, today);
        List<TeacherGroupResponse> groupResponses = activeGroups.stream()
                .map(this::toGroupResponse)
                .toList();

        Set<Long> activeGroupIds = activeGroups.stream().map(Grupo::getId).collect(Collectors.toSet());

        // Students in active groups
        List<Inscripcion> allActiveEnrollments = inscripcionRepository.findAll().stream()
                .filter(i -> activeGroupIds.contains(i.getGrupo().getId()) && i.getEstado() == EstadoInscripcion.ACTIVA)
                .toList();
        long uniqueStudentsCount = allActiveEnrollments.stream()
                .map(i -> i.getAlumno().getId())
                .distinct()
                .count();

        // Schedules
        List<Horario> allSchedules = activeGroups.stream()
                .flatMap(g -> horarioRepository.findByGrupoId(g.getId()).stream())
                .toList();
        List<CatalogResponse> scheduleResponses = allSchedules.stream()
                .map(this::toScheduleResponse)
                .toList();

        // Today's classes
        DayOfWeek todayDow = today.getDayOfWeek();
        List<CatalogResponse> todaysClasses = allSchedules.stream()
                .filter(h -> h.getDia() == todayDow)
                .map(this::toScheduleResponse)
                .toList();

        // Attendance stats
        List<Asistencia> allGroupAttendances = asistenciaRepository.findAll().stream()
                .filter(a -> activeGroupIds.contains(a.getInscripcion().getGrupo().getId()))
                .toList();

        long totalAttendances = allGroupAttendances.size();
        long presentCount = allGroupAttendances.stream()
                .filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE || a.getEstado() == EstadoAsistencia.RETARDO)
                .count();
        double attendanceRate = totalAttendances > 0 ? ((double) presentCount / totalAttendances) * 100.0 : 100.0;

        int attendancesToday = (int) allGroupAttendances.stream()
                .filter(a -> a.getFecha().equals(today))
                .count();

        List<AsistenciaResponse> recentAttendances = allGroupAttendances.stream()
                .sorted(Comparator.comparing(Asistencia::getFecha).thenComparing(Asistencia::getHoraRegistro).reversed())
                .limit(10)
                .map(this::toAttendanceResponse)
                .toList();

        TeacherStatsResponse stats = TeacherStatsResponse.builder()
                .totalGrupos(activeGroups.size())
                .totalAlumnos((int) uniqueStudentsCount)
                .clasesHoy(todaysClasses.size())
                .porcentajeAsistenciaGeneral(Math.round(attendanceRate * 10.0) / 10.0)
                .asistenciasRegistradasHoy(attendancesToday)
                .build();

        return TeacherDashboardResponse.builder()
                .docente(toDocenteResponse(docente))
                .estadisticas(stats)
                .grupos(groupResponses)
                .clasesHoy(todaysClasses)
                .horariosSemana(scheduleResponses)
                .asistenciasRecientes(recentAttendances)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DocenteResponse perfil(String nombreUsuario) {
        return toDocenteResponse(findDocenteByUsername(nombreUsuario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherGroupResponse> misGrupos(String nombreUsuario) {
        Docente docente = findDocenteByUsername(nombreUsuario);
        return getActiveAssignedGroups(docente, LocalDate.now()).stream()
                .map(this::toGroupResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherStudentResponse> alumnosDeGrupo(Long grupoId, String nombreUsuario) {
        Docente docente = findDocenteByUsername(nombreUsuario);
        validateTeacherAssignedToGroup(docente, grupoId, LocalDate.now());

        List<Inscripcion> enrollments = inscripcionRepository.findByGrupoId(grupoId);
        return enrollments.stream()
                .map(this::toStudentResponse)
                .sorted(Comparator.comparing(TeacherStudentResponse::getNombreCompleto))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherStudentResponse> todosMisAlumnos(String nombreUsuario) {
        Docente docente = findDocenteByUsername(nombreUsuario);
        List<Grupo> groups = getActiveAssignedGroups(docente, LocalDate.now());
        Set<Long> groupIds = groups.stream().map(Grupo::getId).collect(Collectors.toSet());

        return inscripcionRepository.findAll().stream()
                .filter(i -> groupIds.contains(i.getGrupo().getId()))
                .map(this::toStudentResponse)
                .sorted(Comparator.comparing(TeacherStudentResponse::getNombreCompleto))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> asistenciasDeGrupo(Long grupoId, LocalDate fecha, String nombreUsuario) {
        Docente docente = findDocenteByUsername(nombreUsuario);
        validateTeacherAssignedToGroup(docente, grupoId, fecha != null ? fecha : LocalDate.now());

        List<Asistencia> list = asistenciaRepository.findAllByOrderByFechaDescHoraRegistroDesc().stream()
                .filter(a -> a.getInscripcion().getGrupo().getId().equals(grupoId))
                .filter(a -> fecha == null || a.getFecha().equals(fecha))
                .toList();

        return list.stream().map(this::toAttendanceResponse).toList();
    }

    @Override
    @Transactional
    public BatchAttendanceResponse registrarPaseListaBatch(BatchAttendanceRequest request, String nombreUsuario) {
        Docente docente = findDocenteByUsername(nombreUsuario);
        validateTeacherAssignedToGroup(docente, request.getGrupoId(), request.getFecha());

        Horario horario = horarioRepository.findById(request.getHorarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado."));

        if (!horario.getGrupo().getId().equals(request.getGrupoId())) {
            throw new IllegalArgumentException("El horario no pertenece al grupo especificado.");
        }

        int presentes = 0;
        int retardos = 0;
        int faltas = 0;
        int total = 0;

        LocalDateTime now = LocalDateTime.now();

        for (BatchAttendanceItemRequest item : request.getAsistencias()) {
            Inscripcion inscripcion = inscripcionRepository.findById(item.getInscripcionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada ID: " + item.getInscripcionId()));

            if (!inscripcion.getGrupo().getId().equals(request.getGrupoId())) {
                throw new IllegalArgumentException("La inscripción ID " + item.getInscripcionId() + " no pertenece al grupo " + request.getGrupoId());
            }

            if (inscripcion.getEstado() != EstadoInscripcion.ACTIVA) {
                // Rule: una inscripción dada de baja no debe generar asistencias
                continue;
            }

            Asistencia asistencia = asistenciaRepository.findByInscripcionAndHorarioAndFecha(
                            inscripcion, horario, request.getFecha())
                    .orElseGet(() -> Asistencia.builder()
                            .inscripcion(inscripcion)
                            .horario(horario)
                            .fecha(request.getFecha())
                            .build());

            asistencia.setHoraRegistro(now);
            asistencia.setEstado(item.getEstado());
            asistenciaRepository.save(asistencia);

            total++;
            if (item.getEstado() == EstadoAsistencia.PRESENTE) presentes++;
            else if (item.getEstado() == EstadoAsistencia.RETARDO) retardos++;
            else if (item.getEstado() == EstadoAsistencia.FALTA) faltas++;
        }

        return BatchAttendanceResponse.builder()
                .totalProcesadas(total)
                .presentes(presentes)
                .retardos(retardos)
                .faltas(faltas)
                .mensaje("Pase de lista registrado correctamente para " + total + " alumno(s).")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogResponse> horarios(String nombreUsuario) {
        Docente docente = findDocenteByUsername(nombreUsuario);
        List<Grupo> groups = getActiveAssignedGroups(docente, LocalDate.now());
        return groups.stream()
                .flatMap(g -> horarioRepository.findByGrupoId(g.getId()).stream())
                .map(this::toScheduleResponse)
                .toList();
    }

    // --- Helper private methods ---

    private Docente findDocenteByUsername(String username) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        return docenteRepository.findByPersonaId(usuario.getPersona().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no cuenta con perfil docente registrado."));
    }

    private List<Grupo> getActiveAssignedGroups(Docente docente, LocalDate date) {
        return asignacionRepository.findByDocenteOrderByFechaInicioDesc(docente).stream()
                .filter(a -> !a.getFechaInicio().isAfter(date) && (a.getFechaFin() == null || !a.getFechaFin().isBefore(date)))
                .map(AsignacionDocente::getGrupo)
                .distinct()
                .toList();
    }

    private void validateTeacherAssignedToGroup(Docente docente, Long grupoId, LocalDate date) {
        boolean isAssigned = asignacionRepository.findByDocenteOrderByFechaInicioDesc(docente).stream()
                .anyMatch(a -> a.getGrupo().getId().equals(grupoId)
                        && !a.getFechaInicio().isAfter(date)
                        && (a.getFechaFin() == null || !a.getFechaFin().isBefore(date)));
        if (!isAssigned) {
            throw new AccessDeniedException("Acceso denegado: el docente no tiene asignación activa en este grupo.");
        }
    }

    private TeacherGroupResponse toGroupResponse(Grupo g) {
        List<CatalogResponse> schedules = horarioRepository.findByGrupoId(g.getId()).stream()
                .map(this::toScheduleResponse)
                .toList();

        long activeStudents = inscripcionRepository.countByGrupoAndEstado(g, EstadoInscripcion.ACTIVA);

        return TeacherGroupResponse.builder()
                .id(g.getId())
                .nombreGrupo(g.getNombreGrupo())
                .cursoId(g.getOferta().getCurso().getId())
                .curso(g.getOferta().getCurso().getNombre())
                .categoriaId(g.getCategoria().getId())
                .categoria(g.getCategoria().getNombre())
                .periodo(g.getOferta().getTipo())
                .fechaInicio(g.getOferta().getFechaInicio())
                .fechaFin(g.getOferta().getFechaFin())
                .totalAlumnos((int) activeStudents)
                .estado(g.getEstado())
                .horarios(schedules)
                .build();
    }

    private TeacherStudentResponse toStudentResponse(Inscripcion i) {
        Alumno alumno = i.getAlumno();
        Persona persona = alumno.getPersona();
        String fullName = (persona.getNombre() + " " +
                (persona.getApellidoPaterno() != null ? persona.getApellidoPaterno() : "") + " " +
                (persona.getApellidoMaterno() != null ? persona.getApellidoMaterno() : "")).trim();

        List<Asistencia> attendances = asistenciaRepository.findByInscripcionAlumnoIdOrderByFechaDescHoraRegistroDesc(alumno.getId()).stream()
                .filter(a -> a.getInscripcion().getId().equals(i.getId()))
                .toList();

        int presents = (int) attendances.stream().filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE).count();
        int delays = (int) attendances.stream().filter(a -> a.getEstado() == EstadoAsistencia.RETARDO).count();
        int absences = (int) attendances.stream().filter(a -> a.getEstado() == EstadoAsistencia.FALTA).count();
        int total = attendances.size();

        double rate = total > 0 ? ((double) (presents + delays) / total) * 100.0 : 100.0;

        return TeacherStudentResponse.builder()
                .inscripcionId(i.getId())
                .alumnoId(alumno.getId())
                .matricula(alumno.getMatricula())
                .nombre(persona.getNombre())
                .apellidoPaterno(persona.getApellidoPaterno())
                .apellidoMaterno(persona.getApellidoMaterno())
                .nombreCompleto(fullName)
                .correo(persona.getCorreo())
                .telefono(persona.getTelefono())
                .fotoUrl(persona.getFotoUrl())
                .grupoId(i.getGrupo().getId())
                .nombreGrupo(i.getGrupo().getNombreGrupo())
                .fechaInscripcion(i.getFechaInscripcion())
                .estadoInscripcion(i.getEstado())
                .totalAsistencias(presents)
                .totalRetardos(delays)
                .totalFaltas(absences)
                .porcentajeAsistencia(Math.round(rate * 10.0) / 10.0)
                .build();
    }

    private DocenteResponse toDocenteResponse(Docente d) {
        Persona p = d.getPersona();
        return DocenteResponse.builder()
                .id(d.getId())
                .personaId(p.getId())
                .nombre(p.getNombre())
                .apellidoPaterno(p.getApellidoPaterno())
                .apellidoMaterno(p.getApellidoMaterno())
                .telefono(p.getTelefono())
                .direccion(p.getDireccion())
                .correo(p.getCorreo())
                .fotoUrl(p.getFotoUrl())
                .especialidad(d.getEspecialidad())
                .estado(d.getEstado())
                .build();
    }

    private CatalogResponse toScheduleResponse(Horario h) {
        return CatalogResponse.builder()
                .id(h.getId())
                .grupoId(h.getGrupo().getId())
                .nombreGrupo(h.getGrupo().getNombreGrupo())
                .dia(h.getDia())
                .horaInicio(h.getHoraInicio())
                .horaFin(h.getHoraFin())
                .build();
    }

    private AsistenciaResponse toAttendanceResponse(Asistencia a) {
        Alumno alumno = a.getInscripcion().getAlumno();
        Persona p = alumno.getPersona();
        return AsistenciaResponse.builder()
                .id(a.getId())
                .matricula(alumno.getMatricula())
                .alumno(p.getNombre() + " " + (p.getApellidoPaterno() != null ? p.getApellidoPaterno() : ""))
                .grupo(a.getInscripcion().getGrupo().getNombreGrupo())
                .fecha(a.getFecha())
                .horaRegistro(a.getHoraRegistro())
                .estado(a.getEstado())
                .fotoUrl(p.getFotoUrl())
                .build();
    }
}
