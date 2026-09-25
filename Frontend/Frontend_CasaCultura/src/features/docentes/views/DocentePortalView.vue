<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DashboardLayout from '../../../components/DashboardLayout.vue'
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
} from '../../../services/docenteService'
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
                        <img v-if="stu.fotoUrl" :src="stu.fotoUrl" alt="" class="avatar-photo" />
                        <span v-else>{{ stu.nombre?.[0] || 'A' }}{{ stu.apellidoPaterno?.[0] || 'L' }}</span>
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
                    <img v-if="student.fotoUrl" :src="student.fotoUrl" alt="" class="avatar-photo" />
                    <span v-else>{{ student.nombre?.[0] || 'A' }}{{ student.apellidoPaterno?.[0] || 'L' }}</span>
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
                  <img v-if="qrResult.fotoUrl" :src="qrResult.fotoUrl" alt="" class="avatar-photo" />
                  <span v-else>{{ qrResult.alumno?.[0] || 'A' }}</span>
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
              <img v-if="teacherProfile?.fotoUrl" :src="teacherProfile.fotoUrl" alt="Foto del docente" class="avatar-photo" />
              <span v-else>{{ teacherDisplayName?.[0] || 'D' }}</span>
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

<style scoped src="../../../assets/styles/docente.css"></style>
