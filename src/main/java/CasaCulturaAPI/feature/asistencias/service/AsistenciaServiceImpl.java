package CasaCulturaAPI.feature.asistencias.service;

import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaQrRequest;
import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaManualRequest;
import CasaCulturaAPI.feature.asistencias.dto.response.AsistenciaResponse;
import CasaCulturaAPI.shared.entity.*;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.shared.repository.*;
import CasaCulturaAPI.feature.alumnos.repository.*;
import CasaCulturaAPI.feature.asistencias.repository.*;
import CasaCulturaAPI.feature.auth.repository.*;
import CasaCulturaAPI.feature.catalogo.repository.*;
import CasaCulturaAPI.feature.docentes.repository.*;
import CasaCulturaAPI.feature.inscripciones.repository.*;
import CasaCulturaAPI.feature.pagos.repository.*;
import CasaCulturaAPI.feature.alumnos.repository.*;
import CasaCulturaAPI.feature.asistencias.repository.*;
import CasaCulturaAPI.feature.auth.repository.*;
import CasaCulturaAPI.feature.catalogo.repository.*;
import CasaCulturaAPI.feature.docentes.repository.*;
import CasaCulturaAPI.feature.inscripciones.repository.*;
import CasaCulturaAPI.feature.pagos.repository.*;
import CasaCulturaAPI.feature.asistencias.service.AsistenciaService;
import CasaCulturaAPI.util.CredentialHash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsistenciaServiceImpl implements AsistenciaService {
    private final AlumnoRepository alumnoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final HorarioRepository horarioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public AsistenciaResponse registrarPorQr(AsistenciaQrRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Alumno alumno = alumnoRepository.findByQrCredentialHashAndEstado(
                        CredentialHash.sha256(request.getCredential()), EstadoRegistro.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Credencial QR inválida o inactiva."));

        for (Inscripcion inscripcion : inscripcionRepository.findByAlumnoAndEstado(alumno, EstadoInscripcion.ACTIVA)) {
            var horarios = horarioRepository.findByGrupoAndDiaAndHoraInicioLessThanEqualAndHoraFinGreaterThan(
                    inscripcion.getGrupo(), now.getDayOfWeek(), now.toLocalTime(), now.toLocalTime());
            if (horarios.isEmpty()) {
                continue;
            }
            Horario horario = horarios.getFirst();
            var existing = asistenciaRepository.findByInscripcionAndHorarioAndFecha(
                    inscripcion, horario, now.toLocalDate());
            Asistencia asistencia = existing.orElseGet(() -> asistenciaRepository.save(Asistencia.builder()
                            .inscripcion(inscripcion)
                            .horario(horario)
                            .fecha(now.toLocalDate())
                            .horaRegistro(now)
                            .estado(now.toLocalTime().isAfter(horario.getHoraInicio().plusMinutes(15))
                                    ? EstadoAsistencia.RETARDO : EstadoAsistencia.PRESENTE)
                            .build()));
            return toResponse(asistencia);
        }
        throw new IllegalArgumentException("No existe un horario activo para la credencial en este momento.");
    }

    @Override
    @Transactional
    public AsistenciaResponse registrarManual(
            AsistenciaManualRequest request, String nombreUsuario, boolean docente) {
        Inscripcion inscripcion = inscripcionRepository.findById(request.getInscripcionId())
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada."));
        Horario horario = horarioRepository.findById(request.getHorarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado."));
        if (!horario.getGrupo().getId().equals(inscripcion.getGrupo().getId())) {
            throw new IllegalArgumentException("El horario no pertenece al grupo de la inscripción.");
        }
        if (inscripcion.getEstado() != EstadoInscripcion.ACTIVA) {
            throw new IllegalArgumentException("La inscripción no está activa.");
        }
        if (docente && !isAssignedToGroup(nombreUsuario, inscripcion.getGrupo().getId(), request.getFecha())) {
            throw new IllegalArgumentException("El docente no está asignado a este grupo.");
        }
        Asistencia asistencia = asistenciaRepository.findByInscripcionAndHorarioAndFecha(
                        inscripcion, horario, request.getFecha())
                .orElseGet(() -> Asistencia.builder()
                        .inscripcion(inscripcion)
                        .horario(horario)
                        .fecha(request.getFecha())
                        .build());
        asistencia.setHoraRegistro(LocalDateTime.now());
        asistencia.setEstado(request.getEstado());
        return toResponse(asistenciaRepository.save(asistencia));
    }

    private boolean isAssignedToGroup(String nombreUsuario, Long grupoId, LocalDate date) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        Docente docente = docenteRepository.findByPersonaId(usuario.getPersona().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un perfil docente."));
        return asignacionRepository.findByDocenteOrderByFechaInicioDesc(docente).stream()
                .anyMatch(x -> x.getGrupo().getId().equals(grupoId)
                        && !x.getFechaInicio().isAfter(date)
                        && (x.getFechaFin() == null || !x.getFechaFin().isBefore(date)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listar(LocalDate fecha, Long alumnoId) {
        List<Asistencia> asistencias;
        if (alumnoId != null) {
            asistencias = asistenciaRepository.findByInscripcionAlumnoIdOrderByFechaDescHoraRegistroDesc(alumnoId);
        } else if (fecha != null) {
            asistencias = asistenciaRepository.findByFechaOrderByHoraRegistroAsc(fecha);
        } else {
            asistencias = asistenciaRepository.findAllByOrderByFechaDescHoraRegistroDesc();
        }
        return asistencias.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listarPorDocente(LocalDate fecha, String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        Docente docente = docenteRepository.findByPersonaId(usuario.getPersona().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un perfil docente."));
        LocalDate targetDate = fecha == null ? LocalDate.now() : fecha;
        Set<Long> groupIds = asignacionRepository.findByDocenteOrderByFechaInicioDesc(docente).stream()
                .filter(x -> !x.getFechaInicio().isAfter(targetDate)
                        && (x.getFechaFin() == null || !x.getFechaFin().isBefore(targetDate)))
                .map(x -> x.getGrupo().getId())
                .collect(Collectors.toSet());
        return asistenciaRepository.findByFechaOrderByHoraRegistroAsc(targetDate).stream()
                .filter(x -> groupIds.contains(x.getInscripcion().getGrupo().getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public int generarFaltas(LocalDate fecha) {
        LocalDateTime now = LocalDateTime.now();
        List<Horario> horarios = horarioRepository.findByDiaAndHoraFinLessThanEqual(
                fecha.getDayOfWeek(),
                fecha.equals(now.toLocalDate()) ? now.toLocalTime() : LocalTime.MAX);
        int generated = 0;
        for (Horario horario : horarios) {
            for (Inscripcion inscripcion : inscripcionRepository.findByGrupoAndEstado(
                    horario.getGrupo(), EstadoInscripcion.ACTIVA)) {
                if (asistenciaRepository.findByInscripcionAndHorarioAndFecha(
                        inscripcion, horario, fecha).isPresent()) {
                    continue;
                }
                asistenciaRepository.save(Asistencia.builder()
                        .inscripcion(inscripcion)
                        .horario(horario)
                        .fecha(fecha)
                        .horaRegistro(LocalDateTime.of(fecha, horario.getHoraFin()))
                        .estado(EstadoAsistencia.FALTA)
                        .build());
                generated++;
            }
        }
        return generated;
    }

    private AsistenciaResponse toResponse(Asistencia asistencia) {
        Alumno alumno = asistencia.getInscripcion().getAlumno();
        Persona persona = alumno.getPersona();
        String nombreCompleto = persona.getNombre() + " " + persona.getApellidoPaterno() +
                (persona.getApellidoMaterno() != null && !persona.getApellidoMaterno().isBlank()
                        ? " " + persona.getApellidoMaterno() : "");
        return AsistenciaResponse.builder()
                .id(asistencia.getId())
                .fotoUrl(persona.getFotoUrl())
                .matricula(alumno.getMatricula())
                .alumno(nombreCompleto.trim())
                .grupo(asistencia.getInscripcion().getGrupo().getNombreGrupo())
                .fecha(asistencia.getFecha())
                .horaRegistro(asistencia.getHoraRegistro())
                .estado(asistencia.getEstado())
                .build();
    }
}
