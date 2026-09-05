package CasaCulturaAPI.feature.alumnos.service;

import CasaCulturaAPI.feature.alumnos.dto.request.AlumnoRequest;
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
import CasaCulturaAPI.feature.alumnos.repository.AlumnoRepository;
import CasaCulturaAPI.feature.docentes.repository.AsignacionDocenteRepository;
import CasaCulturaAPI.feature.inscripciones.repository.InscripcionRepository;
import CasaCulturaAPI.shared.repository.PersonaRepository;
import CasaCulturaAPI.feature.docentes.repository.DocenteRepository;
import CasaCulturaAPI.feature.auth.repository.UsuarioRepository;
import CasaCulturaAPI.feature.alumnos.service.AlumnoService;
import CasaCulturaAPI.util.CredentialHash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlumnoServiceImpl implements AlumnoService {
    private final AlumnoRepository alumnoRepository;
    private final PersonaRepository personaRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final DocenteRepository docenteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public AlumnoCredentialResponse crear(AlumnoRequest request) {
        if (alumnoRepository.existsByMatriculaIgnoreCase(request.getMatricula())) {
            throw new IllegalArgumentException("La matrícula ya existe.");
        }
        Persona persona = personaRepository.save(Persona.builder()
                .nombre(request.getNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .fechaNacimiento(request.getFechaNacimiento())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .correo(request.getCorreo())
                .fotoUrl(request.getFotoUrl())
                .build());
        String credential = UUID.randomUUID().toString();
        Alumno alumno = alumnoRepository.save(Alumno.builder()
                .persona(persona)
                .matricula(request.getMatricula())
                .qrCredentialHash(CredentialHash.sha256(credential))
                .build());
        return AlumnoCredentialResponse.builder()
                .alumno(toResponse(alumno))
                .qrCredential(credential)
                .build();
    }

    @Override
    @Transactional
    public AlumnoCredentialResponse regenerarCredential(Long id) {
        Alumno alumno = find(id);
        String credential = UUID.randomUUID().toString();
        alumno.setQrCredentialHash(CredentialHash.sha256(credential));
        Alumno saved = alumnoRepository.save(alumno);
        return AlumnoCredentialResponse.builder()
                .alumno(toResponse(saved))
                .qrCredential(credential)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlumnoResponse> listar() {
        return alumnoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlumnoResponse> listarPorDocente(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        Docente docente = docenteRepository.findByPersonaId(usuario.getPersona().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un perfil docente."));
        LocalDate today = LocalDate.now();
        List<Long> groupIds = asignacionRepository.findByDocenteOrderByFechaInicioDesc(docente).stream()
                .filter(x -> !x.getFechaInicio().isAfter(today)
                        && (x.getFechaFin() == null || !x.getFechaFin().isBefore(today)))
                .map(x -> x.getGrupo().getId())
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return inscripcionRepository.findByGrupoIdInAndEstado(groupIds, EstadoInscripcion.ACTIVA)
                .stream()
                .map(Inscripcion::getAlumno)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AlumnoResponse obtener(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public AlumnoResponse actualizar(Long id, AlumnoRequest request) {
        Alumno alumno = find(id);
        if (!alumno.getMatricula().equalsIgnoreCase(request.getMatricula())
                && alumnoRepository.existsByMatriculaIgnoreCase(request.getMatricula())) {
            throw new IllegalArgumentException("La matrícula ya existe.");
        }
        Persona persona = alumno.getPersona();
        persona.setNombre(request.getNombre());
        persona.setApellidoPaterno(request.getApellidoPaterno());
        persona.setApellidoMaterno(request.getApellidoMaterno());
        persona.setFechaNacimiento(request.getFechaNacimiento());
        persona.setTelefono(request.getTelefono());
        persona.setDireccion(request.getDireccion());
        persona.setCorreo(request.getCorreo());
        persona.setFotoUrl(request.getFotoUrl());
        alumno.setMatricula(request.getMatricula());
        personaRepository.save(persona);
        return toResponse(alumnoRepository.save(alumno));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Alumno alumno = find(id);
        alumno.setEstado(EstadoRegistro.INACTIVO);
        alumnoRepository.save(alumno);
    }

    private Alumno find(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado."));
    }

    private AlumnoResponse toResponse(Alumno alumno) {
        Persona persona = alumno.getPersona();
        return AlumnoResponse.builder()
                .id(alumno.getId())
                .personaId(persona.getId())
                .nombre(persona.getNombre())
                .apellidoPaterno(persona.getApellidoPaterno())
                .apellidoMaterno(persona.getApellidoMaterno())
                .fechaNacimiento(persona.getFechaNacimiento())
                .telefono(persona.getTelefono())
                .direccion(persona.getDireccion())
                .correo(persona.getCorreo())
                .fotoUrl(persona.getFotoUrl())
                .matricula(alumno.getMatricula())
                .estado(alumno.getEstado())
                .build();
    }
}
