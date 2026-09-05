package CasaCulturaAPI.service.impl;

import CasaCulturaAPI.dto.request.*;
import CasaCulturaAPI.dto.response.*;
import CasaCulturaAPI.entity.*;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.repository.*;
import CasaCulturaAPI.service.interfaces.EnrollmentService;
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
