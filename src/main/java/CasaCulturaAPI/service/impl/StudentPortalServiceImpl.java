package CasaCulturaAPI.service.impl;

import CasaCulturaAPI.dto.response.*;
import CasaCulturaAPI.entity.*;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.repository.*;
import CasaCulturaAPI.service.interfaces.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentPortalServiceImpl implements StudentPortalService {
    private final AlumnoRepository alumnoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final HorarioRepository horarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardResponse dashboard(String nombreUsuario) {
        Alumno alumno = alumno(nombreUsuario);
        List<Inscripcion> inscripciones = activeInscripciones(alumno);
        return StudentDashboardResponse.builder()
                .alumno(toAlumnoResponse(alumno))
                .inscripciones(inscripciones.stream().map(this::toInscripcionResponse).toList())
                .horarios(toSchedules(inscripciones))
                .pagos(pagoRepository.findByInscripcionIn(inscripciones).stream().map(this::toPagoResponse).toList())
                .asistencias(asistenciaRepository.findByInscripcionAlumnoIdOrderByFechaDescHoraRegistroDesc(alumno.getId())
                        .stream().map(this::toAttendanceResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AlumnoResponse perfil(String nombreUsuario) {
        return toAlumnoResponse(alumno(nombreUsuario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogResponse> horarios(String nombreUsuario) {
        return toSchedules(activeInscripciones(alumno(nombreUsuario)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> pagos(String nombreUsuario) {
        List<Inscripcion> inscripciones = activeInscripciones(alumno(nombreUsuario));
        return pagoRepository.findByInscripcionIn(inscripciones).stream().map(this::toPagoResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> asistencias(String nombreUsuario) {
        Alumno alumno = alumno(nombreUsuario);
        return asistenciaRepository.findByInscripcionAlumnoIdOrderByFechaDescHoraRegistroDesc(alumno.getId())
                .stream().map(this::toAttendanceResponse).toList();
    }

    private Alumno alumno(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        return alumnoRepository.findByPersonaId(usuario.getPersona().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un perfil de alumno."));
    }

    private List<Inscripcion> activeInscripciones(Alumno alumno) {
        return inscripcionRepository.findByAlumnoAndEstado(alumno, EstadoInscripcion.ACTIVA);
    }

    private List<CatalogResponse> toSchedules(List<Inscripcion> inscripciones) {
        return inscripciones.stream()
                .flatMap(x -> horarioRepository.findByGrupoId(x.getGrupo().getId()).stream())
                .map(this::toScheduleResponse)
                .toList();
    }

    private AlumnoResponse toAlumnoResponse(Alumno alumno) {
        Persona p = alumno.getPersona();
        return AlumnoResponse.builder().id(alumno.getId()).personaId(p.getId())
                .nombre(p.getNombre()).apellidoPaterno(p.getApellidoPaterno())
                .apellidoMaterno(p.getApellidoMaterno()).fechaNacimiento(p.getFechaNacimiento())
                .telefono(p.getTelefono()).direccion(p.getDireccion()).correo(p.getCorreo())
                .fotoUrl(p.getFotoUrl())
                .matricula(alumno.getMatricula()).estado(alumno.getEstado()).build();
    }

    private InscripcionResponse toInscripcionResponse(Inscripcion x) {
        return InscripcionResponse.builder().id(x.getId()).alumnoId(x.getAlumno().getId())
                .matricula(x.getAlumno().getMatricula()).grupoId(x.getGrupo().getId())
                .grupo(x.getGrupo().getNombreGrupo()).fechaInscripcion(x.getFechaInscripcion())
                .estado(x.getEstado()).build();
    }

    private CatalogResponse toScheduleResponse(Horario x) {
        return CatalogResponse.builder().id(x.getId()).grupoId(x.getGrupo().getId())
                .nombreGrupo(x.getGrupo().getNombreGrupo()).dia(x.getDia())
                .horaInicio(x.getHoraInicio()).horaFin(x.getHoraFin()).build();
    }

    private PagoResponse toPagoResponse(Pago x) {
        return PagoResponse.builder().id(x.getId()).inscripcionId(x.getInscripcion().getId())
                .tipoPago(x.getTipoPago()).periodo(x.getPeriodo())
                .fechaVencimiento(x.getFechaVencimiento()).fechaPago(x.getFechaPago())
                .estado(x.getEstado()).build();
    }

    private AsistenciaResponse toAttendanceResponse(Asistencia x) {
        Alumno alumno = x.getInscripcion().getAlumno();
        Persona p = alumno.getPersona();
        return AsistenciaResponse.builder().id(x.getId()).matricula(alumno.getMatricula())
                .alumno(p.getNombre() + " " + p.getApellidoPaterno())
                .grupo(x.getInscripcion().getGrupo().getNombreGrupo()).fecha(x.getFecha())
                .horaRegistro(x.getHoraRegistro()).estado(x.getEstado()).build();
    }
}
