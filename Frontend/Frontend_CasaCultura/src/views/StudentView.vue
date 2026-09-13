<script setup>
import { computed, onMounted, ref } from 'vue'
import DashboardLayout from '../components/DashboardLayout.vue'
import { clearStoredSession } from '../services/apiService'
import { getStudentDashboard } from '../services/studentService'
import { useRouter } from 'vue-router'
import {
  LayoutDashboard,
  BookOpen,
  Calendar,
  CalendarCheck2,
  CreditCard,
  QrCode,
  Clock,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Sparkles,
  ArrowRight,
  Printer,
  ChevronRight,
  ShieldCheck,
  UserCheck,
  Award
} from 'lucide-vue-next'

const router = useRouter()
const activeView = ref('resumen')
const dashboard = ref(null)
const errorMessage = ref('')
const isLoading = ref(true)

const navItems = [
  { id: 'resumen', label: 'Mi Dashboard', icon: LayoutDashboard },
  { id: 'cursos', label: 'Mis Cursos', icon: BookOpen },
  { id: 'horarios', label: 'Mis Horarios', icon: Calendar },
  { id: 'asistencias', label: 'Asistencias', icon: CalendarCheck2 },
  { id: 'pagos', label: 'Mis Pagos', icon: CreditCard },
  { id: 'credencial', label: 'Mi Credencial', icon: QrCode },
]

onMounted(async () => {
  try {
    dashboard.value = await getStudentDashboard()
  } catch (error) {
    errorMessage.value = error.message || 'No se pudo cargar la información del alumno.'
    if (error.message && (error.message.includes('401') || error.message.includes('403'))) {
      clearStoredSession()
      router.push('/')
    }
  } finally {
    isLoading.value = false
  }
})

// Helpers
const student = computed(() => dashboard.value?.alumno || {})
const studentFullName = computed(() => {
  if (!student.value.nombre) return 'Estudiante'
  return `${student.value.nombre} ${student.value.apellidoPaterno || ''} ${student.value.apellidoMaterno || ''}`.trim()
})

const studentMatricula = computed(() => student.value.matricula || 'ALU-2026')

const activeTitle = computed(() => navItems.find((item) => item.id === activeView.value)?.label || 'Dashboard')

// Metrics
const enrolledCount = computed(() => dashboard.value?.inscripciones?.length || 0)
const schedulesCount = computed(() => dashboard.value?.horarios?.length || 0)

const totalAttendances = computed(() => dashboard.value?.asistencias?.length || 0)
const presentCount = computed(() => dashboard.value?.asistencias?.filter(a => a.estado === 'PRESENTE').length || 0)
const lateCount = computed(() => dashboard.value?.asistencias?.filter(a => a.estado === 'RETARDO').length || 0)
const absentCount = computed(() => dashboard.value?.asistencias?.filter(a => a.estado === 'FALTA').length || 0)

const attendanceRate = computed(() => {
  if (totalAttendances.value === 0) return 100
  return Math.round((presentCount.value / totalAttendances.value) * 100)
})

const pendingPayments = computed(() => {
  return dashboard.value?.pagos?.filter(p => p.estado === 'PENDIENTE' || p.estado === 'VENCIDO').length || 0
})

const paidPaymentsCount = computed(() => {
  return dashboard.value?.pagos?.filter(p => p.estado === 'PAGADO').length || 0
})

const totalPaidAmount = computed(() => {
  return dashboard.value?.pagos
    ?.filter(p => p.estado === 'PAGADO')
    ?.reduce((sum, p) => sum + (Number(p.monto) || 400), 0) || 0
})

// Day names in Spanish
const dayNames = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo'
}

function translateDay(dia) {
  return dayNames[dia] || dia || 'Sin día'
}

function formatDate(value) {
  if (!value) return '-'
  try {
    return new Intl.DateTimeFormat('es-MX', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    }).format(new Date(`${value}T00:00:00`))
  } catch {
    return value
  }
}

function formatCurrency(amount) {
  return new Intl.NumberFormat('es-MX', {
    style: 'currency',
    currency: 'MXN'
  }).format(amount || 0)
}

function printCredential() {
  window.print()
}

// Current date formatted
const currentDateFormatted = computed(() => {
  return new Intl.DateTimeFormat('es-MX', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date())
})
</script>

<template>
  <DashboardLayout
    role="ALUMNO"
    :username="studentMatricula"
    :full-name="studentFullName"
    :badge="studentMatricula"
    :items="navItems"
    :active="activeView"
    @navigate="activeView = $event"
  >
    <!-- LOADING STATE -->
    <div v-if="isLoading" class="state-card loading-state">
      <div class="loader-spinner"></div>
      <div class="loader-text">
        <strong>Cargando tu información académica...</strong>
        <span>Sincronizando talleres, horarios y asistencias</span>
      </div>
    </div>

    <!-- ERROR STATE -->
    <div v-else-if="errorMessage" class="state-card error-state">
      <AlertTriangle :size="32" class="error-icon" />
      <div>
        <strong>Error al obtener tus datos</strong>
        <p>{{ errorMessage }}</p>
      </div>
    </div>

    <!-- MAIN VIEWS -->
    <template v-else-if="dashboard">
      <!-- ============================================================
           1. RESUMEN / MI DASHBOARD
           ============================================================ -->
      <template v-if="activeView === 'resumen'">
        <!-- HERO GREETING BANNER -->
        <div class="hero-welcome-banner">
          <div class="banner-content">
            <div class="banner-pill">
              <Sparkles :size="13" />
              <span>Portal de Estudiantes</span>
            </div>
            <h1 class="banner-title">¡Bienvenido(a), {{ student.nombre }}!</h1>
            <p class="banner-desc">
              Consulta el estado de tus talleres artísticos, horarios semanales, registro de asistencias y comprobantes de pago en la Casa de la Cultura de Tlaxiaco.
            </p>
            <div class="banner-meta">
              <span class="meta-tag">
                <strong>Matrícula:</strong> {{ studentMatricula }}
              </span>
              <span class="meta-tag">
                <span class="status-dot"></span>
                Estudiante Activo
              </span>
              <span class="meta-tag date-tag">
                {{ currentDateFormatted }}
              </span>
            </div>
          </div>
          <div class="banner-visual">
            <div class="visual-badge">
              <Award :size="46" class="award-icon" />
              <span>Ciclo 2026</span>
            </div>
          </div>
        </div>

        <!-- STAT CARDS (Al estilo StatCard de Super Admin) -->
        <div class="stats-row">
          <!-- Card 1: Talleres -->
          <div class="stat-card stat-indigo" @click="activeView = 'cursos'">
            <div class="stat-icon-wrapper">
              <BookOpen :size="22" />
            </div>
            <div class="stat-info">
              <span class="stat-title">Talleres Inscritos</span>
              <div class="stat-value">{{ enrolledCount }}</div>
              <span class="stat-subtitle">
                {{ enrolledCount === 1 ? '1 taller activo' : `${enrolledCount} talleres activos` }}
              </span>
            </div>
            <ChevronRight :size="16" class="stat-arrow" />
          </div>

          <!-- Card 2: Horarios -->
          <div class="stat-card stat-sky" @click="activeView = 'horarios'">
            <div class="stat-icon-wrapper">
              <Clock :size="22" />
            </div>
            <div class="stat-info">
              <span class="stat-title">Horarios de Clase</span>
              <div class="stat-value">{{ schedulesCount }}</div>
              <span class="stat-subtitle">Sesiones semanales</span>
            </div>
            <ChevronRight :size="16" class="stat-arrow" />
          </div>

          <!-- Card 3: Asistencia -->
          <div class="stat-card stat-emerald" @click="activeView = 'asistencias'">
            <div class="stat-icon-wrapper">
              <CalendarCheck2 :size="22" />
            </div>
            <div class="stat-info">
              <span class="stat-title">Asistencia Total</span>
              <div class="stat-value">{{ attendanceRate }}%</div>
              <span class="stat-subtitle">
                {{ presentCount }} de {{ totalAttendances }} clases registradas
              </span>
            </div>
            <ChevronRight :size="16" class="stat-arrow" />
          </div>

          <!-- Card 4: Estado de Pagos -->
          <div class="stat-card stat-amber" @click="activeView = 'pagos'">
            <div class="stat-icon-wrapper">
              <CreditCard :size="22" />
            </div>
            <div class="stat-info">
              <span class="stat-title">Estado de Cuenta</span>
              <div class="stat-value">
                {{ pendingPayments === 0 ? 'Al corriente' : `${pendingPayments} pend.` }}
              </div>
              <span class="stat-subtitle">
                {{ paidPaymentsCount }} pagos registrados
              </span>
            </div>
            <ChevronRight :size="16" class="stat-arrow" />
          </div>
        </div>

        <!-- TWO COLUMN DASHBOARD GRID -->
        <div class="dashboard-grid">
          <!-- Left Column: Active Workshops -->
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3>Mis Talleres y Cursos</h3>
                <p>Cursos en los que estás participando actualmente</p>
              </div>
              <button
                type="button"
                class="panel-action-btn"
                @click="activeView = 'cursos'"
              >
                Ver todos <ArrowRight :size="14" />
              </button>
            </div>

            <div v-if="dashboard.inscripciones.length" class="workshop-cards-list">
              <div
                v-for="insc in dashboard.inscripciones"
                :key="insc.id"
                class="workshop-card"
              >
                <div class="workshop-icon-box">
                  <BookOpen :size="18" />
                </div>
                <div class="workshop-details">
                  <div class="workshop-title-row">
                    <strong>{{ insc.grupo }}</strong>
                    <span class="status-pill status-active">
                      {{ insc.estado || 'Activo' }}
                    </span>
                  </div>
                  <div class="workshop-meta-row">
                    <span><strong>Inscrito:</strong> {{ formatDate(insc.fechaInscripcion) }}</span>
                    <span>&bull;</span>
                    <span>Matrícula: {{ insc.matricula }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty-panel-box">
              <BookOpen :size="28" />
              <p>Aún no tienes inscripciones activas registradas.</p>
            </div>
          </div>

          <!-- Right Column: Weekly Schedule -->
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3>Horarios de la Semana</h3>
                <p>Días y horarios en los que debes asistir</p>
              </div>
              <button
                type="button"
                class="panel-action-btn"
                @click="activeView = 'horarios'"
              >
                Ver agenda <ArrowRight :size="14" />
              </button>
            </div>

            <div v-if="dashboard.horarios.length" class="schedule-mini-list">
              <div
                v-for="sch in dashboard.horarios"
                :key="sch.id"
                class="schedule-mini-card"
              >
                <div class="schedule-day-badge">
                  {{ translateDay(sch.dia) }}
                </div>
                <div class="schedule-mini-info">
                  <strong>{{ sch.nombreGrupo || sch.nombre }}</strong>
                  <span>
                    <Clock :size="12" />
                    {{ sch.horaInicio ? String(sch.horaInicio).substring(0, 5) : '00:00' }} -
                    {{ sch.horaFin ? String(sch.horaFin).substring(0, 5) : '00:00' }}
                  </span>
                </div>
                <span class="taller-tag">Taller</span>
              </div>
            </div>

            <div v-else class="empty-panel-box">
              <Clock :size="28" />
              <p>No hay horarios asignados para tus cursos.</p>
            </div>
          </div>
        </div>

        <!-- RECENT ATTENDANCE AND PAYMENTS ROW -->
        <div class="dashboard-grid">
          <!-- Recent Attendances -->
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3>Asistencias Recientes</h3>
                <p>Últimos registros en tus clases</p>
              </div>
              <button
                type="button"
                class="panel-action-btn"
                @click="activeView = 'asistencias'"
              >
                Historial <ArrowRight :size="14" />
              </button>
            </div>

            <div v-if="dashboard.asistencias.length" class="attendance-mini-list">
              <div
                v-for="att in dashboard.asistencias.slice(0, 4)"
                :key="att.id"
                class="attendance-item"
              >
                <div class="date-chip">
                  <span class="day-num">{{ new Date(`${att.fecha}T00:00:00`).getDate() }}</span>
                  <span class="month-name">{{ new Intl.DateTimeFormat('es-MX', { month: 'short' }).format(new Date(`${att.fecha}T00:00:00`)) }}</span>
                </div>
                <div class="att-info">
                  <strong>{{ att.grupo }}</strong>
                  <small>{{ formatDate(att.fecha) }} &bull; {{ att.horaRegistro ? att.horaRegistro.slice(11, 16) : 'Registro general' }}</small>
                </div>
                <span
                  class="status-pill"
                  :class="{
                    'status-active': att.estado === 'PRESENTE',
                    'status-warning': att.estado === 'RETARDO',
                    'status-danger': att.estado === 'FALTA'
                  }"
                >
                  {{ att.estado }}
                </span>
              </div>
            </div>

            <div v-else class="empty-panel-box">
              <CalendarCheck2 :size="28" />
              <p>Aún no hay registros de asistencia recientes.</p>
            </div>
          </div>

          <!-- Quick ID Card Access Widget -->
          <div class="panel card-highlight-panel">
            <div class="id-banner-content">
              <div class="id-badge-icon">
                <QrCode :size="36" />
              </div>
              <div>
                <h3>Credencial Digital de Estudiante</h3>
                <p>Presenta tu código QR y matrícula oficial para ingresar a las instalaciones o registrar tu asistencia.</p>
              </div>
              <button
                type="button"
                class="primary-btn"
                @click="activeView = 'credencial'"
              >
                <QrCode :size="16" />
                <span>Ver Mi Credencial</span>
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- ============================================================
           2. MIS CURSOS
           ============================================================ -->
      <section v-else-if="activeView === 'cursos'" class="panel full-width-panel">
        <div class="panel-header">
          <div>
            <h2>Cursos y Talleres Inscritos</h2>
            <p>Lista oficial de actividades culturales en las que te encuentras registrado</p>
          </div>
          <span class="badge-count">{{ dashboard.inscripciones.length }} Inscritos</span>
        </div>

        <div v-if="dashboard.inscripciones.length" class="table-container">
          <table class="modern-table">
            <thead>
              <tr>
                <th>Taller / Grupo</th>
                <th>Matrícula</th>
                <th>Fecha de Inscripción</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in dashboard.inscripciones" :key="c.id">
                <td>
                  <div class="table-cell-title">
                    <div class="table-icon-circle">
                      <BookOpen :size="16" />
                    </div>
                    <strong>{{ c.grupo }}</strong>
                  </div>
                </td>
                <td>
                  <span class="code-chip">{{ c.matricula }}</span>
                </td>
                <td>{{ formatDate(c.fechaInscripcion) }}</td>
                <td>
                  <span class="status-pill status-active">{{ c.estado || 'ACTIVA' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="empty-state">
          <BookOpen :size="48" />
          <h3>Sin talleres activos</h3>
          <p>No tienes ningún taller registrado en este momento. Acércate a la coordinación de la Casa de la Cultura para realizar tu inscripción.</p>
        </div>
      </section>

      <!-- ============================================================
           3. MIS HORARIOS
           ============================================================ -->
      <section v-else-if="activeView === 'horarios'" class="panel full-width-panel">
        <div class="panel-header">
          <div>
            <h2>Horarios y Días de Clase</h2>
            <p>Consulta la agenda semanal de tus talleres artísticos y culturales</p>
          </div>
          <span class="badge-count">{{ dashboard.horarios.length }} Sesiones</span>
        </div>

        <div v-if="dashboard.horarios.length" class="schedule-grid-cards">
          <div
            v-for="h in dashboard.horarios"
            :key="h.id"
            class="schedule-card-full"
          >
            <div class="card-day-header">
              <Calendar :size="15" />
              <span>{{ translateDay(h.dia) }}</span>
            </div>
            <div class="card-main-content">
              <h4>{{ h.nombreGrupo || h.nombre }}</h4>
              <div class="time-row">
                <Clock :size="14" class="clock-icon" />
                <span>{{ h.horaInicio ? String(h.horaInicio).substring(0, 5) : '00:00' }} a {{ h.horaFin ? String(h.horaFin).substring(0, 5) : '00:00' }} hrs</span>
              </div>
            </div>
            <div class="card-footer">
              <span class="tag-badge">Taller Cultural</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <Clock :size="48" />
          <h3>Sin horarios registrados</h3>
          <p>No se encontraron horarios asignados para los talleres en los que estás inscrito.</p>
        </div>
      </section>

      <!-- ============================================================
           4. ASISTENCIAS
           ============================================================ -->
      <section v-else-if="activeView === 'asistencias'" class="panel full-width-panel">
        <div class="panel-header">
          <div>
            <h2>Historial de Asistencias</h2>
            <p>Control de cumplimiento en tus clases y talleres culturales</p>
          </div>
        </div>

        <!-- Attendance Stats Overview Bar -->
        <div class="attendance-summary-bar">
          <div class="summary-metric">
            <span>Total Clases</span>
            <strong>{{ totalAttendances }}</strong>
          </div>
          <div class="summary-metric text-emerald">
            <span>Presentes</span>
            <strong>{{ presentCount }}</strong>
          </div>
          <div class="summary-metric text-amber">
            <span>Retardos</span>
            <strong>{{ lateCount }}</strong>
          </div>
          <div class="summary-metric text-rose">
            <span>Faltas</span>
            <strong>{{ absentCount }}</strong>
          </div>
          <div class="summary-progress-col">
            <div class="progress-labels">
              <span>Porcentaje de Asistencia</span>
              <strong>{{ attendanceRate }}%</strong>
            </div>
            <div class="progress-track">
              <div
                class="progress-fill"
                :style="{ width: `${attendanceRate}%` }"
              ></div>
            </div>
          </div>
        </div>

        <div v-if="dashboard.asistencias.length" class="table-container">
          <table class="modern-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Taller / Grupo</th>
                <th>Hora de Registro</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="att in dashboard.asistencias" :key="att.id">
                <td>
                  <strong>{{ formatDate(att.fecha) }}</strong>
                </td>
                <td>{{ att.grupo }}</td>
                <td>{{ att.horaRegistro ? att.horaRegistro.slice(11, 16) + ' hrs' : '-' }}</td>
                <td>
                  <span
                    class="status-pill"
                    :class="{
                      'status-active': att.estado === 'PRESENTE',
                      'status-warning': att.estado === 'RETARDO',
                      'status-danger': att.estado === 'FALTA'
                    }"
                  >
                    {{ att.estado }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="empty-state">
          <CalendarCheck2 :size="48" />
          <h3>No hay registros de asistencia</h3>
          <p>Tu docente registrará la asistencia al iniciar cada una de las sesiones.</p>
        </div>
      </section>

      <!-- ============================================================
           5. MIS PAGOS
           ============================================================ -->
      <section v-else-if="activeView === 'pagos'" class="panel full-width-panel">
        <div class="panel-header">
          <div>
            <h2>Historial de Pagos y Colegiaturas</h2>
            <p>Control financiero de inscripciones y mensualidades</p>
          </div>
        </div>

        <!-- Payments Overview -->
        <div class="payments-overview-bar">
          <div class="summary-metric">
            <span>Pagos Realizados</span>
            <strong>{{ paidPaymentsCount }}</strong>
          </div>
          <div class="summary-metric text-emerald">
            <span>Total Liquidado</span>
            <strong>{{ formatCurrency(totalPaidAmount) }}</strong>
          </div>
          <div class="summary-metric text-amber">
            <span>Pagos Pendientes</span>
            <strong>{{ pendingPayments }}</strong>
          </div>
        </div>

        <div v-if="dashboard.pagos.length" class="table-container">
          <table class="modern-table">
            <thead>
              <tr>
                <th>Concepto</th>
                <th>Periodo</th>
                <th>Monto</th>
                <th>Fecha de Vencimiento</th>
                <th>Fecha de Pago</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in dashboard.pagos" :key="p.id">
                <td>
                  <strong>{{ p.tipoPago || 'Mensualidad' }}</strong>
                </td>
                <td>{{ p.periodo || 'Periodo Regular' }}</td>
                <td>
                  <strong>{{ formatCurrency(p.monto || 400) }}</strong>
                </td>
                <td>{{ formatDate(p.fechaVencimiento) }}</td>
                <td>{{ p.fechaPago ? formatDate(p.fechaPago) : 'Pendiente' }}</td>
                <td>
                  <span
                    class="status-pill"
                    :class="{
                      'status-active': p.estado === 'PAGADO',
                      'status-warning': p.estado === 'PENDIENTE',
                      'status-danger': p.estado === 'VENCIDO'
                    }"
                  >
                    {{ p.estado }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="empty-state">
          <CreditCard :size="48" />
          <h3>Sin registros de pago</h3>
          <p>No se encontraron comprobantes de pago asociados a tu matrícula de estudiante.</p>
        </div>
      </section>

      <!-- ============================================================
           6. MI CREDENCIAL DIGITAL (OFICIAL)
           ============================================================ -->
      <section v-else class="credential-view-container">
        <div class="credential-action-header">
          <div>
            <h2>Credencial Digital Estudiantil</h2>
            <p>Identificación oficial para el acceso y uso de las instalaciones de la Casa de la Cultura</p>
          </div>
          <button
            type="button"
            class="print-btn"
            @click="printCredential"
          >
            <Printer :size="16" />
            <span>Imprimir Credencial</span>
          </button>
        </div>

        <!-- Credential Card Container -->
        <div class="official-credential-card">
          <!-- Institutional Header with Logo -->
          <div class="card-header">
            <div class="header-logo-container">
              <img
                src="../assets/casacul.png"
                alt="Casa de la Cultura Tlaxiaco"
                class="cred-logo-img"
              />
            </div>
            <div class="header-titles">
              <h3>CASA DE LA CULTURA DE TLAXIACO</h3>
              <p>HEROICA CIUDAD DE TLAXIACO, OAXACA</p>
              <span class="cred-cycle">CREDENCIAL OFICIAL &bull; CICLO 2026</span>
            </div>
          </div>

          <!-- Credential Body -->
          <div class="card-body">
            <!-- Left: Avatar / Student Photo -->
            <div class="student-photo-col">
              <div class="photo-frame">
                <span class="student-initials">
                  {{ student.nombre ? student.nombre.charAt(0) : 'A' }}
                </span>
              </div>
              <span class="badge-status-active">
                <span class="dot"></span> ALUMNO ACTIVO
              </span>
            </div>

            <!-- Center: Student Details -->
            <div class="student-info-col">
              <div class="info-group">
                <span class="label">NOMBRE DEL ESTUDIANTE</span>
                <strong class="student-fullname">{{ studentFullName }}</strong>
              </div>

              <div class="info-grid-2">
                <div class="info-group">
                  <span class="label">MATRÍCULA OFICIAL</span>
                  <span class="value-highlight">{{ studentMatricula }}</span>
                </div>
                <div class="info-group">
                  <span class="label">CORREO ELECTRÓNICO</span>
                  <span class="value-text">{{ student.correo || 'Registrado en sistema' }}</span>
                </div>
                <div class="info-group">
                  <span class="label">TELÉFONO</span>
                  <span class="value-text">{{ student.telefono || 'Sin registrar' }}</span>
                </div>
                <div class="info-group">
                  <span class="label">VIGENCIA</span>
                  <span class="value-text">Enero - Diciembre 2026</span>
                </div>
              </div>
            </div>

            <!-- Right: QR Code for Verification -->
            <div class="student-qr-col">
              <div class="qr-box">
                <QrCode :size="120" />
              </div>
              <small class="qr-label">Validación QR Oficial</small>
              <span class="barcode-fake">{{ studentMatricula }}</span>
            </div>
          </div>

          <!-- Bottom Security Strip -->
          <div class="card-footer-strip">
            <span>Dirección de Educación y Cultura &bull; Heroica Ciudad de Tlaxiaco</span>
            <span>Documento Oficial No Transferible</span>
          </div>
        </div>
      </section>
    </template>
  </DashboardLayout>
</template>

<style scoped>
/* ==========================================================
   STATE CARDS (Loading / Error)
   ========================================================== */
.state-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 36px;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.loader-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #4051a3;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loader-text strong {
  display: block;
  font-size: 15px;
  color: #0f172a;
}

.loader-text span {
  font-size: 12px;
  color: #64748b;
}

.error-state {
  border-color: #fecaca;
  background: #fff5f5;
  color: #991b1b;
}

.error-icon {
  color: #dc2626;
  flex-shrink: 0;
}

/* ==========================================================
   HERO WELCOME BANNER
   ========================================================== */
.hero-welcome-banner {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  border-radius: 14px;
  padding: 28px 32px;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  overflow: hidden;
}

.hero-welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.banner-content {
  max-width: 700px;
  position: relative;
  z-index: 2;
}

.banner-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(99, 102, 241, 0.25);
  border: 1px solid rgba(129, 140, 248, 0.4);
  color: #c7d2fe;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 12px;
}

.banner-title {
  margin: 0 0 8px;
  font-size: 25px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.5px;
}

.banner-desc {
  margin: 0 0 18px;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 1.5;
}

.banner-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  font-size: 11.5px;
  color: #e2e8f0;
}

.date-tag {
  text-transform: capitalize;
  color: #94a3b8;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 8px #22c55e;
}

.banner-visual {
  position: relative;
  z-index: 2;
}

.visual-badge {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #a5b4fc;
}

.visual-badge span {
  font-size: 11px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.5px;
}

/* ==========================================================
   STAT CARDS (Super Admin inspired)
   ========================================================== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  position: relative;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.07);
  border-color: #cbd5e1;
}

.stat-icon-wrapper {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.stat-indigo .stat-icon-wrapper {
  background: #eef2ff;
  color: #6366f1;
}

.stat-sky .stat-icon-wrapper {
  background: #e0f2fe;
  color: #0284c7;
}

.stat-emerald .stat-icon-wrapper {
  background: #dcfce7;
  color: #16a34a;
}

.stat-amber .stat-icon-wrapper {
  background: #fef3c7;
  color: #d97706;
}

.stat-info {
  flex: 1;
}

.stat-title {
  display: block;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 21px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.stat-subtitle {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.stat-arrow {
  color: #cbd5e1;
  transition: transform 0.2s ease, color 0.2s ease;
}

.stat-card:hover .stat-arrow {
  transform: translateX(3px);
  color: #4051a3;
}

/* ==========================================================
   PANELS & GRIDS
   ========================================================== */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 22px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.full-width-panel {
  width: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.panel-header h2,
.panel-header h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.panel-header p {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.panel-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #4051a3;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.15s ease;
}

.panel-action-btn:hover {
  background: #eef2ff;
}

.badge-count {
  font-size: 11.5px;
  font-weight: 700;
  color: #4051a3;
  background: #e0e7ff;
  padding: 3px 10px;
  border-radius: 12px;
}

/* Workshop Cards */
.workshop-cards-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.workshop-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.15s ease;
}

.workshop-card:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.workshop-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e0e7ff;
  color: #4051a3;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.workshop-details {
  flex: 1;
}

.workshop-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3px;
}

.workshop-title-row strong {
  font-size: 13.5px;
  color: #0f172a;
}

.workshop-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: #64748b;
}

/* Schedule Mini List */
.schedule-mini-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-mini-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.schedule-day-badge {
  padding: 6px 12px;
  background: #4051a3;
  color: #ffffff;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  min-width: 70px;
  text-align: center;
}

.schedule-mini-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.schedule-mini-info strong {
  font-size: 13px;
  color: #0f172a;
}

.schedule-mini-info span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  color: #64748b;
}

.taller-tag {
  font-size: 10.5px;
  font-weight: 600;
  color: #475569;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
}

/* Attendance Mini List */
.attendance-mini-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attendance-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
  background: #fafafa;
}

.date-chip {
  width: 40px;
  height: 40px;
  background: #eef2ff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.day-num {
  font-size: 14px;
  font-weight: 800;
  color: #4051a3;
  line-height: 1;
}

.month-name {
  font-size: 9px;
  font-weight: 600;
  color: #6366f1;
  text-transform: uppercase;
}

.att-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.att-info strong {
  font-size: 13px;
  color: #0f172a;
}

.att-info small {
  font-size: 11px;
  color: #64748b;
}

/* Card Highlight Panel */
.card-highlight-panel {
  background: linear-gradient(135deg, #eff6ff 0%, #f0fdf4 100%);
  border: 1px solid #bfdbfe;
  display: flex;
  align-items: center;
}

.id-banner-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.id-badge-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: #4051a3;
  color: #ffffff;
  display: grid;
  place-items: center;
}

.id-banner-content h3 {
  margin: 0 0 4px;
  font-size: 17px;
  color: #0f172a;
}

.id-banner-content p {
  margin: 0;
  font-size: 12.5px;
  color: #475569;
  line-height: 1.5;
}

.primary-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #4051a3;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.primary-btn:hover {
  background: #313f80;
}

/* Empty panel helper */
.empty-panel-box {
  padding: 32px;
  text-align: center;
  color: #94a3b8;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}

/* ==========================================================
   TABLES & DETAILED VIEWS
   ========================================================== */
.table-container {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
  text-align: left;
}

.modern-table th {
  background: #f8fafc;
  padding: 12px 16px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.modern-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
  vertical-align: middle;
}

.modern-table tbody tr:hover {
  background: #f8fafc;
}

.table-cell-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eef2ff;
  color: #4051a3;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.code-chip {
  display: inline-block;
  font-family: monospace;
  font-size: 11.5px;
  font-weight: 600;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  color: #1e293b;
}

/* Status Pills */
.status-pill {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.status-active {
  background: #dcfce7;
  color: #15803d;
}

.status-warning {
  background: #fef3c7;
  color: #b45309;
}

.status-danger {
  background: #fee2e2;
  color: #b91c1c;
}

/* Summary Bars */
.attendance-summary-bar,
.payments-overview-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  padding: 16px 20px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 20px;
}

.summary-metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.summary-metric span {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
  text-transform: uppercase;
}

.summary-metric strong {
  font-size: 18px;
  color: #0f172a;
}

.text-emerald strong { color: #16a34a; }
.text-amber strong { color: #d97706; }
.text-rose strong { color: #e11d48; }

.summary-progress-col {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: #475569;
}

.progress-track {
  width: 100%;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #16a34a;
  border-radius: 4px;
  transition: width 0.3s ease;
}

/* Schedule Grid Cards */
.schedule-grid-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.schedule-card-full {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.card-day-header {
  background: #4051a3;
  color: #ffffff;
  padding: 10px 16px;
  font-size: 12.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-main-content {
  padding: 16px;
}

.card-main-content h4 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #0f172a;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #475569;
}

.clock-icon {
  color: #4051a3;
}

.card-footer {
  padding: 10px 16px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.tag-badge {
  font-size: 10.5px;
  font-weight: 700;
  background: #e0e7ff;
  color: #4051a3;
  padding: 2px 8px;
  border-radius: 4px;
}

/* Empty States */
.empty-state {
  padding: 50px 20px;
  text-align: center;
  color: #94a3b8;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-state h3 {
  margin: 0;
  font-size: 16px;
  color: #475569;
}

.empty-state p {
  margin: 0;
  font-size: 13px;
  max-width: 480px;
  line-height: 1.5;
}

/* ==========================================================
   OFFICIAL DIGITAL CREDENTIAL
   ========================================================== */
.credential-view-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
}

.credential-action-header {
  width: 100%;
  max-width: 680px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.credential-action-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #0f172a;
}

.credential-action-header p {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
}

.print-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  background: #ffffff;
  color: #4051a3;
  border: 1.5px solid #4051a3;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.print-btn:hover {
  background: #4051a3;
  color: #ffffff;
}

/* The ID Card Structure */
.official-credential-card {
  width: 100%;
  max-width: 680px;
  background: #ffffff;
  border: 2px solid #334155;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  position: relative;
}

.card-header {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  color: #ffffff;
  padding: 18px 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  border-bottom: 3px solid #f59e0b;
}

.header-logo-container {
  width: 110px;
  height: 48px;
  background: #ffffff;
  border-radius: 8px;
  padding: 4px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.cred-logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.header-titles h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.8px;
  color: #ffffff;
}

.header-titles p {
  margin: 2px 0 0;
  font-size: 10px;
  color: #cbd5e1;
  letter-spacing: 0.5px;
}

.cred-cycle {
  display: inline-block;
  margin-top: 5px;
  font-size: 9.5px;
  font-weight: 700;
  color: #f59e0b;
  letter-spacing: 0.5px;
}

.card-body {
  padding: 26px 24px;
  display: flex;
  gap: 24px;
  align-items: center;
  background: #ffffff;
}

.student-photo-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.photo-frame {
  width: 90px;
  height: 110px;
  background: #f1f5f9;
  border: 2px solid #cbd5e1;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #4051a3;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.student-initials {
  font-size: 40px;
  font-weight: 800;
  color: #4051a3;
}

.badge-status-active {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 9.5px;
  font-weight: 800;
  background: #dcfce7;
  color: #166534;
  padding: 2px 8px;
  border-radius: 10px;
}

.badge-status-active .dot {
  width: 5px;
  height: 5px;
  background: #16a34a;
  border-radius: 50%;
}

.student-info-col {
  flex: 1;
}

.info-group {
  margin-bottom: 10px;
}

.info-group .label {
  display: block;
  font-size: 9.5px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.5px;
  margin-bottom: 2px;
}

.student-fullname {
  display: block;
  font-size: 17px;
  color: #0f172a;
  font-weight: 800;
  line-height: 1.2;
}

.info-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 14px;
  margin-top: 10px;
}

.value-highlight {
  font-size: 14px;
  font-weight: 800;
  color: #4051a3;
  letter-spacing: 0.5px;
}

.value-text {
  font-size: 12px;
  color: #1e293b;
  font-weight: 600;
}

.student-qr-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding-left: 12px;
  border-left: 1px dashed #cbd5e1;
}

.qr-box {
  padding: 8px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #0f172a;
}

.qr-label {
  font-size: 10px;
  color: #64748b;
  font-weight: 600;
}

.barcode-fake {
  font-family: monospace;
  font-size: 11px;
  letter-spacing: 2px;
  color: #334155;
  font-weight: 700;
}

.card-footer-strip {
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  padding: 8px 24px;
  display: flex;
  justify-content: space-between;
  font-size: 9.5px;
  color: #64748b;
  font-weight: 600;
}

/* Print Styles */
@media print {
  @page {
    size: letter portrait;
    margin: 15mm;
  }

  /* Hide headers, banners, and non-credential elements */
  .credential-action-header,
  .hero-welcome-banner,
  .stats-row,
  .dashboard-grid,
  .panel,
  .state-card {
    display: none !important;
  }

  .credential-view-container {
    display: block !important;
    padding: 0 !important;
    margin: 0 !important;
    width: 100% !important;
  }

  .official-credential-card {
    display: block !important;
    width: 600px !important;
    max-width: 100% !important;
    margin: 20px auto !important;
    border: 2px solid #1e293b !important;
    border-radius: 14px !important;
    box-shadow: none !important;
    overflow: hidden !important;
    page-break-inside: avoid !important;
    break-inside: avoid !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
    color-adjust: exact !important;
  }

  .official-credential-card * {
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
    color-adjust: exact !important;
  }

  .card-header {
    background: #172033 !important;
    color: #ffffff !important;
    border-bottom: 3px solid #f59e0b !important;
    padding: 16px 20px !important;
    display: flex !important;
    align-items: center !important;
    gap: 16px !important;
  }

  .header-logo-container {
    background: #ffffff !important;
    border: 1px solid #cbd5e1 !important;
    box-shadow: none !important;
    width: 100px !important;
    height: 44px !important;
  }

  .card-body {
    padding: 22px 20px !important;
    display: flex !important;
    gap: 20px !important;
    align-items: center !important;
    background: #ffffff !important;
  }

  .photo-frame {
    background: #f1f5f9 !important;
    border: 2px solid #94a3b8 !important;
    box-shadow: none !important;
  }

  .badge-status-active {
    background: #dcfce7 !important;
    color: #166534 !important;
    border: 1px solid #86efac !important;
  }

  .qr-box {
    background: #ffffff !important;
    border: 1px solid #94a3b8 !important;
    box-shadow: none !important;
  }

  .card-footer-strip {
    background: #f8fafc !important;
    border-top: 1px solid #e2e8f0 !important;
    padding: 8px 20px !important;
  }
}

/* ==========================================================
   RESPONSIVE DESIGN
   ========================================================== */
@media (max-width: 1100px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 840px) {
  .hero-welcome-banner {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }

  .banner-visual {
    display: none;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .card-body {
    flex-direction: column;
    align-items: flex-start;
  }

  .student-qr-col {
    border-left: none;
    padding-left: 0;
    border-top: 1px dashed #cbd5e1;
    padding-top: 16px;
    width: 100%;
    align-items: center;
  }
}

@media (max-width: 600px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .info-grid-2 {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
