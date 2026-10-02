<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  BarChart3,
  GraduationCap,
  CalendarCheck2,
  UserCheck,
  Calendar,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Download,
  ArrowRight,
  TrendingUp,
  BookOpen
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import { getAttendanceReport } from '../../../services/superAdminService.js'

const router = useRouter()
const { data, showToast, exportToCsv } = useAdminData()

const currentYr = new Date().getFullYear()
const reportDates = reactive({
  from: `${currentYr}-01-01`,
  to: new Date().toISOString().slice(0, 10)
})

const reportData = ref(null)
const isReportLoading = ref(false)

// Calculate overall attendance stats directly from loaded real data
const overallAttendanceStats = computed(() => {
  const all = data.asistencias || []
  const total = all.length
  if (total === 0) {
    return {
      total: 0,
      presentes: 0,
      retardos: 0,
      faltas: 0,
      presentesPct: 0,
      retardosPct: 0,
      faltasPct: 0
    }
  }

  const presentes = all.filter(a => a.estado === 'PRESENTE').length
  const retardos = all.filter(a => a.estado === 'RETARDO').length
  const faltas = all.filter(a => a.estado === 'FALTA' || a.estado === 'INACTIVO').length

  const presentesPct = Math.round((presentes / total) * 100)
  const retardosPct = Math.round((retardos / total) * 100)
  const faltasPct = Math.max(0, 100 - presentesPct - retardosPct)

  return {
    total,
    presentes,
    retardos,
    faltas,
    presentesPct,
    retardosPct,
    faltasPct
  }
})

// Current active metrics (either from custom range API call or general dataset)
const activeMetrics = computed(() => {
  if (reportData.value) {
    const tot = reportData.value.total || 0
    const pres = reportData.value.presentes || 0
    const ret = reportData.value.retardos || 0
    const fal = reportData.value.faltas || 0

    const presPct = tot > 0 ? Math.round((pres / tot) * 100) : 0
    const retPct = tot > 0 ? Math.round((ret / tot) * 100) : 0
    const falPct = tot > 0 ? Math.max(0, 100 - presPct - retPct) : 0

    return {
      isCustom: true,
      total: tot,
      presentes: pres,
      retardos: ret,
      faltas: fal,
      presentesPct: presPct,
      retardosPct: retPct,
      faltasPct: falPct
    }
  }

  return {
    isCustom: false,
    ...overallAttendanceStats.value
  }
})

// Attendance by Workshop
const attendanceByWorkshop = computed(() => {
  if (!data.cursos || data.cursos.length === 0 || !data.asistencias || data.asistencias.length === 0) return []

  return data.cursos.map(c => {
    const ofertaIds = (data.ofertas || []).filter(o => o.cursoId === c.id).map(o => o.id)
    const groupIds = (data.grupos || []).filter(g => (g.ofertaId && ofertaIds.includes(g.ofertaId)) || g.cursoId === c.id || g.nombreCurso === c.nombre).map(g => g.id)
    const enrollmentIds = (data.inscripciones || []).filter(i => groupIds.includes(i.grupoId)).map(i => i.id)

    const workshopAttendances = data.asistencias.filter(a => enrollmentIds.includes(a.inscripcionId))
    const total = workshopAttendances.length
    const presentCount = workshopAttendances.filter(a => a.estado === 'PRESENTE').length
    const lateCount = workshopAttendances.filter(a => a.estado === 'RETARDO').length
    const absentCount = workshopAttendances.filter(a => a.estado === 'FALTA' || a.estado === 'INACTIVO').length

    const rate = total > 0 ? Math.round(((presentCount + (lateCount * 0.5)) / total) * 100) : 100

    return {
      id: c.id,
      nombre: c.nombre,
      total,
      presentCount,
      lateCount,
      absentCount,
      rate
    }
  }).sort((a, b) => b.total - a.total)
})

async function fetchReport() {
  isReportLoading.value = true
  try {
    reportData.value = await getAttendanceReport(reportDates.from, reportDates.to)
    showToast('Reporte analítico generado con éxito.')
  } catch (err) {
    showToast(err.message || 'Error al consultar reporte del servidor', 'error')
  } finally {
    isReportLoading.value = false
  }
}

function handleExportAttendanceReport() {
  const rows = attendanceByWorkshop.value.map(w => ({
    Taller: w.nombre,
    Total_Registros: w.total,
    Asistencias: w.presentCount,
    Retardos: w.lateCount,
    Faltas: w.absentCount,
    Tasa_Asistencia_Pct: `${w.rate}%`
  }))
  exportToCsv('reporte_asistencias_talleres', rows)
}

onMounted(() => {
  // Try fetching current year report from backend on load
  fetchReport()
})
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <BarChart3 :size="28" />
        </div>
        <div>
          <h1>Reportes y Gráficas</h1>
          <p>Estadísticas analíticas de asistencia y desempeño institucional</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="secondary-button" @click="handleExportAttendanceReport">
          <Download :size="16" />
          Exportar Informe
        </button>
      </div>
    </div>

    <div class="special-panel">
      <h2>Indicadores Globales de la Institución</h2>
      <p>Supervisión consolidada de los principales registros y expedientes escolares.</p>

      <div class="special-grid">
        <article class="special-card">
          <div class="card-icon-wrap indigo">
            <GraduationCap :size="24" />
          </div>
          <h3>Padrón Estudiantil</h3>
          <p class="card-amount text-indigo">{{ data.alumnos.length }} alumnos</p>
          <span class="card-sub">{{ data.cursos.length }} talleres y {{ data.grupos.length }} grupos en curso</span>
          <button @click="router.push('/admin/alumnos')">
            Ver padrón <ArrowRight :size="14" />
          </button>
        </article>

        <article class="special-card">
          <div class="card-icon-wrap green">
            <CalendarCheck2 :size="24" />
          </div>
          <h3>Control de Asistencias</h3>
          <p class="card-amount text-success">{{ data.asistencias.length }} registros</p>
          <span class="card-sub">{{ overallAttendanceStats.presentesPct }}% asistencia general registrada</span>
          <button @click="router.push('/admin/asistencias')">
            Registro diario <ArrowRight :size="14" />
          </button>
        </article>

        <article class="special-card">
          <div class="card-icon-wrap amber">
            <UserCheck :size="24" />
          </div>
          <h3>Plantilla Docente</h3>
          <p class="card-amount text-amber">{{ data.docentes.length }} instructores</p>
          <span class="card-sub">Plantilla activa de talleristas</span>
          <button @click="router.push('/admin/docentes')">
            Consultar docentes <ArrowRight :size="14" />
          </button>
        </article>
      </div>

      <!-- GENERADOR DE REPORTES ANALÍTICOS -->
      <div class="report-interactive-box">
        <div class="report-box-header">
          <div>
            <h3>Filtro Analítico por Rango de Fechas</h3>
            <p>Genera métricas consolidadas de asistencias directamente desde el servicio de reportes.</p>
          </div>
        </div>

        <div class="report-controls">
          <div class="date-field">
            <label>Desde:</label>
            <input
              v-model="reportDates.from"
              type="date"
            />
          </div>
          <div class="date-field">
            <label>Hasta:</label>
            <input
              v-model="reportDates.to"
              type="date"
            />
          </div>
          <button
            class="primary-button btn-generate"
            :disabled="isReportLoading"
            @click="fetchReport"
          >
            <BarChart3 :size="16" />
            {{ isReportLoading ? 'Consultando...' : 'Actualizar Métricas' }}
          </button>
        </div>

        <!-- METRIC CARDS -->
        <div class="report-metrics-grid">
          <div class="metric-box total">
            <div class="metric-icon-wrap">
              <Calendar :size="18" />
            </div>
            <span>Total Evaluado</span>
            <strong>{{ activeMetrics.total }} registros</strong>
            <small>{{ activeMetrics.isCustom ? 'Periodo seleccionado' : 'Ciclo general' }}</small>
          </div>

          <div class="metric-box present">
            <div class="metric-icon-wrap">
              <CheckCircle2 :size="18" />
            </div>
            <span>Asistencias</span>
            <strong>{{ activeMetrics.presentes }}</strong>
            <small>{{ activeMetrics.presentesPct }}% de efectividad</small>
          </div>

          <div class="metric-box late">
            <div class="metric-icon-wrap">
              <Clock :size="18" />
            </div>
            <span>Retardos</span>
            <strong>{{ activeMetrics.retardos }}</strong>
            <small>{{ activeMetrics.retardosPct }}% con retardo</small>
          </div>

          <div class="metric-box absent">
            <div class="metric-icon-wrap">
              <AlertTriangle :size="18" />
            </div>
            <span>Faltas</span>
            <strong>{{ activeMetrics.faltas }}</strong>
            <small>{{ activeMetrics.faltasPct }}% inasistencias</small>
          </div>
        </div>

        <!-- VISUAL COMPARATIVE ATTENDANCE BAR -->
        <div class="attendance-visual-bar-card">
          <div class="bar-title-row">
            <h4>Proporción Visual de Asistencias</h4>
            <span class="ratio-tag">{{ activeMetrics.presentesPct }}% Presentes</span>
          </div>

          <div class="attendance-multi-bar">
            <div
              class="bar-seg present"
              :style="{ width: `${activeMetrics.presentesPct}%` }"
              title="Asistencias"
            ></div>
            <div
              class="bar-seg late"
              :style="{ width: `${activeMetrics.retardosPct}%` }"
              title="Retardos"
            ></div>
            <div
              class="bar-seg absent"
              :style="{ width: `${activeMetrics.faltasPct}%` }"
              title="Faltas"
            ></div>
          </div>

          <div class="bar-legend">
            <div class="leg-item">
              <span class="dot green"></span>
              <span>Asistencias: <strong>{{ activeMetrics.presentes }} ({{ activeMetrics.presentesPct }}%)</strong></span>
            </div>
            <div class="leg-item">
              <span class="dot yellow"></span>
              <span>Retardos: <strong>{{ activeMetrics.retardos }} ({{ activeMetrics.retardosPct }}%)</strong></span>
            </div>
            <div class="leg-item">
              <span class="dot red"></span>
              <span>Faltas: <strong>{{ activeMetrics.faltas }} ({{ activeMetrics.faltasPct }}%)</strong></span>
            </div>
          </div>
        </div>
      </div>

      <!-- ASISTENCIA POR TALLER CULTURAL -->
      <div class="workshop-attendance-section">
        <div class="section-title-row">
          <div>
            <h3>Asistencia y Permanencia por Taller</h3>
            <p>Monitoreo del índice de presentismo de cada curso cultural ofertado.</p>
          </div>
        </div>

        <div class="table-container">
          <table class="modern-table">
            <thead>
              <tr>
                <th>Taller Cultural</th>
                <th>Total Clases Registradas</th>
                <th>Presentes</th>
                <th>Retardos</th>
                <th>Faltas</th>
                <th style="text-align: right; padding-right: 20px;">Índice de Asistencia</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ws in attendanceByWorkshop" :key="ws.id">
                <td>
                  <strong>{{ ws.nombre }}</strong>
                </td>
                <td>{{ ws.total }}</td>
                <td><span class="cell-sub text-success">{{ ws.presentCount }}</span></td>
                <td><span class="cell-sub text-amber">{{ ws.lateCount }}</span></td>
                <td><span class="cell-sub text-danger">{{ ws.absentCount }}</span></td>
                <td style="text-align: right; padding-right: 20px;">
                  <div class="rate-badge-wrap">
                    <span
                      class="status"
                      :class="{
                        paid: ws.rate >= 80,
                        pending: ws.rate >= 60 && ws.rate < 80,
                        inactive: ws.rate < 60
                      }"
                    >
                      {{ ws.rate }}%
                    </span>
                  </div>
                </td>
              </tr>
              <tr v-if="attendanceByWorkshop.length === 0">
                <td colspan="6" class="empty-table-cell">
                  No se tienen registros de asistencia asociados a los talleres.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.special-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.special-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.special-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
}

.card-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}

.card-icon-wrap.indigo { background: #eef2ff; color: #6366f1; }
.card-icon-wrap.green { background: #ecfdf5; color: #10b981; }
.card-icon-wrap.amber { background: #fffbeb; color: #f59e0b; }

.special-card h3 {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.card-amount {
  font-size: 22px;
  font-weight: 800;
  margin: 2px 0;
  line-height: 1.2;
}

.text-indigo { color: #4338ca !important; }
.text-success { color: #16a34a !important; }
.text-amber { color: #d97706 !important; }
.text-danger { color: #dc2626 !important; }

.card-sub {
  font-size: 11.5px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.special-card button {
  margin-top: auto;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.2s ease;
}

.special-card button:hover {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

/* REPORT TOOL */
.report-interactive-box {
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid #edf0f4;
}

.report-box-header h3 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #0f172a;
}

.report-box-header p {
  margin: 0 0 16px;
  font-size: 12px;
  color: #64748b;
}

.report-controls {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.date-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.date-field label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.date-field input {
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  font-size: 13px;
  color: #1e293b;
  outline: none;
}

.date-field input:focus {
  border-color: #4f46e5;
}

.btn-generate {
  height: 39px;
  padding: 0 16px;
}

/* METRICS GRID */
.report-metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}

.metric-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.metric-box.total .metric-icon-wrap { background: #eef2ff; color: #4f46e5; }
.metric-box.present .metric-icon-wrap { background: #ecfdf5; color: #10b981; }
.metric-box.late .metric-icon-wrap { background: #fffbeb; color: #f59e0b; }
.metric-box.absent .metric-icon-wrap { background: #fef2f2; color: #ef4444; }

.metric-box span {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
}

.metric-box strong {
  font-size: 20px;
  color: #0f172a;
}

.metric-box small {
  font-size: 11px;
  color: #94a3b8;
}

/* ATTENDANCE MULTI BAR */
.attendance-visual-bar-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 26px;
}

.bar-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.bar-title-row h4 {
  margin: 0;
  font-size: 14px;
  color: #0f172a;
}

.ratio-tag {
  font-size: 11.5px;
  font-weight: 700;
  background: #ecfdf5;
  color: #065f46;
  padding: 3px 9px;
  border-radius: 20px;
}

.attendance-multi-bar {
  height: 14px;
  background: #f1f5f9;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  margin-bottom: 14px;
}

.bar-seg.present { background: #10b981; }
.bar-seg.late { background: #f59e0b; }
.bar-seg.absent { background: #ef4444; }

.bar-legend {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.leg-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.dot.green { background: #10b981; }
.dot.yellow { background: #f59e0b; }
.dot.red { background: #ef4444; }

/* WORKSHOP ATTENDANCE */
.workshop-attendance-section {
  margin-top: 10px;
  padding-top: 22px;
  border-top: 1px solid #edf0f4;
}

.section-title-row {
  margin-bottom: 16px;
}

.section-title-row h3 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #0f172a;
}

.section-title-row p {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.rate-badge-wrap {
  display: inline-flex;
  justify-content: flex-end;
}
</style>
