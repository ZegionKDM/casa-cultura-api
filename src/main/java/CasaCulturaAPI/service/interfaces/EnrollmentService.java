package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.request.*;
import CasaCulturaAPI.dto.response.*;

import java.util.List;

public interface EnrollmentService {
    InscripcionResponse inscribir(InscripcionRequest request);
    List<InscripcionResponse> listarInscripciones();
    PagoResponse registrarPago(PagoRequest request);
    List<PagoResponse> listarPagos();
}
