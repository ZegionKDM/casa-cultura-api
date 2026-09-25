<script setup>
import { ref, reactive } from 'vue'
import {
  BarChart3,
  GraduationCap,
  CalendarCheck2,
  UserCheck
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import { getAttendanceReport } from '../../../services/superAdminService.js'

const { data, showToast } = useAdminData()

const reportDates = reactive({
  from: new Date(new Date().getFullYear(), 0, 1).toISOString().slice(0, 10),
  to: new Date().toISOString().slice(0, 10)
})
const reportData = ref(null)
const isReportLoading = ref(false)

async function fetchReport() {
  isReportLoading.value = true
  try {
    reportData.value = await getAttendanceReport(reportDates.from, reportDates.to)
    showToast('Reporte de asistencias consultado exitosamente.')
  } catch (err) {
    showToast(err.message || 'Error al consultar reporte', 'error')
  } finally {
    isReportLoading.value = false
  }
}
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
          <p>Estadísticas e informes analíticos</p>
        </div>
      </div>
    </div>

    <div class="special-panel">
      <h2>Reportes y Analítica</h2>
      <p>Consulta los informes de asistencia y métricas del ciclo escolar.</p>

      <div class="special-grid">
        <article class="special-card">
          <GraduationCap :size="28" />
          <h3>Padrón Estudiantil</h3>
          <p>{{ data.alumnos.length }} alumnos registrados en {{ data.cursos.length }} talleres culturales.</p>
          <button @click="showToast('Padrón verificado.')">
            Ver padrón
          </button>
        </article>

        <article class="special-card">
          <CalendarCheck2 :size="28" />
          <h3>Control de Asistencias</h3>
          <p>{{ data.asistencias.length }} registros procesados en el sistema.</p>
          <button @click="fetchReport">
            Generar reporte
          </button>
        </article>

        <article class="special-card">
          <UserCheck :size="28" />
          <h3>Plantilla Docente</h3>
          <p>{{ data.docentes.length }} instructores activos asignados.</p>
          <button @click="showToast('Docentes verificados.')">
            Consultar docentes
          </button>
        </article>
      </div>

      <!-- GENERADOR DE REPORTES ANALÍTICOS -->
      <div class="report-interactive-box">
        <h3>Consulta Analítica de Asistencias</h3>
        <p>Filtra por rango de fechas para generar las métricas reales del servidor.</p>

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
            class="primary-button"
            :disabled="isReportLoading"
            @click="fetchReport"
          >
            <BarChart3 :size="16" />
            {{ isReportLoading ? 'Calculando...' : 'Generar Métricas' }}
          </button>
        </div>

        <div
          v-if="reportData"
          class="report-metrics-grid"
        >
          <div class="metric-box total">
            <span>Total Registros</span>
            <strong>{{ reportData.total || 0 }}</strong>
          </div>
          <div class="metric-box present">
            <span>Asistencias</span>
            <strong>{{ reportData.presentes || 0 }} ({{ reportData.total ? Math.round((reportData.presentes / reportData.total) * 100) : 0 }}%)</strong>
          </div>
          <div class="metric-box late">
            <span>Retardos</span>
            <strong>{{ reportData.retardos || 0 }} ({{ reportData.total ? Math.round((reportData.retardos / reportData.total) * 100) : 0 }}%)</strong>
          </div>
          <div class="metric-box absent">
            <span>Faltas</span>
            <strong>{{ reportData.faltas || 0 }} ({{ reportData.total ? Math.round((reportData.faltas / reportData.total) * 100) : 0 }}%)</strong>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
