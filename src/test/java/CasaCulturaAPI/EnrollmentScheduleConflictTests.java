package CasaCulturaAPI;

import CasaCulturaAPI.feature.inscripciones.dto.request.InscripcionRequest;
import CasaCulturaAPI.feature.inscripciones.dto.response.InscripcionResponse;
import CasaCulturaAPI.feature.inscripciones.service.EnrollmentService;
import CasaCulturaAPI.feature.pagos.service.PagoNotificationService;
import CasaCulturaAPI.shared.entity.*;
import CasaCulturaAPI.feature.alumnos.repository.AlumnoRepository;
import CasaCulturaAPI.feature.catalogo.repository.CategoriaEdadRepository;
import CasaCulturaAPI.feature.catalogo.repository.CursoRepository;
import CasaCulturaAPI.feature.catalogo.repository.GrupoRepository;
import CasaCulturaAPI.feature.catalogo.repository.HorarioRepository;
import CasaCulturaAPI.feature.catalogo.repository.OfertaCursoRepository;
import CasaCulturaAPI.feature.inscripciones.repository.InscripcionRepository;
import CasaCulturaAPI.shared.repository.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EnrollmentScheduleConflictTests {
    @Autowired EnrollmentService enrollmentService;
    @Autowired CasaCulturaAPI.feature.catalogo.service.CatalogService catalogService;
    @Autowired PagoNotificationService pagoNotificationService;
    @Autowired PersonaRepository personaRepository;
    @Autowired AlumnoRepository alumnoRepository;
    @Autowired CursoRepository cursoRepository;
    @Autowired CategoriaEdadRepository categoriaRepository;
    @Autowired OfertaCursoRepository ofertaRepository;
    @Autowired GrupoRepository grupoRepository;
    @Autowired HorarioRepository horarioRepository;
    @Autowired InscripcionRepository inscripcionRepository;

    @Test
    void allowsEnrollmentWhenSchedulesDoNotConflict() {
        Alumno alumno = createStudent("ENROLL-OK-" + System.nanoTime());
        Grupo grupo1 = createGroup("PIANO-" + System.nanoTime());
        Grupo grupo2 = createGroup("DANZA-" + System.nanoTime());

        // Grupo 1: Lunes 10:00 - 12:00
        horarioRepository.save(Horario.builder()
                .grupo(grupo1).dia(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(12, 0))
                .build());

        // Grupo 2: Martes 10:00 - 12:00 (diferente día)
        horarioRepository.save(Horario.builder()
                .grupo(grupo2).dia(DayOfWeek.TUESDAY)
                .horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(12, 0))
                .build());

        InscripcionResponse r1 = enrollmentService.inscribir(InscripcionRequest.builder()
                .alumnoId(alumno.getId()).grupoId(grupo1.getId()).build());
        InscripcionResponse r2 = enrollmentService.inscribir(InscripcionRequest.builder()
                .alumnoId(alumno.getId()).grupoId(grupo2.getId()).build());

        assertNotNull(r1);
        assertNotNull(r2);
    }

    @Test
    void rejectsEnrollmentWhenSchedulesOverlapOnSameDay() {
        Alumno alumno = createStudent("ENROLL-CONFLICT-" + System.nanoTime());
        Grupo grupoGuitarra = createGroup("GUITARRA-" + System.nanoTime());
        Grupo grupoPintura = createGroup("PINTURA-" + System.nanoTime());

        // Guitarra: Lunes 16:00 - 18:00
        horarioRepository.save(Horario.builder()
                .grupo(grupoGuitarra).dia(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(18, 0))
                .build());

        // Pintura: Lunes 17:00 - 19:00 (se empalma una hora)
        horarioRepository.save(Horario.builder()
                .grupo(grupoPintura).dia(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(17, 0)).horaFin(LocalTime.of(19, 0))
                .build());

        // Primera inscripción debe ser exitosa
        enrollmentService.inscribir(InscripcionRequest.builder()
                .alumnoId(alumno.getId()).grupoId(grupoGuitarra.getId()).build());

        // Segunda inscripción debe ser rechazada por la Regla de Negocio 2
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                enrollmentService.inscribir(InscripcionRequest.builder()
                        .alumnoId(alumno.getId()).grupoId(grupoPintura.getId()).build())
        );

        assertTrue(ex.getMessage().contains("Conflicto de horario"));
        assertTrue(ex.getMessage().contains("Regla de negocio 2"));
    }

    @Test
    void allowsEnrollmentIfConflictingPreviousGroupWasGivenBaja() {
        Alumno alumno = createStudent("ENROLL-BAJA-" + System.nanoTime());
        Grupo grupoViejo = createGroup("VIEJO-" + System.nanoTime());
        Grupo grupoNuevo = createGroup("NUEVO-" + System.nanoTime());

        // Ambos tienen el mismo horario
        horarioRepository.save(Horario.builder()
                .grupo(grupoViejo).dia(DayOfWeek.WEDNESDAY)
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(17, 0))
                .build());

        horarioRepository.save(Horario.builder()
                .grupo(grupoNuevo).dia(DayOfWeek.WEDNESDAY)
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(17, 0))
                .build());

        // Inscribir al viejo y marcarlo como BAJA
        InscripcionResponse rViejo = enrollmentService.inscribir(InscripcionRequest.builder()
                .alumnoId(alumno.getId()).grupoId(grupoViejo.getId()).build());
        Inscripcion inscripcion = inscripcionRepository.findById(rViejo.getId()).orElseThrow();
        inscripcion.setEstado(EstadoInscripcion.BAJA);
        inscripcionRepository.save(inscripcion);

        // Ahora el alumno sí puede inscribirse al nuevo grupo porque el anterior ya no está activo
        InscripcionResponse rNuevo = enrollmentService.inscribir(InscripcionRequest.builder()
                .alumnoId(alumno.getId()).grupoId(grupoNuevo.getId()).build());

        assertNotNull(rNuevo);
        assertEquals(EstadoInscripcion.ACTIVA, rNuevo.getEstado());
    }

    @Test
    void paymentNotificationServiceExecutesWithoutError() {
        assertDoesNotThrow(() -> {
            int notificados = pagoNotificationService.notificarPagosPorVencer(7);
            assertTrue(notificados >= 0);
        });
    }

    @Test
    void darDeBajaSetsStatusToBajaSuccessfully() {
        Alumno alumno = createStudent("ENROLL-DAR-BAJA-" + System.nanoTime());
        Grupo grupo = createGroup("BAJA-GRP-" + System.nanoTime());

        InscripcionResponse r = enrollmentService.inscribir(InscripcionRequest.builder()
                .alumnoId(alumno.getId()).grupoId(grupo.getId()).build());
        assertEquals(EstadoInscripcion.ACTIVA, r.getEstado());

        InscripcionResponse rBaja = enrollmentService.darDeBaja(r.getId());
        assertEquals(EstadoInscripcion.BAJA, rBaja.getEstado());

        Inscripcion inscripcion = inscripcionRepository.findById(r.getId()).orElseThrow();
        assertEquals(EstadoInscripcion.BAJA, inscripcion.getEstado());
    }

    @Test
    void createsMultipleSchedulesInBatchSuccessfully() {
        Grupo grupo = createGroup("BATCH-GROUP-" + System.nanoTime());
        CasaCulturaAPI.feature.catalogo.dto.request.HorarioBatchRequest request =
                CasaCulturaAPI.feature.catalogo.dto.request.HorarioBatchRequest.builder()
                        .grupoId(grupo.getId())
                        .dias(java.util.List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                        .horaInicio(LocalTime.of(16, 0))
                        .horaFin(LocalTime.of(18, 0))
                        .build();

        var creados = catalogService.crearHorariosBatch(request);
        assertEquals(3, creados.size());

        var guardados = horarioRepository.findByGrupoId(grupo.getId());
        assertEquals(3, guardados.size());
    }

    @Test
    void registersAndUpdatesPaymentSuccessfully() {
        Alumno alumno = createStudent("PAY-TEST-" + System.nanoTime());
        Grupo grupo = createGroup("PAY-GRP-" + System.nanoTime());
        InscripcionResponse r = enrollmentService.inscribir(InscripcionRequest.builder()
                .alumnoId(alumno.getId()).grupoId(grupo.getId()).build());

        CasaCulturaAPI.feature.pagos.dto.request.PagoRequest request = new CasaCulturaAPI.feature.pagos.dto.request.PagoRequest();
        request.setInscripcionId(r.getId());
        request.setTipoPago(TipoPago.INSCRIPCION);
        request.setPeriodo("2026-1");
        request.setMonto(500.0);
        request.setEstado(EstadoPago.PENDIENTE);
        request.setFechaVencimiento(LocalDate.now().plusDays(10));

        var pagoCreado = enrollmentService.registrarPago(request);
        assertNotNull(pagoCreado);
        assertEquals(500.0, pagoCreado.getMonto());
        assertEquals(EstadoPago.PENDIENTE, pagoCreado.getEstado());
        assertEquals(alumno.getMatricula(), pagoCreado.getMatricula());

        var pagoActualizado = enrollmentService.actualizarEstadoPago(pagoCreado.getId(), EstadoPago.PAGADO);
        assertEquals(EstadoPago.PAGADO, pagoActualizado.getEstado());
        assertNotNull(pagoActualizado.getFechaPago());

        var lista = enrollmentService.listarPagos();
        assertFalse(lista.isEmpty());
        assertTrue(lista.stream().anyMatch(p -> p.getId().equals(pagoCreado.getId())));
    }

    private Alumno createStudent(String matricula) {
        Persona persona = personaRepository.save(Persona.builder()
                .nombre("Alumno").apellidoPaterno("Prueba").correo("test@tlaxiaco.gob.mx").build());
        return alumnoRepository.save(Alumno.builder()
                .persona(persona)
                .matricula(matricula)
                .qrCredentialHash(CasaCulturaAPI.util.CredentialHash.sha256(matricula))
                .build());
    }

    private Grupo createGroup(String name) {
        Curso curso = cursoRepository.save(Curso.builder().nombre(name + "-CURSO").build());
        CategoriaEdad categoria = categoriaRepository.save(CategoriaEdad.builder()
                .nombre(name + "-CAT").build());
        OfertaCurso oferta = ofertaRepository.save(OfertaCurso.builder()
                .curso(curso)
                .tipo("Regular")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusMonths(3))
                .build());
        return grupoRepository.save(Grupo.builder()
                .oferta(oferta)
                .categoria(categoria)
                .nombreGrupo(name)
                .build());
    }
}
