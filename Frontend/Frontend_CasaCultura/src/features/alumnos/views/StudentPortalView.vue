<script setup>
import { computed, onMounted, ref } from 'vue'
import DashboardLayout from '../../../components/DashboardLayout.vue'
import { clearStoredSession } from '../../../services/apiService'
import { getStudentDashboard } from '../../../services/studentService'
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
            <div v-if="student.fotoUrl" class="student-banner-photo">
              <img :src="student.fotoUrl" alt="Foto de perfil" class="banner-avatar-img" />
            </div>
            <div v-else class="visual-badge">
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
                src="../../../assets/casacul.png"
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
                <img
                  v-if="student.fotoUrl"
                  :src="student.fotoUrl"
                  alt="Fotografía oficial"
                  class="student-photo-img"
                />
                <span v-else class="student-initials">
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

<style scoped src="../../../assets/styles/student.css"></style>
