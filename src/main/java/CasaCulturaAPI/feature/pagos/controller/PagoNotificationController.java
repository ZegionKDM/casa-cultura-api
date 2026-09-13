package CasaCulturaAPI.feature.pagos.controller;

import CasaCulturaAPI.feature.pagos.service.PagoNotificationService;
import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(Constants.API + "/pagos")
@RequiredArgsConstructor
public class PagoNotificationController {
    private final PagoNotificationService notificationService;

    @PostMapping("/notificar-vencimientos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> notificarVencimientos(
            @RequestParam(name = "dias", defaultValue = "5") int dias) {
        int totalEnviados = notificationService.notificarPagosPorVencer(dias);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Se procesaron las notificaciones de vencimiento.",
                Map.of("notificacionesEnviadas", totalEnviados, "diasAnticipacion", dias)
        ));
    }

    @PostMapping("/{id}/notificar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> notificarPagoIndividual(@PathVariable Long id) {
        boolean enviado = notificationService.notificarPago(id);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                enviado ? "Notificación enviada al alumno por correo." : "No se pudo enviar el correo (verifique dirección registrada).",
                Map.of("pagoId", id, "enviado", enviado)
        ));
    }
}
