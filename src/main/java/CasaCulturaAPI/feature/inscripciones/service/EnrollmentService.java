package CasaCulturaAPI.feature.inscripciones.service;

import CasaCulturaAPI.feature.alumnos.dto.request.*;
import CasaCulturaAPI.feature.asistencias.dto.request.*;
import CasaCulturaAPI.feature.auth.dto.request.*;
import CasaCulturaAPI.feature.catalogo.dto.request.*;
import CasaCulturaAPI.feature.docentes.dto.request.*;
import CasaCulturaAPI.feature.inscripciones.dto.request.*;
import CasaCulturaAPI.feature.pagos.dto.request.*;
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

import java.util.List;

public interface EnrollmentService {
    InscripcionResponse inscribir(InscripcionRequest request);
    List<InscripcionResponse> listarInscripciones();
    PagoResponse registrarPago(PagoRequest request);
    List<PagoResponse> listarPagos();
}
