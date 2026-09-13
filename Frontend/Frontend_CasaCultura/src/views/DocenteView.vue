<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DashboardLayout from '../components/DashboardLayout.vue'
import {
  getTeacherDashboard,
  getTeacherProfile,
  getTeacherGroups,
  getTeacherGroupStudents,
  getAllTeacherStudents,
  getTeacherGroupAttendance,
  saveBatchAttendance,
  saveManualAttendance,
  scanQrAttendance,
  getTeacherSchedules
} from '../services/docenteService'
import {
  LayoutDashboard,
  BookOpen,
  Users,
  CheckCircle2,
  Calendar,
  User,
  Clock,
  Search,
  Check,
  X,
  AlertCircle,
  QrCode,
  ListCheck,
  History,
  RotateCcw,
  Sparkles,
  Phone,
  Mail,
  Award,
  ChevronRight,
  Filter,
  CheckCheck,
  UserCheck,
  AlertTriangle,
  ArrowRight
} from 'lucide-vue-next'

const router = useRouter()
const activeTab = ref('resumen')
const isLoading = ref(true)
const errorMessage = ref('')
const successToast = ref('')

// User session
const currentUser = JSON.parse(
  localStorage.getItem('casa-cultura-user') || sessionStorage.getItem('casa-cultura-user') || '{}'
)

// Main teacher data state
const teacherData = reactive({
  docente: null,
  estadisticas: {
    totalGrupos: 0,
    totalAlumnos: 0,
    clasesHoy: 0,
    porcentajeAsistenciaGeneral: 100,
    asistenciasRegistradasHoy: 0
  },
  grupos: [],
  clasesHoy: [],
  horariosSemana: [],
  asistenciasRecientes: []
})

// Navigation tabs
const navItems = [
  { id: 'resumen', label: 'Mi Dashboard', icon: LayoutDashboard },
  { id: 'grupos', label: 'Mis Talleres y Grupos', icon: BookOpen },
  { id: 'alumnos', label: 'Directorio de Alumnos', icon: Users },
  { id: 'asistencias', label: 'Control de Asistencias', icon: CheckCircle2 },
  { id: 'horarios', label: 'Horarios Semanales', icon: Calendar },
  { id: 'perfil', label: 'Mi Perfil', icon: User }
]

// Formatted Date in Spanish
const formattedDate = computed(() => {
  return new Date().toLocaleDateString('es-MX', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
})

const teacherDisplayName = computed(() => {
  if (teacherData.docente) {
    const d = teacherData.docente
    return `${d.nombre || ''} ${d.apellidoPaterno || ''}`.trim()
  }
  if (currentUser.persona?.nombre) {
    return `${currentUser.persona.nombre} ${currentUser.persona.apellidoPaterno || ''}`.trim()
  }
  return currentUser.nombreUsuario || 'Profesor(a)'
})

const teacherUsername = computed(() => currentUser.nombreUsuario || 'Docente')
const teacherSpecialty = computed(() => teacherData.docente?.especialidad || 'Instructor Titular')

// ==========================================
// LOAD DATA
// ==========================================
async function loadTeacherData() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const dashboard = await getTeacherDashboard()
    teacherData.docente = dashboard.docente
    teacherData.estadisticas = dashboard.estadisticas || teacherData.estadisticas
    teacherData.grupos = dashboard.grupos || []
    teacherData.clasesHoy = dashboard.clasesHoy || []
    teacherData.horariosSemana = dashboard.horariosSemana || []
    teacherData.asistenciasRecientes = dashboard.asistenciasRecientes || []

    // Preselect first group in attendance tab if available
    if (teacherData.grupos.length > 0 && !attendanceGroup.value) {
      attendanceGroup.value = teacherData.grupos[0].id
    }
  } catch (err) {
    errorMessage.value = err.message || 'Error al cargar los datos del docente.'
  } finally {
    isLoading.value = false
  }
}

function showNotification(msg) {
  successToast.value = msg
  setTimeout(() => {
    successToast.value = ''
  }, 4000)
}

// ==========================================
// DIRECTORY OF STUDENTS (ALUMNOS)
// ==========================================
const allStudents = ref([])
const isLoadingStudents = ref(false)
const studentSearch = ref('')
const filterGroupForStudents = ref('')

async function fetchAllStudents() {
  isLoadingStudents.value = true
  try {
    allStudents.value = await getAllTeacherStudents()
  } catch (err) {
    console.error('Error fetching students:', err)
  } finally {
    isLoadingStudents.value = false
  }
}

const filteredStudents = computed(() => {
  let list = allStudents.value
  if (filterGroupForStudents.value) {
    list = list.filter(s => String(s.grupoId) === String(filterGroupForStudents.value))
  }
  if (studentSearch.value.trim()) {
    const q = studentSearch.value.trim().toLowerCase()
    list = list.filter(s =>
      (s.nombreCompleto && s.nombreCompleto.toLowerCase().includes(q)) ||
      (s.matricula && s.matricula.toLowerCase().includes(q)) ||
      (s.correo && s.correo.toLowerCase().includes(q)) ||
      (s.telefono && s.telefono.includes(q)) ||
      (s.nombreGrupo && s.nombreGrupo.toLowerCase().includes(q))
    )
  }
  return list
})

// ==========================================
// ATTENDANCE & ROLL CALL (ASISTENCIAS)
// ==========================================
const attendanceGroup = ref('')
const attendanceDate = ref(new Date().toISOString().slice(0, 10))
const attendanceMode = ref('lista') // 'lista' | 'qr' | 'historial'
const isSavingAttendance = ref(false)
const groupRoster = ref([])
const groupAttendanceHistory = ref([])
const isLoadingRoster = ref(false)
const isLoadingHistory = ref(false)

// Schedules for selected group
const groupSchedules = computed(() => {
  if (!attendanceGroup.value) return []
  const group = teacherData.grupos.find(g => g.id === Number(attendanceGroup.value))
  return group?.horarios || []
})

const selectedScheduleId = ref('')

watch(groupSchedules, (slots) => {
  if (slots.length > 0) {
    const todayName = new Date().toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase()
    const match = slots.find(s => s.dia === todayName)
    selectedScheduleId.value = match ? match.id : slots[0].id
  } else {
    selectedScheduleId.value = ''
  }
}, { immediate: true })

// Fetch students for attendance roll call
async function loadRosterForGroup() {
  if (!attendanceGroup.value) return
  isLoadingRoster.value = true
  try {
    const students = await getTeacherGroupStudents(attendanceGroup.value)
    groupRoster.value = students.map(s => ({
      ...s,
      selectedState: 'PRESENTE'
    }))
  } catch (err) {
    errorMessage.value = err.message || 'Error al cargar lista del grupo.'
  } finally {
    isLoadingRoster.value = false
  }
}

watch(attendanceGroup, () => {
  if (activeTab.value === 'asistencias') {
    if (attendanceMode.value === 'lista') {
      loadRosterForGroup()
    } else if (attendanceMode.value === 'historial') {
      loadHistoryForGroup()
    }
  }
})

function markAllPresent() {
  groupRoster.value.forEach(item => {
    item.selectedState = 'PRESENTE'
  })
}

const rosterStats = computed(() => {
  const total = groupRoster.value.length
  const presentes = groupRoster.value.filter(s => s.selectedState === 'PRESENTE').length
  const retardos = groupRoster.value.filter(s => s.selectedState === 'RETARDO').length
  const faltas = groupRoster.value.filter(s => s.selectedState === 'FALTA').length
  return { total, presentes, retardos, faltas }
})

async function submitBatchAttendance() {
  if (!attendanceGroup.value) {
    alert('Selecciona un grupo.')
    return
  }
  if (!selectedScheduleId.value) {
    alert('Selecciona un bloque de horario para la sesión.')
    return
  }
  if (!attendanceDate.value) {
    alert('Selecciona una fecha.')
    return
  }
  if (groupRoster.value.length === 0) {
    alert('No hay alumnos inscritos en este grupo.')
    return
  }

  isSavingAttendance.value = true
  try {
    const payload = {
      grupoId: Number(attendanceGroup.value),
      horarioId: Number(selectedScheduleId.value),
      fecha: attendanceDate.value,
      asistencias: groupRoster.value.map(s => ({
        inscripcionId: s.inscripcionId,
        estado: s.selectedState
      }))
    }
    const res = await saveBatchAttendance(attendanceGroup.value, payload)
    showNotification(`¡Pase de lista guardado con éxito! (${res.presentes} presentes, ${res.retardos} retardos, ${res.faltas} faltas).`)
    await loadTeacherData()
  } catch (err) {
    alert(err.message || 'Error al guardar el pase de lista.')
  } finally {
    isSavingAttendance.value = false
  }
}

// QR Scan / Fast lookup
const qrInput = ref('')
const qrResult = ref(null)
const isSubmittingQr = ref(false)
const qrError = ref('')

async function submitQrAttendance() {
  qrError.value = ''
  qrResult.value = null
  const code = qrInput.value.trim()
  if (!code) return

  isSubmittingQr.value = true
  try {
    const res = await scanQrAttendance({
      codigoQr: code
    })
    qrResult.value = res
    qrInput.value = ''
    showNotification(`Asistencia registrada: ${res.alumno} (${res.estado})`)
    await loadTeacherData()
  } catch (err) {
    qrError.value = err.message || 'No se pudo registrar la asistencia con este código.'
  } finally {
    isSubmittingQr.value = false
  }
}

// Attendance History
const historyStatusFilter = ref('TODAS')
const historySearch = ref('')

async function loadHistoryForGroup() {
  if (!attendanceGroup.value) return
  isLoadingHistory.value = true
  try {
    groupAttendanceHistory.value = await getTeacherGroupAttendance(attendanceGroup.value)
  } catch (err) {
    console.error('Error fetching group attendance history:', err)
  } finally {
    isLoadingHistory.value = false
  }
}

const filteredGroupHistory = computed(() => {
  let list = groupAttendanceHistory.value
  if (historyStatusFilter.value !== 'TODAS') {
    list = list.filter(a => a.estado === historyStatusFilter.value)
  }
  if (historySearch.value.trim()) {
    const q = historySearch.value.trim().toLowerCase()
    list = list.filter(a =>
      (a.alumno && a.alumno.toLowerCase().includes(q)) ||
      (a.matricula && a.matricula.toLowerCase().includes(q))
    )
  }
  return list
})

function setQuickDateToday() {
  attendanceDate.value = new Date().toISOString().slice(0, 10)
}

function startRollCallForGroup(groupId, scheduleId = null) {
  attendanceGroup.value = groupId
  if (scheduleId) {
    selectedScheduleId.value = scheduleId
  }
  attendanceMode.value = 'lista'
  activeTab.value = 'asistencias'
  loadRosterForGroup()
}

function viewStudentsForGroup(groupId) {
  filterGroupForStudents.value = String(groupId)
  activeTab.value = 'alumnos'
  fetchAllStudents()
}

function onNavigate(tabId) {
  activeTab.value = tabId
  if (tabId === 'alumnos') {
    fetchAllStudents()
  } else if (tabId === 'asistencias') {
    if (attendanceMode.value === 'lista') {
      loadRosterForGroup()
    } else if (attendanceMode.value === 'historial') {
      loadHistoryForGroup()
    }
  }
}

const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const dayNamesEs = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo'
}

function getSchedulesForDay(day) {
  return teacherData.horariosSemana.filter(h => h.dia === day)
}

const totalWeeklyHours = computed(() => {
  let totalMinutes = 0
  teacherData.horariosSemana.forEach(h => {
    if (h.horaInicio && h.horaFin) {
      const [h1, m1] = h.horaInicio.split(':').map(Number)
      const [h2, m2] = h.horaFin.split(':').map(Number)
      totalMinutes += (h2 * 60 + m2) - (h1 * 60 + m1)
    }
  })
  return Math.round((totalMinutes / 60) * 10) / 10
})

onMounted(() => {
  loadTeacherData()
})
</script>

<template>
  <DashboardLayout
    role="DOCENTE"
    :username="teacherUsername"
    :full-name="teacherDisplayName"
    :badge="teacherSpecialty"
    :items="navItems"
    :active="activeTab"
    @navigate="onNavigate"
  >
    <!-- TOAST NOTIFICATION -->
    <transition name="fade">
      <div v-if="successToast" class="toast-success" role="alert">
        <CheckCircle2 class="toast-icon" />
        <span>{{ successToast }}</span>
      </div>
    </transition>

    <!-- LOADING STATE -->
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>Cargando información del portal docente...</p>
    </div>

    <!-- ERROR STATE -->
    <div v-else-if="errorMessage" class="error-banner">
      <AlertCircle class="error-icon" />
      <div class="error-content">
        <h4>Error de conexión</h4>
        <p>{{ errorMessage }}</p>
      </div>
      <button class="btn-retry" @click="loadTeacherData">
        <RotateCcw class="btn-icon" /> Reintentar
      </button>
    </div>

    <!-- MAIN PORTAL WRAPPER -->
    <div v-else class="portal-container">

      <!-- ======================================================= -->
      <!-- 1. HERO INSTITUCIONAL BLANCO / ACCENT (Estilo Supervisor) -->
      <!-- ======================================================= -->
      <section class="teacher-hero-card">
        <div class="hero-left">
          <div class="hero-badge-row">
            <span class="role-pill">
              <span class="pulse-dot"></span> Instructor Titular
            </span>
            <span class="specialty-pill">
              <Award class="chip-icon" /> {{ teacherSpecialty }}
            </span>
            <span class="hero-date">
              <Calendar class="chip-icon" /> {{ formattedDate }}
            </span>
          </div>

          <h1 class="hero-heading">
            ¡Hola, <span class="text-brand">{{ teacherDisplayName }}</span>!
          </h1>
          <p class="hero-subtext">
            Panel de control docente &bull; Casa de la Cultura de la Heroica Ciudad de Tlaxiaco
          </p>
        </div>

        <div class="hero-actions">
          <button
            type="button"
            class="btn-action primary"
            @click="onNavigate('asistencias')"
          >
            <ListCheck class="btn-icon" />
            <span>Pase de Lista Rápido</span>
          </button>
        </div>
      </section>

      <!-- ======================================================= -->
      <!-- 2. TARJETAS DE ESTADÍSTICAS KPI (Estilo Supervisor/Admin) -->
      <!-- ======================================================= -->
      <section class="stats-grid">
        <div class="stat-card" @click="onNavigate('grupos')">
          <div class="stat-icon-wrap icon-blue">
            <BookOpen class="stat-icon" />
          </div>
          <div class="stat-details">
            <span class="stat-label">Grupos Asignados</span>
            <span class="stat-value">{{ teacherData.estadisticas.totalGrupos }}</span>
            <span class="stat-sub">Cursos culturales activos</span>
          </div>
        </div>

        <div class="stat-card" @click="onNavigate('alumnos')">
          <div class="stat-icon-wrap icon-emerald">
            <Users class="stat-icon" />
          </div>
          <div class="stat-details">
            <span class="stat-label">Alumnos Activos</span>
            <span class="stat-value">{{ teacherData.estadisticas.totalAlumnos }}</span>
            <span class="stat-sub">En tus talleres a cargo</span>
          </div>
        </div>

        <div class="stat-card" @click="onNavigate('horarios')">
          <div class="stat-icon-wrap icon-amber">
            <Clock class="stat-icon" />
          </div>
          <div class="stat-details">
            <span class="stat-label">Clases Programadas Hoy</span>
            <span class="stat-value">{{ teacherData.estadisticas.clasesHoy }}</span>
            <span class="stat-sub">{{ teacherData.clasesHoy.length ? 'Sesiones pendientes hoy' : 'Sin clases hoy' }}</span>
          </div>
        </div>

        <div class="stat-card" @click="onNavigate('asistencias')">
          <div class="stat-icon-wrap icon-indigo">
            <CheckCircle2 class="stat-icon" />
          </div>
          <div class="stat-details">
            <span class="stat-label">Asistencia Promedio</span>
            <span class="stat-value">{{ teacherData.estadisticas.porcentajeAsistenciaGeneral }}%</span>
            <span class="stat-sub">{{ teacherData.estadisticas.asistenciasRegistradasHoy }} registros hoy</span>
          </div>
        </div>
      </section>

      <!-- ======================================================= -->
      <!-- TAB 1: MI DASHBOARD (RESUMEN)                          -->
      <!-- ======================================================= -->
      <div v-if="activeTab === 'resumen'" class="tab-content">
        <!-- SESIONES DE HOY PANEL -->
        <div class="panel">
          <div class="panel-header">
            <div class="header-text">
              <div class="title-with-icon">
                <Clock class="panel-icon text-amber" />
                <h3>Sesiones y Clases de Hoy</h3>
              </div>
              <p>Horarios asignados para la jornada de hoy</p>
            </div>
            <span class="badge-count" v-if="teacherData.clasesHoy.length">
              {{ teacherData.clasesHoy.length }} sesión(es)
            </span>
          </div>

          <div v-if="teacherData.clasesHoy.length === 0" class="empty-state">
            <Calendar class="empty-icon" />
            <h4>No tienes clases programadas para el día de hoy</h4>
            <p>Disfruta de tu jornada o prepara los materiales para tus próximas sesiones de taller.</p>
          </div>

          <div v-else class="today-classes-grid">
            <div
              v-for="cls in teacherData.clasesHoy"
              :key="cls.id"
              class="today-class-card"
            >
              <div class="class-time-badge">
                <Clock class="chip-icon" />
                <span>{{ cls.horaInicio?.slice(0, 5) }} - {{ cls.horaFin?.slice(0, 5) }}</span>
              </div>
              <h4 class="class-group-name">{{ cls.nombreGrupo }}</h4>
              <p class="class-workshop-name" v-if="cls.nombre">{{ cls.nombre }}</p>
              <button
                type="button"
                class="btn-action primary btn-sm mt-3"
                @click="startRollCallForGroup(cls.grupoId, cls.id)"
              >
                <ListCheck class="btn-icon" /> Pasar Lista
              </button>
            </div>
          </div>
        </div>

        <!-- MIS GRUPOS PANEL -->
        <div class="panel mt-4">
          <div class="panel-header">
            <div class="header-text">
              <div class="title-with-icon">
                <BookOpen class="panel-icon text-blue" />
                <h3>Mis Grupos y Talleres Asignados</h3>
              </div>
              <p>Listado de grupos donde impartes clase actualmente</p>
            </div>
            <button class="link-btn" @click="onNavigate('grupos')">
              Ver todos los grupos <ChevronRight class="btn-icon" />
            </button>
          </div>

          <div v-if="teacherData.grupos.length === 0" class="empty-state">
            <BookOpen class="empty-icon" />
            <h4>Sin grupos asignados</h4>
            <p>Comunícate con la administración para que te asignen tus talleres de este ciclo.</p>
          </div>

          <div v-else class="groups-grid">
            <div
              v-for="grp in teacherData.grupos"
              :key="grp.id"
              class="group-card"
            >
              <div class="group-card-top">
                <span class="badge-category">{{ grp.categoria || 'Taller' }}</span>
                <span class="badge-students">
                  <Users class="chip-icon" /> {{ grp.totalAlumnos }} alumno(s)
                </span>
              </div>

              <h4 class="group-title">{{ grp.nombreGrupo }}</h4>
              <p class="group-subtitle">{{ grp.curso }}</p>

              <div class="schedule-chips-container" v-if="grp.horarios?.length">
                <span
                  v-for="h in grp.horarios"
                  :key="h.id"
                  class="schedule-chip"
                >
                  {{ dayNamesEs[h.dia] || h.dia }}: {{ h.horaInicio?.slice(0, 5) }} - {{ h.horaFin?.slice(0, 5) }}
                </span>
              </div>

              <div class="group-card-buttons">
                <button
                  type="button"
                  class="btn-action primary flex-1"
                  @click="startRollCallForGroup(grp.id)"
                >
                  <ListCheck class="btn-icon" /> Pasar Lista
                </button>
                <button
                  type="button"
                  class="btn-action secondary"
                  @click="viewStudentsForGroup(grp.id)"
                >
                  <Users class="btn-icon" /> Alumnos
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- ÚLTIMAS ASISTENCIAS REGISTRADAS -->
        <div class="panel mt-4" v-if="teacherData.asistenciasRecientes?.length">
          <div class="panel-header">
            <div class="header-text">
              <div class="title-with-icon">
                <History class="panel-icon text-indigo" />
                <h3>Últimas Asistencias Registradas</h3>
              </div>
              <p>Historial reciente de asistencia de tus alumnos</p>
            </div>
            <button class="link-btn" @click="onNavigate('asistencias')">
              Historial completo <ChevronRight class="btn-icon" />
            </button>
          </div>

          <div class="table-responsive">
            <table class="table-custom">
              <thead>
                <tr>
                  <th>Alumno</th>
                  <th>Matrícula</th>
                  <th>Grupo / Taller</th>
                  <th>Fecha</th>
                  <th>Hora Registro</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="att in teacherData.asistenciasRecientes" :key="att.id">
                  <td class="font-bold text-slate">{{ att.alumno }}</td>
                  <td><span class="matricula-chip">{{ att.matricula }}</span></td>
                  <td>{{ att.grupo }}</td>
                  <td>{{ att.fecha }}</td>
                  <td>{{ att.horaRegistro?.slice(11, 16) || '--:--' }}</td>
                  <td>
                    <span
                      class="badge-status"
                      :class="{
                        'status-present': att.estado === 'PRESENTE',
                        'status-delay': att.estado === 'RETARDO',
                        'status-absent': att.estado === 'FALTA'
                      }"
                    >
                      {{ att.estado }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ======================================================= -->
      <!-- TAB 2: MIS TALLERES Y GRUPOS (DETALLADO)               -->
      <!-- ======================================================= -->
      <div v-if="activeTab === 'grupos'" class="tab-content">
        <div class="section-title-bar">
          <div>
            <h2>Mis Talleres y Grupos Asignados</h2>
            <p>Consulta la programación, cupos y horarios de los cursos bajo tu instrucción</p>
          </div>
        </div>

        <div v-if="teacherData.grupos.length === 0" class="empty-state">
          <BookOpen class="empty-icon" />
          <h4>Sin grupos asignados</h4>
          <p>Actualmente no tienes grupos asignados en el ciclo escolar vigente.</p>
        </div>

        <div v-else class="groups-full-grid">
          <div
            v-for="grp in teacherData.grupos"
            :key="grp.id"
            class="group-detail-card"
          >
            <div class="detail-card-top">
              <span class="badge-category">{{ grp.categoria || 'Categoría Única' }}</span>
              <span class="badge-period" v-if="grp.periodo">{{ grp.periodo }}</span>
            </div>

            <div class="detail-card-body">
              <h3 class="detail-group-title">{{ grp.nombreGrupo }}</h3>
              <p class="detail-course-title">{{ grp.curso }}</p>

              <div class="detail-metrics-row">
                <div class="metric-box">
                  <Users class="metric-icon" />
                  <div>
                    <strong>{{ grp.totalAlumnos }}</strong>
                    <span>Inscritos</span>
                  </div>
                </div>
                <div class="metric-box">
                  <Calendar class="metric-icon" />
                  <div>
                    <strong>{{ grp.horarios?.length || 0 }}</strong>
                    <span>Sesiones / sem.</span>
                  </div>
                </div>
              </div>

              <div class="detail-schedules">
                <span class="detail-schedules-label">Horarios oficiales:</span>
                <div class="schedule-chips-container">
                  <span
                    v-for="h in grp.horarios"
                    :key="h.id"
                    class="schedule-chip"
                  >
                    <strong>{{ dayNamesEs[h.dia] || h.dia }}:</strong> {{ h.horaInicio?.slice(0, 5) }} - {{ h.horaFin?.slice(0, 5) }}
                  </span>
                </div>
              </div>
            </div>

            <div class="detail-card-footer">
              <button
                type="button"
                class="btn-action primary flex-1"
                @click="startRollCallForGroup(grp.id)"
              >
                <ListCheck class="btn-icon" /> Pase de Lista
              </button>
              <button
                type="button"
                class="btn-action secondary flex-1"
                @click="viewStudentsForGroup(grp.id)"
              >
                <Users class="btn-icon" /> Ver Alumnos
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================= -->
      <!-- TAB 3: DIRECTORIO DE ALUMNOS                           -->
      <!-- ======================================================= -->
      <div v-if="activeTab === 'alumnos'" class="tab-content">
        <div class="section-title-bar">
          <div>
            <h2>Directorio de Alumnos</h2>
            <p>Padrón de estudiantes inscritos en tus grupos con seguimiento de asistencia</p>
          </div>
          <span class="badge-pill-count" v-if="filteredStudents.length">
            {{ filteredStudents.length }} alumno(s)
          </span>
        </div>

        <!-- Filter & Search Toolbar -->
        <div class="filter-card">
          <div class="search-input-wrap">
            <Search class="search-icon" />
            <input
              type="text"
              v-model="studentSearch"
              placeholder="Buscar por nombre, matrícula ALU-..., teléfono o correo..."
              class="input-control"
            />
            <button
              v-if="studentSearch"
              type="button"
              class="clear-search-btn"
              @click="studentSearch = ''"
            >
              <X class="btn-icon" />
            </button>
          </div>

          <div class="select-input-wrap">
            <Filter class="select-icon" />
            <select v-model="filterGroupForStudents" class="select-control">
              <option value="">Todos mis grupos</option>
              <option
                v-for="g in teacherData.grupos"
                :key="g.id"
                :value="String(g.id)"
              >
                {{ g.nombreGrupo }} ({{ g.curso }})
              </option>
            </select>
          </div>
        </div>

        <div v-if="isLoadingStudents" class="loading-state">
          <div class="spinner"></div>
          <p>Cargando padrón de alumnos...</p>
        </div>

        <div v-else-if="filteredStudents.length === 0" class="empty-state">
          <Users class="empty-icon" />
          <h4>No se encontraron alumnos</h4>
          <p>No hay estudiantes que coincidan con la búsqueda o el grupo seleccionado.</p>
        </div>

        <!-- Students Table -->
        <div v-else class="panel no-padding">
          <div class="table-responsive">
            <table class="table-custom">
              <thead>
                <tr>
                  <th>Estudiante</th>
                  <th>Matrícula</th>
                  <th>Taller / Grupo</th>
                  <th>Contacto</th>
                  <th>Asistencia</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="stu in filteredStudents" :key="stu.inscripcionId">
                  <td>
                    <div class="user-row-cell">
                      <div class="student-avatar">
                        {{ stu.nombre?.[0] || 'A' }}{{ stu.apellidoPaterno?.[0] || 'L' }}
                      </div>
                      <div>
                        <strong class="text-slate block">{{ stu.nombreCompleto }}</strong>
                        <span class="text-sub text-xs">Inscrito: {{ stu.fechaInscripcion }}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span class="matricula-chip">{{ stu.matricula }}</span>
                  </td>
                  <td>
                    <span class="group-pill">{{ stu.nombreGrupo }}</span>
                  </td>
                  <td>
                    <div class="contact-list">
                      <span v-if="stu.telefono" class="contact-line">
                        <Phone class="item-icon text-indigo" /> {{ stu.telefono }}
                      </span>
                      <span v-if="stu.correo" class="contact-line">
                        <Mail class="item-icon text-indigo" /> {{ stu.correo }}
                      </span>
                      <span v-if="!stu.telefono && !stu.correo" class="text-muted text-xs">Sin datos</span>
                    </div>
                  </td>
                  <td>
                    <div class="attendance-meter">
                      <div class="meter-bar">
                        <div
                          class="meter-fill"
                          :style="{ width: `${stu.porcentajeAsistencia}%` }"
                          :class="{
                            'fill-green': stu.porcentajeAsistencia >= 80,
                            'fill-amber': stu.porcentajeAsistencia >= 60 && stu.porcentajeAsistencia < 80,
                            'fill-red': stu.porcentajeAsistencia < 60
                          }"
                        ></div>
                      </div>
                      <div class="meter-labels">
                        <strong class="text-xs text-slate">{{ stu.porcentajeAsistencia }}%</strong>
                        <span class="text-xs text-sub">P:{{ stu.totalAsistencias }} R:{{ stu.totalRetardos }} F:{{ stu.totalFaltas }}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span
                      class="badge-status"
                      :class="stu.estadoInscripcion === 'ACTIVA' ? 'status-present' : 'status-absent'"
                    >
                      {{ stu.estadoInscripcion }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ======================================================= -->
      <!-- TAB 4: CONTROL DE ASISTENCIAS                          -->
      <!-- ======================================================= -->
      <div v-if="activeTab === 'asistencias'" class="tab-content">
        <div class="section-title-bar">
          <div>
            <h2>Control de Asistencias y Pase de Lista</h2>
            <p>Registra asistencias en lote, escanea credenciales QR o consulta el historial del taller</p>
          </div>

          <!-- Mode Switcher Tabs -->
          <div class="mode-switcher">
            <button
              type="button"
              class="switcher-tab"
              :class="{ 'is-active': attendanceMode === 'lista' }"
              @click="attendanceMode = 'lista'; loadRosterForGroup()"
            >
              <ListCheck class="btn-icon" /> Pase de Lista
            </button>
            <button
              type="button"
              class="switcher-tab"
              :class="{ 'is-active': attendanceMode === 'qr' }"
              @click="attendanceMode = 'qr'"
            >
              <QrCode class="btn-icon" /> Lector QR
            </button>
            <button
              type="button"
              class="switcher-tab"
              :class="{ 'is-active': attendanceMode === 'historial' }"
              @click="attendanceMode = 'historial'; loadHistoryForGroup()"
            >
              <History class="btn-icon" /> Historial
            </button>
          </div>
        </div>

        <!-- Attendance Controls Toolbar -->
        <div class="filter-card">
          <div class="form-item">
            <label class="form-label">Taller / Grupo:</label>
            <select
              v-model="attendanceGroup"
              class="select-control"
              @change="attendanceMode === 'lista' ? loadRosterForGroup() : loadHistoryForGroup()"
            >
              <option value="" disabled>Selecciona un grupo</option>
              <option
                v-for="g in teacherData.grupos"
                :key="g.id"
                :value="g.id"
              >
                {{ g.nombreGrupo }} ({{ g.curso }})
              </option>
            </select>
          </div>

          <div class="form-item" v-if="attendanceMode === 'lista'">
            <label class="form-label">Horario / Sesión:</label>
            <select v-model="selectedScheduleId" class="select-control">
              <option value="" disabled>Selecciona horario</option>
              <option
                v-for="s in groupSchedules"
                :key="s.id"
                :value="s.id"
              >
                {{ dayNamesEs[s.dia] || s.dia }}: {{ s.horaInicio?.slice(0, 5) }} - {{ s.horaFin?.slice(0, 5) }}
              </option>
            </select>
          </div>

          <div class="form-item">
            <label class="form-label">Fecha de Sesión:</label>
            <div class="date-group">
              <input
                type="date"
                v-model="attendanceDate"
                class="input-control"
                @change="attendanceMode === 'historial' ? loadHistoryForGroup() : null"
              />
              <button
                type="button"
                class="btn-action secondary btn-sm"
                @click="setQuickDateToday(); attendanceMode === 'historial' ? loadHistoryForGroup() : null"
              >
                Hoy
              </button>
            </div>
          </div>
        </div>

        <!-- MODE 1: ROSTER ROLL CALL -->
        <div v-if="attendanceMode === 'lista'" class="mt-4">
          <div v-if="isLoadingRoster" class="loading-state">
            <div class="spinner"></div>
            <p>Cargando lista de alumnos del grupo...</p>
          </div>

          <div v-else-if="groupRoster.length === 0" class="empty-state">
            <Users class="empty-icon" />
            <h4>No hay alumnos en este grupo</h4>
            <p>Este grupo no cuenta con alumnos inscritos activos para pasar lista.</p>
          </div>

          <div v-else class="panel">
            <div class="roster-top-bar">
              <div class="roster-metrics">
                <span class="count-chip">Total: <strong>{{ rosterStats.total }}</strong></span>
                <span class="count-chip green">Presentes: <strong>{{ rosterStats.presentes }}</strong></span>
                <span class="count-chip amber">Retardos: <strong>{{ rosterStats.retardos }}</strong></span>
                <span class="count-chip red">Faltas: <strong>{{ rosterStats.faltas }}</strong></span>
              </div>

              <button
                type="button"
                class="btn-action secondary btn-sm"
                @click="markAllPresent"
              >
                <CheckCheck class="btn-icon text-emerald" /> Marcar Todos Presentes
              </button>
            </div>

            <div class="roster-items-list">
              <div
                v-for="(student, idx) in groupRoster"
                :key="student.inscripcionId"
                class="roster-item-card"
                :class="{
                  'border-green': student.selectedState === 'PRESENTE',
                  'border-amber': student.selectedState === 'RETARDO',
                  'border-red': student.selectedState === 'FALTA'
                }"
              >
                <span class="item-index">{{ idx + 1 }}</span>

                <div class="item-user-info">
                  <div class="student-avatar-sm">
                    {{ student.nombre?.[0] || 'A' }}{{ student.apellidoPaterno?.[0] || 'L' }}
                  </div>
                  <div>
                    <strong class="text-slate block">{{ student.nombreCompleto }}</strong>
                    <span class="matricula-chip text-xs">{{ student.matricula }}</span>
                  </div>
                </div>

                <!-- 3 State Segmented Toggle -->
                <div class="segmented-state-picker">
                  <button
                    type="button"
                    class="seg-btn seg-present"
                    :class="{ 'is-selected': student.selectedState === 'PRESENTE' }"
                    @click="student.selectedState = 'PRESENTE'"
                  >
                    <Check class="btn-icon" /> Presente
                  </button>
                  <button
                    type="button"
                    class="seg-btn seg-delay"
                    :class="{ 'is-selected': student.selectedState === 'RETARDO' }"
                    @click="student.selectedState = 'RETARDO'"
                  >
                    <Clock class="btn-icon" /> Retardo
                  </button>
                  <button
                    type="button"
                    class="seg-btn seg-absent"
                    :class="{ 'is-selected': student.selectedState === 'FALTA' }"
                    @click="student.selectedState = 'FALTA'"
                  >
                    <X class="btn-icon" /> Falta
                  </button>
                </div>
              </div>
            </div>

            <div class="roster-save-bar">
              <p class="text-sub text-sm">
                Se guardará el pase de lista para los <strong>{{ rosterStats.total }}</strong> alumnos seleccionados.
              </p>
              <button
                type="button"
                class="btn-action primary"
                :disabled="isSavingAttendance"
                @click="submitBatchAttendance"
              >
                <span v-if="isSavingAttendance" class="spinner-sm"></span>
                <CheckCircle2 v-else class="btn-icon" />
                {{ isSavingAttendance ? 'Guardando...' : 'Guardar Pase de Lista' }}
              </button>
            </div>
          </div>
        </div>

        <!-- MODE 2: QR SCANNER -->
        <div v-if="attendanceMode === 'qr'" class="mt-4">
          <div class="panel max-w-2xl mx-auto text-center">
            <div class="qr-intro-header">
              <div class="qr-icon-circle">
                <QrCode class="qr-icon-lg text-indigo" />
              </div>
              <h3>Pase de Lista por Credencial QR</h3>
              <p>Acerca el lector óptico a la credencial del alumno o digita su código/matrícula directamente.</p>
            </div>

            <form @submit.prevent="submitQrAttendance" class="qr-form-row">
              <input
                type="text"
                v-model="qrInput"
                placeholder="Escanea el código QR o escribe la matrícula ALU-..."
                class="input-control qr-text-input"
                autofocus
              />
              <button
                type="submit"
                class="btn-action primary"
                :disabled="isSubmittingQr || !qrInput.trim()"
              >
                <span v-if="isSubmittingQr" class="spinner-sm"></span>
                <CheckCircle2 v-else class="btn-icon" />
                Registrar
              </button>
            </form>

            <div v-if="qrError" class="alert-banner error mt-3">
              <AlertCircle class="alert-icon" />
              <span>{{ qrError }}</span>
            </div>

            <div v-if="qrResult" class="qr-feedback-card mt-4">
              <div class="feedback-header">
                <CheckCircle2 class="feedback-icon" />
                <h4>¡Asistencia Registrada Exitosamente!</h4>
              </div>
              <div class="feedback-body">
                <div class="student-avatar-lg">
                  {{ qrResult.alumno?.[0] || 'A' }}
                </div>
                <div class="feedback-text">
                  <strong class="text-lg text-slate">{{ qrResult.alumno }}</strong>
                  <span class="text-sub text-sm">Matrícula: {{ qrResult.matricula }}</span>
                  <span class="text-sub text-sm">Taller: {{ qrResult.grupo }}</span>
                  <span class="text-sub text-sm">Hora de Registro: {{ qrResult.horaRegistro?.slice(11, 19) }}</span>
                </div>
                <div class="badge-status status-present text-sm">
                  {{ qrResult.estado }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- MODE 3: HISTORY -->
        <div v-if="attendanceMode === 'historial'" class="mt-4">
          <div class="filter-card">
            <div class="search-input-wrap">
              <Search class="search-icon" />
              <input
                type="text"
                v-model="historySearch"
                placeholder="Buscar alumno en el historial..."
                class="input-control"
              />
            </div>

            <div class="history-tabs">
              <button
                class="hist-tab"
                :class="{ 'is-active': historyStatusFilter === 'TODAS' }"
                @click="historyStatusFilter = 'TODAS'"
              >
                Todas
              </button>
              <button
                class="hist-tab tab-green"
                :class="{ 'is-active': historyStatusFilter === 'PRESENTE' }"
                @click="historyStatusFilter = 'PRESENTE'"
              >
                Presentes
              </button>
              <button
                class="hist-tab tab-amber"
                :class="{ 'is-active': historyStatusFilter === 'RETARDO' }"
                @click="historyStatusFilter = 'RETARDO'"
              >
                Retardos
              </button>
              <button
                class="hist-tab tab-red"
                :class="{ 'is-active': historyStatusFilter === 'FALTA' }"
                @click="historyStatusFilter = 'FALTA'"
              >
                Faltas
              </button>
            </div>
          </div>

          <div v-if="isLoadingHistory" class="loading-state">
            <div class="spinner"></div>
            <p>Cargando historial de asistencias...</p>
          </div>

          <div v-else-if="filteredGroupHistory.length === 0" class="empty-state">
            <History class="empty-icon" />
            <h4>Sin asistencias encontradas</h4>
            <p>No se encontraron registros para los filtros seleccionados en este grupo.</p>
          </div>

          <div v-else class="panel no-padding">
            <div class="table-responsive">
              <table class="table-custom">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Hora</th>
                    <th>Alumno</th>
                    <th>Matrícula</th>
                    <th>Taller / Grupo</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="h in filteredGroupHistory" :key="h.id">
                    <td><strong class="text-slate">{{ h.fecha }}</strong></td>
                    <td>{{ h.horaRegistro?.slice(11, 16) || '--:--' }}</td>
                    <td class="font-bold text-slate">{{ h.alumno }}</td>
                    <td><span class="matricula-chip">{{ h.matricula }}</span></td>
                    <td>{{ h.grupo }}</td>
                    <td>
                      <span
                        class="badge-status"
                        :class="{
                          'status-present': h.estado === 'PRESENTE',
                          'status-delay': h.estado === 'RETARDO',
                          'status-absent': h.estado === 'FALTA'
                        }"
                      >
                        {{ h.estado }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================= -->
      <!-- TAB 5: HORARIOS SEMANALES                              -->
      <!-- ======================================================= -->
      <div v-if="activeTab === 'horarios'" class="tab-content">
        <div class="section-title-bar">
          <div>
            <h2>Horarios Semanales de Clases</h2>
            <p>Distribución de sesiones impartidas por día de la semana</p>
          </div>
          <span class="badge-pill-count">
            Total horas: {{ totalWeeklyHours }} hrs/semana
          </span>
        </div>

        <div class="weekly-columns-grid">
          <div
            v-for="day in daysOfWeek"
            :key="day"
            class="day-col-card"
          >
            <div class="day-col-header">
              <span class="day-title">{{ dayNamesEs[day] }}</span>
              <span class="day-sub">{{ getSchedulesForDay(day).length }} clase(s)</span>
            </div>

            <div class="day-slots-wrap">
              <div v-if="getSchedulesForDay(day).length === 0" class="slot-empty">
                <span>Sin clases programadas</span>
              </div>

              <div
                v-for="s in getSchedulesForDay(day)"
                :key="s.id"
                class="slot-card"
              >
                <div class="slot-time-row">
                  <Clock class="chip-icon text-indigo" />
                  <strong>{{ s.horaInicio?.slice(0, 5) }} - {{ s.horaFin?.slice(0, 5) }}</strong>
                </div>
                <h4 class="slot-title">{{ s.nombreGrupo }}</h4>
                <p class="slot-workshop" v-if="s.nombre">{{ s.nombre }}</p>
                <button
                  type="button"
                  class="btn-action secondary btn-xs w-full mt-2"
                  @click="startRollCallForGroup(s.grupoId, s.id)"
                >
                  <ListCheck class="btn-icon" /> Pasar Lista
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================= -->
      <!-- TAB 6: MI PERFIL                                       -->
      <!-- ======================================================= -->
      <div v-if="activeTab === 'perfil'" class="tab-content">
        <div class="section-title-bar">
          <div>
            <h2>Mi Perfil Institucional</h2>
            <p>Datos curriculares y configuración de tu cuenta docente</p>
          </div>
        </div>

        <div class="profile-layout-grid">
          <div class="panel text-center profile-summary-card">
            <div class="avatar-large-circle">
              {{ teacherDisplayName?.[0] || 'D' }}
            </div>
            <h3 class="mt-3 text-slate font-bold text-lg">{{ teacherDisplayName }}</h3>
            <span class="badge-category inline-block mt-1">
              <Award class="chip-icon" /> {{ teacherSpecialty }}
            </span>
            <div class="mt-3">
              <span class="badge-status status-present">Docente Activo</span>
            </div>
          </div>

          <div class="panel">
            <div class="panel-header border-b pb-3 mb-4">
              <div class="header-text">
                <h3>Información de Cuenta y Contacto</h3>
                <p>Datos personales registrados en la Casa de la Cultura</p>
              </div>
            </div>

            <div class="profile-rows-list">
              <div class="profile-data-row">
                <span class="data-label">Nombre de Usuario:</span>
                <span class="data-val font-mono">{{ teacherUsername }}</span>
              </div>
              <div class="profile-data-row">
                <span class="data-label">Especialidad Artística:</span>
                <span class="data-val font-bold text-indigo">{{ teacherData.docente?.especialidad || 'Sin especialidad' }}</span>
              </div>
              <div class="profile-data-row">
                <span class="data-label">Correo Institucional:</span>
                <span class="data-val">{{ teacherData.docente?.correo || 'No registrado' }}</span>
              </div>
              <div class="profile-data-row">
                <span class="data-label">Teléfono:</span>
                <span class="data-val">{{ teacherData.docente?.telefono || 'No registrado' }}</span>
              </div>
              <div class="profile-data-row">
                <span class="data-label">Dirección:</span>
                <span class="data-val">{{ teacherData.docente?.direccion || 'Tlaxiaco, Oaxaca' }}</span>
              </div>
              <div class="profile-data-row">
                <span class="data-label">Carga Académica Semanal:</span>
                <span class="data-val font-bold text-slate">{{ teacherData.grupos.length }} grupos &bull; {{ totalWeeklyHours }} hrs/semana</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </DashboardLayout>
</template>

<style scoped>
/* ==========================================================================
   PORTAL DOCENTE - OFFICIAL CLEAN WHITE/SLATE THEME (Matching Supervisor & Admin)
   ========================================================================== */

.portal-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Toast */
.toast-success {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: #10b981;
  color: white;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  font-size: 13.5px;
  font-weight: 600;
}
.toast-icon { width: 20px; height: 20px; flex-shrink: 0; }

/* Hero Section */
.teacher-hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-badge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.role-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  color: #4338ca;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 700;
  text-transform: uppercase;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px #10b981;
}

.specialty-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #334155;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 600;
}

.hero-date {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: #64748b;
  text-transform: capitalize;
}

.hero-heading {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.text-brand {
  color: #4f46e5;
}

.hero-subtext {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.hero-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: all 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon { width: 26px; height: 26px; }

.icon-blue { background: #e0f2fe; color: #0284c7; }
.icon-emerald { background: #dcfce7; color: #16a34a; }
.icon-amber { background: #fef3c7; color: #d97706; }
.icon-indigo { background: #e0e7ff; color: #4f46e5; }

.stat-details {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.stat-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* Panels */
.panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.03);
}
.panel.no-padding {
  padding: 0;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-text h3 {
  margin: 0;
  font-size: 16.5px;
  font-weight: 700;
  color: #0f172a;
}
.header-text p {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-icon {
  width: 20px;
  height: 20px;
}
.text-amber { color: #f59e0b; }
.text-blue { color: #0284c7; }
.text-indigo { color: #4f46e5; }
.text-emerald { color: #10b981; }

.badge-count {
  font-size: 11.5px;
  font-weight: 700;
  color: #4f46e5;
  background: #e0e7ff;
  padding: 3px 10px;
  border-radius: 12px;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: transparent;
  border: none;
  color: #4f46e5;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
}
.link-btn:hover { background: #eef2ff; }

/* Today's Classes */
.today-classes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}
.today-class-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}
.class-time-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #b45309;
  font-size: 11.5px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  align-self: flex-start;
  margin-bottom: 10px;
}
.class-group-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 2px;
}
.class-workshop-name {
  font-size: 12.5px;
  color: #64748b;
  margin: 0;
}

/* Groups Cards */
.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}
.group-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: all 0.2s;
}
.group-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
}

.group-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.badge-category {
  font-size: 11px;
  font-weight: 700;
  color: #4338ca;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 3px 8px;
  border-radius: 6px;
  text-transform: uppercase;
}
.badge-students {
  font-size: 12px;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.group-title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 2px;
}
.group-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px;
}

.schedule-chips-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}
.schedule-chip {
  font-size: 11px;
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #e2e8f0;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 500;
}

.group-card-buttons {
  display: flex;
  gap: 8px;
  margin-top: auto;
}

/* Full Group Detail Card */
.groups-full-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}
.group-detail-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.03);
}
.detail-card-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.badge-period {
  font-size: 11.5px;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 6px;
}
.detail-group-title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 4px;
}
.detail-course-title {
  font-size: 13.5px;
  color: #64748b;
  margin: 0 0 16px;
}
.detail-metrics-row {
  display: flex;
  gap: 18px;
  padding: 12px 0;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 14px;
}
.metric-box {
  display: flex;
  align-items: center;
  gap: 8px;
}
.metric-icon { width: 20px; height: 20px; color: #4f46e5; }
.metric-box strong { font-size: 15px; color: #0f172a; display: block; }
.metric-box span { font-size: 11px; color: #64748b; }

.detail-schedules { margin-bottom: 18px; }
.detail-schedules-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  display: block;
  margin-bottom: 6px;
}
.detail-card-footer {
  display: flex;
  gap: 10px;
  margin-top: auto;
}

/* Filter Card */
.filter-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  flex-wrap: wrap;
}

.search-input-wrap {
  flex: 1;
  min-width: 260px;
  position: relative;
  display: flex;
  align-items: center;
}
.search-icon {
  position: absolute;
  left: 12px;
  width: 17px;
  height: 17px;
  color: #94a3b8;
}
.select-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 220px;
}
.select-icon {
  position: absolute;
  left: 12px;
  width: 16px;
  height: 16px;
  color: #94a3b8;
}

.input-control, .select-control {
  width: 100%;
  padding: 9px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font-size: 13px;
  color: #0f172a;
  background: #ffffff;
  outline: none;
  transition: border-color 0.15s;
}
.search-input-wrap .input-control { padding-left: 38px; padding-right: 36px; }
.select-input-wrap .select-control { padding-left: 36px; }
.input-control:focus, .select-control:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.clear-search-btn {
  position: absolute;
  right: 10px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 200px;
}
.form-label {
  font-size: 11px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.date-group {
  display: flex;
  gap: 8px;
}

/* Buttons System (Supervisor Style) */
.btn-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 9px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}
.btn-action.primary {
  background: #4f46e5;
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}
.btn-action.primary:hover:not(:disabled) {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.32);
}
.btn-action.secondary {
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #e2e8f0;
}
.btn-action.secondary:hover {
  background: #e2e8f0;
  color: #0f172a;
}
.btn-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-sm { padding: 6px 12px; font-size: 12px; }
.btn-xs { padding: 5px 10px; font-size: 11.5px; }
.btn-icon { width: 15px; height: 15px; }

/* Table System (Clean Light White/Slate) */
.table-responsive { overflow-x: auto; }
.table-custom {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.table-custom th {
  background: #f8fafc;
  padding: 12px 18px;
  color: #475569;
  font-size: 11.5px;
  text-transform: uppercase;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #e2e8f0;
}
.table-custom td {
  padding: 14px 18px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
  color: #334155;
}
.table-custom tr:hover td {
  background: #f8fafc;
}

.text-slate { color: #0f172a; }
.font-bold { font-weight: 700; }
.text-sub { color: #64748b; }
.text-xs { font-size: 11px; }
.text-sm { font-size: 12.5px; }
.text-lg { font-size: 16px; }
.block { display: block; }
.flex-1 { flex: 1; }
.w-full { width: 100%; }

.matricula-chip {
  display: inline-block;
  font-family: monospace;
  font-size: 11.5px;
  font-weight: 600;
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
}

.group-pill {
  display: inline-block;
  font-size: 11.5px;
  font-weight: 600;
  color: #4338ca;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 2px 8px;
  border-radius: 6px;
}

/* Status Badges */
.badge-status {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.status-present {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}
.status-delay {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}
.status-absent {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

/* User row in table */
.user-row-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.student-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
  flex-shrink: 0;
}
.student-avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 11px;
  flex-shrink: 0;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.contact-line {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #475569;
}
.item-icon { width: 13px; height: 13px; flex-shrink: 0; }

.attendance-meter { width: 130px; }
.meter-bar {
  width: 100%;
  height: 6px;
  background: #e2e8f0;
  border-radius: 9999px;
  overflow: hidden;
  margin-bottom: 4px;
}
.meter-fill { height: 100%; border-radius: 9999px; }
.fill-green { background: #10b981; }
.fill-amber { background: #f59e0b; }
.fill-red { background: #ef4444; }
.meter-labels {
  display: flex;
  justify-content: space-between;
}

/* Section Title Bar */
.section-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}
.section-title-bar h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}
.section-title-bar p {
  margin: 3px 0 0;
  font-size: 13px;
  color: #64748b;
}
.badge-pill-count {
  font-size: 12px;
  font-weight: 700;
  color: #4338ca;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 4px 12px;
  border-radius: 20px;
}

/* Mode Switcher */
.mode-switcher {
  display: flex;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 4px;
  gap: 4px;
}
.switcher-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: transparent;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}
.switcher-tab:hover { color: #0f172a; }
.switcher-tab.is-active {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

/* Roster Roll Call */
.roster-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 16px;
}
.roster-metrics {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.count-chip {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #e2e8f0;
}
.count-chip.green { background: #ecfdf5; color: #047857; border-color: #a7f3d0; }
.count-chip.amber { background: #fffbeb; color: #b45309; border-color: #fde68a; }
.count-chip.red { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }

.roster-items-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.roster-item-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 18px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  transition: all 0.15s;
  flex-wrap: wrap;
}
.roster-item-card:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}
.border-green { border-left: 4px solid #10b981; }
.border-amber { border-left: 4px solid #f59e0b; }
.border-red { border-left: 4px solid #ef4444; }

.item-index {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  width: 20px;
}
.item-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 220px;
}

.segmented-state-picker {
  display: flex;
  gap: 6px;
}
.seg-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}
.seg-present.is-selected {
  background: #10b981;
  border-color: #059669;
  color: white;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.25);
}
.seg-delay.is-selected {
  background: #f59e0b;
  border-color: #d97706;
  color: white;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.25);
}
.seg-absent.is-selected {
  background: #ef4444;
  border-color: #dc2626;
  color: white;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.25);
}

.roster-save-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 14px;
}

/* QR Mode */
.max-w-2xl { max-width: 650px; }
.mx-auto { margin-left: auto; margin-right: auto; }
.qr-intro-header {
  margin-bottom: 24px;
}
.qr-icon-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}
.qr-icon-lg { width: 32px; height: 32px; }
.qr-intro-header h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}
.qr-intro-header p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
.qr-form-row {
  display: flex;
  gap: 10px;
}
.qr-text-input {
  font-size: 14px;
  padding: 11px 16px;
}
.qr-feedback-card {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 12px;
  padding: 16px 20px;
  text-align: left;
}
.feedback-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #047857;
  margin-bottom: 12px;
}
.feedback-header h4 { margin: 0; font-size: 15px; font-weight: 700; }
.feedback-icon { width: 20px; height: 20px; }
.feedback-body {
  display: flex;
  align-items: center;
  gap: 16px;
}
.student-avatar-lg {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
  flex-shrink: 0;
}
.feedback-text {
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* History Tabs */
.history-tabs {
  display: flex;
  gap: 6px;
}
.hist-tab {
  padding: 6px 14px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.hist-tab.is-active {
  background: #4f46e5;
  color: white;
  border-color: #4f46e5;
}
.tab-green.is-active { background: #10b981; border-color: #10b981; }
.tab-amber.is-active { background: #f59e0b; border-color: #f59e0b; }
.tab-red.is-active { background: #ef4444; border-color: #ef4444; }

/* Weekly Schedules Grid */
.weekly-columns-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
}
.day-col-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}
.day-col-header {
  padding: 12px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.day-title {
  font-size: 13.5px;
  font-weight: 800;
  color: #0f172a;
}
.day-sub {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}
.day-slots-wrap {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 140px;
}
.slot-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 110px;
  color: #94a3b8;
  font-size: 12px;
  font-style: italic;
  text-align: center;
}
.slot-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #4f46e5;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
}
.slot-time-row {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #4f46e5;
  margin-bottom: 4px;
}
.slot-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 2px;
}
.slot-workshop {
  font-size: 11.5px;
  color: #64748b;
  margin: 0;
}

/* Profile Grid */
.profile-layout-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
}
@media (max-width: 768px) {
  .profile-layout-grid {
    grid-template-columns: 1fr;
  }
}
.profile-summary-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 36px 20px;
}
.avatar-large-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 800;
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.3);
}
.profile-rows-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.profile-data-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13.5px;
}
.data-label { color: #64748b; }
.data-val { font-weight: 600; color: #0f172a; }

/* Empty & Loading States */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
}
.empty-icon {
  width: 44px;
  height: 44px;
  color: #94a3b8;
  margin-bottom: 10px;
}
.empty-state h4 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
.empty-state p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #64748b;
}
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}
.spinner-sm {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Alert Banner */
.error-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  padding: 14px 20px;
  border-radius: 12px;
}
.error-icon { width: 22px; height: 22px; flex-shrink: 0; }
.error-content h4 { margin: 0; font-size: 14px; font-weight: 700; }
.error-content p { margin: 2px 0 0; font-size: 12.5px; }
.btn-retry {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: white;
  border: 1px solid #fca5a5;
  color: #b91c1c;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.mt-2 { margin-top: 8px; }
.mt-3 { margin-top: 12px; }
.mt-4 { margin-top: 18px; }
.border-b { border-bottom: 1px solid #e2e8f0; }
.pb-3 { padding-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.text-center { text-align: center; }
.inline-block { display: inline-block; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
