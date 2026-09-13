package CasaCulturaAPI.feature.pagos.service;

public interface PagoNotificationService {
    int notificarPagosPorVencer(int diasAnticipacion);
    boolean notificarPago(Long pagoId);
}
