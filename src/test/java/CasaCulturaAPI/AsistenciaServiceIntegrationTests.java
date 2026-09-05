package CasaCulturaAPI;

import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaQrRequest;
import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaManualRequest;
import CasaCulturaAPI.feature.asistencias.dto.response.AsistenciaResponse;
import CasaCulturaAPI.shared.entity.*;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AsistenciaServiceIntegrationTests {
    @Autowired AsistenciaService asistenciaService;
    @Autowired PersonaRepository personaRepository;
    @Autowired AlumnoRepository alumnoRepository;
    @Autowired CursoRepository cursoRepository;
    @Autowired CategoriaEdadRepository categoriaRepository;
    @Autowired OfertaCursoRepository ofertaRepository;
    @Autowired GrupoRepository grupoRepository;
    @Autowired HorarioRepository horarioRepository;
    @Autowired InscripcionRepository inscripcionRepository;
    @Autowired AsistenciaRepository asistenciaRepository;

    @Test
    void qrRegistrationIsPresentAndIdempotent() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String credential = "QR-TEST-" + System.nanoTime();
        Alumno alumno = createStudent(credential);
        Grupo grupo = createGroup("QR-GROUP-" + System.nanoTime());
        Horario horario = horarioRepository.save(Horario.builder()
                .grupo(grupo)
                .dia(today.getDayOfWeek())
                .horaInicio(now.minusMinutes(2))
                .horaFin(now.plusMinutes(10))
                .build());
        Inscripcion inscripcion = inscripcionRepository.save(Inscripcion.builder()
                .alumno(alumno).grupo(grupo).build());

        AsistenciaQrRequest request = new AsistenciaQrRequest();
        request.setCredential(credential);
        AsistenciaResponse first = asistenciaService.registrarPorQr(request);
        AsistenciaResponse second = asistenciaService.registrarPorQr(request);

        assertEquals(EstadoAsistencia.PRESENTE, first.getEstado());
        assertEquals(first.getId(), second.getId());
        assertNotNull(asistenciaRepository.findByInscripcionAndHorarioAndFecha(
                inscripcion, horario, today).orElse(null));
    }

    @Test
    void completedScheduleGeneratesAbsence() {
        LocalDate today = LocalDate.now();
        Alumno alumno = createStudent("ABSENCE-TEST-" + System.nanoTime());
        Grupo grupo = createGroup("ABSENCE-GROUP-" + System.nanoTime());
        Horario horario = horarioRepository.save(Horario.builder()
                .grupo(grupo)
                .dia(today.getDayOfWeek())
                .horaInicio(LocalTime.now().minusHours(2))
                .horaFin(LocalTime.now().minusHours(1))
                .build());
        Inscripcion inscripcion = inscripcionRepository.save(Inscripcion.builder()
                .alumno(alumno).grupo(grupo).build());

        int generated = asistenciaService.generarFaltas(today);

        assertEquals(1, generated);
        assertEquals(EstadoAsistencia.FALTA,
                asistenciaRepository.findByInscripcionAndHorarioAndFecha(
                        inscripcion, horario, today).orElseThrow().getEstado());
    }

    @Test
    void authorizedManualAttendanceUpdatesTheDailyRecord() {
        LocalDate today = LocalDate.now();
        Alumno alumno = createStudent("MANUAL-TEST-" + System.nanoTime());
        Grupo grupo = createGroup("MANUAL-GROUP-" + System.nanoTime());
        Horario horario = horarioRepository.save(Horario.builder()
                .grupo(grupo).dia(today.getDayOfWeek())
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(10, 0)).build());
        Inscripcion inscripcion = inscripcionRepository.save(Inscripcion.builder()
                .alumno(alumno).grupo(grupo).build());
        AsistenciaManualRequest request = new AsistenciaManualRequest();
        request.setInscripcionId(inscripcion.getId());
        request.setHorarioId(horario.getId());
        request.setFecha(today);
        request.setEstado(EstadoAsistencia.RETARDO);

        AsistenciaResponse response = asistenciaService.registrarManual(request, "admin", false);

        assertEquals(EstadoAsistencia.RETARDO, response.getEstado());
    }

    private Alumno createStudent(String credential) {
        Persona persona = personaRepository.save(Persona.builder()
                .nombre("Test").apellidoPaterno("Alumno").build());
        return alumnoRepository.save(Alumno.builder()
                .persona(persona)
                .matricula(credential)
                .qrCredentialHash(CasaCulturaAPI.util.CredentialHash.sha256(credential))
                .build());
    }

    private Grupo createGroup(String name) {
        Curso curso = cursoRepository.save(Curso.builder().nombre(name + "-COURSE").build());
        CategoriaEdad categoria = categoriaRepository.save(CategoriaEdad.builder()
                .nombre(name + "-CATEGORY").build());
        OfertaCurso oferta = ofertaRepository.save(OfertaCurso.builder()
                .curso(curso).tipo("Test").fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusDays(1)).build());
        return grupoRepository.save(Grupo.builder()
                .oferta(oferta).categoria(categoria).nombreGrupo(name).build());
    }
}
