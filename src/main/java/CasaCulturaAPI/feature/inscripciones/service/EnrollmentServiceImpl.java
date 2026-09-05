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
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;

    @Override @Transactional
    public InscripcionResponse inscribir(InscripcionRequest request) {
        Alumno alumno = alumnoRepository.findById(request.getAlumnoId()).orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado."));
        Grupo grupo = grupoRepository.findById(request.getGrupoId()).orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));
        boolean duplicate = inscripcionRepository.findByAlumnoAndEstado(alumno, EstadoInscripcion.ACTIVA)
                .stream().anyMatch(x -> x.getGrupo().getId().equals(grupo.getId()));
        if (duplicate) throw new IllegalArgumentException("El alumno ya tiene una inscripción activa en este grupo.");
        return toResponse(inscripcionRepository.save(Inscripcion.builder().alumno(alumno).grupo(grupo).build()));
    }

    @Override @Transactional(readOnly = true)
    public List<InscripcionResponse> listarInscripciones() { return inscripcionRepository.findAll().stream().map(this::toResponse).toList(); }

    @Override @Transactional
    public PagoResponse registrarPago(PagoRequest request) {
        Inscripcion inscripcion = inscripcionRepository.findById(request.getInscripcionId())
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada."));
        return toResponse(pagoRepository.save(Pago.builder().inscripcion(inscripcion).tipoPago(request.getTipoPago())
                .periodo(request.getPeriodo()).fechaVencimiento(request.getFechaVencimiento())
                .fechaPago(request.getFechaPago()).estado(request.getEstado()).build()));
    }

    @Override @Transactional(readOnly = true)
    public List<PagoResponse> listarPagos() { return pagoRepository.findAll().stream().map(this::toResponse).toList(); }

    private InscripcionResponse toResponse(Inscripcion x) {
        return InscripcionResponse.builder().id(x.getId()).alumnoId(x.getAlumno().getId()).matricula(x.getAlumno().getMatricula())
                .grupoId(x.getGrupo().getId()).grupo(x.getGrupo().getNombreGrupo()).fechaInscripcion(x.getFechaInscripcion()).estado(x.getEstado()).build();
    }
    private PagoResponse toResponse(Pago x) {
        return PagoResponse.builder().id(x.getId()).inscripcionId(x.getInscripcion().getId()).tipoPago(x.getTipoPago())
                .periodo(x.getPeriodo()).fechaVencimiento(x.getFechaVencimiento()).fechaPago(x.getFechaPago()).estado(x.getEstado()).build();
    }
}
