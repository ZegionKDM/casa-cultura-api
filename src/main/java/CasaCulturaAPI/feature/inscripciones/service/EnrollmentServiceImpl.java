package CasaCulturaAPI.feature.inscripciones.service;

import CasaCulturaAPI.feature.alumnos.dto.request.*;
import CasaCulturaAPI.feature.asistencias.dto.request.*;
import CasaCulturaAPI.feature.auth.dto.request.*;
import CasaCulturaAPI.feature.catalogo.dto.request.*;
import CasaCulturaAPI.feature.docentes.dto.request.*;
import CasaCulturaAPI.feature.inscripciones.dto.request.*;
import CasaCulturaAPI.feature.pagos.dto.request.*;
import CasaCulturaAPI.shared.dto.*;
import CasaCulturaAPI.feature.alumnos.dto.response.*;
import CasaCulturaAPI.feature.asistencias.dto.response.*;
import CasaCulturaAPI.feature.auth.dto.response.*;
import CasaCulturaAPI.feature.catalogo.dto.response.*;
import CasaCulturaAPI.feature.docentes.dto.response.*;
import CasaCulturaAPI.feature.inscripciones.dto.response.*;
import CasaCulturaAPI.feature.pagos.dto.response.*;
import CasaCulturaAPI.feature.portal.dto.response.*;
import CasaCulturaAPI.feature.reportes.dto.response.*;
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
import CasaCulturaAPI.feature.inscripciones.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final AlumnoRepository alumnoRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;

    @Override @Transactional
    public InscripcionResponse inscribir(InscripcionRequest request) {
        Alumno alumno = alumnoRepository.findById(request.getAlumnoId()).orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado."));
        Grupo grupo = grupoRepository.findById(request.getGrupoId()).orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));
        boolean duplicate = inscripcionRepository.findByAlumnoAndEstado(alumno, EstadoInscripcion.ACTIVA)
                .stream().anyMatch(x -> x.getGrupo().getId().equals(grupo.getId()));
        if (duplicate) throw new IllegalArgumentException("El alumno ya tiene una inscripción activa en este grupo.");
        
        validarNoEmpalmeHorarios(alumno, grupo);

        return toResponse(inscripcionRepository.save(Inscripcion.builder().alumno(alumno).grupo(grupo).build()));
    }

    private void validarNoEmpalmeHorarios(Alumno alumno, Grupo nuevoGrupo) {
        List<Horario> nuevosHorarios = horarioRepository.findByGrupoId(nuevoGrupo.getId());
        if (nuevosHorarios.isEmpty()) {
            return;
        }

        List<Inscripcion> activas = inscripcionRepository.findByAlumnoAndEstado(alumno, EstadoInscripcion.ACTIVA);
        if (activas.isEmpty()) {
            return;
        }

        List<Grupo> gruposActivos = activas.stream().map(Inscripcion::getGrupo).toList();
        List<Horario> horariosExistentes = horarioRepository.findByGrupoIn(gruposActivos);

        for (Horario nuevo : nuevosHorarios) {
            for (Horario existente : horariosExistentes) {
                if (nuevo.getDia().equals(existente.getDia())) {
                    // Overlap condition: start1 < end2 && end1 > start2
                    if (nuevo.getHoraInicio().isBefore(existente.getHoraFin()) && nuevo.getHoraFin().isAfter(existente.getHoraInicio())) {
                        throw new IllegalArgumentException(String.format(
                                "Conflicto de horario (Regla de negocio 2): El taller '%s' (%s %s - %s) se empalma con el taller '%s' (%s %s - %s) en el que el alumno ya está inscrito.",
                                nuevoGrupo.getNombreGrupo(),
                                nuevo.getDia(), nuevo.getHoraInicio(), nuevo.getHoraFin(),
                                existente.getGrupo().getNombreGrupo(),
                                existente.getDia(), existente.getHoraInicio(), existente.getHoraFin()
                        ));
                    }
                }
            }
        }
    }

    @Override @Transactional(readOnly = true)
    public List<InscripcionResponse> listarInscripciones() { return inscripcionRepository.findAll().stream().map(this::toResponse).toList(); }

    @Override @Transactional
    public InscripcionResponse darDeBaja(Long id) {
        Inscripcion inscripcion = inscripcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada."));
        inscripcion.setEstado(EstadoInscripcion.BAJA);
        return toResponse(inscripcionRepository.save(inscripcion));
    }

    @Override @Transactional
    public PagoResponse registrarPago(PagoRequest request) {
        Inscripcion inscripcion = inscripcionRepository.findById(request.getInscripcionId())
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada."));

        Double monto = request.getMonto();
        if (monto == null || monto <= 0) {
            monto = switch (request.getTipoPago()) {
                case INSCRIPCION -> 500.0;
                case MENSUALIDAD -> 400.0;
                case RECARGO -> 150.0;
            };
        }

        java.time.LocalDate fechaPago = request.getFechaPago();
        if (request.getEstado() == EstadoPago.PAGADO && fechaPago == null) {
            fechaPago = java.time.LocalDate.now();
        }

        return toResponse(pagoRepository.save(Pago.builder()
                .inscripcion(inscripcion)
                .tipoPago(request.getTipoPago())
                .periodo(request.getPeriodo())
                .fechaVencimiento(request.getFechaVencimiento())
                .fechaPago(fechaPago)
                .estado(request.getEstado())
                .monto(monto)
                .build()));
    }

    @Override @Transactional(readOnly = true)
    public List<PagoResponse> listarPagos() {
        return pagoRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override @Transactional
    public PagoResponse actualizarEstadoPago(Long id, EstadoPago nuevoEstado) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + id));
        pago.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoPago.PAGADO && pago.getFechaPago() == null) {
            pago.setFechaPago(java.time.LocalDate.now());
        }
        return toResponse(pagoRepository.save(pago));
    }

    private InscripcionResponse toResponse(Inscripcion x) {
        return InscripcionResponse.builder().id(x.getId()).alumnoId(x.getAlumno().getId()).matricula(x.getAlumno().getMatricula())
                .grupoId(x.getGrupo().getId()).grupo(x.getGrupo().getNombreGrupo()).fechaInscripcion(x.getFechaInscripcion()).estado(x.getEstado()).build();
    }
    private PagoResponse toResponse(Pago x) {
        Inscripcion ins = x.getInscripcion();
        Alumno al = ins != null ? ins.getAlumno() : null;
        Persona per = al != null ? al.getPersona() : null;

        String nombreCompleto = "Alumno";
        String matricula = "";
        String grupo = "Taller";

        if (per != null) {
            nombreCompleto = per.getNombre() + " " + per.getApellidoPaterno() +
                    (per.getApellidoMaterno() != null && !per.getApellidoMaterno().isBlank() ? " " + per.getApellidoMaterno() : "");
        }
        if (al != null) {
            matricula = al.getMatricula();
        }
        if (ins != null && ins.getGrupo() != null) {
            grupo = ins.getGrupo().getNombreGrupo();
        }

        Double monto = x.getMonto();
        if (monto == null) {
            monto = switch (x.getTipoPago()) {
                case INSCRIPCION -> 500.0;
                case MENSUALIDAD -> 400.0;
                case RECARGO -> 150.0;
            };
        }

        return PagoResponse.builder()
                .id(x.getId())
                .inscripcionId(ins != null ? ins.getId() : null)
                .tipoPago(x.getTipoPago())
                .periodo(x.getPeriodo())
                .fechaVencimiento(x.getFechaVencimiento())
                .fechaPago(x.getFechaPago())
                .estado(x.getEstado())
                .monto(monto)
                .alumno(nombreCompleto.trim())
                .matricula(matricula)
                .grupo(grupo)
                .build();
    }
}
