package CasaCulturaAPI.feature.pagos.service;

import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.feature.pagos.repository.PagoRepository;
import CasaCulturaAPI.notification.EmailNotificationPort;
import CasaCulturaAPI.shared.entity.EstadoPago;
import CasaCulturaAPI.shared.entity.Pago;
import CasaCulturaAPI.shared.entity.Persona;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoNotificationServiceImpl implements PagoNotificationService {
    private final PagoRepository pagoRepository;
    private final EmailNotificationPort emailNotificationPort;

    @Override
    @Transactional(readOnly = true)
    public int notificarPagosPorVencer(int diasAnticipacion) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(Math.max(1, diasAnticipacion));
        List<Pago> pagosPorVencer = pagoRepository.findByEstadoAndFechaVencimientoBetween(
                EstadoPago.PENDIENTE, hoy, limite);

        int enviados = 0;
        for (Pago pago : pagosPorVencer) {
            if (enviarCorreoPago(pago, "Aviso: Pago próximo a vencer - Casa de la Cultura de Tlaxiaco")) {
                enviados++;
            }
        }
        return enviados;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean notificarPago(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado."));
        return enviarCorreoPago(pago, "Notificación de estado de pago - Casa de la Cultura de Tlaxiaco");
    }

    private boolean enviarCorreoPago(Pago pago, String asunto) {
        Persona persona = pago.getInscripcion().getAlumno().getPersona();
        String correo = persona.getCorreo();
        if (correo == null || correo.trim().isBlank()) {
            return false;
        }

        String nombreCompleto = persona.getNombre() + " " + persona.getApellidoPaterno() +
                (persona.getApellidoMaterno() != null && !persona.getApellidoMaterno().isBlank()
                        ? " " + persona.getApellidoMaterno() : "");

        String taller = pago.getInscripcion().getGrupo().getNombreGrupo();
        String periodo = pago.getPeriodo() != null ? pago.getPeriodo() : "N/A";
        String vencimiento = pago.getFechaVencimiento() != null ? pago.getFechaVencimiento().toString() : "No especificado";

        String cuerpo = String.format("""
                Estimado(a) %s:
                
                Le informamos sobre el estatus de su pago en el Sistema de Gestión Escolar y Cultural (SIGEC Tlaxiaco):
                
                · Taller / Grupo: %s
                · Concepto: %s
                · Periodo: %s
                · Fecha de vencimiento: %s
                · Estado actual: %s
                
                Por favor, acuda al área administrativa de la Casa de la Cultura para realizar o verificar su pago oportunamente.
                
                Atentamente,
                Casa de la Cultura del H. Ayuntamiento de la Heroica Ciudad de Tlaxiaco
                """,
                nombreCompleto.trim(),
                taller,
                pago.getTipoPago(),
                periodo,
                vencimiento,
                pago.getEstado()
        );

        try {
            emailNotificationPort.send(correo.trim(), asunto, cuerpo);
            log.info("Notificación de pago enviada exitosamente a {} para el pago ID {}", correo, pago.getId());
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo enviar la notificación de pago por correo a {} (Pago ID {}): {}",
                    correo, pago.getId(), ex.getMessage());
            return false;
        }
    }
}
