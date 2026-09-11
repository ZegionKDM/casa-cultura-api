package CasaCulturaAPI.feature.docentes.service;

import CasaCulturaAPI.feature.docentes.dto.request.AsignacionDocenteRequest;
import CasaCulturaAPI.feature.docentes.dto.response.AsignacionDocenteResponse;
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
import CasaCulturaAPI.feature.docentes.service.AsignacionDocenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignacionDocenteServiceImpl implements AsignacionDocenteService {
    private final AsignacionDocenteRepository repository;
    private final DocenteRepository docenteRepository;
    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public AsignacionDocenteResponse asignar(AsignacionDocenteRequest request) {
        if (request.getFechaFin() != null && !request.getFechaInicio().isBefore(request.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
        Docente docente = docenteRepository.findById(request.getDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado."));
        Grupo grupo = grupoRepository.findById(request.getGrupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));

        List<AsignacionDocente> actuales = repository.findByGrupoOrderByFechaInicioDesc(grupo);
        actuales.stream()
                .filter(x -> x.getFechaFin() == null || !x.getFechaFin().isBefore(request.getFechaInicio()))
                .findFirst()
                .ifPresent(x -> {
                    LocalDate fin = request.getFechaInicio().minusDays(1);
                    if (x.getFechaInicio().isBefore(fin)) {
                        x.setFechaFin(fin);
                        repository.save(x);
                    } else {
                        throw new IllegalArgumentException("El grupo ya tiene una asignación activa en ese periodo.");
                    }
                });

        return toResponse(repository.save(AsignacionDocente.builder().docente(docente).grupo(grupo)
                .fechaInicio(request.getFechaInicio()).fechaFin(request.getFechaFin()).build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionDocenteResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionDocenteResponse> listarPorGrupo(Long grupoId) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));
        return repository.findByGrupoOrderByFechaInicioDesc(grupo).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionDocenteResponse> listarPorUsuario(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        Docente docente = docenteRepository.findByPersonaId(usuario.getPersona().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un perfil docente."));
        LocalDate today = LocalDate.now();
        return repository.findByDocenteOrderByFechaInicioDesc(docente).stream()
                .filter(x -> isActive(x, today))
                .map(this::toResponse)
                .toList();
    }

    private boolean isActive(AsignacionDocente assignment, LocalDate date) {
        return !assignment.getFechaInicio().isAfter(date)
                && (assignment.getFechaFin() == null || !assignment.getFechaFin().isBefore(date));
    }

    private AsignacionDocenteResponse toResponse(AsignacionDocente x) {
        return AsignacionDocenteResponse.builder().id(x.getId()).docenteId(x.getDocente().getId())
                .grupoId(x.getGrupo().getId()).fechaInicio(x.getFechaInicio()).fechaFin(x.getFechaFin()).build();
    }
}
