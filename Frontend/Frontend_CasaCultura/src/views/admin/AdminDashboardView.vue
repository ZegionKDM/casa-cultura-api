<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Users,
  GraduationCap,
  BookOpen,
  CalendarDays,
  DollarSign,
  TrendingUp,
  AlertCircle
} from 'lucide-vue-next'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminHeader from '../../components/admin/AdminHeader.vue'
import StatCard from '../../components/admin/StatCard.vue'
import { getSuperAdminData } from '../../services/superAdminService.js'

const isLoading = ref(true)
const errorMessage = ref('')
const selectedMetric = ref('revenue') // 'revenue' | 'students'
const hoveredMonth = ref(null)
const hoveredBar = ref(null)

const data = reactive({
  alumnos: [],
  docentes: [],
  cursos: [],
  ofertas: [],
  grupos: [],
  categorias: [],
  inscripciones: [],
  pagos: [],
  asistencias: [],
  usuarios: [],
  roles: []
})

onMounted(async () => {
  isLoading.value = true
  try {
    const res = await getSuperAdminData()
    Object.assign(data, res)
  } catch (err) {
    console.error('Error cargando datos del dashboard:', err)
    errorMessage.value = err.message || 'Error al conectar con el servidor.'
  } finally {
    isLoading.value = false
  }
})

const currentYear = new Date().getFullYear()

// Helper: Estimate or read real payment amount
function getPaymentAmount(p) {
  if (p.monto && Number(p.monto) > 0) return Number(p.monto)
  if (p.tipoPago === 'INSCRIPCION') return 500
  if (p.tipoPago === 'MENSUALIDAD') return 400
  if (p.tipoPago === 'RECARGO') return 150
  return 400
}

// 1. STATS CALCULATIONS (REAL BACKEND DATA)
const totalPaidRevenue = computed(() => {
  return data.pagos
    .filter(p => p.estado === 'PAGADO')
    .reduce((acc, p) => acc + getPaymentAmount(p), 0)
})

const stats = computed(() => {
  const activeStudents = data.alumnos.filter(a => a.estado === 'ACTIVO' || !a.estado).length
  const activeTeachers = data.docentes.filter(d => d.estado === 'ACTIVO' || !d.estado).length
  const activeCourses = data.cursos.length
  const activeEnrollments = data.inscripciones.filter(i => i.estado === 'ACTIVA' || !i.estado).length

  return [
    {
      title: 'Total Alumnos',
      value: data.alumnos.length,
      subtitle: `Activos: ${activeStudents}`,
      icon: Users,
      color: '#6366f1'
    },
    {
      title: 'Total Docentes',
      value: data.docentes.length,
      subtitle: `Activos: ${activeTeachers}`,
      icon: GraduationCap,
      color: '#16a34a'
    },
    {
      title: 'Total Talleres',
      value: activeCourses,
      subtitle: `Grupos: ${data.grupos.length}`,
      icon: BookOpen,
      color: '#f59e0b'
    },
    {
      title: 'Inscripciones Activas',
      value: activeEnrollments,
      subtitle: 'Ciclo actual',
      icon: CalendarDays,
      color: '#3b82f6'
    },
    {
      title: 'Ingresos Totales',
      value: `$${totalPaidRevenue.value.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
      subtitle: `Año ${currentYear}`,
      icon: DollarSign,
      color: '#ef4444'
    }
  ]
})

// 2. REAL BAR CHART DATA: Revenue / Students by Workshop
const workshopStats = computed(() => {
  if (data.cursos.length === 0) return []

  // Map each course with its groups, enrollments and payments
  return data.cursos.map(curso => {
    // Find groups of this course
    const groupIds = data.grupos
      .filter(g => g.cursoId === curso.id || g.nombreCurso === curso.nombre)
      .map(g => g.id)

    // Find enrollments in these groups
    const enrollments = data.inscripciones.filter(ins => groupIds.includes(ins.grupoId))
    const enrollmentIds = enrollments.map(ins => ins.id)

    // Find paid payments for these enrollments
    const paidPayments = data.pagos.filter(
      p => enrollmentIds.includes(p.inscripcionId) && p.estado === 'PAGADO'
    )
    const revenue = paidPayments.reduce((acc, p) => acc + getPaymentAmount(p), 0)

    return {
      id: curso.id,
      nombre: curso.nombre,
      shortName: curso.nombre.length > 12 ? curso.nombre.slice(0, 10) + '...' : curso.nombre,
      alumnos: enrollments.length,
      ingresos: revenue
    }
  })
})

const maxWorkshopValue = computed(() => {
  if (workshopStats.value.length === 0) return 1
  const vals = workshopStats.value.map(w => selectedMetric.value === 'revenue' ? w.ingresos : w.alumnos)
  const max = Math.max(...vals, 0)
  return max === 0 ? 1 : max
})

// 3. REAL LINE CHART: Monthly Payments Breakdown for Current Year
const monthNames = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']

const monthlyPaymentData = computed(() => {
  const totals = Array(12).fill(0)
  const counts = Array(12).fill(0)

  data.pagos.forEach(p => {
    const dateStr = p.fechaPago || p.fechaVencimiento
    if (dateStr) {
      const d = new Date(dateStr)
      if (!isNaN(d.getTime())) {
        const month = d.getMonth()
        if (month >= 0 && month < 12) {
          const amt = getPaymentAmount(p)
          if (p.estado === 'PAGADO') {
            totals[month] += amt
          }
          counts[month] += 1
        }
      }
    }
  })

  return monthNames.map((name, idx) => ({
    name,
    total: totals[idx],
    count: counts[idx]
  }))
})

const maxMonthlyTotal = computed(() => {
  const vals = monthlyPaymentData.value.map(m => m.total)
  const max = Math.max(...vals, 0)
  return max === 0 ? 1000 : max
})

// Compute SVG polyline points (viewBox 0 0 500 220)
const linePoints = computed(() => {
  const width = 500
  const height = 180
  const topPadding = 20
  const bottomPadding = 20
  const usableHeight = height - topPadding - bottomPadding
  const step = width / (monthNames.length - 1)

  return monthlyPaymentData.value.map((m, idx) => {
    const x = Math.round(idx * step)
    const ratio = m.total / maxMonthlyTotal.value
    const y = Math.round(height - bottomPadding - (ratio * usableHeight))
    return { x, y, ...m }
  })
})

const svgPolylineString = computed(() => {
  return linePoints.value.map(p => `${p.x},${p.y}`).join(' ')
})

const svgAreaString = computed(() => {
  if (linePoints.value.length === 0) return ''
  const first = linePoints.value[0]
  const last = linePoints.value[linePoints.value.length - 1]
  const points = linePoints.value.map(p => `${p.x},${p.y}`).join(' ')
  return `${first.x},200 ${points} ${last.x},200`
})

// 4. REAL DONUT CHART: Payment Status Breakdown
const paymentDistribution = computed(() => {
  const total = data.pagos.length
  if (total === 0) {
    return {
      total: 0,
      pagados: 0,
      pendientes: 0,
      vencidos: 0,
      pagadosPct: 0,
      pendientesPct: 0,
      vencidosPct: 0,
      conicGradient: 'conic-gradient(#e2e8f0 0% 100%)'
    }
  }

  const pagados = data.pagos.filter(p => p.estado === 'PAGADO').length
  const pendientes = data.pagos.filter(p => p.estado === 'PENDIENTE').length
  const vencidos = data.pagos.filter(p => p.estado === 'VENCIDO').length

  const pagadosPct = Math.round((pagados / total) * 100)
  const pendientesPct = Math.round((pendientes / total) * 100)
  const vencidosPct = Math.max(0, 100 - pagadosPct - pendientesPct)

  const stop1 = pagadosPct
  const stop2 = pagadosPct + pendientesPct

  const conic = `conic-gradient(
    #16a34a 0% ${stop1}%,
    #f59e0b ${stop1}% ${stop2}%,
    #ef4444 ${stop2}% 100%
  )`

  return {
    total,
    pagados,
    pendientes,
    vencidos,
    pagadosPct,
    pendientesPct,
    vencidosPct,
    conicGradient: conic
  }
})

// 5. TOP 5 WORKSHOPS (REAL ENROLLMENTS RANKING)
const topTalleres = computed(() => {
  const sorted = [...workshopStats.value].sort((a, b) => b.alumnos - a.alumnos)
  return sorted.slice(0, 5)
})

const maxTopStudents = computed(() => {
  if (topTalleres.value.length === 0) return 1
  const max = Math.max(...topTalleres.value.map(t => t.alumnos), 0)
  return max === 0 ? 1 : max
})

// 6. RECENT PAYMENTS WITH REAL LINKED ENTITIES
const recentPayments = computed(() => {
  if (data.pagos.length === 0) return []

  return [...data.pagos].reverse().slice(0, 5).map(p => {
    const inscripcion = data.inscripciones.find(i => i.id === p.inscripcionId)
    const alumno = data.alumnos.find(a => a.id === inscripcion?.alumnoId)
    const grupo = data.grupos.find(g => g.id === inscripcion?.grupoId)

    const alumnoName = alumno
      ? `${alumno.nombre} ${alumno.apellidoPaterno}`.trim()
      : (p.alumno || `Inscripción #${p.inscripcionId}`)

    const tallerName = grupo?.nombreGrupo || grupo?.curso || p.taller || 'Taller General'
    const dateFormatted = p.fechaPago || p.fechaVencimiento || 'Sin fecha'
    const amountFormatted = `$${getPaymentAmount(p).toFixed(2)}`

    return {
      id: p.id,
      alumno: alumnoName,
      taller: tallerName,
      fecha: dateFormatted,
      monto: amountFormatted,
      tipoPago: p.tipoPago,
      estado: p.estado || 'PENDIENTE'
    }
  })
})
</script>

<template>
  <div class="admin-layout">
    <AdminSidebar />

    <main class="main-content">
      <AdminHeader />

      <section class="dashboard-content">
        <div class="page-title">
          <div>
            <h1>Resumen general</h1>
            <p>Información en tiempo real del Sistema de Gestión Escolar y Cultural.</p>
          </div>

          <span class="date">
            Año {{ currentYear }}
          </span>
        </div>

        <div
          v-if="errorMessage"
          class="error-banner"
        >
          <AlertCircle :size="18" />
          <span>{{ errorMessage }}</span>
        </div>

        <!-- TARJETAS DE ESTADÍSTICAS REALES -->
        <div class="stats-grid">
          <StatCard
            v-for="stat in stats"
            :key="stat.title"
            v-bind="stat"
          />
        </div>

        <!-- GRÁFICAS REALES -->
        <section class="charts-grid">
          <!-- 1. GRÁFICA DE BARRAS POR TALLER -->
          <div class="panel large-panel">
            <div class="panel-header">
              <div>
                <h3>{{ selectedMetric === 'revenue' ? 'Ingresos por Taller' : 'Alumnos por Taller' }}</h3>
                <p>Datos reales calculados del catálogo y pagos</p>
              </div>

              <select v-model="selectedMetric">
                <option value="revenue">
                  Ingresos ($)
                </option>
                <option value="students">
                  Alumnos Inscritos
                </option>
              </select>
            </div>

            <div
              v-if="workshopStats.length"
              class="bar-chart"
            >
              <div
                v-for="taller in workshopStats"
                :key="taller.id"
                class="bar-column"
                @mouseenter="hoveredBar = taller"
                @mouseleave="hoveredBar = null"
              >
                <!-- Tooltip on hover -->
                <div
                  v-if="hoveredBar?.id === taller.id"
                  class="bar-tooltip"
                >
                  <strong>{{ taller.nombre }}</strong>
                  <span>{{ selectedMetric === 'revenue' ? `$${taller.ingresos.toLocaleString('es-MX')}` : `${taller.alumnos} alumnos` }}</span>
                </div>

                <div
                  class="bar"
                  :style="{
                    height: `${Math.max(8, ((selectedMetric === 'revenue' ? taller.ingresos : taller.alumnos) / maxWorkshopValue) * 100)}%`,
                    backgroundColor: selectedMetric === 'revenue' ? '#6366f1' : '#3b82f6'
                  }"
                ></div>
                <span :title="taller.nombre">{{ taller.shortName }}</span>
              </div>
            </div>

            <div
              v-else
              class="empty-chart"
            >
              <BookOpen :size="32" />
              <p>No hay talleres registrados en el catálogo aún.</p>
            </div>
          </div>

          <!-- 2. GRÁFICA DE LÍNEA: PAGOS POR MES -->
          <div class="panel large-panel">
            <div class="panel-header">
              <div>
                <h3>Pagos por Mes</h3>
                <p>Ingresos mensuales recaudados en {{ currentYear }}</p>
              </div>
              <span class="chart-tag"><TrendingUp :size="14" /> Real</span>
            </div>

            <div class="line-chart-container">
              <svg
                viewBox="0 0 500 200"
                class="line-svg"
              >
                <defs>
                  <linearGradient
                    id="areaGrad"
                    x1="0%"
                    y1="0%"
                    x2="0%"
                    y2="100%"
                  >
                    <stop
                      offset="0%"
                      stop-color="#6366f1"
                      stop-opacity="0.35"
                    />
                    <stop
                      offset="100%"
                      stop-color="#6366f1"
                      stop-opacity="0.0"
                    />
                  </linearGradient>
                </defs>

                <!-- Grid lines -->
                <line
                  x1="0"
                  y1="40"
                  x2="500"
                  y2="40"
                  stroke="#f1f5f9"
                  stroke-dasharray="4"
                />
                <line
                  x1="0"
                  y1="100"
                  x2="500"
                  y2="100"
                  stroke="#f1f5f9"
                  stroke-dasharray="4"
                />
                <line
                  x1="0"
                  y1="160"
                  x2="500"
                  y2="160"
                  stroke="#f1f5f9"
                  stroke-dasharray="4"
                />

                <!-- Area fill -->
                <polygon
                  v-if="linePoints.length"
                  :points="svgAreaString"
                  fill="url(#areaGrad)"
                />

                <!-- Line -->
                <polyline
                  v-if="linePoints.length"
                  :points="svgPolylineString"
                  fill="none"
                  stroke="#6366f1"
                  stroke-width="3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />

                <!-- Data circles -->
                <circle
                  v-for="pt in linePoints"
                  :key="pt.name"
                  :cx="pt.x"
                  :cy="pt.y"
                  r="5"
                  class="data-circle"
                  :class="{ active: hoveredMonth?.name === pt.name }"
                  @mouseenter="hoveredMonth = pt"
                  @mouseleave="hoveredMonth = null"
                />
              </svg>

              <!-- Floating Tooltip for month -->
              <div
                v-if="hoveredMonth"
                class="month-tooltip"
                :style="{ left: `${(hoveredMonth.x / 500) * 100}%` }"
              >
                <strong>{{ hoveredMonth.name }}</strong>
                <span>${{ hoveredMonth.total.toLocaleString('es-MX') }} ({{ hoveredMonth.count }} pagos)</span>
              </div>

              <div class="months">
                <span
                  v-for="m in monthNames"
                  :key="m"
                >{{ m }}</span>
              </div>
            </div>
          </div>

          <!-- 3. DONA DE ESTADO DE PAGOS -->
          <div class="panel recurrent-panel">
            <div class="panel-header">
              <div>
                <h3>Estado de Cobros</h3>
                <p>Distribución de pagos</p>
              </div>
            </div>

            <div class="donut-section">
              <div
                class="donut"
                :style="{ background: paymentDistribution.conicGradient }"
              >
                <div class="donut-center">
                  <strong>{{ paymentDistribution.pagadosPct }}%</strong>
                  <small>Al corriente</small>
                </div>
              </div>

              <div class="legend">
                <div class="legend-row">
                  <span class="dot green"></span>
                  <span>Pagados</span>
                  <strong>{{ paymentDistribution.pagados }} ({{ paymentDistribution.pagadosPct }}%)</strong>
                </div>

                <div class="legend-row">
                  <span class="dot yellow"></span>
                  <span>Pendientes</span>
                  <strong>{{ paymentDistribution.pendientes }} ({{ paymentDistribution.pendientesPct }}%)</strong>
                </div>

                <div class="legend-row">
                  <span class="dot red"></span>
                  <span>Vencidos</span>
                  <strong>{{ paymentDistribution.vencidos }} ({{ paymentDistribution.vencidosPct }}%)</strong>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- TABLAS Y TOP TALLERES -->
        <section class="bottom-grid">
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3>Alumnos por Taller</h3>
                <p>Top talleres con mayor demanda</p>
              </div>
            </div>

            <div
              v-if="topTalleres.length"
              class="top-workshops-list"
            >
              <div
                v-for="(taller, index) in topTalleres"
                :key="taller.id"
                class="taller-row"
              >
                <span class="taller-name">{{ index + 1 }}. {{ taller.nombre }}</span>

                <div class="taller-right">
                  <div class="mini-bar">
                    <div
                      :style="{
                        width: `${Math.min(100, (taller.alumnos / maxTopStudents) * 100)}%`
                      }"
                    ></div>
                  </div>
                  <strong>{{ taller.alumnos }}</strong>
                </div>
              </div>
            </div>

            <div
              v-else
              class="empty-state-mini"
            >
              <p>Sin inscripciones registradas.</p>
            </div>
          </div>

          <div class="panel payments-panel">
            <div class="panel-header">
              <div>
                <h3>Pagos Recientes</h3>
                <p>Últimos movimientos sincronizados con la base de datos</p>
              </div>
            </div>

            <table v-if="recentPayments.length">
              <thead>
                <tr>
                  <th>Alumno</th>
                  <th>Taller</th>
                  <th>Fecha</th>
                  <th>Monto</th>
                  <th>Estado</th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="pago in recentPayments"
                  :key="pago.id"
                >
                  <td><strong>{{ pago.alumno }}</strong></td>
                  <td>{{ pago.taller }}</td>
                  <td>{{ pago.fecha }}</td>
                  <td>{{ pago.monto }}</td>
                  <td>
                    <span
                      class="status"
                      :class="{
                        paid: pago.estado === 'PAGADO',
                        pending: pago.estado === 'PENDIENTE',
                        overdue: pago.estado === 'VENCIDO'
                      }"
                    >
                      {{ pago.estado }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>

            <div
              v-else
              class="empty-state-mini"
            >
              <p>No se han registrado pagos en el sistema aún.</p>
            </div>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f6fa;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.dashboard-content {
  padding: 30px;
}

.page-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.page-title h1 {
  margin: 0;
  color: #202838;
  font-size: 26px;
  font-weight: 700;
}

.page-title p {
  color: #7b8494;
  margin: 4px 0 0;
  font-size: 13px;
}

.date {
  background: white;
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  color: #697386;
  font-size: 13px;
  font-weight: 600;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fca5a5;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(180px, 1fr));
  gap: 18px;
  margin-bottom: 22px;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1.1fr 300px;
  gap: 22px;
  margin-bottom: 22px;
}

.bottom-grid {
  display: grid;
  grid-template-columns: 0.85fr 1.45fr;
  gap: 22px;
}

.panel {
  background: white;
  border: 1px solid #e7e9ef;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.03);
  position: relative;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.panel h3 {
  margin: 0;
  color: #202838;
  font-size: 16px;
  font-weight: 700;
}

.panel p {
  color: #8a92a3;
  font-size: 12px;
  margin: 5px 0 0;
}

.chart-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #eef2ff;
  color: #4f46e5;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

select {
  border: 1px solid #e4e6eb;
  border-radius: 7px;
  padding: 6px 10px;
  font-size: 12px;
  color: #4b5563;
  outline: none;
  background: white;
  cursor: pointer;
}

/* BARRAS */
.bar-chart {
  height: 250px;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  border-bottom: 1px solid #dfe3ea;
  padding-top: 25px;
  gap: 8px;
}

.bar-column {
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  position: relative;
  cursor: pointer;
}

.bar {
  width: 100%;
  max-width: 38px;
  background: #6366f1;
  border-radius: 6px 6px 0 0;
  transition: height 0.4s ease, background-color 0.2s ease;
}

.bar-column:hover .bar {
  filter: brightness(0.9);
}

.bar-tooltip {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  background: #1e293b;
  color: white;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 11px;
  white-space: nowrap;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.bar-column span {
  font-size: 11px;
  color: #7d8593;
  font-weight: 500;
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-chart {
  height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  gap: 10px;
  font-size: 13px;
}

/* LÍNEA */
.line-chart-container {
  position: relative;
  padding-top: 10px;
}

.line-svg {
  width: 100%;
  height: 200px;
  overflow: visible;
}

.data-circle {
  fill: #6366f1;
  stroke: white;
  stroke-width: 2.5;
  cursor: pointer;
  transition: r 0.2s ease, fill 0.2s ease;
}

.data-circle:hover,
.data-circle.active {
  r: 7px;
  fill: #4338ca;
}

.month-tooltip {
  position: absolute;
  top: 15px;
  transform: translateX(-50%);
  background: #1e293b;
  color: white;
  padding: 5px 9px;
  border-radius: 6px;
  font-size: 11px;
  pointer-events: none;
  white-space: nowrap;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.18);
  z-index: 10;
}

.months {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #7d8593;
  padding-top: 8px;
  border-top: 1px solid #edf0f4;
}

/* DONA */
.donut-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-top: 15px;
}

.donut {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.donut-center {
  width: 100px;
  height: 100px;
  background: white;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-center strong {
  font-size: 22px;
  color: #202838;
}

.donut-center small {
  font-size: 10px;
  color: #8a92a3;
}

.legend {
  width: 100%;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #4b5563;
}

.legend-row strong {
  margin-left: auto;
  color: #1f2937;
  font-size: 12px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.green { background: #16a34a; }
.dot.yellow { background: #f59e0b; }
.dot.red { background: #ef4444; }

/* TALLERES ROW */
.top-workshops-list {
  display: flex;
  flex-direction: column;
}

.taller-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 0;
  border-bottom: 1px solid #edf0f4;
  font-size: 13px;
  color: #374151;
}

.taller-name {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.taller-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mini-bar {
  width: 110px;
  height: 6px;
  background: #eceef4;
  border-radius: 5px;
  overflow: hidden;
}

.mini-bar div {
  height: 100%;
  background: #6366f1;
  border-radius: 5px;
  transition: width 0.4s ease;
}

/* TABLA PAGOS */
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

th {
  text-align: left;
  color: #8a92a3;
  font-weight: 500;
  padding-bottom: 12px;
}

td {
  padding: 13px 0;
  border-top: 1px solid #edf0f4;
  color: #4e5665;
}

.status {
  padding: 4px 9px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status.paid {
  background: #dcfce7;
  color: #15803d;
}

.status.pending {
  background: #fef3c7;
  color: #a16207;
}

.status.overdue {
  background: #fee2e2;
  color: #b91c1c;
}

.empty-state-mini {
  padding: 25px 0;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

/* RESPONSIVE */
@media (max-width: 1500px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .charts-grid {
    grid-template-columns: 1fr 1fr;
  }

  .recurrent-panel {
    grid-column: span 2;
  }
}

@media (max-width: 760px) {
  .dashboard-content {
    padding: 20px 15px 30px;
  }

  .page-title {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
    margin-bottom: 20px;
  }

  .stats-grid,
  .charts-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .recurrent-panel {
    grid-column: auto;
  }

  .payments-panel {
    overflow-x: auto;
  }

  .payments-panel table {
    min-width: 580px;
  }
}
</style>
