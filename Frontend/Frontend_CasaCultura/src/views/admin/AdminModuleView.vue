<script setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Plus,
  Search,
  Download,
  MoreVertical,
  Users,
  ShieldCheck,
  GraduationCap,
  UserCheck,
  BookOpen,
  CalendarDays,
  ClipboardList,
  CreditCard,
  BarChart3,
  Settings,
  Database,
  User,
  Pencil,
  Trash2,
  KeyRound,
  QrCode,
  CheckCircle2,
  AlertCircle,
  X,
  CalendarCheck2,
  Printer,
  UserPlus,
  Check,
  ChevronDown,
  ChevronUp,
  Clock,
  Layers,
  FolderPlus,
  Tag,
  Calendar,
  Sparkles,
  List,
  XCircle,
  DollarSign,
  AlertTriangle,
  Mail
} from 'lucide-vue-next'

import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminHeader from '../../components/admin/AdminHeader.vue'
import {
  getSuperAdminData,
  createStudent,
  updateStudent,
  deactivateStudent,
  generateStudentCredential,
  generateStudentUser,
  createTeacher,
  updateTeacher,
  deactivateTeacher,
  assignTeacherToGroup,
  createCourse,
  updateCourse,
  createAgeCategory,
  createOffer,
  createGroup,
  createSchedule,
  createSchedulesBatch,
  deleteSchedule,
  createEnrollment,
  deactivateEnrollment,
  createPayment,
  updatePaymentStatus,
  createUser,
  deactivateUser,
  resetUserPassword,
  createRole,
  updateRole,
  deleteRole,
  registerAttendance,
  generateAbsences,
  getAttendanceReport
} from '../../services/superAdminService.js'

const route = useRoute()
const search = ref('')
const isLoading = ref(true)
const isSaving = ref(false)
const toast = reactive({ show: false, message: '', type: 'success' })

function showToast(msg, type = 'success') {
  toast.message = msg
  toast.type = type
  toast.show = true
  setTimeout(() => { toast.show = false }, 3500)
}

// Data Store (All relational blocks)
const data = reactive({
  alumnos: [],
  docentes: [],
  cursos: [],
  ofertas: [],
  grupos: [],
  categorias: [],
  horarios: [],
  asignacionesDocentes: [],
  inscripciones: [],
  pagos: [],
  asistencias: [],
  usuarios: [],
  roles: []
})

async function loadAllData() {
  isLoading.value = true
  try {
    const res = await getSuperAdminData()
    Object.assign(data, res)
  } catch (err) {
    showToast(err.message || 'Error cargando información', 'error')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadAllData)

watch(() => route.path, () => {
  search.value = ''
  activeActionMenu.value = null
  errors.value = {}
})

const moduleType = computed(() => route.meta.type || 'users')

const icons = {
  users: Users,
  roles: ShieldCheck,
  students: GraduationCap,
  teachers: UserCheck,
  workshops: BookOpen,
  schedules: CalendarDays,
  registrations: ClipboardList,
  attendance: UserCheck,
  payments: CreditCard,
  monetization: BarChart3,
  reports: BarChart3,
  settings: Settings,
  logs: Database
}

const moduleIcon = computed(() => icons[moduleType.value] || User)

const titles = {
  users: 'Nuevo Usuario',
  roles: 'Nuevo Rol',
  students: 'Nuevo Alumno',
  teachers: 'Nuevo Docente',
  workshops: 'Nuevo Taller Cultural',
  schedules: 'Nuevo Horario',
  registrations: 'Nueva Inscripción',
  attendance: 'Registrar Asistencia',
  payments: 'Registrar Pago'
}

const buttonText = computed(() => titles[moduleType.value] || null)

// Actions dropdown state
const activeActionMenu = ref(null)
function toggleActionMenu(id) {
  activeActionMenu.value = activeActionMenu.value === id ? null : id
}

// Modal State
const showModal = ref(false)
const modalMode = ref('create') 
// 'create' | 'edit' | 'reset-pass' | 'credential' | 'student-user' | 'assign-teacher' 
// | 'create-full-workshop' | 'create-direct-group' | 'add-schedule-group' | 'assign-teacher-group' | 'create-age-category' | 'edit-course'
const editingId = ref(null)
const errors = ref({})

// Searchable Person Picker State (for User creation)
const personSearch = ref('')
const personTypeFilter = ref('ALL') // 'ALL' | 'ALUMNO' | 'DOCENTE'
const selectedPerson = ref(null)

const allPersons = computed(() => {
  const existingUserPersonaIds = new Set((data.usuarios || []).map(u => Number(u.personaId)))
  const list = []
  data.alumnos.forEach(a => {
    const pId = Number(a.personaId || a.persona?.id || a.id)
    list.push({
      id: pId,
      tipo: 'ALUMNO',
      nombreCompleto: `${a.nombre} ${a.apellidoPaterno} ${a.apellidoMaterno || ''}`.trim(),
      detalle: `Matrícula: ${a.matricula}`,
      correo: a.correo || 'Sin correo',
      telefono: a.telefono || '',
      matricula: a.matricula,
      hasUser: existingUserPersonaIds.has(pId),
      raw: a
    })
  })
  data.docentes.forEach(d => {
    const pId = Number(d.personaId || d.persona?.id || d.id)
    list.push({
      id: pId,
      tipo: 'DOCENTE',
      nombreCompleto: `${d.nombre} ${d.apellidoPaterno} ${d.apellidoMaterno || ''}`.trim(),
      detalle: `Especialidad: ${d.especialidad || 'General'}`,
      correo: d.correo || 'Sin correo',
      telefono: d.telefono || '',
      especialidad: d.especialidad,
      hasUser: existingUserPersonaIds.has(pId),
      raw: d
    })
  })
  return list
})

const filteredPersons = computed(() => {
  let list = allPersons.value.filter(p => !p.hasUser)
  if (personTypeFilter.value !== 'ALL') {
    list = list.filter(p => p.tipo === personTypeFilter.value)
  }
  const q = personSearch.value.trim().toLowerCase()
  if (!q) return list.slice(0, 15)
  return list.filter(p =>
    p.nombreCompleto.toLowerCase().includes(q) ||
    p.detalle.toLowerCase().includes(q) ||
    p.correo.toLowerCase().includes(q) ||
    p.telefono.includes(q)
  ).slice(0, 25)
})

function selectPersonForUser(p) {
  selectedPerson.value = p
  formUser.personaId = p.id
  errors.value.personaId = null

  if (!formUser.nombreUsuario) {
    if (p.tipo === 'ALUMNO' && p.matricula) {
      formUser.nombreUsuario = p.matricula.toLowerCase().replace(/[^a-z0-9]/g, '')
    } else {
      const parts = p.nombreCompleto.toLowerCase().split(' ')
      formUser.nombreUsuario = `${parts[0]}.${parts[1] || 'user'}`.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9.]/g, '')
    }
  }

  if (!formUser.rolId) {
    const targetRoleName = p.tipo === 'ALUMNO' ? 'ALUMNO' : 'DOCENTE'
    const foundRole = data.roles.find(r => r.nombre.toUpperCase().includes(targetRoleName))
    if (foundRole) formUser.rolId = foundRole.id
  }
}

function clearSelectedPerson() {
  selectedPerson.value = null
  formUser.personaId = ''
}

const userCreationType = ref('admin') // 'admin' | 'existing'
const formAdminPerson = reactive({
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  correo: '',
  telefono: ''
})

function setUserCreationType(type) {
  userCreationType.value = type
  errors.value = {}
  if (type === 'admin') {
    selectedPerson.value = null
    formUser.personaId = ''
    if (!formUser.rolId) {
      const supRole = data.roles.find(r => r.nombre.toUpperCase().includes('SUPERVISOR'))
      if (supRole) formUser.rolId = supRole.id
    }
  } else {
    formUser.rolId = ''
  }
}

function onAdminPersonNameInput() {
  if (formAdminPerson.nombre && formAdminPerson.apellidoPaterno && (!formUser.nombreUsuario || formUser.nombreUsuario.includes('.'))) {
    const p1 = formAdminPerson.nombre.trim().toLowerCase().split(' ')[0]
    const p2 = formAdminPerson.apellidoPaterno.trim().toLowerCase()
    formUser.nombreUsuario = `${p1}.${p2}`.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9.]/g, '')
  }
}

// ----------------------------------------------------
// WORKSHOP HIERARCHY LOGIC (OPTION A)
// ----------------------------------------------------
const expandedWorkshops = ref(new Set())

watch(() => data.cursos, (newCourses) => {
  if (newCourses && newCourses.length > 0) {
    newCourses.forEach(c => expandedWorkshops.value.add(c.id))
  }
}, { immediate: true })

function toggleWorkshopExpand(courseId) {
  if (expandedWorkshops.value.has(courseId)) {
    expandedWorkshops.value.delete(courseId)
  } else {
    expandedWorkshops.value.add(courseId)
  }
}

function expandAllWorkshops() {
  data.cursos.forEach(c => expandedWorkshops.value.add(c.id))
}

function collapseAllWorkshops() {
  expandedWorkshops.value.clear()
}

const dayNamesMap = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
  LUNES: 'Lunes',
  MARTES: 'Martes',
  MIERCOLES: 'Miércoles',
  JUEVES: 'Jueves',
  VIERNES: 'Viernes',
  SABADO: 'Sábado',
  DOMINGO: 'Domingo'
}

function formatDay(d) {
  return dayNamesMap[d] || d
}

function formatTime(t) {
  if (!t) return ''
  return String(t).substring(0, 5)
}

const workshopsWithHierarchy = computed(() => {
  const q = search.value.trim().toLowerCase()

  return data.cursos.map(curso => {
    // Find all ofertas for this course
    const ofertas = data.ofertas.filter(o => o.cursoId === curso.id || o.curso === curso.nombre)
    const ofertaIds = new Set(ofertas.map(o => o.id))

    // Find all grupos for this course
    const grupos = data.grupos.filter(g =>
      (g.ofertaId && ofertaIds.has(g.ofertaId)) ||
      g.cursoId === curso.id ||
      g.nombreCurso === curso.nombre ||
      g.curso === curso.nombre
    ).map(grupo => {
      // Find category
      const categoria = data.categorias.find(c => c.id === grupo.categoriaId) || { nombre: grupo.categoria || 'General' }

      // Find teacher assignment
      const asignacion = (data.asignacionesDocentes || [])
        .filter(a => a.grupoId === grupo.id)
        .sort((a, b) => b.id - a.id)[0]

      let docente = null
      if (asignacion) {
        docente = data.docentes.find(d => d.id === asignacion.docenteId) || {
          id: asignacion.docenteId,
          nombre: asignacion.docente || asignacion.nombreDocente || 'Docente Asignado'
        }
      }

      // Find schedules
      const horarios = (data.horarios || []).filter(h => h.grupoId === grupo.id)

      // Find enrolled students
      const inscritos = (data.inscripciones || []).filter(i => i.grupoId === grupo.id)

      return {
        ...grupo,
        categoriaObj: categoria,
        categoriaNombre: categoria.nombre || 'General',
        docenteAsignado: docente,
        asignacionActual: asignacion,
        horariosList: horarios,
        totalInscritos: inscritos.length,
        inscripcionesList: inscritos
      }
    })

    const totalAlumnos = grupos.reduce((acc, g) => acc + g.totalInscritos, 0)
    const ofertaActual = ofertas[ofertas.length - 1] || null

    return {
      ...curso,
      ofertas,
      ofertaActual,
      grupos,
      totalGrupos: grupos.length,
      totalAlumnos
    }
  }).filter(c => {
    if (!q) return true
    const matchCourse = c.nombre.toLowerCase().includes(q)
    const matchGroup = c.grupos.some(g =>
      g.nombreGrupo?.toLowerCase().includes(q) ||
      g.categoriaNombre?.toLowerCase().includes(q) ||
      (g.docenteAsignado && `${g.docenteAsignado.nombre} ${g.docenteAsignado.apellidoPaterno || ''}`.toLowerCase().includes(q))
    )
    return matchCourse || matchGroup
  })
})

// Quick stats for workshops view
const workshopStats = computed(() => {
  const totalCourses = data.cursos.length
  const totalGroups = data.grupos.length
  const assignedTeachersCount = new Set((data.asignacionesDocentes || []).map(a => a.docenteId)).size
  const totalStudents = data.inscripciones.length
  return { totalCourses, totalGroups, assignedTeachersCount, totalStudents }
})

// ==========================================
// SCHEDULES (HORARIOS) EXPERIENCE STATE & COMPUTEDS
// ==========================================
const scheduleViewMode = ref('calendar') // 'calendar' | 'groups' | 'list'
const scheduleDayFilter = ref('ALL') // 'ALL' | 'MONDAY' | 'TUESDAY' | ...
const scheduleCourseFilter = ref('ALL') // 'ALL' | courseId

const weekDays = [
  { key: 'MONDAY', name: 'Lunes', short: 'LUN' },
  { key: 'TUESDAY', name: 'Martes', short: 'MAR' },
  { key: 'WEDNESDAY', name: 'Miércoles', short: 'MIÉ' },
  { key: 'THURSDAY', name: 'Jueves', short: 'JUE' },
  { key: 'FRIDAY', name: 'Viernes', short: 'VIE' },
  { key: 'SATURDAY', name: 'Sábado', short: 'SÁB' },
  { key: 'SUNDAY', name: 'Domingo', short: 'DOM' }
]

const enrichedSchedules = computed(() => {
  const q = search.value.trim().toLowerCase()
  const dayFilter = scheduleDayFilter.value
  const courseFilter = scheduleCourseFilter.value

  return (data.horarios || []).map(h => {
    const grupo = data.grupos.find(g => g.id === h.grupoId) || {}
    const oferta = data.ofertas.find(o => o.id === grupo.ofertaId) || {}
    const curso = data.cursos.find(c => c.id === oferta.cursoId) || {}
    const categoria = data.categorias.find(cat => cat.id === grupo.categoriaId) || {}

    let docenteName = 'Sin docente asignado'
    const asignacion = (data.asignacionesDocentes || []).find(a => a.grupoId === grupo.id)
    if (asignacion) {
      const d = data.docentes.find(doc => doc.id === asignacion.docenteId)
      if (d) docenteName = `${d.nombre} ${d.apellidoPaterno || ''}`
      else if (asignacion.docente) docenteName = asignacion.docente
    }

    let durationText = ''
    let durationMinutes = 0
    if (h.horaInicio && h.horaFin) {
      const [sh, sm] = String(h.horaInicio).split(':').map(Number)
      const [eh, em] = String(h.horaFin).split(':').map(Number)
      const diffMins = (eh * 60 + (em || 0)) - (sh * 60 + (sm || 0))
      if (diffMins > 0) {
        durationMinutes = diffMins
        const hours = Math.floor(diffMins / 60)
        const mins = diffMins % 60
        durationText = hours > 0 ? (mins > 0 ? `${hours}h ${mins}m` : `${hours}h`) : `${mins}m`
      }
    }

    const colors = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4', '#3b82f6', '#14b8a6']
    const colorIndex = (curso.id || h.grupoId || h.id || 0) % colors.length
    const themeColor = colors[colorIndex]

    return {
      ...h,
      grupo,
      nombreGrupo: grupo.nombreGrupo || h.nombreGrupo || 'Grupo Cultural',
      curso,
      cursoId: curso.id,
      nombreCurso: curso.nombre || h.nombre || 'Taller Cultural',
      categoriaNombre: categoria.nombre || 'General',
      docenteName,
      diaTexto: formatDay(h.dia),
      diaKey: String(h.dia).toUpperCase(),
      horaInicioFormateada: formatTime(h.horaInicio),
      horaFinFormateada: formatTime(h.horaFin),
      rangoHorario: `${formatTime(h.horaInicio)} - ${formatTime(h.horaFin)}`,
      durationText,
      durationMinutes,
      themeColor
    }
  }).filter(item => {
    if (dayFilter !== 'ALL' && item.diaKey !== dayFilter) return false
    if (courseFilter !== 'ALL' && String(item.cursoId) !== String(courseFilter)) return false
    if (q) {
      return item.nombreCurso.toLowerCase().includes(q) ||
             item.nombreGrupo.toLowerCase().includes(q) ||
             item.docenteName.toLowerCase().includes(q) ||
             item.diaTexto.toLowerCase().includes(q) ||
             item.rangoHorario.includes(q)
    }
    return true
  }).sort((a, b) => (a.horaInicio || '').localeCompare(b.horaInicio || ''))
})

const scheduleStats = computed(() => {
  const all = data.horarios || []
  const groupIdsWithSched = new Set(all.map(h => h.grupoId))

  let totalMinutes = 0
  const dayCounts = {}
  all.forEach(h => {
    const dKey = String(h.dia).toUpperCase()
    dayCounts[dKey] = (dayCounts[dKey] || 0) + 1
    if (h.horaInicio && h.horaFin) {
      const [sh, sm] = String(h.horaInicio).split(':').map(Number)
      const [eh, em] = String(h.horaFin).split(':').map(Number)
      const diff = (eh * 60 + (em || 0)) - (sh * 60 + (sm || 0))
      if (diff > 0) totalMinutes += diff
    }
  })

  let peakDayKey = ''
  let peakCount = 0
  for (const [day, count] of Object.entries(dayCounts)) {
    if (count > peakCount) {
      peakCount = count
      peakDayKey = day
    }
  }

  const peakDayName = peakDayKey ? `${formatDay(peakDayKey)} (${peakCount} clases)` : 'Sin clases'
  const totalHours = (totalMinutes / 60).toFixed(1).replace('.0', '')

  return {
    totalSessions: all.length,
    groupsWithSchedule: groupIdsWithSched.size,
    totalGroups: data.grupos.length,
    totalHours,
    peakDay: peakDayName
  }
})

const schedulesByDay = computed(() => {
  const todayDayNumber = new Date().getDay()
  const dayMapJS = { 1: 'MONDAY', 2: 'TUESDAY', 3: 'WEDNESDAY', 4: 'THURSDAY', 5: 'FRIDAY', 6: 'SATURDAY', 0: 'SUNDAY' }
  const todayKey = dayMapJS[todayDayNumber]

  const activeDays = scheduleDayFilter.value === 'ALL'
    ? weekDays
    : weekDays.filter(wd => wd.key === scheduleDayFilter.value)

  return activeDays.map(wd => {
    const sessions = enrichedSchedules.value.filter(s => s.diaKey === wd.key)
    return {
      ...wd,
      isToday: wd.key === todayKey,
      sessions,
      totalSessions: sessions.length
    }
  })
})

const schedulesByGroup = computed(() => {
  const q = search.value.trim().toLowerCase()
  const courseFilter = scheduleCourseFilter.value

  return data.grupos.map(g => {
    const oferta = data.ofertas.find(o => o.id === g.ofertaId) || {}
    const curso = data.cursos.find(c => c.id === oferta.cursoId) || {}
    const categoria = data.categorias.find(cat => cat.id === g.categoriaId) || {}

    const groupSchedules = (data.horarios || []).filter(h => h.grupoId === g.id).map(h => ({
      ...h,
      diaTexto: formatDay(h.dia),
      diaKey: String(h.dia).toUpperCase(),
      horaInicioFormateada: formatTime(h.horaInicio),
      horaFinFormateada: formatTime(h.horaFin),
      rangoHorario: `${formatTime(h.horaInicio)} - ${formatTime(h.horaFin)}`
    })).sort((a, b) => {
      const order = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
      return order.indexOf(a.diaKey) - order.indexOf(b.diaKey)
    })

    let totalMinutes = 0
    groupSchedules.forEach(h => {
      if (h.horaInicio && h.horaFin) {
        const [sh, sm] = String(h.horaInicio).split(':').map(Number)
        const [eh, em] = String(h.horaFin).split(':').map(Number)
        const diff = (eh * 60 + (em || 0)) - (sh * 60 + (sm || 0))
        if (diff > 0) totalMinutes += diff
      }
    })
    const totalHours = (totalMinutes / 60).toFixed(1).replace('.0', '')

    let docenteName = 'Sin docente asignado'
    const asignacion = (data.asignacionesDocentes || []).find(a => a.grupoId === g.id)
    if (asignacion) {
      const d = data.docentes.find(doc => doc.id === asignacion.docenteId)
      if (d) docenteName = `${d.nombre} ${d.apellidoPaterno || ''}`
      else if (asignacion.docente) docenteName = asignacion.docente
    }

    const inscritos = (data.inscripciones || []).filter(i => i.grupoId === g.id).length

    return {
      ...g,
      cursoId: curso.id,
      cursoNombre: curso.nombre || 'Taller Cultural',
      categoriaNombre: categoria.nombre || 'General',
      docenteName,
      totalInscritos: inscritos,
      horarios: groupSchedules,
      totalSesiones: groupSchedules.length,
      totalHours
    }
  }).filter(g => {
    if (courseFilter !== 'ALL' && String(g.cursoId) !== String(courseFilter)) return false
    if (q) {
      return (g.nombreGrupo && g.nombreGrupo.toLowerCase().includes(q)) ||
             (g.cursoNombre && g.cursoNombre.toLowerCase().includes(q)) ||
             (g.docenteName && g.docenteName.toLowerCase().includes(q))
    }
    return true
  })
})

async function handleDeleteSchedule(scheduleId, desc = '') {
  if (!confirm(`¿Estás seguro de eliminar este horario ${desc ? `"${desc}"` : ''}?`)) {
    return
  }
  isSaving.value = true
  try {
    await deleteSchedule(scheduleId)
    showToast('Horario eliminado exitosamente.')
    await loadAllData()
  } catch (err) {
    showToast(err.message || 'Error al eliminar el horario.', 'error')
  } finally {
    isSaving.value = false
  }
}

const weekDaysList = [
  { key: 'MONDAY', label: 'Lunes', short: 'Lun' },
  { key: 'TUESDAY', label: 'Martes', short: 'Mar' },
  { key: 'WEDNESDAY', label: 'Miércoles', short: 'Mié' },
  { key: 'THURSDAY', label: 'Jueves', short: 'Jue' },
  { key: 'FRIDAY', label: 'Viernes', short: 'Vie' },
  { key: 'SATURDAY', label: 'Sábado', short: 'Sáb' },
  { key: 'SUNDAY', label: 'Domingo', short: 'Dom' }
]

function toggleSlotDay(slot, dayKey) {
  if (!slot.dias) slot.dias = []
  const idx = slot.dias.indexOf(dayKey)
  if (idx >= 0) {
    if (slot.dias.length > 1) {
      slot.dias.splice(idx, 1)
    }
  } else {
    slot.dias.push(dayKey)
  }
}

function applySlotPreset(slot, preset) {
  if (preset === 'MWF') {
    slot.dias = ['MONDAY', 'WEDNESDAY', 'FRIDAY']
  } else if (preset === 'TTH') {
    slot.dias = ['TUESDAY', 'THURSDAY']
  } else if (preset === 'WEEKDAYS') {
    slot.dias = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
  } else if (preset === 'WEEKEND') {
    slot.dias = ['SATURDAY', 'SUNDAY']
  } else if (preset === 'SAT') {
    slot.dias = ['SATURDAY']
  }
}

function addSlotToSchedule(target) {
  if (!target.slots) target.slots = []
  target.slots.push({
    dias: ['SATURDAY'],
    horaInicio: '10:00',
    horaFin: '12:00'
  })
}

function removeSlotFromSchedule(target, index) {
  if (target.slots && target.slots.length > 1) {
    target.slots.splice(index, 1)
  }
}

function countTotalSessions(slots) {
  if (!slots || !slots.length) return 0
  return slots.reduce((acc, s) => acc + (s.dias ? s.dias.length : 0), 0)
}

function openScheduleModalForGroup(grupoId) {
  editingId.value = null
  errors.value = {}
  modalMode.value = 'create'
  formSchedule.grupoId = grupoId
  formSchedule.dia = 'MONDAY'
  formSchedule.horaInicio = '16:00'
  formSchedule.horaFin = '18:00'
  formSchedule.slots = [
    { dias: ['MONDAY', 'WEDNESDAY', 'FRIDAY'], horaInicio: '16:00', horaFin: '18:00' }
  ]
  showModal.value = true
}

function openScheduleModalForDay(dayKey) {
  editingId.value = null
  errors.value = {}
  modalMode.value = 'create'
  formSchedule.grupoId = data.grupos[0]?.id || ''
  formSchedule.dia = dayKey
  formSchedule.horaInicio = '16:00'
  formSchedule.horaFin = '18:00'
  formSchedule.slots = [
    { dias: [dayKey], horaInicio: '16:00', horaFin: '18:00' }
  ]
  showModal.value = true
}

// Forms state
const formUser = reactive({ personaId: '', rolId: '', nombreUsuario: '', password: '' })
const formRole = reactive({ nombre: '', descripcion: '' })
const formStudent = reactive({
  nombre: '', apellidoPaterno: '', apellidoMaterno: '', matricula: '',
  fechaNacimiento: '', telefono: '', direccion: '', correo: '', fotoUrl: ''
})
const formTeacher = reactive({
  nombre: '', apellidoPaterno: '', apellidoMaterno: '', especialidad: '',
  fechaNacimiento: '', telefono: '', direccion: '', correo: ''
})
const formTeacherAssignment = reactive({
  docenteId: '', grupoId: '', fechaInicio: new Date().toISOString().slice(0, 10), fechaFin: ''
})
const formSchedule = reactive({
  grupoId: '',
  dia: 'MONDAY',
  horaInicio: '16:00',
  horaFin: '18:00',
  slots: [
    { dias: ['MONDAY', 'WEDNESDAY', 'FRIDAY'], horaInicio: '16:00', horaFin: '18:00' }
  ]
})
const formEnrollment = reactive({ alumnoId: '', grupoId: '' })

// Enhanced Enrollment (Inscripciones) State & Computeds
const enrollmentStudentSearch = ref('')
const selectedEnrollmentStudent = ref(null)
const registrationStatusFilter = ref('ALL') // 'ALL' | 'ACTIVA' | 'BAJA'
const registrationCourseFilter = ref('ALL') // 'ALL' | courseId

const registrationStats = computed(() => {
  const total = data.inscripciones.length
  const active = data.inscripciones.filter(i => i.estado === 'ACTIVA' || !i.estado).length
  const bajas = data.inscripciones.filter(i => i.estado === 'BAJA').length
  const uniqueStudents = new Set(data.inscripciones.map(i => i.alumnoId)).size
  return { total, active, bajas, uniqueStudents }
})

const filteredStudentsForEnrollment = computed(() => {
  const q = enrollmentStudentSearch.value.trim().toLowerCase()
  const all = data.alumnos || []
  if (!q) return all.slice(0, 10)
  return all.filter(a => {
    const fullName = `${a.nombre} ${a.apellidoPaterno || ''} ${a.apellidoMaterno || ''}`.toLowerCase()
    const mat = (a.matricula || '').toLowerCase()
    const tel = (a.telefono || '').replace(/\D/g, '')
    const em = (a.correo || '').toLowerCase()
    return fullName.includes(q) || mat.includes(q) || tel.includes(q) || em.includes(q)
  }).slice(0, 20)
})

const selectedEnrollmentGroupInfo = computed(() => {
  if (!formEnrollment.grupoId) return null
  const g = data.grupos.find(x => x.id === Number(formEnrollment.grupoId))
  if (!g) return null
  const oferta = data.ofertas.find(o => o.id === g.ofertaId) || {}
  const curso = data.cursos.find(c => c.id === oferta.cursoId) || {}
  const cat = data.categorias.find(c => c.id === g.categoriaId) || {}

  let docenteName = 'Sin docente asignado'
  const asignacion = (data.asignacionesDocentes || []).find(a => a.grupoId === g.id)
  if (asignacion) {
    const d = data.docentes.find(doc => doc.id === asignacion.docenteId)
    if (d) docenteName = `${d.nombre} ${d.apellidoPaterno || ''}`
    else if (asignacion.docente) docenteName = asignacion.docente
  } else if (g.docente) {
    docenteName = g.docente
  }

  const groupSchedules = (data.horarios || []).filter(h => h.grupoId === g.id).map(h => ({
    diaTexto: formatDay(h.dia),
    rangoHorario: `${formatTime(h.horaInicio)} - ${formatTime(h.horaFin)}`
  }))

  const enrolledCount = (data.inscripciones || []).filter(i => i.grupoId === g.id && i.estado !== 'BAJA').length
  return {
    ...g,
    nombreCurso: curso.nombre || g.curso || 'Taller Cultural',
    nombreCategoria: cat.nombre || 'General',
    docenteName,
    schedules: groupSchedules,
    enrolledCount
  }
})

function selectStudentForEnrollment(alumno) {
  selectedEnrollmentStudent.value = alumno
  formEnrollment.alumnoId = alumno.id
  if (errors.value.alumnoId) delete errors.value.alumnoId
}

function clearSelectedStudentForEnrollment() {
  selectedEnrollmentStudent.value = null
  formEnrollment.alumnoId = ''
  enrollmentStudentSearch.value = ''
}
const formAttendance = reactive({
  inscripcionId: '', horarioId: '', fecha: new Date().toISOString().slice(0, 10), estado: 'PRESENTE'
})
const formPayment = reactive({
  inscripcionId: '', tipoPago: 'INSCRIPCION', periodo: `${new Date().getFullYear()}-1`,
  fechaVencimiento: '', fechaPago: new Date().toISOString().slice(0, 10), estado: 'PAGADO', monto: 500
})

// Enhanced Attendance (Asistencias) State & Computeds
const attendanceStudentSearch = ref('')
const selectedAttendanceEnrollment = ref(null)
const attendanceStatusFilter = ref('ALL') // 'ALL' | 'PRESENTE' | 'RETARDO' | 'FALTA'
const attendanceCourseFilter = ref('ALL')
const attendanceDateFilter = ref('')

const attendanceStats = computed(() => {
  const total = data.asistencias.length
  const presentes = data.asistencias.filter(a => a.estado === 'PRESENTE').length
  const retardos = data.asistencias.filter(a => a.estado === 'RETARDO').length
  const faltas = data.asistencias.filter(a => a.estado === 'FALTA' || a.estado === 'JUSTIFICADA').length
  return {
    total,
    presentes,
    presentesPct: total ? Math.round((presentes / total) * 100) : 0,
    retardos,
    retardosPct: total ? Math.round((retardos / total) * 100) : 0,
    faltas,
    faltasPct: total ? Math.round((faltas / total) * 100) : 0
  }
})

const filteredEnrollmentsForAttendance = computed(() => {
  const q = attendanceStudentSearch.value.trim().toLowerCase()
  const list = data.inscripciones.filter(i => i.estado === 'ACTIVA' || !i.estado)
  if (!q) return list.slice(0, 10)
  return list.filter(i => {
    const al = data.alumnos.find(a => a.id === i.alumnoId)
    const per = al?.persona
    const fullName = `${al?.nombre || ''} ${al?.apellidoPaterno || ''} ${al?.apellidoMaterno || ''} ${per?.nombre || ''} ${i.alumno || ''}`.toLowerCase()
    const mat = (i.matricula || al?.matricula || '').toLowerCase()
    const grp = (i.grupo || '').toLowerCase()
    const tel = (al?.telefono || per?.telefono || '').replace(/\D/g, '')
    const em = (al?.correo || per?.correo || '').toLowerCase()
    return fullName.includes(q) || mat.includes(q) || grp.includes(q) || tel.includes(q) || em.includes(q)
  }).slice(0, 20)
})

function selectEnrollmentForAttendance(item) {
  selectedAttendanceEnrollment.value = item
  formAttendance.inscripcionId = item.id
  if (errors.value.inscripcionId) delete errors.value.inscripcionId
}

function clearSelectedEnrollmentForAttendance() {
  selectedAttendanceEnrollment.value = null
  formAttendance.inscripcionId = ''
  attendanceStudentSearch.value = ''
}

// Enhanced Payments (Pagos) State & Computeds
const paymentStudentSearch = ref('')
const selectedPaymentEnrollment = ref(null)
const paymentStatusFilter = ref('ALL') // 'ALL' | 'PAGADO' | 'PENDIENTE' | 'VENCIDO'
const paymentConceptFilter = ref('ALL') // 'ALL' | 'INSCRIPCION' | 'MENSUALIDAD' | 'RECARGO'
const paymentCourseFilter = ref('ALL')

const paymentsStats = computed(() => {
  const totalCount = data.pagos.length
  const paidList = data.pagos.filter(p => p.estado === 'PAGADO')
  const pendingList = data.pagos.filter(p => p.estado === 'PENDIENTE')
  const overdueList = data.pagos.filter(p => p.estado === 'VENCIDO')

  const totalPaidAmt = paidList.reduce((acc, p) => acc + (Number(p.monto) || (p.tipoPago === 'INSCRIPCION' ? 500 : 400)), 0)
  const totalPendingAmt = pendingList.reduce((acc, p) => acc + (Number(p.monto) || 400), 0)
  const totalOverdueAmt = overdueList.reduce((acc, p) => acc + (Number(p.monto) || 400), 0)

  return {
    totalCount,
    paidCount: paidList.length,
    totalPaidAmt,
    pendingCount: pendingList.length,
    totalPendingAmt,
    overdueCount: overdueList.length,
    totalOverdueAmt
  }
})

const filteredEnrollmentsForPayment = computed(() => {
  const q = paymentStudentSearch.value.trim().toLowerCase()
  const list = data.inscripciones.filter(i => i.estado === 'ACTIVA' || !i.estado)
  if (!q) return list.slice(0, 10)
  return list.filter(i => {
    const al = data.alumnos.find(a => a.id === i.alumnoId)
    const per = al?.persona
    const fullName = `${al?.nombre || ''} ${al?.apellidoPaterno || ''} ${al?.apellidoMaterno || ''} ${per?.nombre || ''} ${i.alumno || ''}`.toLowerCase()
    const mat = (i.matricula || al?.matricula || '').toLowerCase()
    const grp = (i.grupo || '').toLowerCase()
    const tel = (al?.telefono || per?.telefono || '').replace(/\D/g, '')
    const em = (al?.correo || per?.correo || '').toLowerCase()
    return fullName.includes(q) || mat.includes(q) || grp.includes(q) || tel.includes(q) || em.includes(q)
  }).slice(0, 20)
})

function selectEnrollmentForPayment(item) {
  selectedPaymentEnrollment.value = item
  formPayment.inscripcionId = item.id
  if (errors.value.inscripcionId) delete errors.value.inscripcionId
  if (formPayment.tipoPago === 'INSCRIPCION') formPayment.monto = 500
  else if (formPayment.tipoPago === 'MENSUALIDAD') formPayment.monto = 400
  else if (formPayment.tipoPago === 'RECARGO') formPayment.monto = 150
}

function clearSelectedEnrollmentForPayment() {
  selectedPaymentEnrollment.value = null
  formPayment.inscripcionId = ''
  paymentStudentSearch.value = ''
}

function onPaymentConceptChange() {
  if (formPayment.tipoPago === 'INSCRIPCION') formPayment.monto = 500
  else if (formPayment.tipoPago === 'MENSUALIDAD') formPayment.monto = 400
  else if (formPayment.tipoPago === 'RECARGO') formPayment.monto = 150
}

async function handleQuickMarkAsPaid(pagoId) {
  try {
    await updatePaymentStatus(pagoId, 'PAGADO')
    showToast('Pago registrado como PAGADO exitosamente.')
    await loadAllData()
  } catch (err) {
    showToast(err.message || 'Error al actualizar estado del pago', 'error')
  }
}
const formPassword = reactive({ passwordNueva: '' })
const formStudentUser = reactive({ password: '' })
const credentialInfo = ref(null)

// Option A: Specific Forms
const formFullWorkshop = reactive({
  nombreCurso: '',
  tipoOferta: `SEMESTRAL ${new Date().getFullYear()}-1`,
  fechaInicio: new Date().toISOString().slice(0, 10),
  fechaFin: new Date(new Date().setMonth(new Date().getMonth() + 5)).toISOString().slice(0, 10),
  crearGrupoInicial: true,
  nombreGrupo: 'Grupo A - Niños',
  categoriaId: '',
  docenteId: '',
  dia: 'MONDAY',
  horaInicio: '16:00',
  horaFin: '17:30'
})

const formDirectGroup = reactive({
  cursoId: '',
  ofertaId: '',
  nombreCurso: '',
  nombreGrupo: '',
  categoriaId: '',
  docenteId: '',
  agregarHorarioInicial: true,
  dia: 'MONDAY',
  horaInicio: '16:00',
  horaFin: '17:30'
})

const selectedTargetGroup = ref(null)
const formScheduleDirect = reactive({
  grupoId: '',
  nombreGrupo: '',
  dia: 'MONDAY',
  horaInicio: '16:00',
  horaFin: '17:30',
  slots: [
    { dias: ['MONDAY', 'WEDNESDAY', 'FRIDAY'], horaInicio: '16:00', horaFin: '17:30' }
  ]
})

const formTeacherDirect = reactive({
  grupoId: '',
  nombreGrupo: '',
  docenteId: '',
  fechaInicio: new Date().toISOString().slice(0, 10),
  fechaFin: ''
})

const formCategoryDirect = reactive({
  nombre: ''
})

const formEditCourse = reactive({
  id: null,
  nombre: ''
})

// Report View state
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

// ----------------------------------------------------

const todayDate = new Date().toISOString().slice(0, 10)

// Input sanitizers & strict character limiters
function filterNameInput(event, target, key) {
  const clean = event.target.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s.'-]/g, '')
  target[key] = clean
  if (errors.value[key]) delete errors.value[key]
}

function filterPhoneInput(event, target, key) {
  const clean = event.target.value.replace(/\D/g, '').slice(0, 10)
  target[key] = clean
  if (errors.value[key]) delete errors.value[key]
}

function filterMatriculaInput(event, target, key) {
  const clean = event.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 30)
  target[key] = clean
  if (errors.value[key]) delete errors.value[key]
}

function filterUsernameInput(event, target, key) {
  const clean = event.target.value.toLowerCase().replace(/[^a-z0-9._-]/g, '').slice(0, 80)
  target[key] = clean
  if (errors.value[key]) delete errors.value[key]
}

function filterRoleNameInput(event, target, key) {
  const clean = event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '').slice(0, 50)
  target[key] = clean
  if (errors.value[key]) delete errors.value[key]
}

function blockInvalidNumberKeys(event) {
  if (['e', 'E', '+', '-'].includes(event.key)) {
    event.preventDefault()
  }
}

// VALIDATION LOGIC MATCHING BACKEND CONSTRAINTS
// ----------------------------------------------------
function validateName(val) {
  if (!val || !val.trim()) return 'Este campo es obligatorio.'
  if (val.trim().length < 2) return 'Debe tener al menos 2 caracteres.'
  if (val.trim().length > 80) return 'No puede exceder los 80 caracteres.'
  if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s.'-]+$/.test(val.trim())) return 'Solo se permiten letras y espacios.'
  return null
}

function validateEmail(val, required = false) {
  if (!val || !val.trim()) {
    return required ? 'El correo es obligatorio.' : null
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val.trim())) return 'Ingresa un correo electrónico válido.'
  if (val.trim().length > 160) return 'El correo no puede exceder 160 caracteres.'
  return null
}

function validatePhone(val, required = false) {
  if (!val || !val.trim()) {
    return required ? 'El teléfono es obligatorio.' : null
  }
  const clean = val.replace(/[\s-]/g, '')
  if (!/^\d{10}$/.test(clean)) return 'El teléfono debe contener exactamente 10 dígitos numéricos.'
  return null
}

function validateMatricula(val) {
  if (!val || !val.trim()) return 'La matrícula es obligatoria.'
  if (val.trim().length < 3) return 'Mínimo 3 caracteres.'
  if (val.trim().length > 30) return 'Máximo 30 caracteres.'
  if (!/^[a-zA-Z0-9-_]+$/.test(val.trim())) return 'Solo letras, números y guiones.'
  return null
}

function validateUsername(val) {
  if (!val || !val.trim()) return 'El nombre de usuario es obligatorio.'
  if (val.trim().length < 3) return 'Debe tener al menos 3 caracteres.'
  if (val.trim().length > 80) return 'Máximo 80 caracteres.'
  if (!/^[a-zA-Z0-9._-]+$/.test(val.trim())) return 'Solo caracteres alfanuméricos, puntos o guiones.'
  return null
}

function validatePassword(val) {
  if (!val) return 'La contraseña es obligatoria.'
  if (val.length < 8) return 'La contraseña debe tener al menos 8 caracteres.'
  if (val.length > 100) return 'La contraseña no puede exceder 100 caracteres.'
  return null
}

function validateForm() {
  errors.value = {}

  if (modalMode.value === 'reset-pass') {
    const err = validatePassword(formPassword.passwordNueva)
    if (err) errors.value.passwordNueva = err
  } else if (modalMode.value === 'student-user') {
    const err = validatePassword(formStudentUser.password)
    if (err) errors.value.password = err
  } else if (modalMode.value === 'assign-teacher') {
    if (!formTeacherAssignment.docenteId) errors.value.docenteId = 'Selecciona un docente.'
    if (!formTeacherAssignment.grupoId) errors.value.grupoId = 'Selecciona un grupo.'
    if (!formTeacherAssignment.fechaInicio) errors.value.fechaInicio = 'La fecha de inicio es requerida.'
    if (formTeacherAssignment.fechaInicio && formTeacherAssignment.fechaFin && formTeacherAssignment.fechaFin < formTeacherAssignment.fechaInicio) {
      errors.value.fechaFin = 'La fecha de fin debe ser posterior a la de inicio.'
    }
  } else if (modalMode.value === 'create-full-workshop') {
    if (!formFullWorkshop.nombreCurso || !formFullWorkshop.nombreCurso.trim()) {
      errors.value.nombreCurso = 'El nombre del taller o curso es obligatorio.'
    }
    if (!formFullWorkshop.tipoOferta?.trim()) {
      errors.value.tipoOferta = 'El tipo de ciclo u oferta es obligatorio.'
    }
    if (!formFullWorkshop.fechaInicio) errors.value.fechaInicio = 'Fecha de inicio obligatoria.'
    if (!formFullWorkshop.fechaFin) errors.value.fechaFin = 'Fecha de fin obligatoria.'
    if (formFullWorkshop.fechaInicio && formFullWorkshop.fechaFin && formFullWorkshop.fechaFin < formFullWorkshop.fechaInicio) {
      errors.value.fechaFin = 'La fecha de fin debe ser posterior a la de inicio.'
    }
    if (formFullWorkshop.crearGrupoInicial) {
      if (!formFullWorkshop.nombreGrupo?.trim()) errors.value.nombreGrupo = 'El nombre del grupo es obligatorio.'
      if (!formFullWorkshop.categoriaId) errors.value.categoriaId = 'Selecciona una categoría de edad.'
      if (formFullWorkshop.horaInicio && formFullWorkshop.horaFin && formFullWorkshop.horaFin <= formFullWorkshop.horaInicio) {
        errors.value.horaFin = 'La hora de fin debe ser posterior a la de inicio.'
      }
    }
  } else if (modalMode.value === 'create-direct-group') {
    if (!formDirectGroup.ofertaId) errors.value.ofertaId = 'Selecciona o define una oferta/ciclo.'
    if (!formDirectGroup.nombreGrupo?.trim()) errors.value.nombreGrupo = 'El nombre del grupo es obligatorio.'
    if (!formDirectGroup.categoriaId) errors.value.categoriaId = 'Selecciona una categoría de edad.'
    if (formDirectGroup.agregarHorarioInicial) {
      if (formDirectGroup.horaInicio && formDirectGroup.horaFin && formDirectGroup.horaFin <= formDirectGroup.horaInicio) {
        errors.value.horaFin = 'La hora de fin debe ser posterior a la de inicio.'
      }
    }
  } else if (modalMode.value === 'add-schedule-group') {
    if (!formScheduleDirect.grupoId) errors.value.grupoId = 'El grupo es obligatorio.'
    if (!formScheduleDirect.slots || !formScheduleDirect.slots.length) {
      errors.value.slots = 'Agrega al menos un bloque de horario.'
    } else {
      formScheduleDirect.slots.forEach((s, idx) => {
        if (!s.dias || !s.dias.length) errors.value[`slot_${idx}_dias`] = 'Selecciona al menos un día.'
        if (!s.horaInicio) errors.value[`slot_${idx}_inicio`] = 'Hora de inicio requerida.'
        if (!s.horaFin) errors.value[`slot_${idx}_fin`] = 'Hora de fin requerida.'
        if (s.horaInicio && s.horaFin && s.horaFin <= s.horaInicio) {
          errors.value[`slot_${idx}_fin`] = 'La hora de fin debe ser posterior a la de inicio.'
        }
      })
    }
  } else if (modalMode.value === 'assign-teacher-group') {
    if (!formTeacherDirect.grupoId) errors.value.grupoId = 'El grupo es obligatorio.'
    if (!formTeacherDirect.docenteId) errors.value.docenteId = 'Selecciona un instructor/docente.'
    if (!formTeacherDirect.fechaInicio) errors.value.fechaInicio = 'La fecha de inicio es requerida.'
    if (formTeacherDirect.fechaInicio && formTeacherDirect.fechaFin && formTeacherDirect.fechaFin < formTeacherDirect.fechaInicio) {
      errors.value.fechaFin = 'La fecha de fin debe ser posterior a la de inicio.'
    }
  } else if (modalMode.value === 'create-age-category') {
    if (!formCategoryDirect.nombre?.trim()) errors.value.nombre = 'El nombre de la categoría es obligatorio.'
  } else if (modalMode.value === 'edit-course') {
    if (!formEditCourse.nombre?.trim()) errors.value.nombre = 'El nombre del curso es obligatorio.'
  } else if (moduleType.value === 'users') {
    if (userCreationType.value === 'existing') {
      if (!formUser.personaId) errors.value.personaId = 'Debes buscar y seleccionar a una persona de la lista.'
    } else {
      if (!formAdminPerson.nombre?.trim()) errors.value.adminNombre = 'El nombre es obligatorio.'
      if (!formAdminPerson.apellidoPaterno?.trim()) errors.value.adminApellidoPaterno = 'El apellido paterno es obligatorio.'
    }
    if (!formUser.rolId) errors.value.rolId = 'Selecciona un rol de acceso.'
    const uErr = validateUsername(formUser.nombreUsuario)
    if (uErr) errors.value.nombreUsuario = uErr
    const pErr = validatePassword(formUser.password)
    if (pErr) errors.value.password = pErr
  } else if (moduleType.value === 'roles') {
    if (!formRole.nombre || !formRole.nombre.trim()) {
      errors.value.nombre = 'El nombre del rol es obligatorio.'
    } else if (formRole.nombre.trim().length > 50) {
      errors.value.nombre = 'Máximo 50 caracteres.'
    }
  } else if (moduleType.value === 'students') {
    const nErr = validateName(formStudent.nombre)
    if (nErr) errors.value.nombre = nErr
    const apErr = validateName(formStudent.apellidoPaterno)
    if (apErr) errors.value.apellidoPaterno = apErr
    if (formStudent.apellidoMaterno?.trim()) {
      const amErr = validateName(formStudent.apellidoMaterno)
      if (amErr) errors.value.apellidoMaterno = amErr
    }
    const mErr = validateMatricula(formStudent.matricula)
    if (mErr) errors.value.matricula = mErr
    const emErr = validateEmail(formStudent.correo, true)
    if (emErr) errors.value.correo = emErr
    const phErr = validatePhone(formStudent.telefono, true)
    if (phErr) errors.value.telefono = phErr
    if (!formStudent.fechaNacimiento) {
      errors.value.fechaNacimiento = 'La fecha de nacimiento es obligatoria.'
    } else {
      const d = new Date(formStudent.fechaNacimiento)
      if (d > new Date()) errors.value.fechaNacimiento = 'La fecha de nacimiento no puede ser futura.'
    }
    if (!formStudent.direccion || !formStudent.direccion.trim()) {
      errors.value.direccion = 'La dirección es obligatoria.'
    } else if (formStudent.direccion.trim().length < 5) {
      errors.value.direccion = 'Ingresa una dirección válida (mínimo 5 caracteres).'
    }
  } else if (moduleType.value === 'teachers') {
    const nErr = validateName(formTeacher.nombre)
    if (nErr) errors.value.nombre = nErr
    const apErr = validateName(formTeacher.apellidoPaterno)
    if (apErr) errors.value.apellidoPaterno = apErr
    if (formTeacher.apellidoMaterno?.trim()) {
      const amErr = validateName(formTeacher.apellidoMaterno)
      if (amErr) errors.value.apellidoMaterno = amErr
    }
    if (!formTeacher.especialidad || !formTeacher.especialidad.trim()) {
      errors.value.especialidad = 'La especialidad cultural es obligatoria.'
    }
    const emErr = validateEmail(formTeacher.correo, true)
    if (emErr) errors.value.correo = emErr
    const phErr = validatePhone(formTeacher.telefono, true)
    if (phErr) errors.value.telefono = phErr
    if (!formTeacher.fechaNacimiento) {
      errors.value.fechaNacimiento = 'La fecha de nacimiento es obligatoria.'
    } else {
      const d = new Date(formTeacher.fechaNacimiento)
      if (d > new Date()) errors.value.fechaNacimiento = 'La fecha de nacimiento no puede ser futura.'
    }
    if (!formTeacher.direccion || !formTeacher.direccion.trim()) {
      errors.value.direccion = 'La dirección es obligatoria.'
    } else if (formTeacher.direccion.trim().length < 5) {
      errors.value.direccion = 'Ingresa una dirección válida (mínimo 5 caracteres).'
    }
  } else if (moduleType.value === 'schedules') {
    if (!formSchedule.grupoId) errors.value.grupoId = 'Selecciona un grupo.'
    if (!formSchedule.slots || !formSchedule.slots.length) {
      errors.value.slots = 'Agrega al menos un bloque de horario.'
    } else {
      formSchedule.slots.forEach((s, idx) => {
        if (!s.dias || !s.dias.length) errors.value[`slot_${idx}_dias`] = 'Selecciona al menos un día.'
        if (!s.horaInicio) errors.value[`slot_${idx}_inicio`] = 'Hora de inicio requerida.'
        if (!s.horaFin) errors.value[`slot_${idx}_fin`] = 'Hora de fin requerida.'
        if (s.horaInicio && s.horaFin && s.horaFin <= s.horaInicio) {
          errors.value[`slot_${idx}_fin`] = 'La hora de fin debe ser posterior a la de inicio.'
        }
      })
    }
  } else if (moduleType.value === 'registrations') {
    if (!formEnrollment.alumnoId) errors.value.alumnoId = 'Debes buscar y seleccionar un alumno de la lista.'
    if (!formEnrollment.grupoId) errors.value.grupoId = 'Selecciona el grupo o taller al que se inscribirá.'
  } else if (moduleType.value === 'attendance') {
    if (!formAttendance.inscripcionId) errors.value.inscripcionId = 'Debes buscar y seleccionar un alumno inscrito de la lista.'
    if (!formAttendance.fecha) errors.value.fecha = 'La fecha es obligatoria.'
  } else if (moduleType.value === 'payments') {
    if (!formPayment.inscripcionId) errors.value.inscripcionId = 'Debes buscar y seleccionar un alumno inscrito de la lista.'
    if (!formPayment.tipoPago) errors.value.tipoPago = 'Selecciona un tipo de pago.'
    if (!formPayment.periodo?.trim()) errors.value.periodo = 'El periodo es obligatorio.'
    if (!formPayment.monto || Number(formPayment.monto) <= 0) errors.value.monto = 'Ingresa un monto mayor a 0.'
  }

  return Object.keys(errors.value).length === 0
}

// Open Modal logic
function openCreateModal() {
  editingId.value = null
  errors.value = {}
  personSearch.value = ''
  personTypeFilter.value = 'ALL'
  selectedPerson.value = null

  if (moduleType.value === 'workshops') {
    modalMode.value = 'create-full-workshop'
    Object.assign(formFullWorkshop, {
      nombreCurso: '',
      tipoOferta: `SEMESTRAL ${new Date().getFullYear()}-1`,
      fechaInicio: new Date().toISOString().slice(0, 10),
      fechaFin: new Date(new Date().setMonth(new Date().getMonth() + 5)).toISOString().slice(0, 10),
      crearGrupoInicial: true,
      nombreGrupo: 'Grupo A - Niños',
      categoriaId: data.categorias[0]?.id || '',
      docenteId: data.docentes[0]?.id || '',
      dia: 'MONDAY',
      horaInicio: '16:00',
      horaFin: '17:30'
    })
  } else if (moduleType.value === 'users') {
    modalMode.value = 'create'
    userCreationType.value = 'admin'
    selectedPerson.value = null
    personSearch.value = ''
    Object.assign(formUser, { personaId: '', rolId: '', nombreUsuario: '', password: '' })
    Object.assign(formAdminPerson, { nombre: '', apellidoPaterno: '', apellidoMaterno: '', correo: '', telefono: '' })
    const supRole = data.roles.find(r => r.nombre.toUpperCase().includes('SUPERVISOR'))
    if (supRole) formUser.rolId = supRole.id
  } else if (moduleType.value === 'roles') {
    modalMode.value = 'create'
    Object.assign(formRole, { nombre: '', descripcion: '' })
  } else if (moduleType.value === 'students') {
    modalMode.value = 'create'
    const nextNum = String(data.alumnos.length + 1).padStart(3, '0')
    const currentYr = new Date().getFullYear()
    Object.assign(formStudent, {
      nombre: '', apellidoPaterno: '', apellidoMaterno: '', matricula: `ALU-${currentYr}-${nextNum}`,
      fechaNacimiento: '', telefono: '', direccion: '', correo: '', fotoUrl: ''
    })
  } else if (moduleType.value === 'teachers') {
    modalMode.value = 'create'
    Object.assign(formTeacher, {
      nombre: '', apellidoPaterno: '', apellidoMaterno: '', especialidad: '',
      fechaNacimiento: '', telefono: '', direccion: '', correo: ''
    })
  } else if (moduleType.value === 'schedules') {
    modalMode.value = 'create'
    Object.assign(formSchedule, {
      grupoId: data.grupos[0]?.id || '',
      dia: 'MONDAY',
      horaInicio: '16:00',
      horaFin: '18:00',
      slots: [
        { dias: ['MONDAY', 'WEDNESDAY', 'FRIDAY'], horaInicio: '16:00', horaFin: '18:00' }
      ]
    })
  } else if (moduleType.value === 'registrations') {
    modalMode.value = 'create'
    selectedEnrollmentStudent.value = null
    enrollmentStudentSearch.value = ''
    Object.assign(formEnrollment, {
      alumnoId: '',
      grupoId: data.grupos[0]?.id || ''
    })
  } else if (moduleType.value === 'attendance') {
    modalMode.value = 'create'
    selectedAttendanceEnrollment.value = null
    attendanceStudentSearch.value = ''
    Object.assign(formAttendance, {
      inscripcionId: '', horarioId: '',
      fecha: new Date().toISOString().slice(0, 10), estado: 'PRESENTE'
    })
  } else if (moduleType.value === 'payments') {
    modalMode.value = 'create'
    selectedPaymentEnrollment.value = null
    paymentStudentSearch.value = ''
    Object.assign(formPayment, {
      inscripcionId: '', tipoPago: 'INSCRIPCION',
      periodo: `${new Date().getFullYear()}-1`, fechaVencimiento: '', fechaPago: new Date().toISOString().slice(0, 10),
      estado: 'PAGADO', monto: 500
    })
  }
  showModal.value = true
}

// Option A: Quick Workshop Actions
function openAddGroupToCourseModal(course) {
  modalMode.value = 'create-direct-group'
  errors.value = {}

  let ofertaId = course.ofertaActual?.id || ''
  if (!ofertaId && course.ofertas?.length > 0) {
    ofertaId = course.ofertas[0].id
  }

  Object.assign(formDirectGroup, {
    cursoId: course.id,
    ofertaId: ofertaId,
    nombreCurso: course.nombre,
    nombreGrupo: `Grupo ${String.fromCharCode(65 + course.grupos.length)}`,
    categoriaId: data.categorias[0]?.id || '',
    docenteId: data.docentes[0]?.id || '',
    agregarHorarioInicial: true,
    dia: 'MONDAY',
    horaInicio: '16:00',
    horaFin: '17:30'
  })
  showModal.value = true
}

function openAddScheduleToGroupModal(group, courseName) {
  modalMode.value = 'add-schedule-group'
  selectedTargetGroup.value = group
  errors.value = {}
  Object.assign(formScheduleDirect, {
    grupoId: group.id,
    nombreGrupo: `${courseName ? courseName + ' - ' : ''}${group.nombreGrupo}`,
    dia: 'MONDAY',
    horaInicio: '16:00',
    horaFin: '17:30',
    slots: [
      { dias: ['MONDAY', 'WEDNESDAY', 'FRIDAY'], horaInicio: '16:00', horaFin: '17:30' }
    ]
  })
  showModal.value = true
}

function openAssignTeacherToGroupModal(group, courseName) {
  modalMode.value = 'assign-teacher-group'
  selectedTargetGroup.value = group
  errors.value = {}
  Object.assign(formTeacherDirect, {
    grupoId: group.id,
    nombreGrupo: `${courseName ? courseName + ' - ' : ''}${group.nombreGrupo}`,
    docenteId: group.docenteAsignado?.id || data.docentes[0]?.id || '',
    fechaInicio: new Date().toISOString().slice(0, 10),
    fechaFin: ''
  })
  showModal.value = true
}

function openCreateCategoryModal() {
  modalMode.value = 'create-age-category'
  errors.value = {}
  formCategoryDirect.nombre = ''
  showModal.value = true
}

function openEditCourseModal(course) {
  modalMode.value = 'edit-course'
  errors.value = {}
  formEditCourse.id = course.id
  formEditCourse.nombre = course.nombre
  showModal.value = true
}

function openAssignTeacherModal(teacher) {
  modalMode.value = 'assign-teacher'
  editingId.value = teacher.id
  errors.value = {}
  Object.assign(formTeacherAssignment, {
    docenteId: teacher.id,
    grupoId: data.grupos[0]?.id || '',
    fechaInicio: new Date().toISOString().slice(0, 10),
    fechaFin: ''
  })
  showModal.value = true
  activeActionMenu.value = null
}

function openEditModal(item) {
  modalMode.value = 'edit'
  editingId.value = item.id
  errors.value = {}

  if (moduleType.value === 'roles') {
    Object.assign(formRole, { nombre: item.nombre, descripcion: item.descripcion || '' })
  } else if (moduleType.value === 'students') {
    Object.assign(formStudent, {
      nombre: item.nombre, apellidoPaterno: item.apellidoPaterno,
      apellidoMaterno: item.apellidoMaterno || '', matricula: item.matricula,
      fechaNacimiento: item.fechaNacimiento || '', telefono: item.telefono || '',
      direccion: item.direccion || '', correo: item.correo || '', fotoUrl: item.fotoUrl || ''
    })
  } else if (moduleType.value === 'teachers') {
    Object.assign(formTeacher, {
      nombre: item.nombre, apellidoPaterno: item.apellidoPaterno,
      apellidoMaterno: item.apellidoMaterno || '', especialidad: item.especialidad || '',
      telefono: item.telefono || '', direccion: item.direccion || '', correo: item.correo || ''
    })
  }
  showModal.value = true
  activeActionMenu.value = null
}

function openPasswordModal(item) {
  modalMode.value = 'reset-pass'
  editingId.value = item.id
  errors.value = {}
  formPassword.passwordNueva = ''
  showModal.value = true
  activeActionMenu.value = null
}

function openStudentUserModal(item) {
  modalMode.value = 'student-user'
  editingId.value = item.id
  errors.value = {}
  formStudentUser.password = 'Alumno123!'
  showModal.value = true
  activeActionMenu.value = null
}

async function viewStudentCredential(item) {
  try {
    const cred = await generateStudentCredential(item.id)
    credentialInfo.value = cred
    modalMode.value = 'credential'
    editingId.value = item.id
    showModal.value = true
  } catch (err) {
    showToast(err.message || 'Error al obtener credencial', 'error')
  }
  activeActionMenu.value = null
}

function printCredential() {
  window.print()
}

async function handleGenerateAbsences() {
  try {
    const count = await generateAbsences()
    showToast(`Se generaron ${count} faltas para el día de hoy.`)
    await loadAllData()
  } catch (err) {
    showToast(err.message || 'Error generando faltas', 'error')
  }
}

// Submit Form Handler
async function handleModalSubmit() {
  if (!validateForm()) {
    showToast('Por favor corrige los campos marcados en rojo.', 'error')
    return
  }

  isSaving.value = true
  try {
    if (modalMode.value === 'create-full-workshop') {
      // 1. Create Course
      const createdCourse = await createCourse({ nombre: formFullWorkshop.nombreCurso.trim() })
      const courseId = createdCourse.id || createdCourse.data?.id

      // 2. Create Offer for this Course
      const createdOffer = await createOffer({
        cursoId: Number(courseId),
        tipo: formFullWorkshop.tipoOferta.trim(),
        fechaInicio: formFullWorkshop.fechaInicio,
        fechaFin: formFullWorkshop.fechaFin
      })
      const offerId = createdOffer.id || createdOffer.data?.id

      // 3. (Optional) Create Initial Group with Category, Teacher and Schedule
      if (formFullWorkshop.crearGrupoInicial) {
        const createdGroup = await createGroup({
          ofertaId: Number(offerId),
          categoriaId: Number(formFullWorkshop.categoriaId),
          nombreGrupo: formFullWorkshop.nombreGrupo.trim()
        })
        const groupId = createdGroup.id || createdGroup.data?.id

        if (formFullWorkshop.docenteId) {
          await assignTeacherToGroup({
            docenteId: Number(formFullWorkshop.docenteId),
            grupoId: Number(groupId),
            fechaInicio: formFullWorkshop.fechaInicio,
            fechaFin: formFullWorkshop.fechaFin || null
          })
        }

        if (formFullWorkshop.dia && formFullWorkshop.horaInicio && formFullWorkshop.horaFin) {
          await createSchedule({
            grupoId: Number(groupId),
            dia: formFullWorkshop.dia,
            horaInicio: formFullWorkshop.horaInicio.length === 5 ? `${formFullWorkshop.horaInicio}:00` : formFullWorkshop.horaInicio,
            horaFin: formFullWorkshop.horaFin.length === 5 ? `${formFullWorkshop.horaFin}:00` : formFullWorkshop.horaFin
          })
        }
      }
      showToast('¡Taller cultural y grupo estructurados exitosamente!')
    } else if (modalMode.value === 'create-direct-group') {
      let ofertaId = formDirectGroup.ofertaId
      if (!ofertaId) {
        // Create an offer on the fly if needed
        const newOff = await createOffer({
          cursoId: Number(formDirectGroup.cursoId),
          tipo: `SEMESTRAL ${new Date().getFullYear()}-1`,
          fechaInicio: new Date().toISOString().slice(0, 10),
          fechaFin: new Date(new Date().setMonth(new Date().getMonth() + 5)).toISOString().slice(0, 10)
        })
        ofertaId = newOff.id || newOff.data?.id
      }

      const createdGroup = await createGroup({
        ofertaId: Number(ofertaId),
        categoriaId: Number(formDirectGroup.categoriaId),
        nombreGrupo: formDirectGroup.nombreGrupo.trim()
      })
      const groupId = createdGroup.id || createdGroup.data?.id

      if (formDirectGroup.docenteId) {
        await assignTeacherToGroup({
          docenteId: Number(formDirectGroup.docenteId),
          grupoId: Number(groupId),
          fechaInicio: new Date().toISOString().slice(0, 10),
          fechaFin: null
        })
      }

      if (formDirectGroup.agregarHorarioInicial && formDirectGroup.dia && formDirectGroup.horaInicio && formDirectGroup.horaFin) {
        await createSchedule({
          grupoId: Number(groupId),
          dia: formDirectGroup.dia,
          horaInicio: formDirectGroup.horaInicio.length === 5 ? `${formDirectGroup.horaInicio}:00` : formDirectGroup.horaInicio,
          horaFin: formDirectGroup.horaFin.length === 5 ? `${formDirectGroup.horaFin}:00` : formDirectGroup.horaFin
        })
      }
      showToast('¡Nuevo grupo agregado con éxito al taller!')
    } else if (modalMode.value === 'add-schedule-group') {
      const slotsPayload = (formScheduleDirect.slots || []).map(s => ({
        dias: s.dias,
        horaInicio: s.horaInicio.length === 5 ? `${s.horaInicio}:00` : s.horaInicio,
        horaFin: s.horaFin.length === 5 ? `${s.horaFin}:00` : s.horaFin
      }))
      const created = await createSchedulesBatch({
        grupoId: Number(formScheduleDirect.grupoId),
        slots: slotsPayload
      })
      const count = Array.isArray(created) ? created.length : 1
      showToast(`¡Se asignaron exitosamente ${count} horario(s) al grupo!`)
    } else if (modalMode.value === 'assign-teacher-group') {
      await assignTeacherToGroup({
        docenteId: Number(formTeacherDirect.docenteId),
        grupoId: Number(formTeacherDirect.grupoId),
        fechaInicio: formTeacherDirect.fechaInicio,
        fechaFin: formTeacherDirect.fechaFin || null
      })
      showToast('Docente instructor asignado al grupo.')
    } else if (modalMode.value === 'create-age-category') {
      await createAgeCategory({ nombre: formCategoryDirect.nombre.trim() })
      showToast('Nueva categoría de edad registrada.')
    } else if (modalMode.value === 'edit-course') {
      await updateCourse(formEditCourse.id, { nombre: formEditCourse.nombre.trim() })
      showToast('Nombre del taller actualizado.')
    } else if (modalMode.value === 'reset-pass') {
      await resetUserPassword(editingId.value, formPassword.passwordNueva.trim())
      showToast('Contraseña restablecida correctamente.')
    } else if (modalMode.value === 'student-user') {
      await generateStudentUser(editingId.value, { password: formStudentUser.password.trim() })
      showToast('Acceso y usuario creados para el alumno.')
    } else if (modalMode.value === 'assign-teacher') {
      await assignTeacherToGroup({
        docenteId: Number(formTeacherAssignment.docenteId),
        grupoId: Number(formTeacherAssignment.grupoId),
        fechaInicio: formTeacherAssignment.fechaInicio,
        fechaFin: formTeacherAssignment.fechaFin || null
      })
      showToast('Docente asignado al grupo exitosamente.')
    } else if (moduleType.value === 'users') {
      if (userCreationType.value === 'admin') {
        await createUser({
          nombre: formAdminPerson.nombre.trim(),
          apellidoPaterno: formAdminPerson.apellidoPaterno.trim(),
          apellidoMaterno: formAdminPerson.apellidoMaterno?.trim() || null,
          correo: formAdminPerson.correo?.trim() || null,
          telefono: formAdminPerson.telefono ? formAdminPerson.telefono.replace(/[\s-]/g, '') : null,
          rolId: Number(formUser.rolId),
          nombreUsuario: formUser.nombreUsuario.trim(),
          password: formUser.password
        })
      } else {
        await createUser({
          personaId: Number(formUser.personaId),
          rolId: Number(formUser.rolId),
          nombreUsuario: formUser.nombreUsuario.trim(),
          password: formUser.password
        })
      }
      showToast('Usuario creado exitosamente.')
    } else if (moduleType.value === 'roles') {
      const payload = {
        nombre: formRole.nombre.trim().toUpperCase(),
        descripcion: formRole.descripcion?.trim() || ''
      }
      if (modalMode.value === 'edit') {
        await updateRole(editingId.value, payload)
        showToast('Rol actualizado correctamente.')
      } else {
        await createRole(payload)
        showToast('Rol creado exitosamente.')
      }
    } else if (moduleType.value === 'students') {
      const payload = {
        nombre: formStudent.nombre.trim(),
        apellidoPaterno: formStudent.apellidoPaterno.trim(),
        apellidoMaterno: formStudent.apellidoMaterno?.trim() || null,
        matricula: formStudent.matricula.trim(),
        fechaNacimiento: formStudent.fechaNacimiento || null,
        telefono: formStudent.telefono ? formStudent.telefono.replace(/[\s-]/g, '') : null,
        correo: formStudent.correo?.trim() || null,
        direccion: formStudent.direccion?.trim() || null,
        fotoUrl: formStudent.fotoUrl?.trim() || null
      }
      if (modalMode.value === 'edit') {
        await updateStudent(editingId.value, payload)
        showToast('Alumno actualizado correctamente.')
      } else {
        await createStudent(payload)
        showToast('Alumno registrado exitosamente.')
      }
    } else if (moduleType.value === 'teachers') {
      const payload = {
        nombre: formTeacher.nombre.trim(),
        apellidoPaterno: formTeacher.apellidoPaterno.trim(),
        apellidoMaterno: formTeacher.apellidoMaterno?.trim() || null,
        especialidad: formTeacher.especialidad?.trim() || null,
        fechaNacimiento: formTeacher.fechaNacimiento || null,
        telefono: formTeacher.telefono ? formTeacher.telefono.replace(/[\s-]/g, '') : null,
        correo: formTeacher.correo?.trim() || null,
        direccion: formTeacher.direccion?.trim() || null
      }
      if (modalMode.value === 'edit') {
        await updateTeacher(editingId.value, payload)
        showToast('Docente actualizado correctamente.')
      } else {
        await createTeacher(payload)
        showToast('Docente registrado exitosamente.')
      }
    } else if (moduleType.value === 'schedules') {
      const slotsPayload = (formSchedule.slots || []).map(s => ({
        dias: s.dias,
        horaInicio: s.horaInicio.length === 5 ? `${s.horaInicio}:00` : s.horaInicio,
        horaFin: s.horaFin.length === 5 ? `${s.horaFin}:00` : s.horaFin
      }))
      const created = await createSchedulesBatch({
        grupoId: Number(formSchedule.grupoId),
        slots: slotsPayload
      })
      const count = Array.isArray(created) ? created.length : 1
      showToast(`¡Se registraron exitosamente ${count} horario(s) para el grupo!`)
    } else if (moduleType.value === 'registrations') {
      await createEnrollment({
        alumnoId: Number(formEnrollment.alumnoId),
        grupoId: Number(formEnrollment.grupoId)
      })
      showToast('Inscripción registrada correctamente.')
    } else if (moduleType.value === 'attendance') {
      await registerAttendance({
        inscripcionId: Number(formAttendance.inscripcionId),
        horarioId: formAttendance.horarioId ? Number(formAttendance.horarioId) : null,
        fecha: formAttendance.fecha,
        estado: formAttendance.estado
      })
      showToast('Asistencia guardada exitosamente.')
    } else if (moduleType.value === 'payments') {
      await createPayment({
        inscripcionId: Number(formPayment.inscripcionId),
        tipoPago: formPayment.tipoPago,
        periodo: formPayment.periodo.trim(),
        fechaVencimiento: formPayment.fechaVencimiento || null,
        fechaPago: formPayment.fechaPago || null,
        estado: formPayment.estado,
        monto: Number(formPayment.monto)
      })
      showToast('Pago registrado correctamente.')
    }

    showModal.value = false
    await loadAllData()
  } catch (err) {
    showToast(err.message || 'Ocurrió un error al guardar.', 'error')
  } finally {
    isSaving.value = false
  }
}

// Delete / Deactivate Handlers
async function handleDeactivate(type, id) {
  if (!confirm('¿Estás seguro de realizar esta acción?')) return
  try {
    if (type === 'user') await deactivateUser(id)
    if (type === 'role') await deleteRole(id)
    if (type === 'student') await deactivateStudent(id)
    if (type === 'teacher') await deactivateTeacher(id)
    if (type === 'registration') await deactivateEnrollment(id)
    showToast('Registro actualizado/desactivado.')
    await loadAllData()
  } catch (err) {
    showToast(err.message || 'Error al desactivar', 'error')
  }
  activeActionMenu.value = null
}

// Export CSV handler
function exportToCsv() {
  const items = currentTableRows.value
  if (!items || items.length === 0) {
    showToast('No hay datos disponibles para exportar', 'error')
    return
  }

  const keys = Object.keys(items[0]).filter(k => k !== 'raw')
  const csvContent = [
    keys.join(','),
    ...items.map(row => keys.map(k => `"${String(row[k] || '').replace(/"/g, '""')}"`).join(','))
  ].join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.setAttribute('href', url)
  link.setAttribute('download', `${moduleType.value}_${new Date().toISOString().slice(0, 10)}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  showToast('Archivo CSV exportado exitosamente.')
}

// Table columns and processed data
const columns = computed(() => {
  switch (moduleType.value) {
    case 'users':
      return ['Usuario', 'Persona Asociada', 'Rol Asignado', 'Estado']
    case 'roles':
      return ['Rol', 'Descripción', 'Permisos', 'Estado']
    case 'students':
      return ['Alumno', 'Matrícula', 'Contacto', 'Estado']
    case 'teachers':
      return ['Docente', 'Especialidad', 'Contacto', 'Estado']
    case 'schedules':
      return ['Grupo', 'Día', 'Horario', 'Estado']
    case 'registrations':
      return ['Alumno', 'Grupo Asignado', 'Fecha Inscripción', 'Estado']
    case 'attendance':
      return ['Alumno', 'Taller / Grupo', 'Fecha y Hora', 'Estado']
    case 'payments':
      return ['Alumno', 'Concepto y Monto', 'Taller / Periodo', 'Estado']
    default:
      return ['Nombre', 'Detalle', 'Información', 'Estado']
  }
})

const currentTableRows = computed(() => {
  const q = search.value.trim().toLowerCase()

  if (moduleType.value === 'users') {
    const list = data.usuarios.map(u => ({
      id: u.id,
      nombre: u.nombreUsuario,
      subtexto: u.nombreRol || u.rol || 'SUPER_ADMIN',
      correo: u.persona ? `${u.persona.nombre} ${u.persona.apellidoPaterno || ''}` : (u.correo || 'Usuario del Sistema'),
      rol: u.rol || u.nombreRol || 'SUPER_ADMIN',
      estado: u.estado || 'ACTIVO',
      raw: u
    }))
    return q ? list.filter(x => x.nombre.toLowerCase().includes(q) || x.correo.toLowerCase().includes(q) || x.rol.toLowerCase().includes(q)) : list
  }

  if (moduleType.value === 'roles') {
    const list = data.roles.map(r => ({
      id: r.id,
      nombre: r.nombre,
      subtexto: r.descripcion || 'Acceso general',
      correo: r.descripcion || 'Sin descripción detallada',
      rol: 'Permisos del Sistema',
      estado: 'ACTIVO',
      raw: r
    }))
    return q ? list.filter(x => x.nombre.toLowerCase().includes(q) || x.correo.toLowerCase().includes(q)) : list
  }

  if (moduleType.value === 'students') {
    const list = data.alumnos.map(a => ({
      id: a.id,
      nombre: `${a.nombre} ${a.apellidoPaterno} ${a.apellidoMaterno || ''}`.trim(),
      subtexto: `Mat: ${a.matricula}`,
      correo: a.correo || a.telefono || 'Sin contacto directo',
      rol: a.matricula,
      estado: a.estado || 'ACTIVO',
      raw: a
    }))
    return q ? list.filter(x => x.nombre.toLowerCase().includes(q) || x.rol.toLowerCase().includes(q) || x.correo.toLowerCase().includes(q)) : list
  }

  if (moduleType.value === 'teachers') {
    const list = data.docentes.map(d => ({
      id: d.id,
      nombre: `${d.nombre} ${d.apellidoPaterno} ${d.apellidoMaterno || ''}`.trim(),
      subtexto: d.especialidad || 'Docente Cultural',
      correo: d.especialidad || 'General',
      rol: d.correo || d.telefono || 'Sin correo',
      estado: d.estado || 'ACTIVO',
      raw: d
    }))
    return q ? list.filter(x => x.nombre.toLowerCase().includes(q) || x.correo.toLowerCase().includes(q) || x.rol.toLowerCase().includes(q)) : list
  }

  if (moduleType.value === 'schedules') {
    const list = (data.horarios || []).map(h => {
      const g = data.grupos.find(grp => grp.id === h.grupoId)
      const oferta = g ? data.ofertas.find(o => o.id === g.ofertaId) : null
      const curso = oferta ? data.cursos.find(c => c.id === oferta.cursoId) : null
      const diaTexto = formatDay(h.dia)
      const horaTexto = `${formatTime(h.horaInicio)} - ${formatTime(h.horaFin)}`
      return {
        id: h.id,
        nombre: g ? g.nombreGrupo : (h.nombreGrupo || 'Grupo'),
        subtexto: curso ? curso.nombre : (h.nombre || 'Taller'),
        correo: diaTexto,
        rol: horaTexto,
        estado: 'PROGRAMADO',
        raw: h
      }
    })
    return q ? list.filter(x => x.nombre.toLowerCase().includes(q) || x.subtexto.toLowerCase().includes(q) || x.correo.toLowerCase().includes(q)) : list
  }

  if (moduleType.value === 'registrations') {
    let list = data.inscripciones.map(ins => {
      const alumno = data.alumnos.find(a => a.id === ins.alumnoId)
      const grupo = data.grupos.find(g => g.id === ins.grupoId)
      const oferta = grupo ? data.ofertas.find(o => o.id === grupo.ofertaId) : null
      const curso = oferta ? data.cursos.find(c => c.id === oferta.cursoId) : null
      const fullName = alumno
        ? `${alumno.nombre} ${alumno.apellidoPaterno || ''} ${alumno.apellidoMaterno || ''}`.trim()
        : (ins.matricula || ins.alumno || `Alumno #${ins.alumnoId}`)
      const matricula = alumno?.matricula || ins.matricula || ''
      const tallerNombre = curso?.nombre || (grupo ? grupo.nombreGrupo : (ins.grupo || 'Taller Cultural'))
      const grupoNombre = grupo?.nombreGrupo || 'Grupo'
      return {
        id: ins.id,
        nombre: fullName,
        subtexto: matricula,
        correo: grupo ? `${grupoNombre} · ${tallerNombre}` : tallerNombre,
        rol: ins.fechaInscripcion ? String(ins.fechaInscripcion).slice(0, 10) : '2026-05-20',
        estado: ins.estado || 'ACTIVA',
        cursoId: curso?.id || null,
        alumno,
        grupo,
        raw: ins
      }
    })

    if (registrationStatusFilter.value !== 'ALL') {
      list = list.filter(item => item.estado === registrationStatusFilter.value)
    }

    if (registrationCourseFilter.value !== 'ALL') {
      list = list.filter(item => item.cursoId === Number(registrationCourseFilter.value))
    }

    if (q) {
      list = list.filter(x =>
        x.nombre.toLowerCase().includes(q) ||
        x.subtexto.toLowerCase().includes(q) ||
        x.correo.toLowerCase().includes(q) ||
        (x.alumno?.telefono && x.alumno.telefono.includes(q)) ||
        (x.alumno?.correo && x.alumno.correo.toLowerCase().includes(q))
      )
    }

    return list
  }

  if (moduleType.value === 'attendance') {
    let list = data.asistencias.map(a => {
      const ins = data.inscripciones.find(i => i.matricula === a.matricula || i.alumno === a.alumno)
      const al = data.alumnos.find(x => x.matricula === a.matricula || x.id === ins?.alumnoId)
      const per = al?.persona

      const studentName = a.alumno || (per ? `${per.nombre} ${per.apellidoPaterno} ${per.apellidoMaterno || ''}`.trim() : 'Alumno')
      const matricula = a.matricula || al?.matricula || ins?.matricula || 'ALU-REG'
      const hora = a.horaRegistro ? (typeof a.horaRegistro === 'string' && a.horaRegistro.length >= 16 ? a.horaRegistro.slice(11, 16) : '') : ''

      return {
        id: a.id,
        nombre: studentName,
        subtexto: matricula,
        correo: a.grupo || ins?.grupo || 'Taller',
        rol: a.fecha ? `${a.fecha}${hora ? ' · ' + hora + ' hrs' : ''}` : 'Fecha no reg.',
        estado: a.estado || 'PRESENTE',
        alumnoData: al,
        raw: a
      }
    })

    if (attendanceStatusFilter.value !== 'ALL') {
      if (attendanceStatusFilter.value === 'FALTA') {
        list = list.filter(x => x.estado === 'FALTA' || x.estado === 'JUSTIFICADA')
      } else {
        list = list.filter(x => x.estado === attendanceStatusFilter.value)
      }
    }

    if (attendanceCourseFilter.value !== 'ALL') {
      const selectedCourse = data.cursos.find(c => c.id === Number(attendanceCourseFilter.value))
      if (selectedCourse) {
        list = list.filter(x => (x.correo || '').toLowerCase().includes(selectedCourse.nombre.toLowerCase()))
      }
    }

    if (attendanceDateFilter.value) {
      list = list.filter(x => x.raw.fecha === attendanceDateFilter.value)
    }

    if (q) {
      list = list.filter(x => {
        const per = x.alumnoData?.persona
        const matchName = (x.nombre || '').toLowerCase().includes(q) ||
          `${per?.nombre || ''} ${per?.apellidoPaterno || ''} ${per?.apellidoMaterno || ''}`.toLowerCase().includes(q)
        const matchMat = (x.subtexto || '').toLowerCase().includes(q)
        const matchGrp = (x.correo || '').toLowerCase().includes(q)
        const matchDate = (x.rol || '').toLowerCase().includes(q) || (x.raw.fecha || '').includes(q)
        const matchPhone = (per?.telefono || x.alumnoData?.telefono || '').includes(q)
        const matchEmail = (per?.correo || x.alumnoData?.correo || '').toLowerCase().includes(q)
        return matchName || matchMat || matchGrp || matchDate || matchPhone || matchEmail
      })
    }

    return list
  }

  if (moduleType.value === 'payments') {
    let list = data.pagos.map(p => {
      const ins = data.inscripciones.find(i => i.id === p.inscripcionId)
      const al = data.alumnos.find(a => a.id === ins?.alumnoId)
      const per = al?.persona

      const studentName = p.alumno || (per ? `${per.nombre} ${per.apellidoPaterno} ${per.apellidoMaterno || ''}`.trim() : (ins?.alumno || 'Alumno'))
      const matricula = p.matricula || al?.matricula || ins?.matricula || ''
      const montoNum = Number(p.monto) || (p.tipoPago === 'INSCRIPCION' ? 500 : p.tipoPago === 'RECARGO' ? 150 : 400)
      const montoFormatted = montoNum.toLocaleString('es-MX', { style: 'currency', currency: 'MXN' })

      return {
        id: p.id,
        nombre: studentName,
        subtexto: `${matricula ? matricula + ' · ' : ''}Ref #${p.id}`,
        correo: `${p.tipoPago} · ${montoFormatted}`,
        rol: `${ins?.grupo || p.grupo || 'Taller'} (${p.periodo || '2026-1'})${p.fechaVencimiento ? ' · Vence: ' + p.fechaVencimiento : ''}`,
        estado: p.estado || 'PAGADO',
        alumnoData: al,
        inscripcionData: ins,
        raw: p
      }
    })

    if (paymentStatusFilter.value !== 'ALL') {
      list = list.filter(x => x.estado === paymentStatusFilter.value)
    }

    if (paymentConceptFilter.value !== 'ALL') {
      list = list.filter(x => x.raw.tipoPago === paymentConceptFilter.value)
    }

    if (paymentCourseFilter.value !== 'ALL') {
      const selectedCourse = data.cursos.find(c => c.id === Number(paymentCourseFilter.value))
      if (selectedCourse) {
        list = list.filter(x => (x.rol || '').toLowerCase().includes(selectedCourse.nombre.toLowerCase()))
      }
    }

    if (q) {
      list = list.filter(x => {
        const per = x.alumnoData?.persona
        const matchName = (x.nombre || '').toLowerCase().includes(q) ||
          `${per?.nombre || ''} ${per?.apellidoPaterno || ''} ${per?.apellidoMaterno || ''}`.toLowerCase().includes(q)
        const matchMat = (x.subtexto || '').toLowerCase().includes(q)
        const matchConcept = (x.correo || '').toLowerCase().includes(q)
        const matchGrp = (x.rol || '').toLowerCase().includes(q)
        const matchPhone = (per?.telefono || x.alumnoData?.telefono || '').includes(q)
        const matchEmail = (per?.correo || x.alumnoData?.correo || '').toLowerCase().includes(q)
        return matchName || matchMat || matchConcept || matchGrp || matchPhone || matchEmail
      })
    }

    return list
  }

  return []
})

// REAL CALCULATIONS FOR SPECIAL MODULES (Monetización, Config, Logs)
const monetizationStats = computed(() => {
  const paidList = data.pagos.filter(p => p.estado === 'PAGADO')
  const pendingList = data.pagos.filter(p => p.estado === 'PENDIENTE')
  const overdueList = data.pagos.filter(p => p.estado === 'VENCIDO')

  const totalPaidAmt = paidList.reduce((acc, p) => acc + (p.monto || (p.tipoPago === 'INSCRIPCION' ? 500 : 400)), 0)
  const totalPendingAmt = pendingList.reduce((acc, p) => acc + (p.monto || 400), 0)
  const totalCount = data.pagos.length || 1
  const compliancePct = Math.round((paidList.length / totalCount) * 100)

  const inscripcionesAmt = paidList.filter(p => p.tipoPago === 'INSCRIPCION').reduce((a, p) => a + (p.monto || 500), 0)
  const mensualidadesAmt = paidList.filter(p => p.tipoPago === 'MENSUALIDAD').reduce((a, p) => a + (p.monto || 400), 0)
  const recargosAmt = paidList.filter(p => p.tipoPago === 'RECARGO').reduce((a, p) => a + (p.monto || 150), 0)

  return {
    totalPaidAmt,
    totalPendingAmt,
    compliancePct,
    totalCount: data.pagos.length,
    paidCount: paidList.length,
    pendingCount: pendingList.length,
    overdueCount: overdueList.length,
    inscripcionesAmt,
    mensualidadesAmt,
    recargosAmt
  }
})

const specialContent = computed(() => {
  const m = monetizationStats.value

  const monetizationData = {
    title: 'Resumen Financiero y Recaudación',
    description: 'Estadísticas económicas calculadas en tiempo real a partir del módulo de pagos.',
    cards: [
      {
        name: 'Ingresos Recaudados',
        description: `$${m.totalPaidAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 })} MXN`,
        action: 'Ver pagos'
      },
      {
        name: 'Pagos Pendientes',
        description: `$${m.totalPendingAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 })} MXN (${m.pendingCount} adeudos)`,
        action: 'Revisar adeudos'
      },
      {
        name: 'Tasa de Cumplimiento',
        description: `${m.compliancePct}% de pagos completados`,
        action: 'Auditar cuotas'
      }
    ]
  }

  const reportsData = {
    title: 'Reportes y Analítica',
    description: 'Consulta los informes de asistencia y métricas del ciclo escolar.',
    cards: [
      {
        name: 'Padrón Estudiantil',
        description: `${data.alumnos.length} alumnos registrados en ${data.cursos.length} talleres culturales.`,
        action: 'Ver padrón'
      },
      {
        name: 'Control de Asistencias',
        description: `${data.asistencias.length} registros procesados en el sistema.`,
        action: 'Generar reporte'
      },
      {
        name: 'Plantilla Docente',
        description: `${data.docentes.length} instructores activos asignados.`,
        action: 'Consultar docentes'
      }
    ]
  }

  const settingsData = {
    title: 'Configuración General del Sistema',
    description: 'Parámetros institucionales de SIGEC Casa de la Cultura de Tlaxiaco.',
    cards: [
      {
        name: 'Datos de la Institución',
        description: 'Casa de la Cultura de Tlaxiaco · Heroica Ciudad de Tlaxiaco, Oaxaca.',
        action: 'Editar información'
      },
      {
        name: 'Seguridad y Accesos JWT',
        description: 'Políticas de contraseñas seguras y caducidad de tokens activa.',
        action: 'Configurar'
      },
      {
        name: 'Notificaciones y Credenciales',
        description: 'Generación digital de credenciales con código QR activada.',
        action: 'Ver estado'
      }
    ]
  }

  const logsData = {
    title: 'Bitácora de Operaciones',
    description: 'Eventos del sistema y sincronización de datos con el backend Spring Boot.',
    cards: [
      {
        name: 'Auditoría de Usuarios',
        description: `${data.usuarios.length} cuentas administrativas y operativas activas.`,
        action: 'Ver auditoría'
      },
      {
        name: 'Sincronización de Catálogo',
        description: `${data.cursos.length} cursos y ${data.grupos.length} grupos verificados en base de datos.`,
        action: 'Ver catálogo'
      },
      {
        name: 'Seguridad de Contraseñas',
        description: 'Cifrado BCrypt y autorización basada en roles (RBAC) activa.',
        action: 'Revisar políticas'
      }
    ]
  }

  const contentMap = {
    monetization: monetizationData,
    reports: reportsData,
    settings: settingsData,
    logs: logsData
  }

  return contentMap[moduleType.value] || null
})
</script>

<template>
  <div class="admin-layout">
    <AdminSidebar />

    <main class="main-content">
      <AdminHeader />

      <section class="module-content">
        <!-- Toast feedback -->
        <transition name="fade">
          <div
            v-if="toast.show"
            class="toast-alert"
            :class="toast.type"
          >
            <CheckCircle2
              v-if="toast.type === 'success'"
              :size="18"
            />
            <AlertCircle
              v-else
              :size="18"
            />
            <span>{{ toast.message }}</span>
            <button
              class="toast-close"
              @click="toast.show = false"
            >
              <X :size="14" />
            </button>
          </div>
        </transition>

        <!-- Module Header Row -->
        <div class="page-title">
          <div class="title-with-icon">
            <div class="title-icon">
              <component
                :is="moduleIcon"
                :size="28"
              />
            </div>

            <div>
              <h1>{{ route.meta.title }}</h1>
              <p>{{ route.meta.subtitle }}</p>
            </div>
          </div>

          <div class="header-actions-group">
            <button
              v-if="moduleType === 'attendance'"
              class="secondary-button"
              @click="handleGenerateAbsences"
            >
              <CalendarCheck2 :size="16" />
              Generar Faltas de Hoy
            </button>

            <button
              v-if="moduleType === 'workshops'"
              class="secondary-button"
              @click="openCreateCategoryModal"
            >
              <Tag :size="16" />
              + Nueva Categoría de Edad
            </button>

            <button
              v-if="buttonText"
              class="primary-button"
              @click="openCreateModal"
            >
              <Plus :size="18" />
              {{ buttonText }}
            </button>
          </div>
        </div>

        <!-- SPECIAL CONTENT (Monetization, Reports, Settings, Logs) -->
        <template v-if="specialContent">
          <div class="special-panel">
            <h2>{{ specialContent.title }}</h2>
            <p>{{ specialContent.description }}</p>

            <div class="special-grid">
              <article
                v-for="card in specialContent.cards"
                :key="card.name"
                class="special-card"
              >
                <component
                  :is="moduleIcon"
                  :size="28"
                />

                <h3>{{ card.name }}</h3>
                <p>{{ card.description }}</p>

                <button @click="showToast(`Sección '${card.name}' verificada en base de datos.`)">
                  {{ card.action || 'Administrar' }}
                </button>
              </article>
            </div>

            <!-- DESGLOSE FINANCIERO EN MONETIZACIÓN -->
            <div
              v-if="moduleType === 'monetizacion'"
              class="monetization-breakdown-box"
            >
              <h3>Desglose Real por Concepto de Pago</h3>
              <div class="concepts-grid">
                <div class="concept-card">
                  <span>Inscripciones</span>
                  <strong>${{ monetizationStats.inscripcionesAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }}</strong>
                  <small>Cuotas de nuevo ingreso</small>
                </div>
                <div class="concept-card">
                  <span>Mensualidades</span>
                  <strong>${{ monetizationStats.mensualidadesAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }}</strong>
                  <small>Cuotas periódicas de taller</small>
                </div>
                <div class="concept-card">
                  <span>Recargos / Trámites</span>
                  <strong>${{ monetizationStats.recargosAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }}</strong>
                  <small>Extemporáneos y constancias</small>
                </div>
              </div>
            </div>

            <!-- GENERADOR DE REPORTES ANALÍTICOS -->
            <div
              v-if="moduleType === 'reports'"
              class="report-interactive-box"
            >
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
        </template>

        <!-- ======================================================== -->
        <!-- OPTION A: HIERARCHICAL WORKSHOPS & GROUPS EXPERIENCE    -->
        <!-- ======================================================== -->
        <template v-else-if="moduleType === 'workshops'">
          <!-- Top Stats Bar -->
          <div class="workshop-stats-banner">
            <div class="ws-stat-item">
              <div class="ws-stat-icon course-icon">
                <BookOpen :size="20" />
              </div>
              <div class="ws-stat-info">
                <strong>{{ workshopStats.totalCourses }}</strong>
                <span>Talleres / Cursos</span>
              </div>
            </div>

            <div class="ws-stat-item">
              <div class="ws-stat-icon group-icon">
                <Layers :size="20" />
              </div>
              <div class="ws-stat-info">
                <strong>{{ workshopStats.totalGroups }}</strong>
                <span>Grupos Abiertos</span>
              </div>
            </div>

            <div class="ws-stat-item">
              <div class="ws-stat-icon teacher-icon">
                <UserCheck :size="20" />
              </div>
              <div class="ws-stat-info">
                <strong>{{ workshopStats.assignedTeachersCount }}</strong>
                <span>Docentes con Grupo</span>
              </div>
            </div>

            <div class="ws-stat-item">
              <div class="ws-stat-icon student-icon">
                <GraduationCap :size="20" />
              </div>
              <div class="ws-stat-info">
                <strong>{{ workshopStats.totalStudents }}</strong>
                <span>Inscripciones Activas</span>
              </div>
            </div>
          </div>

          <!-- Toolbar with Search and Expand Controls -->
          <div class="toolbar ws-toolbar">
            <div class="search-box ws-search">
              <Search :size="18" />
              <input
                v-model="search"
                type="text"
                placeholder="Buscar taller, grupo, categoría de edad, docente..."
              />
            </div>

            <div class="ws-expand-controls">
              <button
                class="secondary-button small"
                @click="expandAllWorkshops"
              >
                Expandir Todo
              </button>
              <button
                class="secondary-button small"
                @click="collapseAllWorkshops"
              >
                Colapsar Todo
              </button>
            </div>
          </div>

          <!-- Workshop Hierarchical List -->
          <div
            v-if="workshopsWithHierarchy.length > 0"
            class="workshops-tree-container"
          >
            <div
              v-for="course in workshopsWithHierarchy"
              :key="course.id"
              class="course-card-hierarchy"
              :class="{ 'is-expanded': expandedWorkshops.has(course.id) }"
            >
              <!-- Course Card Header (Level 1: Course & Current Cycle) -->
              <div
                class="course-header-bar"
                @click="toggleWorkshopExpand(course.id)"
              >
                <div class="course-title-section">
                  <div class="course-avatar-box">
                    <BookOpen :size="22" />
                  </div>
                  <div>
                    <div class="course-title-row">
                      <h2>{{ course.nombre }}</h2>
                      <span
                        v-if="course.ofertaActual"
                        class="cycle-pill"
                      >
                        <Calendar :size="12" />
                        {{ course.ofertaActual.tipo }}
                        <small v-if="course.ofertaActual.fechaInicio">({{ course.ofertaActual.fechaInicio }} a {{ course.ofertaActual.fechaFin }})</small>
                      </span>
                      <span
                        v-else
                        class="cycle-pill no-cycle"
                      >
                        Sin ciclo activo
                      </span>
                    </div>
                    <div class="course-meta-tags">
                      <span class="meta-tag">
                        <strong>{{ course.totalGrupos }}</strong> {{ course.totalGrupos === 1 ? 'Grupo' : 'Grupos' }}
                      </span>
                      <span class="meta-bullet">•</span>
                      <span class="meta-tag">
                        <strong>{{ course.totalAlumnos }}</strong> {{ course.totalAlumnos === 1 ? 'Alumno inscrito' : 'Alumnos inscritos' }}
                      </span>
                    </div>
                  </div>
                </div>

                <div
                  class="course-actions-section"
                  @click.stop
                >
                  <button
                    class="primary-action-btn"
                    title="Agregar un nuevo grupo a este taller"
                    @click="openAddGroupToCourseModal(course)"
                  >
                    <Plus :size="15" />
                    + Agregar Grupo
                  </button>

                  <button
                    class="icon-action-btn"
                    title="Editar nombre del taller"
                    @click="openEditCourseModal(course)"
                  >
                    <Pencil :size="15" />
                  </button>

                  <button
                    class="expand-toggle-btn"
                    @click="toggleWorkshopExpand(course.id)"
                  >
                    <ChevronUp
                      v-if="expandedWorkshops.has(course.id)"
                      :size="19"
                    />
                    <ChevronDown
                      v-else
                      :size="19"
                    />
                  </button>
                </div>
              </div>

              <!-- Course Expanded Body (Level 2: Groups Grid) -->
              <div
                v-if="expandedWorkshops.has(course.id)"
                class="course-groups-body"
              >
                <div
                  v-if="course.grupos.length > 0"
                  class="groups-grid"
                >
                  <div
                    v-for="group in course.grupos"
                    :key="group.id"
                    class="group-card-item"
                  >
                    <!-- Group Header: Title & Category -->
                    <div class="group-card-header">
                      <div>
                        <div class="group-title-row">
                          <h4>{{ group.nombreGrupo }}</h4>
                          <span class="category-badge">
                            <Tag :size="11" />
                            {{ group.categoriaNombre }}
                          </span>
                        </div>
                        <small class="group-id-sub">Grupo ID: #{{ group.id }}</small>
                      </div>
                    </div>

                    <!-- Teacher Section -->
                    <div class="group-section-block">
                      <div class="section-label-row">
                        <span class="section-label">Instructor / Docente</span>
                        <button
                          class="sub-action-link"
                          @click="openAssignTeacherToGroupModal(group, course.nombre)"
                        >
                          {{ group.docenteAsignado ? 'Cambiar' : '+ Asignar' }}
                        </button>
                      </div>

                      <div
                        v-if="group.docenteAsignado"
                        class="teacher-assigned-badge"
                      >
                        <div class="teacher-avatar">
                          {{ group.docenteAsignado.nombre?.charAt(0).toUpperCase() }}
                        </div>
                        <div class="teacher-info">
                          <strong>{{ group.docenteAsignado.nombre }} {{ group.docenteAsignado.apellidoPaterno || '' }}</strong>
                          <small>{{ group.docenteAsignado.especialidad || 'Docente de Taller' }}</small>
                        </div>
                      </div>
                      <div
                        v-else
                        class="no-teacher-badge"
                      >
                        <AlertCircle :size="14" />
                        <span>Sin docente asignado todavía</span>
                      </div>
                    </div>

                    <!-- Schedules Section -->
                    <div class="group-section-block">
                      <div class="section-label-row">
                        <span class="section-label">Horarios Semanales</span>
                        <button
                          class="sub-action-link"
                          @click="openAddScheduleToGroupModal(group, course.nombre)"
                        >
                          + Horario
                        </button>
                      </div>

                      <div
                        v-if="group.horariosList && group.horariosList.length > 0"
                        class="schedules-pill-list"
                      >
                        <div
                          v-for="sched in group.horariosList"
                          :key="sched.id"
                          class="schedule-tag"
                        >
                          <Clock :size="12" />
                          <span><strong>{{ formatDay(sched.dia) }}:</strong> {{ formatTime(sched.horaInicio) }} - {{ formatTime(sched.horaFin) }}</span>
                        </div>
                      </div>
                      <div
                        v-else
                        class="no-schedules-box"
                      >
                        <span>Sin horarios programados</span>
                      </div>
                    </div>

                    <!-- Students & Enrollment Stats -->
                    <div class="group-card-footer">
                      <div class="students-count-tag">
                        <GraduationCap :size="14" />
                        <span><strong>{{ group.totalInscritos }}</strong> alumnos inscritos</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Empty state for this specific course -->
                <div
                  v-else
                  class="empty-course-groups"
                >
                  <FolderPlus :size="36" />
                  <h4>No hay grupos creados para {{ course.nombre }}</h4>
                  <p>Crea el primer grupo (por ejemplo: "Grupo A - Niños" o "Grupo B - Adultos") para comenzar a inscribir alumnos y programar clases.</p>
                  <button
                    class="primary-button small"
                    @click="openAddGroupToCourseModal(course)"
                  >
                    <Plus :size="16" />
                    + Crear Primer Grupo
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty state when no courses match -->
          <div
            v-else
            class="coming-panel"
          >
            <BookOpen :size="58" />
            <h2>No se encontraron talleres culturales</h2>
            <p v-if="search">No hay cursos o grupos que coincidan con "{{ search }}".</p>
            <p v-else>Comienza agregando el primer taller cultural del catálogo.</p>
            <button
              class="primary-button"
              @click="openCreateModal"
            >
              <Plus :size="18" />
              Nuevo Taller Cultural
            </button>
          </div>
        </template>

        <!-- ======================================================== -->
        <!-- OPTION B: RICH SCHEDULES & TIMETABLE EXPERIENCE          -->
        <!-- ======================================================== -->
        <template v-else-if="moduleType === 'schedules'">
          <!-- Top Stats Banner -->
          <div class="schedules-stats-banner">
            <div class="sc-stat-item">
              <div class="sc-stat-icon calendar-icon">
                <CalendarDays :size="20" />
              </div>
              <div class="sc-stat-info">
                <strong>{{ scheduleStats.totalSessions }}</strong>
                <span>Sesiones Semanales</span>
              </div>
            </div>

            <div class="sc-stat-item">
              <div class="sc-stat-icon group-icon">
                <Layers :size="20" />
              </div>
              <div class="sc-stat-info">
                <strong>{{ scheduleStats.groupsWithSchedule }} / {{ scheduleStats.totalGroups }}</strong>
                <span>Grupos Programados</span>
              </div>
            </div>

            <div class="sc-stat-item">
              <div class="sc-stat-icon hours-icon">
                <Clock :size="20" />
              </div>
              <div class="sc-stat-info">
                <strong>{{ scheduleStats.totalHours }} hrs</strong>
                <span>Carga Horaria Semanal</span>
              </div>
            </div>

            <div class="sc-stat-item">
              <div class="sc-stat-icon peak-icon">
                <Sparkles :size="20" />
              </div>
              <div class="sc-stat-info">
                <strong>{{ scheduleStats.peakDay }}</strong>
                <span>Día con Mayor Actividad</span>
              </div>
            </div>
          </div>

          <!-- Controls & Filters Toolbar -->
          <div class="schedules-toolbar">
            <div class="schedules-filters-left">
              <!-- Search box -->
              <div class="search-box">
                <Search :size="18" />
                <input
                  v-model="search"
                  type="text"
                  placeholder="Buscar por taller, grupo, docente o día..."
                />
              </div>

              <!-- Day Filter dropdown -->
              <div class="filter-select-wrapper">
                <Calendar :size="16" class="select-icon" />
                <select v-model="scheduleDayFilter" class="filter-select">
                  <option value="ALL">Todos los Días</option>
                  <option v-for="wd in weekDays" :key="wd.key" :value="wd.key">
                    {{ wd.name }}
                  </option>
                </select>
              </div>

              <!-- Workshop Filter dropdown -->
              <div class="filter-select-wrapper">
                <BookOpen :size="16" class="select-icon" />
                <select v-model="scheduleCourseFilter" class="filter-select">
                  <option value="ALL">Todos los Talleres</option>
                  <option v-for="c in data.cursos" :key="c.id" :value="c.id">
                    {{ c.nombre }}
                  </option>
                </select>
              </div>
            </div>

            <div class="schedules-controls-right">
              <!-- View Mode Switcher -->
              <div class="view-mode-toggle">
                <button
                  type="button"
                  class="mode-btn"
                  :class="{ active: scheduleViewMode === 'calendar' }"
                  title="Vista Parrilla Semanal"
                  @click="scheduleViewMode = 'calendar'"
                >
                  <CalendarDays :size="15" />
                  <span>Parrilla Semanal</span>
                </button>
                <button
                  type="button"
                  class="mode-btn"
                  :class="{ active: scheduleViewMode === 'groups' }"
                  title="Vista por Grupos y Talleres"
                  @click="scheduleViewMode = 'groups'"
                >
                  <Layers :size="15" />
                  <span>Por Grupos</span>
                </button>
                <button
                  type="button"
                  class="mode-btn"
                  :class="{ active: scheduleViewMode === 'list' }"
                  title="Vista Tabla Detallada"
                  @click="scheduleViewMode = 'list'"
                >
                  <List :size="15" />
                  <span>Tabla</span>
                </button>
              </div>

              <!-- Primary Action -->
              <button type="button" class="primary-button" @click="openCreateModal">
                <Plus :size="17" />
                Programar Horario
              </button>
            </div>
          </div>

          <!-- 1. VISTA PARRILLA SEMANAL (CALENDAR / TIMETABLE GRID) -->
          <div v-if="scheduleViewMode === 'calendar'" class="calendar-timetable-grid">
            <div
              v-for="col in schedulesByDay"
              :key="col.key"
              class="day-column"
              :class="{ 'is-today': col.isToday }"
            >
              <!-- Day Column Header -->
              <div class="day-column-header">
                <div class="day-title-row">
                  <span class="day-short">{{ col.short }}</span>
                  <span class="day-name">{{ col.name }}</span>
                  <span v-if="col.isToday" class="today-badge">HOY</span>
                </div>
                <div class="day-meta-row">
                  <span class="sessions-counter">
                    {{ col.totalSessions === 1 ? '1 clase' : `${col.totalSessions} clases` }}
                  </span>
                  <button
                    type="button"
                    class="quick-add-day-btn"
                    title="Programar clase para este día"
                    @click="openScheduleModalForDay(col.key)"
                  >
                    <Plus :size="13" />
                  </button>
                </div>
              </div>

              <!-- Sessions in this day -->
              <div class="day-sessions-container">
                <div
                  v-for="session in col.sessions"
                  :key="session.id"
                  class="session-card"
                  :style="{ '--accent-color': session.themeColor }"
                >
                  <div class="session-card-top">
                    <div class="time-range-badge">
                      <Clock :size="12" />
                      <span>{{ session.horaInicioFormateada }} - {{ session.horaFinFormateada }}</span>
                    </div>
                    <span v-if="session.durationText" class="duration-pill">
                      {{ session.durationText }}
                    </span>
                  </div>

                  <div class="session-course-name">
                    {{ session.nombreCurso }}
                  </div>

                  <div class="session-group-name">
                    {{ session.nombreGrupo }}
                  </div>

                  <div class="session-card-bottom">
                    <div class="session-teacher" :title="session.docenteName">
                      <UserCheck :size="12" />
                      <span>{{ session.docenteName }}</span>
                    </div>

                    <button
                      type="button"
                      class="session-delete-btn"
                      title="Eliminar este horario"
                      @click="handleDeleteSchedule(session.id, `${session.nombreCurso} - ${session.diaTexto} ${session.rangoHorario}`)"
                    >
                      <Trash2 :size="13" />
                    </button>
                  </div>
                </div>

                <!-- Empty Day State -->
                <div v-if="col.sessions.length === 0" class="empty-day-state">
                  <div class="empty-day-icon">
                    <CalendarDays :size="20" />
                  </div>
                  <span>Sin clases programadas</span>
                  <button
                    type="button"
                    class="empty-day-add-link"
                    @click="openScheduleModalForDay(col.key)"
                  >
                    + Asignar clase
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 2. VISTA POR GRUPOS Y TALLERES (GROUP AGENDAS) -->
          <div v-else-if="scheduleViewMode === 'groups'" class="groups-schedules-grid">
            <div
              v-for="grp in schedulesByGroup"
              :key="grp.id"
              class="group-schedule-card"
            >
              <div class="group-schedule-header">
                <div class="group-header-left">
                  <div class="group-course-badge">
                    <BookOpen :size="13" />
                    <span>{{ grp.cursoNombre }}</span>
                  </div>
                  <h4>{{ grp.nombreGrupo }}</h4>
                  <div class="group-meta-chips">
                    <span class="meta-chip category">
                      <Tag :size="11" />
                      {{ grp.categoriaNombre }}
                    </span>
                    <span class="meta-chip teacher">
                      <UserCheck :size="11" />
                      {{ grp.docenteName }}
                    </span>
                    <span class="meta-chip students">
                      <GraduationCap :size="11" />
                      {{ grp.totalInscritos }} alumnos
                    </span>
                  </div>
                </div>

                <div class="group-header-right">
                  <button
                    type="button"
                    class="add-schedule-to-group-btn"
                    @click="openScheduleModalForGroup(grp.id)"
                  >
                    <Plus :size="14" />
                    Agregar Horario
                  </button>
                </div>
              </div>

              <!-- List of scheduled days for this group -->
              <div class="group-schedules-body">
                <div class="schedule-slots-title">
                  <span>Días y horarios asignados:</span>
                  <strong v-if="grp.horarios.length">
                    {{ grp.totalSesiones }} {{ grp.totalSesiones === 1 ? 'sesión semanal' : 'sesiones semanales' }} ({{ grp.totalHours }} hrs/sem)
                  </strong>
                </div>

                <div v-if="grp.horarios.length" class="schedule-slots-flex">
                  <div
                    v-for="sched in grp.horarios"
                    :key="sched.id"
                    class="schedule-slot-pill"
                  >
                    <div class="slot-day-badge">
                      {{ sched.diaTexto }}
                    </div>
                    <div class="slot-time-range">
                      <Clock :size="13" />
                      <span>{{ sched.rangoHorario }}</span>
                    </div>
                    <button
                      type="button"
                      class="slot-remove-btn"
                      title="Eliminar este horario"
                      @click="handleDeleteSchedule(sched.id, `${grp.nombreGrupo} - ${sched.diaTexto} ${sched.rangoHorario}`)"
                    >
                      <Trash2 :size="12" />
                    </button>
                  </div>
                </div>

                <div v-else class="group-no-schedules-alert">
                  <AlertCircle :size="16" />
                  <span>Este grupo aún no tiene horarios asignados en la semana.</span>
                  <button
                    type="button"
                    class="assign-now-link"
                    @click="openScheduleModalForGroup(grp.id)"
                  >
                    Programar ahora
                  </button>
                </div>
              </div>
            </div>

            <!-- Empty groups state -->
            <div v-if="schedulesByGroup.length === 0" class="empty-state-large">
              <Layers :size="48" />
              <h3>No se encontraron grupos</h3>
              <p>Crea grupos en el módulo de Talleres para poder programarles horarios semanales.</p>
            </div>
          </div>

          <!-- 3. VISTA TABLA DETALLADA (DETAILED REAL TABLE) -->
          <div v-else-if="scheduleViewMode === 'list'" class="table-panel">
            <table v-if="enrichedSchedules.length">
              <thead>
                <tr>
                  <th>Taller Cultural</th>
                  <th>Grupo</th>
                  <th>Día</th>
                  <th>Horario</th>
                  <th>Duración</th>
                  <th>Docente</th>
                  <th style="text-align: right; padding-right: 20px;">Acciones</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in enrichedSchedules" :key="item.id">
                  <td>
                    <div class="course-cell">
                      <div class="course-dot" :style="{ backgroundColor: item.themeColor }"></div>
                      <strong>{{ item.nombreCurso }}</strong>
                    </div>
                  </td>
                  <td>
                    <span class="group-badge-cell">{{ item.nombreGrupo }}</span>
                  </td>
                  <td>
                    <span class="day-tag-badge" :class="item.diaKey.toLowerCase()">
                      {{ item.diaTexto }}
                    </span>
                  </td>
                  <td>
                    <div class="time-cell">
                      <Clock :size="13" />
                      <span>{{ item.rangoHorario }}</span>
                    </div>
                  </td>
                  <td>
                    <span class="duration-cell">{{ item.durationText || '-' }}</span>
                  </td>
                  <td>{{ item.docenteName }}</td>
                  <td style="text-align: right; padding-right: 20px;">
                    <button
                      type="button"
                      class="action-button-danger"
                      title="Eliminar horario"
                      @click="handleDeleteSchedule(item.id, `${item.nombreCurso} (${item.diaTexto} ${item.rangoHorario})`)"
                    >
                      <Trash2 :size="15" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-else class="empty-state-large">
              <CalendarDays :size="48" />
              <h3>No se encontraron horarios</h3>
              <p v-if="search">No hay horarios que coincidan con "{{ search }}".</p>
              <p v-else>Aún no se han programado horarios para los talleres culturales.</p>
              <button type="button" class="primary-button" @click="openCreateModal">
                <Plus :size="16" />
                Programar Primer Horario
              </button>
            </div>
          </div>
        </template>

        <!-- TABLE VIEW FOR CRUD MODULES (Users, Students, Teachers, Roles, etc.) -->
        <template v-else-if="currentTableRows.length || search || ['registrations', 'attendance', 'payments'].includes(moduleType)">
          <!-- REGISTRATIONS DEDICATED EXPERIENCE BANNER -->
          <div v-if="moduleType === 'registrations'" class="registrations-stats-banner">
            <div class="rg-stat-card">
              <div class="rg-stat-icon blue">
                <ClipboardList :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Total Inscripciones</span>
                <span class="rg-stat-value">{{ registrationStats.total }}</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon indigo">
                <GraduationCap :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Alumnos Únicos</span>
                <span class="rg-stat-value">{{ registrationStats.uniqueStudents }}</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon green">
                <CheckCircle2 :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Inscripciones Activas</span>
                <span class="rg-stat-value text-green">{{ registrationStats.active }}</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon gray">
                <AlertCircle :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Bajas Registradas</span>
                <span class="rg-stat-value text-gray">{{ registrationStats.bajas }}</span>
              </div>
            </div>
          </div>

          <!-- ATTENDANCE DEDICATED EXPERIENCE BANNER -->
          <div v-else-if="moduleType === 'attendance'" class="registrations-stats-banner attendance-banner">
            <div class="rg-stat-card">
              <div class="rg-stat-icon blue">
                <ClipboardList :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Total Registros</span>
                <span class="rg-stat-value">{{ attendanceStats.total }}</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon green">
                <UserCheck :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Presentes</span>
                <span class="rg-stat-value text-green">{{ attendanceStats.presentes }} ({{ attendanceStats.presentesPct }}%)</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon amber">
                <Clock :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Retardos</span>
                <span class="rg-stat-value text-amber">{{ attendanceStats.retardos }} ({{ attendanceStats.retardosPct }}%)</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon red">
                <XCircle :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Faltas / Justif.</span>
                <span class="rg-stat-value text-red">{{ attendanceStats.faltas }} ({{ attendanceStats.faltasPct }}%)</span>
              </div>
            </div>
          </div>

          <!-- PAYMENTS DEDICATED EXPERIENCE BANNER -->
          <div v-else-if="moduleType === 'payments'" class="registrations-stats-banner payments-banner">
            <div class="rg-stat-card">
              <div class="rg-stat-icon blue">
                <CreditCard :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Total Recaudado</span>
                <span class="rg-stat-value">${{ paymentsStats.totalPaidAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }}</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon green">
                <CheckCircle2 :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Pagos Realizados</span>
                <span class="rg-stat-value text-green">{{ paymentsStats.paidCount }} pagos</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon amber">
                <Clock :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Pendientes de Cobro</span>
                <span class="rg-stat-value text-amber">${{ paymentsStats.totalPendingAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }} ({{ paymentsStats.pendingCount }})</span>
              </div>
            </div>

            <div class="rg-stat-card">
              <div class="rg-stat-icon red">
                <AlertCircle :size="20" />
              </div>
              <div class="rg-stat-content">
                <span class="rg-stat-label">Pagos Vencidos</span>
                <span class="rg-stat-value text-red">${{ paymentsStats.totalOverdueAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }} ({{ paymentsStats.overdueCount }})</span>
              </div>
            </div>
          </div>

          <!-- REGISTRATION SPECIAL CONTROLS (OR STANDARD TOOLBAR) -->
          <div v-if="moduleType === 'registrations'" class="registration-controls-bar">
            <!-- Search by person / workshop -->
            <div class="search-box rg-search-box">
              <Search :size="18" />
              <input
                v-model="search"
                type="text"
                placeholder="Buscar por nombre, apellidos, matrícula (ej. ALU-...), correo o taller..."
              />
              <button
                v-if="search"
                type="button"
                class="input-clear"
                @click="search = ''"
              >
                <X :size="14" />
              </button>
            </div>

            <!-- Status Tabs -->
            <div class="rg-status-tabs">
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: registrationStatusFilter === 'ALL' }"
                @click="registrationStatusFilter = 'ALL'"
              >
                Todas ({{ registrationStats.total }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: registrationStatusFilter === 'ACTIVA' }"
                @click="registrationStatusFilter = 'ACTIVA'"
              >
                Activas ({{ registrationStats.active }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: registrationStatusFilter === 'BAJA' }"
                @click="registrationStatusFilter = 'BAJA'"
              >
                Bajas ({{ registrationStats.bajas }})
              </button>
            </div>

            <!-- Workshop Filter -->
            <div class="rg-filter-select-wrapper">
              <select v-model="registrationCourseFilter" class="rg-course-select">
                <option value="ALL">Todos los talleres culturales</option>
                <option v-for="c in data.cursos" :key="c.id" :value="c.id">
                  {{ c.nombre }}
                </option>
              </select>
            </div>

            <button
              class="secondary-button"
              @click="exportToCsv"
            >
              <Download :size="17" />
              Exportar CSV
            </button>
          </div>

          <!-- ATTENDANCE SPECIAL CONTROLS -->
          <div v-else-if="moduleType === 'attendance'" class="registration-controls-bar">
            <!-- Search by student / matrícula / taller / fecha -->
            <div class="search-box rg-search-box">
              <Search :size="18" />
              <input
                v-model="search"
                type="text"
                placeholder="Buscar por alumno, matrícula (ej. ALU-...), taller o fecha..."
              />
              <button
                v-if="search"
                type="button"
                class="input-clear"
                @click="search = ''"
              >
                <X :size="14" />
              </button>
            </div>

            <!-- Status Tabs -->
            <div class="rg-status-tabs">
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: attendanceStatusFilter === 'ALL' }"
                @click="attendanceStatusFilter = 'ALL'"
              >
                Todas ({{ attendanceStats.total }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: attendanceStatusFilter === 'PRESENTE' }"
                @click="attendanceStatusFilter = 'PRESENTE'"
              >
                Presentes ({{ attendanceStats.presentes }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: attendanceStatusFilter === 'RETARDO' }"
                @click="attendanceStatusFilter = 'RETARDO'"
              >
                Retardos ({{ attendanceStats.retardos }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: attendanceStatusFilter === 'FALTA' }"
                @click="attendanceStatusFilter = 'FALTA'"
              >
                Faltas ({{ attendanceStats.faltas }})
              </button>
            </div>

            <!-- Workshop Filter -->
            <div class="rg-filter-select-wrapper">
              <select v-model="attendanceCourseFilter" class="rg-course-select">
                <option value="ALL">Todos los talleres</option>
                <option v-for="c in data.cursos" :key="c.id" :value="c.id">
                  {{ c.nombre }}
                </option>
              </select>
            </div>

            <!-- Date Filter & Quick Today -->
            <div class="rg-filter-date-wrapper">
              <input v-model="attendanceDateFilter" type="date" class="rg-date-input" title="Filtrar por fecha específica" />
              <button v-if="attendanceDateFilter" type="button" class="btn-clear-date" title="Quitar filtro de fecha" @click="attendanceDateFilter = ''">
                <X :size="12" />
              </button>
              <button
                type="button"
                class="btn-today-shortcut"
                :class="{ active: attendanceDateFilter === todayDate }"
                @click="attendanceDateFilter = attendanceDateFilter === todayDate ? '' : todayDate"
              >
                Hoy
              </button>
            </div>

            <button
              class="secondary-button"
              @click="exportToCsv"
            >
              <Download :size="17" />
              Exportar CSV
            </button>
          </div>

          <!-- PAYMENTS SPECIAL CONTROLS -->
          <div v-else-if="moduleType === 'payments'" class="registration-controls-bar">
            <!-- Search by student / matrícula / concepto / taller / periodo -->
            <div class="search-box rg-search-box">
              <Search :size="18" />
              <input
                v-model="search"
                type="text"
                placeholder="Buscar por alumno, matrícula (ej. ALU-...), concepto, taller, periodo..."
              />
              <button
                v-if="search"
                type="button"
                class="input-clear"
                @click="search = ''"
              >
                <X :size="14" />
              </button>
            </div>

            <!-- Status Tabs -->
            <div class="rg-status-tabs">
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: paymentStatusFilter === 'ALL' }"
                @click="paymentStatusFilter = 'ALL'"
              >
                Todos ({{ paymentsStats.totalCount }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: paymentStatusFilter === 'PAGADO' }"
                @click="paymentStatusFilter = 'PAGADO'"
              >
                Pagados ({{ paymentsStats.paidCount }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: paymentStatusFilter === 'PENDIENTE' }"
                @click="paymentStatusFilter = 'PENDIENTE'"
              >
                Pendientes ({{ paymentsStats.pendingCount }})
              </button>
              <button
                type="button"
                class="rg-tab-btn"
                :class="{ active: paymentStatusFilter === 'VENCIDO' }"
                @click="paymentStatusFilter = 'VENCIDO'"
              >
                Vencidos ({{ paymentsStats.overdueCount }})
              </button>
            </div>

            <!-- Concept Filter -->
            <div class="rg-filter-select-wrapper">
              <select v-model="paymentConceptFilter" class="rg-course-select">
                <option value="ALL">Todos los conceptos</option>
                <option value="INSCRIPCION">Inscripción</option>
                <option value="MENSUALIDAD">Mensualidad</option>
                <option value="RECARGO">Recargo / Trámite</option>
              </select>
            </div>

            <!-- Workshop Filter -->
            <div class="rg-filter-select-wrapper">
              <select v-model="paymentCourseFilter" class="rg-course-select">
                <option value="ALL">Todos los talleres</option>
                <option v-for="c in data.cursos" :key="c.id" :value="c.id">
                  {{ c.nombre }}
                </option>
              </select>
            </div>

            <button
              class="secondary-button"
              @click="exportToCsv"
            >
              <Download :size="17" />
              Exportar CSV
            </button>
          </div>

          <div v-else class="toolbar">
            <div class="search-box">
              <Search :size="18" />
              <input
                v-model="search"
                type="text"
                placeholder="Buscar por nombre, correo, matrícula..."
              />
            </div>

            <button
              class="secondary-button"
              @click="exportToCsv"
            >
              <Download :size="17" />
              Exportar CSV
            </button>
          </div>

          <div class="table-panel">
            <table>
              <thead>
                <tr>
                  <th
                    v-for="col in columns"
                    :key="col"
                  >
                    {{ col }}
                  </th>
                  <th style="text-align: right; padding-right: 20px;">
                    Acciones
                  </th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="item in currentTableRows"
                  :key="item.id"
                >
                  <td>
                    <div class="user-cell">
                      <div class="avatar">
                        {{ item.nombre ? item.nombre.charAt(0).toUpperCase() : 'U' }}
                      </div>
                      <div>
                        <strong>{{ item.nombre }}</strong>
                        <span
                          v-if="item.subtexto"
                          class="cell-sub"
                        >{{ item.subtexto }}</span>
                      </div>
                    </div>
                  </td>

                  <td>{{ item.correo }}</td>

                  <td>{{ item.rol }}</td>

                  <td>
                    <span
                      class="status"
                      :class="{
                        inactive: ['INACTIVO', 'FALTA', 'VENCIDO', 'BAJA'].includes(item.estado),
                        pending: ['RETARDO', 'PENDIENTE'].includes(item.estado)
                      }"
                    >
                      {{ item.estado }}
                    </span>
                  </td>

                  <td style="text-align: right; position: relative;">
                    <button
                      class="action-button"
                      aria-label="Acciones"
                      @click="toggleActionMenu(item.id)"
                    >
                      <MoreVertical :size="19" />
                    </button>

                    <!-- Kebab Popover Menu -->
                    <div
                      v-if="activeActionMenu === item.id"
                      class="row-menu"
                    >
                      <button
                        v-if="moduleType === 'students'"
                        @click="viewStudentCredential(item)"
                      >
                        <QrCode :size="15" /> Ver Credencial QR
                      </button>

                      <button
                        v-if="moduleType === 'students'"
                        @click="openStudentUserModal(item)"
                      >
                        <KeyRound :size="15" /> Crear Acceso Usuario
                      </button>

                      <button
                        v-if="moduleType === 'teachers'"
                        @click="openAssignTeacherModal(item.raw)"
                      >
                        <UserPlus :size="15" /> Asignar a Grupo
                      </button>

                      <button
                        v-if="moduleType === 'users'"
                        @click="openPasswordModal(item)"
                      >
                        <KeyRound :size="15" /> Cambiar Contraseña
                      </button>

                      <button
                        v-if="['students', 'teachers', 'roles'].includes(moduleType)"
                        @click="openEditModal(item.raw)"
                      >
                        <Pencil :size="15" /> Editar
                      </button>

                      <button
                        v-if="moduleType === 'registrations' && item.estado === 'ACTIVA'"
                        class="delete-opt"
                        @click="handleDeactivate('registration', item.id)"
                      >
                        <Trash2 :size="15" /> Dar de baja inscripción
                      </button>

                      <button
                        v-if="moduleType === 'payments' && item.estado !== 'PAGADO'"
                        @click="handleQuickMarkAsPaid(item.id)"
                      >
                        <Check :size="15" /> Marcar como Pagado
                      </button>

                      <button
                        v-else-if="!['registrations', 'payments', 'attendance'].includes(moduleType)"
                        class="delete-opt"
                        @click="handleDeactivate(
                          moduleType === 'users' ? 'user' :
                          moduleType === 'roles' ? 'role' :
                          moduleType === 'students' ? 'student' :
                          moduleType === 'teachers' ? 'teacher' : 'record',
                          item.id
                        )"
                      >
                        <Trash2 :size="15" /> Dar de baja
                      </button>
                    </div>
                  </td>
                </tr>

                <tr v-if="currentTableRows.length === 0">
                  <td
                    :colspan="columns.length + 1"
                    class="empty-row"
                  >
                    No se encontraron registros con el criterio de búsqueda.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <!-- EMPTY STATE -->
        <template v-else>
          <div class="coming-panel">
            <component
              :is="moduleIcon"
              :size="58"
            />
            <h2>{{ route.meta.title }}</h2>
            <p>No hay información registrada aún en este módulo. Puedes comenzar agregando un nuevo registro.</p>

            <button
              v-if="buttonText"
              class="primary-button"
              @click="openCreateModal"
            >
              <Plus :size="18" />
              {{ buttonText }}
            </button>
          </div>
        </template>
      </section>
    </main>

    <!-- ======================================================== -->
    <!-- MODAL DIALOGS                                            -->
    <!-- ======================================================== -->
    <div
      v-if="showModal"
      class="modal-overlay"
      @click.self="showModal = false"
    >
      <div class="modal-card" :class="{ 'modal-card-wide': modalMode === 'add-schedule-group' || moduleType === 'schedules' || moduleType === 'registrations' || modalMode === 'create-full-workshop' }">
        <div class="modal-header">
          <h3>
            <template v-if="modalMode === 'create-full-workshop'">
              Nuevo Taller Cultural Integral
            </template>
            <template v-else-if="modalMode === 'create-direct-group'">
              Agregar Grupo a Taller: {{ formDirectGroup.nombreCurso }}
            </template>
            <template v-else-if="modalMode === 'add-schedule-group'">
              Programar Horarios: {{ formScheduleDirect.nombreGrupo }}
            </template>
            <template v-else-if="modalMode === 'assign-teacher-group'">
              Asignar Instructor: {{ formTeacherDirect.nombreGrupo }}
            </template>
            <template v-else-if="modalMode === 'create-age-category'">
              Nueva Categoría de Edad
            </template>
            <template v-else-if="modalMode === 'edit-course'">
              Editar Nombre del Taller
            </template>
            <template v-else-if="modalMode === 'reset-pass'">
              Restablecer Contraseña de Usuario
            </template>
            <template v-else-if="modalMode === 'student-user'">
              Crear Acceso de Alumno
            </template>
            <template v-else-if="modalMode === 'credential'">
              Credencial Estudiantil Digital
            </template>
            <template v-else-if="modalMode === 'assign-teacher'">
              Asignar Docente a Grupo
            </template>
            <template v-else-if="modalMode === 'edit'">
              Editar {{ titles[moduleType]?.replace('Nuevo ', '') || 'Registro' }}
            </template>
            <template v-else>
              {{ buttonText }}
            </template>
          </h3>
          <button
            class="close-btn"
            @click="showModal = false"
          >
            <X :size="18" />
          </button>
        </div>

        <form
          class="modal-body"
          @submit.prevent="handleModalSubmit"
        >
          <!-- 1. OPTION A: FULL WORKSHOP MODAL (COURSE + CYCLE + INITIAL GROUP) -->
          <template v-if="modalMode === 'create-full-workshop'">
            <div class="form-step-banner">
              <span class="step-num">1</span>
              <div>
                <strong>Datos de la Disciplina / Taller Cultural</strong>
                <p>Define la disciplina cultural (ej. Pintura y Artes Plásticas, Danza Folclórica, Guitarra Clásica).</p>
              </div>
            </div>

            <div class="field">
              <label>Nombre del Taller / Curso *</label>
              <input
                v-model="formFullWorkshop.nombreCurso"
                type="text"
                maxlength="120"
                placeholder="Ej. Guitarra Clásica y Cuerdas"
                :class="{ 'has-error': errors.nombreCurso }"
              />
              <span
                v-if="errors.nombreCurso"
                class="error-text"
              >{{ errors.nombreCurso }}</span>
            </div>

            <div class="form-step-banner">
              <span class="step-num">2</span>
              <div>
                <strong>Ciclo Escolar / Modalidad (Oferta)</strong>
                <p>Establece el periodo y vigencia de este taller.</p>
              </div>
            </div>

            <div class="field">
              <label>Nombre o Tipo de Ciclo *</label>
              <input
                v-model="formFullWorkshop.tipoOferta"
                type="text"
                maxlength="80"
                placeholder="Ej. SEMESTRAL 2026-1, TALLER DE VERANO 2026..."
                :class="{ 'has-error': errors.tipoOferta }"
              />
              <span
                v-if="errors.tipoOferta"
                class="error-text"
              >{{ errors.tipoOferta }}</span>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Fecha de Inicio *</label>
                <input
                  v-model="formFullWorkshop.fechaInicio"
                  type="date"
                  :class="{ 'has-error': errors.fechaInicio }"
                />
                <span
                  v-if="errors.fechaInicio"
                  class="error-text"
                >{{ errors.fechaInicio }}</span>
              </div>
              <div class="field">
                <label>Fecha de Fin *</label>
                <input
                  v-model="formFullWorkshop.fechaFin"
                  type="date"
                  :class="{ 'has-error': errors.fechaFin }"
                />
                <span
                  v-if="errors.fechaFin"
                  class="error-text"
                >{{ errors.fechaFin }}</span>
              </div>
            </div>

            <div class="form-step-banner">
              <span class="step-num">3</span>
              <div>
                <strong>Estructura del Primer Grupo</strong>
                <p>Crea el primer grupo para este taller asignando su categoría, docente y horario inicial.</p>
              </div>
            </div>

            <div class="checkbox-field-box">
              <label class="custom-checkbox">
                <input
                  v-model="formFullWorkshop.crearGrupoInicial"
                  type="checkbox"
                />
                <span class="checkmark"></span>
                <span class="checkbox-label-text">
                  <strong>Crear el primer grupo de inmediato</strong> (Recomendado)
                </span>
              </label>
            </div>

            <template v-if="formFullWorkshop.crearGrupoInicial">
              <div class="form-grid-2">
                <div class="field">
                  <label>Nombre del Grupo *</label>
                  <input
                    v-model="formFullWorkshop.nombreGrupo"
                    type="text"
                    maxlength="100"
                    placeholder="Ej. Grupo A - Niños"
                    :class="{ 'has-error': errors.nombreGrupo }"
                  />
                  <span
                    v-if="errors.nombreGrupo"
                    class="error-text"
                  >{{ errors.nombreGrupo }}</span>
                </div>

                <div class="field">
                  <label>Categoría de Edad *</label>
                  <select
                    v-model="formFullWorkshop.categoriaId"
                    :class="{ 'has-error': errors.categoriaId }"
                  >
                    <option value="">
                      Selecciona una categoría
                    </option>
                    <option
                      v-for="cat in data.categorias"
                      :key="cat.id"
                      :value="cat.id"
                    >
                      {{ cat.nombre }}
                    </option>
                  </select>
                  <span
                    v-if="errors.categoriaId"
                    class="error-text"
                  >{{ errors.categoriaId }}</span>
                </div>
              </div>

              <div class="field">
                <label>Instructor / Docente Asignado (Opcional)</label>
                <select v-model="formFullWorkshop.docenteId">
                  <option value="">
                    -- Sin docente asignado por ahora --
                  </option>
                  <option
                    v-for="d in data.docentes"
                    :key="d.id"
                    :value="d.id"
                  >
                    {{ d.nombre }} {{ d.apellidoPaterno }} {{ d.apellidoMaterno || '' }} ({{ d.especialidad || 'General' }})
                  </option>
                </select>
              </div>

              <div class="schedule-subform-box">
                <span class="subform-title"><Clock :size="14" /> Horario Inicial de Clase</span>
                <div class="form-grid-3">
                  <div class="field">
                    <label>Día</label>
                    <select v-model="formFullWorkshop.dia">
                      <option value="MONDAY">
                        Lunes
                      </option>
                      <option value="TUESDAY">
                        Martes
                      </option>
                      <option value="WEDNESDAY">
                        Miércoles
                      </option>
                      <option value="THURSDAY">
                        Jueves
                      </option>
                      <option value="FRIDAY">
                        Viernes
                      </option>
                      <option value="SATURDAY">
                        Sábado
                      </option>
                      <option value="SUNDAY">
                        Domingo
                      </option>
                    </select>
                  </div>
                  <div class="field">
                    <label>Hora Inicio</label>
                    <input
                      v-model="formFullWorkshop.horaInicio"
                      type="time"
                    />
                  </div>
                  <div class="field">
                    <label>Hora Fin</label>
                    <input
                      v-model="formFullWorkshop.horaFin"
                      type="time"
                    />
                  </div>
                </div>
              </div>
            </template>
          </template>

          <!-- 2. OPTION A: DIRECT GROUP MODAL -->
          <template v-else-if="modalMode === 'create-direct-group'">
            <div class="context-banner">
              <BookOpen :size="18" />
              <span>Agregando grupo para el taller: <strong>{{ formDirectGroup.nombreCurso }}</strong></span>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Nombre del Grupo *</label>
                <input
                  v-model="formDirectGroup.nombreGrupo"
                  type="text"
                  maxlength="100"
                  placeholder="Ej. Grupo B - Jóvenes y Adultos"
                  :class="{ 'has-error': errors.nombreGrupo }"
                />
                <span
                  v-if="errors.nombreGrupo"
                  class="error-text"
                >{{ errors.nombreGrupo }}</span>
              </div>

              <div class="field">
                <label>Categoría de Edad *</label>
                <select
                  v-model="formDirectGroup.categoriaId"
                  :class="{ 'has-error': errors.categoriaId }"
                >
                  <option value="">
                    Selecciona una categoría
                  </option>
                  <option
                    v-for="cat in data.categorias"
                    :key="cat.id"
                    :value="cat.id"
                  >
                    {{ cat.nombre }}
                  </option>
                </select>
                <span
                  v-if="errors.categoriaId"
                  class="error-text"
                >{{ errors.categoriaId }}</span>
              </div>
            </div>

            <div class="field">
              <label>Instructor / Docente Asignado (Opcional)</label>
              <select v-model="formDirectGroup.docenteId">
                <option value="">
                  -- Sin docente asignado por ahora --
                </option>
                <option
                  v-for="d in data.docentes"
                  :key="d.id"
                  :value="d.id"
                >
                  {{ d.nombre }} {{ d.apellidoPaterno }} {{ d.apellidoMaterno || '' }} ({{ d.especialidad || 'General' }})
                </option>
              </select>
            </div>

            <div class="checkbox-field-box">
              <label class="custom-checkbox">
                <input
                  v-model="formDirectGroup.agregarHorarioInicial"
                  type="checkbox"
                />
                <span class="checkmark"></span>
                <span class="checkbox-label-text">
                  <strong>Programar horario inicial para este grupo</strong>
                </span>
              </label>
            </div>

            <template v-if="formDirectGroup.agregarHorarioInicial">
              <div class="form-grid-3">
                <div class="field">
                  <label>Día</label>
                  <select v-model="formDirectGroup.dia">
                    <option value="MONDAY">
                      Lunes
                    </option>
                    <option value="TUESDAY">
                      Martes
                    </option>
                    <option value="WEDNESDAY">
                      Miércoles
                    </option>
                    <option value="THURSDAY">
                      Jueves
                    </option>
                    <option value="FRIDAY">
                      Viernes
                    </option>
                    <option value="SATURDAY">
                      Sábado
                    </option>
                    <option value="SUNDAY">
                      Domingo
                    </option>
                  </select>
                </div>
                <div class="field">
                  <label>Hora Inicio</label>
                  <input
                    v-model="formDirectGroup.horaInicio"
                    type="time"
                  />
                </div>
                <div class="field">
                  <label>Hora Fin</label>
                  <input
                    v-model="formDirectGroup.horaFin"
                    type="time"
                  />
                </div>
              </div>
            </template>
          </template>

          <!-- 3. OPTION A: ADD SCHEDULE TO GROUP (MULTI-DAY / MULTI-SLOT BATCH) -->
          <template v-else-if="modalMode === 'add-schedule-group'">
            <div class="context-banner">
              <Clock :size="18" />
              <span>Programando horarios para: <strong>{{ formScheduleDirect.nombreGrupo }}</strong></span>
            </div>

            <div class="schedule-batch-container">
              <div
                v-for="(slot, idx) in formScheduleDirect.slots"
                :key="idx"
                class="schedule-slot-card"
              >
                <div class="slot-card-header">
                  <div class="slot-badge">
                    <CalendarDays :size="15" />
                    <span>Bloque de Horario #{{ idx + 1 }}</span>
                  </div>
                  <button
                    v-if="formScheduleDirect.slots.length > 1"
                    type="button"
                    class="btn-remove-slot"
                    @click="removeSlotFromSchedule(formScheduleDirect, idx)"
                    title="Eliminar este bloque"
                  >
                    <Trash2 :size="14" />
                    <span>Quitar bloque</span>
                  </button>
                </div>

                <!-- Presets chips -->
                <div class="slot-presets">
                  <span class="preset-label">Atajos rápidos:</span>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'MWF')"
                  >
                    Lun, Mié, Vie
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'TTH')"
                  >
                    Mar, Jue
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'WEEKDAYS')"
                  >
                    Lun a Vie
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'WEEKEND')"
                  >
                    Sáb y Dom
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'SAT')"
                  >
                    Solo Sábado
                  </button>
                </div>

                <!-- Multi-Day Pills Selector -->
                <div class="field">
                  <label>Días de la semana para este horario *</label>
                  <div class="days-pills-row">
                    <button
                      v-for="day in weekDaysList"
                      :key="day.key"
                      type="button"
                      class="day-pill-btn"
                      :class="{ 'active': slot.dias && slot.dias.includes(day.key) }"
                      @click="toggleSlotDay(slot, day.key)"
                    >
                      <Check v-if="slot.dias && slot.dias.includes(day.key)" :size="13" class="pill-check-icon" />
                      <span class="pill-day-label">{{ day.short }}</span>
                      <span class="pill-day-full">{{ day.label }}</span>
                    </button>
                  </div>
                  <span
                    v-if="errors[`slot_${idx}_dias`]"
                    class="error-text"
                  >{{ errors[`slot_${idx}_dias`] }}</span>
                </div>

                <!-- Times row -->
                <div class="form-grid-2">
                  <div class="field">
                    <label>Hora de Inicio *</label>
                    <input
                      v-model="slot.horaInicio"
                      type="time"
                      :class="{ 'has-error': errors[`slot_${idx}_inicio`] }"
                    />
                    <span
                      v-if="errors[`slot_${idx}_inicio`]"
                      class="error-text"
                    >{{ errors[`slot_${idx}_inicio`] }}</span>
                  </div>

                  <div class="field">
                    <label>Hora de Fin *</label>
                    <input
                      v-model="slot.horaFin"
                      type="time"
                      :class="{ 'has-error': errors[`slot_${idx}_fin`] }"
                    />
                    <span
                      v-if="errors[`slot_${idx}_fin`]"
                      class="error-text"
                    >{{ errors[`slot_${idx}_fin`] }}</span>
                  </div>
                </div>
              </div>

              <!-- Add another slot button & summary -->
              <div class="schedule-batch-footer">
                <button
                  type="button"
                  class="btn-add-slot"
                  @click="addSlotToSchedule(formScheduleDirect)"
                >
                  <Plus :size="16" />
                  <span>Agregar otro bloque de horario (ej. fines de semana u otro turno)</span>
                </button>

                <div class="batch-summary-info">
                  <Sparkles :size="16" />
                  <span>
                    Se programarán <strong>{{ countTotalSessions(formScheduleDirect.slots) }} sesión(es)</strong> en la base de datos en una sola operación.
                  </span>
                </div>
              </div>
            </div>
          </template>

          <!-- 4. OPTION A: ASSIGN TEACHER TO GROUP -->
          <template v-else-if="modalMode === 'assign-teacher-group'">
            <div class="context-banner">
              <UserCheck :size="18" />
              <span>Asignando instructor a: <strong>{{ formTeacherDirect.nombreGrupo }}</strong></span>
            </div>

            <div class="field">
              <label>Selecciona al Docente / Instructor *</label>
              <select
                v-model="formTeacherDirect.docenteId"
                :class="{ 'has-error': errors.docenteId }"
              >
                <option value="">
                  -- Seleccionar Docente --
                </option>
                <option
                  v-for="d in data.docentes"
                  :key="d.id"
                  :value="d.id"
                >
                  {{ d.nombre }} {{ d.apellidoPaterno }} {{ d.apellidoMaterno || '' }} ({{ d.especialidad || 'General' }})
                </option>
              </select>
              <span
                v-if="errors.docenteId"
                class="error-text"
              >{{ errors.docenteId }}</span>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Fecha de Inicio de Asignación *</label>
                <input
                  v-model="formTeacherDirect.fechaInicio"
                  type="date"
                  :class="{ 'has-error': errors.fechaInicio }"
                />
                <span
                  v-if="errors.fechaInicio"
                  class="error-text"
                >{{ errors.fechaInicio }}</span>
              </div>
              <div class="field">
                <label>Fecha de Fin (Opcional)</label>
                <input
                  v-model="formTeacherDirect.fechaFin"
                  type="date"
                />
              </div>
            </div>
          </template>

          <!-- 5. OPTION A: CREATE AGE CATEGORY -->
          <template v-else-if="modalMode === 'create-age-category'">
            <p class="modal-help-text">
              Las categorías de edad permiten clasificar los grupos en los talleres (ej. Niños, Adolescentes, Adultos, Tercera Edad).
            </p>
            <div class="field">
              <label>Nombre de la Categoría de Edad *</label>
              <input
                v-model="formCategoryDirect.nombre"
                type="text"
                maxlength="100"
                placeholder="Ej. Infantil (6 a 12 años)"
                :class="{ 'has-error': errors.nombre }"
              />
              <span
                v-if="errors.nombre"
                class="error-text"
              >{{ errors.nombre }}</span>
            </div>
          </template>

          <!-- 6. OPTION A: EDIT COURSE NAME -->
          <template v-else-if="modalMode === 'edit-course'">
            <div class="field">
              <label>Nombre del Taller / Curso *</label>
              <input
                v-model="formEditCourse.nombre"
                type="text"
                maxlength="120"
                :class="{ 'has-error': errors.nombre }"
              />
              <span
                v-if="errors.nombre"
                class="error-text"
              >{{ errors.nombre }}</span>
            </div>
          </template>

          <!-- 7. STANDARD MODALS (Users, Students, Teachers, Roles, etc.) -->
          <template v-else-if="modalMode === 'credential'">
            <div
              v-if="credentialInfo"
              class="credential-preview"
            >
              <div class="cred-header">
                <h4>CASA DE LA CULTURA DE TLAXIACO</h4>
                <span>Credencial Digital Estudiantil</span>
              </div>
              <div class="cred-body">
                <div class="cred-qr">
                  <div class="qr-box">
                    <QrCode :size="110" />
                  </div>
                  <small>{{ credentialInfo.matricula }}</small>
                </div>
                <div class="cred-info">
                  <strong>{{ credentialInfo.nombreCompleto }}</strong>
                  <p><strong>Matrícula:</strong> {{ credentialInfo.matricula }}</p>
                  <p><strong>Vigencia:</strong> {{ credentialInfo.vigencia || 'Ciclo 2026' }}</p>
                  <span class="cred-badge">ESTUDIANTE ACTIVO</span>
                </div>
              </div>
            </div>
            <div class="cred-actions">
              <button
                type="button"
                class="secondary-button"
                @click="printCredential"
              >
                <Printer :size="16" /> Imprimir Credencial
              </button>
            </div>
          </template>

          <!-- RESET PASSWORD FORM -->
          <template v-else-if="modalMode === 'reset-pass'">
            <p class="modal-help-text">
              Ingresa la nueva contraseña para el usuario seleccionado.
            </p>
            <div class="field">
              <label>Nueva Contraseña * (Mínimo 8 caracteres)</label>
              <input
                v-model="formPassword.passwordNueva"
                type="password"
                placeholder="••••••••"
                :class="{ 'has-error': errors.passwordNueva }"
              />
              <span
                v-if="errors.passwordNueva"
                class="error-text"
              >{{ errors.passwordNueva }}</span>
            </div>
          </template>

          <!-- STUDENT USER FORM -->
          <template v-else-if="modalMode === 'student-user'">
            <p class="modal-help-text">
              Se generará automáticamente un usuario y contraseña para que el alumno acceda al portal de estudiantes.
            </p>
            <div class="field">
              <label>Contraseña Inicial * (Mínimo 8 caracteres)</label>
              <input
                v-model="formStudentUser.password"
                type="text"
                placeholder="Alumno123!"
                :class="{ 'has-error': errors.password }"
              />
              <span
                v-if="errors.password"
                class="error-text"
              >{{ errors.password }}</span>
            </div>
          </template>

          <!-- ASSIGN TEACHER FORM -->
          <template v-else-if="modalMode === 'assign-teacher'">
            <div class="field">
              <label>Docente Seleccionado *</label>
              <select
                v-model="formTeacherAssignment.docenteId"
                :class="{ 'has-error': errors.docenteId }"
              >
                <option
                  v-for="d in data.docentes"
                  :key="d.id"
                  :value="d.id"
                >
                  {{ d.nombre }} {{ d.apellidoPaterno }} ({{ d.especialidad || 'Docente' }})
                </option>
              </select>
            </div>
            <div class="field">
              <label>Grupo a Asignar *</label>
              <select
                v-model="formTeacherAssignment.grupoId"
                :class="{ 'has-error': errors.grupoId }"
              >
                <option value="">
                  Selecciona un grupo
                </option>
                <option
                  v-for="g in data.grupos"
                  :key="g.id"
                  :value="g.id"
                >
                  {{ g.nombreGrupo }} - {{ g.curso || 'Taller' }}
                </option>
              </select>
              <span
                v-if="errors.grupoId"
                class="error-text"
              >{{ errors.grupoId }}</span>
            </div>
            <div class="form-grid-2">
              <div class="field">
                <label>Fecha de Inicio *</label>
                <input
                  v-model="formTeacherAssignment.fechaInicio"
                  type="date"
                  :class="{ 'has-error': errors.fechaInicio }"
                />
              </div>
              <div class="field">
                <label>Fecha de Fin (Opcional)</label>
                <input
                  v-model="formTeacherAssignment.fechaFin"
                  type="date"
                />
              </div>
            </div>
          </template>

          <!-- USER FORM -->
          <template v-else-if="moduleType === 'users'">
            <!-- Tipo de Creación: Personal Administrativo vs Alumno/Docente -->
            <div class="user-creation-type-toggle">
              <button
                type="button"
                class="type-toggle-btn"
                :class="{ active: userCreationType === 'admin' }"
                @click="setUserCreationType('admin')"
              >
                <ShieldCheck :size="16" />
                <div class="toggle-btn-text">
                  <strong>Personal Administrativo</strong>
                  <small>Supervisor o Super Admin</small>
                </div>
              </button>
              <button
                type="button"
                class="type-toggle-btn"
                :class="{ active: userCreationType === 'existing' }"
                @click="setUserCreationType('existing')"
              >
                <UserCheck :size="16" />
                <div class="toggle-btn-text">
                  <strong>Vincular Existente</strong>
                  <small>Alumno o Docente Registrado</small>
                </div>
              </button>
            </div>

            <!-- MODO 1: PERSONAL ADMINISTRATIVO (Campos de Persona Directos) -->
            <div
              v-if="userCreationType === 'admin'"
              class="admin-person-section"
            >
              <div class="user-helper-banner">
                <ShieldCheck :size="16" class="helper-icon" />
                <span>Ingresa los datos personales del supervisor o administrador. Se creará automáticamente su registro en el sistema.</span>
              </div>

              <div class="form-grid-2">
                <div class="field">
                  <label>Nombre(s) *</label>
                  <input
                    v-model="formAdminPerson.nombre"
                    type="text"
                    maxlength="50"
                    placeholder="Ej. Roberto"
                    :class="{ 'has-error': errors.adminNombre }"
                    @input="onAdminPersonNameInput"
                  />
                  <span
                    v-if="errors.adminNombre"
                    class="error-text"
                  >{{ errors.adminNombre }}</span>
                </div>
                <div class="field">
                  <label>Apellido Paterno *</label>
                  <input
                    v-model="formAdminPerson.apellidoPaterno"
                    type="text"
                    maxlength="50"
                    placeholder="Ej. Gómez"
                    :class="{ 'has-error': errors.adminApellidoPaterno }"
                    @input="onAdminPersonNameInput"
                  />
                  <span
                    v-if="errors.adminApellidoPaterno"
                    class="error-text"
                  >{{ errors.adminApellidoPaterno }}</span>
                </div>
              </div>

              <div class="form-grid-3">
                <div class="field">
                  <label>Apellido Materno</label>
                  <input
                    v-model="formAdminPerson.apellidoMaterno"
                    type="text"
                    maxlength="50"
                    placeholder="Ej. Morales"
                  />
                </div>
                <div class="field">
                  <label>Correo Electrónico</label>
                  <input
                    v-model="formAdminPerson.correo"
                    type="email"
                    maxlength="100"
                    placeholder="ejemplo@casacultura.org"
                  />
                </div>
                <div class="field">
                  <label>Teléfono</label>
                  <input
                    v-model="formAdminPerson.telefono"
                    type="tel"
                    maxlength="15"
                    placeholder="10 dígitos"
                  />
                </div>
              </div>
            </div>

            <!-- MODO 2: VINCULAR ALUMNO O DOCENTE EXISTENTE -->
            <div
              v-else
              class="field"
            >
              <label>Persona Asociada (Alumno o Docente) *</label>
              <div
                v-if="selectedPerson"
                class="selected-person-card"
              >
                <div class="person-avatar">
                  {{ selectedPerson.nombreCompleto.charAt(0) }}
                </div>
                <div class="person-details">
                  <strong>{{ selectedPerson.nombreCompleto }}</strong>
                  <span>{{ selectedPerson.detalle }} &bull; {{ selectedPerson.correo }}</span>
                </div>
                <button
                  type="button"
                  class="clear-person-btn"
                  @click="clearSelectedPerson"
                >
                  <X :size="14" /> Cambiar
                </button>
              </div>

              <div
                v-else
                class="searchable-picker-container"
              >
                <div class="picker-search-bar">
                  <div class="picker-search-input">
                    <Search :size="15" />
                    <input
                      v-model="personSearch"
                      type="text"
                      placeholder="Escribe el nombre, matrícula o correo..."
                    />
                    <button
                      v-if="personSearch"
                      type="button"
                      class="input-clear"
                      @click="personSearch = ''"
                    >
                      <X :size="13" />
                    </button>
                  </div>
                  <div class="filter-tabs">
                    <button
                      type="button"
                      class="tab-btn"
                      :class="{ active: personTypeFilter === 'ALL' }"
                      @click="personTypeFilter = 'ALL'"
                    >
                      Todos
                    </button>
                    <button
                      type="button"
                      class="tab-btn"
                      :class="{ active: personTypeFilter === 'ALUMNO' }"
                      @click="personTypeFilter = 'ALUMNO'"
                    >
                      Alumnos
                    </button>
                    <button
                      type="button"
                      class="tab-btn"
                      :class="{ active: personTypeFilter === 'DOCENTE' }"
                      @click="personTypeFilter = 'DOCENTE'"
                    >
                      Docentes
                    </button>
                  </div>
                </div>

                <div class="picker-results-list">
                  <div
                    v-for="p in filteredPersons"
                    :key="`${p.tipo}-${p.id}`"
                    class="picker-item"
                    @click="selectPersonForUser(p)"
                  >
                    <div class="person-avatar small">
                      {{ p.nombreCompleto.charAt(0) }}
                    </div>
                    <div class="picker-item-info">
                      <div class="name-row">
                        <strong>{{ p.nombreCompleto }}</strong>
                        <span
                          class="tag-badge"
                          :class="p.tipo === 'ALUMNO' ? 'tag-student' : 'tag-teacher'"
                        >{{ p.tipo }}</span>
                      </div>
                      <small>{{ p.detalle }} &bull; {{ p.correo }}</small>
                    </div>
                    <Check
                      :size="16"
                      class="picker-select-icon"
                    />
                  </div>

                  <div
                    v-if="filteredPersons.length === 0"
                    class="picker-no-results"
                  >
                    <AlertCircle :size="16" /> No se encontraron personas disponibles sin usuario.
                  </div>
                </div>
              </div>
              <span
                v-if="errors.personaId"
                class="error-text"
              >{{ errors.personaId }}</span>
            </div>

            <!-- CAMPOS COMUNES: ROL, NOMBRE DE USUARIO Y CONTRASEÑA -->
            <div class="form-grid-2">
              <div class="field">
                <label>Rol Asignado *</label>
                <select
                  v-model="formUser.rolId"
                  :class="{ 'has-error': errors.rolId }"
                >
                  <option value="">
                    Selecciona un rol
                  </option>
                  <option
                    v-for="r in data.roles"
                    :key="r.id"
                    :value="r.id"
                  >
                    {{ r.nombre }}
                  </option>
                </select>
                <span
                  v-if="errors.rolId"
                  class="error-text"
                >{{ errors.rolId }}</span>
              </div>

              <div class="field">
                <label>Nombre de Usuario *</label>
                <input
                  :value="formUser.nombreUsuario"
                  type="text"
                  maxlength="80"
                  placeholder="ej. roberto.gomez"
                  :class="{ 'has-error': errors.nombreUsuario }"
                  @input="filterUsernameInput($event, formUser, 'nombreUsuario')"
                />
                <span
                  v-if="errors.nombreUsuario"
                  class="error-text"
                >{{ errors.nombreUsuario }}</span>
              </div>
            </div>

            <div class="field">
              <label>Contraseña * (Mínimo 8 caracteres)</label>
              <input
                v-model="formUser.password"
                type="password"
                placeholder="••••••••"
                :class="{ 'has-error': errors.password }"
              />
              <span
                v-if="errors.password"
                class="error-text"
              >{{ errors.password }}</span>
            </div>
          </template>

          <!-- ROLE FORM -->
          <template v-else-if="moduleType === 'roles'">
            <div class="field">
              <label>Nombre del Rol *</label>
              <input
                :value="formRole.nombre"
                type="text"
                maxlength="50"
                placeholder="Ej. COORDINADOR_ACADEMICO"
                :class="{ 'has-error': errors.nombre }"
                @input="filterRoleNameInput($event, formRole, 'nombre')"
              />
              <span
                v-if="errors.nombre"
                class="error-text"
              >{{ errors.nombre }}</span>
            </div>
            <div class="field">
              <label>Descripción del Rol</label>
              <input
                v-model="formRole.descripcion"
                type="text"
                maxlength="200"
                placeholder="Descripción de los permisos y responsabilidades"
              />
            </div>
          </template>

          <!-- STUDENT FORM -->
          <template v-else-if="moduleType === 'students'">
            <div class="form-grid-2">
              <div class="field">
                <label>Nombre(s) *</label>
                <input
                  :value="formStudent.nombre"
                  type="text"
                  maxlength="80"
                  placeholder="Nombre de pila"
                  :class="{ 'has-error': errors.nombre }"
                  @input="filterNameInput($event, formStudent, 'nombre')"
                />
                <span
                  v-if="errors.nombre"
                  class="error-text"
                >{{ errors.nombre }}</span>
              </div>
              <div class="field">
                <label>Apellido Paterno *</label>
                <input
                  :value="formStudent.apellidoPaterno"
                  type="text"
                  maxlength="80"
                  placeholder="Primer apellido"
                  :class="{ 'has-error': errors.apellidoPaterno }"
                  @input="filterNameInput($event, formStudent, 'apellidoPaterno')"
                />
                <span
                  v-if="errors.apellidoPaterno"
                  class="error-text"
                >{{ errors.apellidoPaterno }}</span>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Apellido Materno</label>
                <input
                  :value="formStudent.apellidoMaterno"
                  type="text"
                  maxlength="80"
                  placeholder="Segundo apellido (opcional)"
                  :class="{ 'has-error': errors.apellidoMaterno }"
                  @input="filterNameInput($event, formStudent, 'apellidoMaterno')"
                />
              </div>
              <div class="field">
                <label>Matrícula Oficial *</label>
                <input
                  :value="formStudent.matricula"
                  type="text"
                  maxlength="30"
                  placeholder="ALU-2026-001"
                  :class="{ 'has-error': errors.matricula }"
                  @input="filterMatriculaInput($event, formStudent, 'matricula')"
                />
                <span
                  v-if="errors.matricula"
                  class="error-text"
                >{{ errors.matricula }}</span>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Correo Electrónico *</label>
                <input
                  v-model="formStudent.correo"
                  type="email"
                  maxlength="160"
                  placeholder="correo@ejemplo.com"
                  :class="{ 'has-error': errors.correo }"
                  @input="delete errors.correo"
                />
                <span
                  v-if="errors.correo"
                  class="error-text"
                >{{ errors.correo }}</span>
              </div>
              <div class="field">
                <label>Teléfono (10 dígitos) *</label>
                <input
                  :value="formStudent.telefono"
                  type="tel"
                  maxlength="10"
                  placeholder="9531234567"
                  :class="{ 'has-error': errors.telefono }"
                  @input="filterPhoneInput($event, formStudent, 'telefono')"
                />
                <span
                  v-if="errors.telefono"
                  class="error-text"
                >{{ errors.telefono }}</span>
              </div>
            </div>

            <div class="field">
              <label>Fecha de Nacimiento *</label>
              <input
                v-model="formStudent.fechaNacimiento"
                type="date"
                :max="todayDate"
                min="1920-01-01"
                :class="{ 'has-error': errors.fechaNacimiento }"
                @change="delete errors.fechaNacimiento"
              />
              <span
                v-if="errors.fechaNacimiento"
                class="error-text"
              >{{ errors.fechaNacimiento }}</span>
            </div>

            <div class="field">
              <label>Dirección *</label>
              <input
                v-model="formStudent.direccion"
                type="text"
                maxlength="255"
                placeholder="Calle, Número, Colonia, Municipio"
                :class="{ 'has-error': errors.direccion }"
                @input="delete errors.direccion"
              />
              <span
                v-if="errors.direccion"
                class="error-text"
              >{{ errors.direccion }}</span>
            </div>
          </template>

          <!-- TEACHER FORM -->
          <template v-else-if="moduleType === 'teachers'">
            <div class="form-grid-2">
              <div class="field">
                <label>Nombre(s) *</label>
                <input
                  :value="formTeacher.nombre"
                  type="text"
                  maxlength="80"
                  placeholder="Nombre(s)"
                  :class="{ 'has-error': errors.nombre }"
                  @input="filterNameInput($event, formTeacher, 'nombre')"
                />
                <span
                  v-if="errors.nombre"
                  class="error-text"
                >{{ errors.nombre }}</span>
              </div>
              <div class="field">
                <label>Apellido Paterno *</label>
                <input
                  :value="formTeacher.apellidoPaterno"
                  type="text"
                  maxlength="80"
                  placeholder="Primer apellido"
                  :class="{ 'has-error': errors.apellidoPaterno }"
                  @input="filterNameInput($event, formTeacher, 'apellidoPaterno')"
                />
                <span
                  v-if="errors.apellidoPaterno"
                  class="error-text"
                >{{ errors.apellidoPaterno }}</span>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Apellido Materno</label>
                <input
                  :value="formTeacher.apellidoMaterno"
                  type="text"
                  maxlength="80"
                  placeholder="Segundo apellido"
                  :class="{ 'has-error': errors.apellidoMaterno }"
                  @input="filterNameInput($event, formTeacher, 'apellidoMaterno')"
                />
              </div>
              <div class="field">
                <label>Especialidad Cultural *</label>
                <input
                  v-model="formTeacher.especialidad"
                  type="text"
                  maxlength="100"
                  placeholder="Ej. Guitarra Clásica, Danza, Pintura"
                  :class="{ 'has-error': errors.especialidad }"
                  @input="delete errors.especialidad"
                />
                <span
                  v-if="errors.especialidad"
                  class="error-text"
                >{{ errors.especialidad }}</span>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Correo Electrónico *</label>
                <input
                  v-model="formTeacher.correo"
                  type="email"
                  maxlength="160"
                  placeholder="profesor@ejemplo.com"
                  :class="{ 'has-error': errors.correo }"
                  @input="delete errors.correo"
                />
                <span
                  v-if="errors.correo"
                  class="error-text"
                >{{ errors.correo }}</span>
              </div>
              <div class="field">
                <label>Teléfono (10 dígitos) *</label>
                <input
                  :value="formTeacher.telefono"
                  type="tel"
                  maxlength="10"
                  placeholder="9531234567"
                  :class="{ 'has-error': errors.telefono }"
                  @input="filterPhoneInput($event, formTeacher, 'telefono')"
                />
                <span
                  v-if="errors.telefono"
                  class="error-text"
                >{{ errors.telefono }}</span>
              </div>
            </div>

            <div class="field">
              <label>Fecha de Nacimiento *</label>
              <input
                v-model="formTeacher.fechaNacimiento"
                type="date"
                :max="todayDate"
                min="1920-01-01"
                :class="{ 'has-error': errors.fechaNacimiento }"
                @change="delete errors.fechaNacimiento"
              />
              <span
                v-if="errors.fechaNacimiento"
                class="error-text"
              >{{ errors.fechaNacimiento }}</span>
            </div>

            <div class="field">
              <label>Dirección *</label>
              <input
                v-model="formTeacher.direccion"
                type="text"
                maxlength="255"
                placeholder="Domicilio completo"
                :class="{ 'has-error': errors.direccion }"
                @input="delete errors.direccion"
              />
              <span
                v-if="errors.direccion"
                class="error-text"
              >{{ errors.direccion }}</span>
            </div>
          </template>

          <!-- SCHEDULE FORM (MULTI-DAY / MULTI-SLOT BATCH) -->
          <template v-else-if="moduleType === 'schedules'">
            <div class="field">
              <label>Grupo / Taller Académico *</label>
              <select
                v-model="formSchedule.grupoId"
                :class="{ 'has-error': errors.grupoId }"
                @change="delete errors.grupoId"
              >
                <option value="">
                  Selecciona un grupo
                </option>
                <option
                  v-for="g in data.grupos"
                  :key="g.id"
                  :value="g.id"
                >
                  {{ g.nombreGrupo }} ({{ g.curso || 'Taller' }})
                </option>
              </select>
              <span
                v-if="errors.grupoId"
                class="error-text"
              >{{ errors.grupoId }}</span>
            </div>

            <div class="schedule-batch-container">
              <div
                v-for="(slot, idx) in formSchedule.slots"
                :key="idx"
                class="schedule-slot-card"
              >
                <div class="slot-card-header">
                  <div class="slot-badge">
                    <CalendarDays :size="15" />
                    <span>Bloque de Horario #{{ idx + 1 }}</span>
                  </div>
                  <button
                    v-if="formSchedule.slots.length > 1"
                    type="button"
                    class="btn-remove-slot"
                    @click="removeSlotFromSchedule(formSchedule, idx)"
                    title="Eliminar este bloque"
                  >
                    <Trash2 :size="14" />
                    <span>Quitar bloque</span>
                  </button>
                </div>

                <!-- Presets chips -->
                <div class="slot-presets">
                  <span class="preset-label">Atajos rápidos:</span>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'MWF')"
                  >
                    Lun, Mié, Vie
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'TTH')"
                  >
                    Mar, Jue
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'WEEKDAYS')"
                  >
                    Lun a Vie
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'WEEKEND')"
                  >
                    Sáb y Dom
                  </button>
                  <button
                    type="button"
                    class="preset-chip"
                    @click="applySlotPreset(slot, 'SAT')"
                  >
                    Solo Sábado
                  </button>
                </div>

                <!-- Multi-Day Pills Selector -->
                <div class="field">
                  <label>Días de la semana para este horario *</label>
                  <div class="days-pills-row">
                    <button
                      v-for="day in weekDaysList"
                      :key="day.key"
                      type="button"
                      class="day-pill-btn"
                      :class="{ 'active': slot.dias && slot.dias.includes(day.key) }"
                      @click="toggleSlotDay(slot, day.key)"
                    >
                      <Check v-if="slot.dias && slot.dias.includes(day.key)" :size="13" class="pill-check-icon" />
                      <span class="pill-day-label">{{ day.short }}</span>
                      <span class="pill-day-full">{{ day.label }}</span>
                    </button>
                  </div>
                  <span
                    v-if="errors[`slot_${idx}_dias`]"
                    class="error-text"
                  >{{ errors[`slot_${idx}_dias`] }}</span>
                </div>

                <!-- Times row -->
                <div class="form-grid-2">
                  <div class="field">
                    <label>Hora de Inicio *</label>
                    <input
                      v-model="slot.horaInicio"
                      type="time"
                      :class="{ 'has-error': errors[`slot_${idx}_inicio`] }"
                    />
                    <span
                      v-if="errors[`slot_${idx}_inicio`]"
                      class="error-text"
                    >{{ errors[`slot_${idx}_inicio`] }}</span>
                  </div>

                  <div class="field">
                    <label>Hora de Fin *</label>
                    <input
                      v-model="slot.horaFin"
                      type="time"
                      :class="{ 'has-error': errors[`slot_${idx}_fin`] }"
                    />
                    <span
                      v-if="errors[`slot_${idx}_fin`]"
                      class="error-text"
                    >{{ errors[`slot_${idx}_fin`] }}</span>
                  </div>
                </div>
              </div>

              <!-- Add another slot button & summary -->
              <div class="schedule-batch-footer">
                <button
                  type="button"
                  class="btn-add-slot"
                  @click="addSlotToSchedule(formSchedule)"
                >
                  <Plus :size="16" />
                  <span>Agregar otro bloque de horario (ej. fines de semana u otro turno)</span>
                </button>

                <div class="batch-summary-info">
                  <Sparkles :size="16" />
                  <span>
                    Se programarán <strong>{{ countTotalSessions(formSchedule.slots) }} sesión(es)</strong> en la base de datos en una sola operación.
                  </span>
                </div>
              </div>
            </div>
          </template>

          <!-- REGISTRATION / INSCRIPCION FORM (ENHANCED SEARCHABLE STUDENT PICKER) -->
          <template v-else-if="moduleType === 'registrations'">
            <!-- Student Picker -->
            <div class="field">
              <label>Alumno a Inscribir *</label>

              <!-- Selected Student Card -->
              <div
                v-if="selectedEnrollmentStudent"
                class="selected-person-card"
              >
                <div class="person-avatar">
                  {{ selectedEnrollmentStudent.nombre.charAt(0).toUpperCase() }}
                </div>
                <div class="person-details">
                  <div class="name-row">
                    <strong>{{ selectedEnrollmentStudent.nombre }} {{ selectedEnrollmentStudent.apellidoPaterno }} {{ selectedEnrollmentStudent.apellidoMaterno || '' }}</strong>
                    <span class="tag-badge tag-student">{{ selectedEnrollmentStudent.matricula }}</span>
                  </div>
                  <span>
                    {{ selectedEnrollmentStudent.correo || 'Sin correo registrado' }}
                    <template v-if="selectedEnrollmentStudent.telefono">&bull; Tel: {{ selectedEnrollmentStudent.telefono }}</template>
                  </span>
                </div>
                <button
                  type="button"
                  class="clear-person-btn"
                  @click="clearSelectedStudentForEnrollment"
                >
                  <X :size="14" /> Cambiar Alumno
                </button>
              </div>

              <!-- Searchable Student Picker -->
              <div
                v-else
                class="searchable-picker-container"
              >
                <div class="picker-search-bar">
                  <div class="picker-search-input">
                    <Search :size="15" />
                    <input
                      v-model="enrollmentStudentSearch"
                      type="text"
                      placeholder="Buscar por nombre, apellidos, matrícula o teléfono..."
                    />
                    <button
                      v-if="enrollmentStudentSearch"
                      type="button"
                      class="input-clear"
                      @click="enrollmentStudentSearch = ''"
                    >
                      <X :size="13" />
                    </button>
                  </div>
                </div>

                <div class="picker-results-list">
                  <div
                    v-for="a in filteredStudentsForEnrollment"
                    :key="a.id"
                    class="picker-item"
                    @click="selectStudentForEnrollment(a)"
                  >
                    <div class="person-avatar small">
                      {{ a.nombre.charAt(0).toUpperCase() }}
                    </div>
                    <div class="picker-item-info">
                      <div class="name-row">
                        <strong>{{ a.nombre }} {{ a.apellidoPaterno }} {{ a.apellidoMaterno || '' }}</strong>
                        <span class="tag-badge tag-student">{{ a.matricula }}</span>
                      </div>
                      <small>
                        {{ a.correo || 'Sin correo' }}
                        <template v-if="a.telefono">&bull; {{ a.telefono }}</template>
                      </small>
                    </div>
                    <Check
                      :size="16"
                      class="picker-select-icon"
                    />
                  </div>

                  <div
                    v-if="filteredStudentsForEnrollment.length === 0"
                    class="picker-no-results"
                  >
                    <AlertCircle :size="16" /> No se encontraron alumnos con ese criterio de búsqueda.
                  </div>
                </div>
              </div>
              <span
                v-if="errors.alumnoId"
                class="error-text"
              >{{ errors.alumnoId }}</span>
            </div>

            <!-- Target Group Selector -->
            <div class="field">
              <label>Grupo / Taller Destino *</label>
              <select
                v-model="formEnrollment.grupoId"
                :class="{ 'has-error': errors.grupoId }"
                @change="delete errors.grupoId"
              >
                <option value="">
                  -- Selecciona un grupo de taller cultural --
                </option>
                <option
                  v-for="g in data.grupos"
                  :key="g.id"
                  :value="g.id"
                >
                  {{ g.nombreGrupo }} ({{ g.curso || 'Taller Cultural' }})
                </option>
              </select>
              <span
                v-if="errors.grupoId"
                class="error-text"
              >{{ errors.grupoId }}</span>
            </div>

            <!-- Selected Group Preview Card -->
            <div
              v-if="selectedEnrollmentGroupInfo"
              class="enrollment-group-preview"
            >
              <div class="preview-header">
                <div>
                  <h4 class="preview-title">{{ selectedEnrollmentGroupInfo.nombreCurso }} - {{ selectedEnrollmentGroupInfo.nombreGrupo }}</h4>
                  <span class="preview-cat">Categoría: {{ selectedEnrollmentGroupInfo.nombreCategoria }} &bull; Instructor: {{ selectedEnrollmentGroupInfo.docenteName }}</span>
                </div>
                <span class="preview-capacity-badge">
                  <Users :size="13" /> {{ selectedEnrollmentGroupInfo.enrolledCount }} alumnos activos
                </span>
              </div>

              <!-- Schedules Preview -->
              <div class="preview-schedules">
                <span class="sched-label"><Clock :size="13" /> Horarios de clase asignados:</span>
                <div v-if="selectedEnrollmentGroupInfo.schedules.length" class="sched-badges">
                  <span
                    v-for="(s, sIdx) in selectedEnrollmentGroupInfo.schedules"
                    :key="sIdx"
                    class="sched-pill"
                  >
                    <strong>{{ s.diaTexto }}</strong>: {{ s.rangoHorario }}
                  </span>
                </div>
                <span v-else class="text-muted-small">Este grupo aún no tiene horarios semanales programados.</span>
              </div>

              <!-- Collision Warning Notification -->
              <div class="conflict-check-note">
                <ShieldCheck :size="15" />
                <span>Validación automática: El sistema comprobará que no existan empalmes de horario con otras clases del alumno.</span>
              </div>
            </div>
          </template>

          <!-- ATTENDANCE FORM -->
          <template v-else-if="moduleType === 'attendance'">
            <div class="field">
              <label>Alumno y Taller Inscrito *</label>

              <!-- Selected Student Preview Card -->
              <div
                v-if="selectedAttendanceEnrollment"
                class="selected-person-card"
              >
                <div class="person-avatar">
                  {{ selectedAttendanceEnrollment.alumno ? selectedAttendanceEnrollment.alumno.charAt(0).toUpperCase() : 'A' }}
                </div>
                <div class="person-details">
                  <div class="name-row">
                    <strong>{{ selectedAttendanceEnrollment.alumno }}</strong>
                    <span v-if="selectedAttendanceEnrollment.matricula" class="tag-badge tag-student">{{ selectedAttendanceEnrollment.matricula }}</span>
                  </div>
                  <span>
                    {{ selectedAttendanceEnrollment.grupo }}
                  </span>
                </div>
                <button
                  type="button"
                  class="clear-person-btn"
                  @click="clearSelectedEnrollmentForAttendance"
                >
                  <X :size="14" /> Cambiar Alumno
                </button>
              </div>

              <!-- Searchable Student Picker -->
              <div
                v-else
                class="searchable-picker-container"
              >
                <div class="picker-search-bar">
                  <div class="picker-search-input">
                    <Search :size="15" />
                    <input
                      v-model="attendanceStudentSearch"
                      type="text"
                      placeholder="Buscar por nombre, matrícula o taller cultural..."
                    />
                    <button
                      v-if="attendanceStudentSearch"
                      type="button"
                      class="input-clear"
                      @click="attendanceStudentSearch = ''"
                    >
                      <X :size="13" />
                    </button>
                  </div>
                </div>

                <div class="picker-results-list">
                  <div
                    v-for="item in filteredEnrollmentsForAttendance"
                    :key="item.id"
                    class="picker-item"
                    @click="selectEnrollmentForAttendance(item)"
                  >
                    <div class="person-avatar small">
                      {{ item.alumno ? item.alumno.charAt(0).toUpperCase() : 'A' }}
                    </div>
                    <div class="picker-item-info">
                      <div class="name-row">
                        <strong>{{ item.alumno }}</strong>
                        <span v-if="item.matricula" class="tag-badge tag-student">{{ item.matricula }}</span>
                      </div>
                      <small>{{ item.grupo }}</small>
                    </div>
                    <Check
                      :size="16"
                      class="picker-select-icon"
                    />
                  </div>

                  <div
                    v-if="filteredEnrollmentsForAttendance.length === 0"
                    class="picker-no-results"
                  >
                    <span>No se encontraron inscripciones que coincidan con la búsqueda.</span>
                  </div>
                </div>
              </div>

              <span
                v-if="errors.inscripcionId"
                class="error-text"
              >{{ errors.inscripcionId }}</span>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Fecha de la Clase *</label>
                <input
                  v-model="formAttendance.fecha"
                  type="date"
                  :class="{ 'has-error': errors.fecha }"
                />
                <span
                  v-if="errors.fecha"
                  class="error-text"
                >{{ errors.fecha }}</span>
              </div>
              <div class="field">
                <label>Estado de Asistencia *</label>
                <select v-model="formAttendance.estado">
                  <option value="PRESENTE">
                    PRESENTE
                  </option>
                  <option value="RETARDO">
                    RETARDO
                  </option>
                  <option value="FALTA">
                    FALTA
                  </option>
                  <option value="JUSTIFICADA">
                    JUSTIFICADA
                  </option>
                </select>
              </div>
            </div>
          </template>

          <!-- PAYMENT FORM -->
          <template v-else-if="moduleType === 'payments'">
            <div class="field">
              <label>Alumno y Taller a Cobrar *</label>

              <!-- Selected Student Preview Card -->
              <div
                v-if="selectedPaymentEnrollment"
                class="selected-person-card"
              >
                <div class="person-avatar">
                  {{ selectedPaymentEnrollment.alumno ? selectedPaymentEnrollment.alumno.charAt(0).toUpperCase() : 'A' }}
                </div>
                <div class="person-details">
                  <div class="name-row">
                    <strong>{{ selectedPaymentEnrollment.alumno }}</strong>
                    <span v-if="selectedPaymentEnrollment.matricula" class="tag-badge tag-student">{{ selectedPaymentEnrollment.matricula }}</span>
                  </div>
                  <span>
                    {{ selectedPaymentEnrollment.grupo }}
                  </span>
                </div>
                <button
                  type="button"
                  class="clear-person-btn"
                  @click="clearSelectedEnrollmentForPayment"
                >
                  <X :size="14" /> Cambiar Alumno
                </button>
              </div>

              <!-- Searchable Student Picker -->
              <div
                v-else
                class="searchable-picker-container"
              >
                <div class="picker-search-bar">
                  <div class="picker-search-input">
                    <Search :size="15" />
                    <input
                      v-model="paymentStudentSearch"
                      type="text"
                      placeholder="Buscar por nombre, matrícula o taller cultural..."
                    />
                    <button
                      v-if="paymentStudentSearch"
                      type="button"
                      class="input-clear"
                      @click="paymentStudentSearch = ''"
                    >
                      <X :size="13" />
                    </button>
                  </div>
                </div>

                <div class="picker-results-list">
                  <div
                    v-for="item in filteredEnrollmentsForPayment"
                    :key="item.id"
                    class="picker-item"
                    @click="selectEnrollmentForPayment(item)"
                  >
                    <div class="person-avatar small">
                      {{ item.alumno ? item.alumno.charAt(0).toUpperCase() : 'A' }}
                    </div>
                    <div class="picker-item-info">
                      <div class="name-row">
                        <strong>{{ item.alumno }}</strong>
                        <span v-if="item.matricula" class="tag-badge tag-student">{{ item.matricula }}</span>
                      </div>
                      <small>{{ item.grupo }}</small>
                    </div>
                    <Check
                      :size="16"
                      class="picker-select-icon"
                    />
                  </div>

                  <div
                    v-if="filteredEnrollmentsForPayment.length === 0"
                    class="picker-no-results"
                  >
                    <span>No se encontraron inscripciones que coincidan con la búsqueda.</span>
                  </div>
                </div>
              </div>

              <span
                v-if="errors.inscripcionId"
                class="error-text"
              >{{ errors.inscripcionId }}</span>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Concepto de Pago *</label>
                <select v-model="formPayment.tipoPago" @change="onPaymentConceptChange">
                  <option value="INSCRIPCION">
                    INSCRIPCIÓN ($500.00 MXN)
                  </option>
                  <option value="MENSUALIDAD">
                    MENSUALIDAD ($400.00 MXN)
                  </option>
                  <option value="RECARGO">
                    RECARGO / TRÁMITE ($150.00 MXN)
                  </option>
                </select>
              </div>
              <div class="field">
                <label>Monto a Cobrar ($ MXN) *</label>
                <input
                  v-model="formPayment.monto"
                  type="number"
                  min="1"
                  max="100000"
                  step="0.01"
                  placeholder="500"
                  :class="{ 'has-error': errors.monto }"
                  @keydown="blockInvalidNumberKeys"
                />
                <span
                  v-if="errors.monto"
                  class="error-text"
                >{{ errors.monto }}</span>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Periodo Escolar *</label>
                <input
                  v-model="formPayment.periodo"
                  type="text"
                  maxlength="50"
                  placeholder="Ej. 2026-1, Mayo 2026..."
                  :class="{ 'has-error': errors.periodo }"
                />
                <span
                  v-if="errors.periodo"
                  class="error-text"
                >{{ errors.periodo }}</span>
              </div>
              <div class="field">
                <label>Estado del Pago *</label>
                <select v-model="formPayment.estado">
                  <option value="PAGADO">
                    PAGADO (Completado)
                  </option>
                  <option value="PENDIENTE">
                    PENDIENTE (Por Cobrar)
                  </option>
                  <option value="VENCIDO">
                    VENCIDO (En Mora)
                  </option>
                </select>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="field">
                <label>Fecha de Pago</label>
                <input
                  v-model="formPayment.fechaPago"
                  type="date"
                />
              </div>
              <div class="field">
                <label>Fecha de Vencimiento (Opcional)</label>
                <input
                  v-model="formPayment.fechaVencimiento"
                  type="date"
                />
              </div>
            </div>
          </template>

          <div class="modal-footer">
            <button
              type="button"
              class="cancel-btn"
              @click="showModal = false"
            >
              Cerrar
            </button>
            <button
              v-if="modalMode !== 'credential'"
              type="submit"
              class="primary-button"
              :disabled="isSaving"
            >
              {{ isSaving ? 'Guardando...' : (modalMode === 'add-schedule-group' || moduleType === 'schedules' ? 'Guardar Todos los Horarios' : 'Guardar Registro') }}
            </button>
          </div>
        </form>
      </div>
    </div>
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

.module-content {
  padding: 30px;
  position: relative;
}

/* TOAST */
.toast-alert {
  position: fixed;
  top: 25px;
  right: 25px;
  z-index: 999;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.toast-alert.success {
  background: #dcfce7;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.toast-alert.error {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.toast-close {
  background: transparent;
  border: none;
  cursor: pointer;
  color: inherit;
  display: flex;
  align-items: center;
  margin-left: 8px;
}

.page-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 15px;
}

.title-icon {
  width: 52px;
  height: 52px;
  border-radius: 13px;
  background: #e9edff;
  color: #4051a3;
  display: flex;
  align-items: center;
  justify-content: center;
}

h1 {
  margin: 0;
  color: #202838;
  font-size: 24px;
  font-weight: 700;
}

.page-title p {
  margin: 4px 0 0;
  color: #7d8594;
  font-size: 13px;
}

.header-actions-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.primary-button {
  border: none;
  background: #4051a3;
  color: white;
  padding: 11px 18px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.primary-button:hover {
  background: #313f82;
}

.primary-button.small {
  padding: 7px 13px;
  font-size: 12px;
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
}

.search-box {
  width: 340px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: white;
  border: 1px solid #e2e5eb;
  border-radius: 9px;
  padding: 0 14px;
  color: #8b93a1;
}

.search-box input {
  width: 100%;
  border: none;
  outline: none;
  padding: 12px 0;
  font-size: 13px;
  color: #202838;
}

.secondary-button {
  background: white;
  border: 1px solid #dfe3ea;
  border-radius: 8px;
  padding: 9px 15px;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.secondary-button:hover {
  background: #f9fafb;
}

.secondary-button.small {
  padding: 6px 12px;
  font-size: 12px;
}

.table-panel,
.special-panel,
.coming-panel {
  background: white;
  border: 1px solid #e6e9ef;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.03);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  color: #838b99;
  font-size: 12px;
  font-weight: 600;
  padding: 14px;
  background: #f7f8fa;
}

td {
  padding: 16px 14px;
  border-bottom: 1px solid #edf0f4;
  color: #4e5665;
  font-size: 13px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-cell strong {
  display: block;
  color: #202838;
  font-size: 13px;
}

.cell-sub {
  display: block;
  font-size: 11px;
  color: #8a92a3;
  margin-top: 2px;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #e9edff;
  color: #4051a3;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
}

.status {
  background: #dcfce7;
  color: #15803d;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status.inactive {
  background: #fee2e2;
  color: #b91c1c;
}

.status.pending {
  background: #fef3c7;
  color: #a16207;
}

.action-button {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #667085;
  padding: 6px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-button:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.row-menu {
  position: absolute;
  right: 14px;
  top: 50px;
  z-index: 50;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  min-width: 190px;
  padding: 6px 0;
}

.row-menu button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border: none;
  background: transparent;
  color: #374151;
  font-size: 12px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  width: 100%;
}

.row-menu button:hover {
  background: #f3f4f6;
}

.row-menu button.delete-opt {
  color: #dc2626;
}

.empty-row {
  text-align: center;
  padding: 40px;
  color: #8b92a0;
  font-size: 13px;
}

/* ======================================================== */
/* OPTION A: WORKSHOPS HIERARCHY STYLES                     */
/* ======================================================== */
.workshop-stats-banner {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 22px;
}

.ws-stat-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.ws-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ws-stat-icon.course-icon { background: #e0e7ff; color: #4338ca; }
.ws-stat-icon.group-icon { background: #ede9fe; color: #6d28d9; }
.ws-stat-icon.teacher-icon { background: #fef3c7; color: #b45309; }
.ws-stat-icon.student-icon { background: #dcfce7; color: #15803d; }

.ws-stat-info strong {
  display: block;
  font-size: 20px;
  color: #1e293b;
  line-height: 1.1;
}

.ws-stat-info span {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.ws-toolbar {
  align-items: center;
}

.ws-search {
  width: 420px;
}

.ws-expand-controls {
  display: flex;
  gap: 8px;
}

.workshops-tree-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.course-card-hierarchy {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.03);
  overflow: hidden;
  transition: all 0.2s ease;
}

.course-card-hierarchy.is-expanded {
  border-color: #cbd5e1;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
}

.course-header-bar {
  padding: 18px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  background: #ffffff;
  transition: background-color 0.15s ease;
  user-select: none;
}

.course-header-bar:hover {
  background: #f8fafc;
}

.course-title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.course-avatar-box {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: #4051a3;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.course-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.course-title-row h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.cycle-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.cycle-pill.no-cycle {
  background: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

.course-meta-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.meta-tag {
  font-size: 12px;
  color: #64748b;
}

.meta-tag strong {
  color: #334155;
}

.meta-bullet {
  color: #cbd5e1;
  font-size: 12px;
}

.course-actions-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.primary-action-btn {
  background: #4051a3;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 12.5px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.primary-action-btn:hover {
  background: #313f82;
}

.icon-action-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  border-radius: 8px;
  padding: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.icon-action-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.expand-toggle-btn {
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.expand-toggle-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
}

/* Course Expanded Body */
.course-groups-body {
  padding: 20px 22px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.group-card-item {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.group-card-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.group-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.group-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.group-title-row h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.category-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10.5px;
  font-weight: 600;
}

.group-id-sub {
  font-size: 10.5px;
  color: #94a3b8;
  display: block;
  margin-top: 2px;
}

.group-section-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sub-action-link {
  background: transparent;
  border: none;
  color: #4051a3;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.sub-action-link:hover {
  text-decoration: underline;
}

.teacher-assigned-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #fdf8e6;
  border: 1px solid #fde68a;
  border-radius: 8px;
}

.teacher-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #d97706;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: bold;
  flex-shrink: 0;
}

.teacher-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.teacher-info strong {
  font-size: 12px;
  color: #78350f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.teacher-info small {
  font-size: 10.5px;
  color: #92400e;
}

.no-teacher-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #fef2f2;
  border: 1px dashed #fca5a5;
  border-radius: 8px;
  font-size: 11px;
  color: #b91c1c;
}

.schedules-pill-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.schedule-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 11px;
  color: #334155;
}

.schedule-tag strong {
  color: #0f172a;
}

.no-schedules-box {
  padding: 6px 10px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  font-size: 11px;
  color: #94a3b8;
}

.group-card-footer {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.students-count-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: #475569;
}

.students-count-tag strong {
  color: #1e293b;
}

.empty-course-groups {
  padding: 30px 20px;
  text-align: center;
  background: white;
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #64748b;
}

.empty-course-groups h4 {
  margin: 10px 0 4px;
  color: #1e293b;
  font-size: 15px;
}

.empty-course-groups p {
  margin: 0 0 16px;
  font-size: 12.5px;
  max-width: 460px;
}

/* Form Steps & Banners in Modals */
.form-step-banner {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 10px 14px;
  background: #f8fafc;
  border-left: 3px solid #4051a3;
  border-radius: 0 8px 8px 0;
  margin: 18px 0 12px;
}

.step-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #4051a3;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: bold;
  flex-shrink: 0;
}

.form-step-banner strong {
  display: block;
  font-size: 12.5px;
  color: #1e293b;
}

.form-step-banner p {
  margin: 2px 0 0;
  font-size: 11px;
  color: #64748b;
}

.checkbox-field-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
}

.custom-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 12.5px;
  color: #1e293b;
  user-select: none;
}

.custom-checkbox input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.schedule-subform-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  margin-top: 10px;
}

.subform-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 700;
  color: #4051a3;
  margin-bottom: 8px;
}

.context-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  color: #1d4ed8;
  font-size: 12.5px;
  margin-bottom: 16px;
}

.form-grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}

/* BATCH SCHEDULE STYLES */
.schedule-batch-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 10px;
}

.schedule-slot-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s ease;
}

.schedule-slot-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.slot-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.slot-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #e0e7ff;
  color: #4338ca;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.btn-remove-slot {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: transparent;
  border: 1px solid #fecaca;
  color: #ef4444;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-remove-slot:hover {
  background: #fef2f2;
  border-color: #f87171;
}

.slot-presets {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.preset-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.preset-chip {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #334155;
  padding: 3px 9px;
  border-radius: 14px;
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preset-chip:hover {
  background: #e2e8f0;
  color: #0f172a;
  border-color: #cbd5e1;
}

.days-pills-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-top: 6px;
}

.day-pill-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  border-radius: 8px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  user-select: none;
}

.day-pill-btn:hover {
  border-color: #94a3b8;
  background: #f8fafc;
}

.day-pill-btn.active {
  background: #4f46e5;
  border-color: #4338ca;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.35);
}

.day-pill-btn .pill-day-label {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
}

.day-pill-btn .pill-day-full {
  font-size: 10px;
  font-weight: 500;
  opacity: 0.8;
}

.day-pill-btn.active .pill-day-full {
  opacity: 0.95;
  color: #e0e7ff;
}

.pill-check-icon {
  position: absolute;
  top: 3px;
  right: 3px;
}

.schedule-batch-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 6px;
}

.btn-add-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px;
  border: 1.5px dashed #cbd5e1;
  border-radius: 10px;
  background: #ffffff;
  color: #4f46e5;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-slot:hover {
  border-color: #6366f1;
  background: #f5f7ff;
  color: #4338ca;
}

.batch-summary-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  color: #065f46;
  font-size: 12.5px;
}

/* REGISTRATIONS (INSCRIPCIONES) ENHANCED STYLES */
.registrations-stats-banner {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.rg-stat-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.rg-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.rg-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rg-stat-icon.blue {
  background: #eff6ff;
  color: #2563eb;
}

.rg-stat-icon.indigo {
  background: #eef2ff;
  color: #4f46e5;
}

.rg-stat-icon.green {
  background: #ecfdf5;
  color: #10b981;
}

.rg-stat-icon.gray {
  background: #f1f5f9;
  color: #64748b;
}

.rg-stat-content {
  display: flex;
  flex-direction: column;
}

.rg-stat-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.rg-stat-value {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.rg-stat-value.text-green {
  color: #16a34a;
}

.rg-stat-value.text-gray {
  color: #64748b;
}

.registration-controls-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  background: white;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.rg-search-box {
  flex: 1;
  min-width: 280px;
  position: relative;
}

.rg-search-box input {
  width: 100%;
}

.rg-status-tabs {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 8px;
  gap: 4px;
}

.rg-tab-btn {
  border: none;
  background: transparent;
  color: #64748b;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.rg-tab-btn:hover {
  color: #1e293b;
}

.rg-tab-btn.active {
  background: white;
  color: #4338ca;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.rg-filter-select-wrapper select {
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: white;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  outline: none;
}

.rg-stat-icon.amber {
  background: #fef3c7;
  color: #d97706;
}

.rg-stat-icon.red {
  background: #fee2e2;
  color: #dc2626;
}

.rg-stat-value.text-amber {
  color: #d97706;
}

.rg-stat-value.text-red {
  color: #dc2626;
}

.rg-filter-select-wrapper select:focus {
  border-color: #6366f1;
}

.rg-filter-date-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  padding: 4px 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.rg-date-input {
  border: none;
  background: transparent;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  font-family: inherit;
}

.btn-clear-date {
  border: none;
  background: #e2e8f0;
  color: #64748b;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
  transition: all 0.2s;
}

.btn-clear-date:hover {
  background: #cbd5e1;
  color: #0f172a;
}

.btn-today-shortcut {
  border: 1px solid #cbd5e1;
  background: white;
  color: #475569;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-today-shortcut:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-today-shortcut.active {
  background: #4338ca;
  color: white;
  border-color: #4338ca;
}

/* ENROLLMENT GROUP PREVIEW */
.enrollment-group-preview {
  margin-top: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.preview-title {
  margin: 0 0 2px;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.preview-cat {
  font-size: 11.5px;
  color: #64748b;
}

.preview-capacity-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.preview-schedules {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sched-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  color: #475569;
}

.sched-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sched-pill {
  display: inline-block;
  background: white;
  border: 1px solid #cbd5e1;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  color: #334155;
}

.sched-pill strong {
  color: #4338ca;
}

.conflict-check-note {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  color: #166534;
  font-size: 11px;
  font-weight: 500;
}

/* SPECIAL VIEW */
.special-panel h2 {
  margin-top: 0;
  color: #202838;
  font-size: 20px;
}

.special-panel > p {
  color: #7d8594;
  font-size: 13px;
  margin-top: 4px;
}

.special-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 25px;
}

.special-card {
  border: 1px solid #e7e9ef;
  border-radius: 12px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  color: #4051a3;
}

.special-card h3 {
  margin: 12px 0 6px;
  color: #202838;
  font-size: 16px;
}

.special-card p {
  color: #7d8594;
  font-size: 13px;
  margin-bottom: 16px;
  flex: 1;
}

.special-card button {
  align-self: flex-start;
  border: none;
  background: #eef1f8;
  color: #4051a3;
  padding: 8px 14px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.special-card button:hover {
  background: #dfe5f5;
}

/* MONETIZATION BREAKDOWN */
.monetization-breakdown-box {
  margin-top: 30px;
  padding-top: 25px;
  border-top: 1px solid #edf0f4;
}

.monetization-breakdown-box h3 {
  margin: 0 0 16px;
  color: #202838;
  font-size: 17px;
}

.concepts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.concept-card {
  padding: 18px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.concept-card span {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

.concept-card strong {
  font-size: 20px;
  color: #1e293b;
}

.concept-card small {
  font-size: 11px;
  color: #94a3b8;
}

/* REPORT TOOL */
.report-interactive-box {
  margin-top: 30px;
  padding-top: 25px;
  border-top: 1px solid #edf0f4;
}

.report-interactive-box h3 {
  margin: 0;
  color: #202838;
  font-size: 17px;
}

.report-interactive-box p {
  color: #7d8594;
  font-size: 13px;
  margin: 4px 0 16px;
}

.report-controls {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.date-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.date-field label {
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
}

.date-field input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  font-size: 13px;
  color: #1f2937;
}

.report-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 24px;
}

.metric-box {
  padding: 18px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: 1px solid #e5e7eb;
}

.metric-box span {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

.metric-box strong {
  font-size: 22px;
  font-weight: 700;
}

.metric-box.total { background: #f8fafc; color: #1e293b; }
.metric-box.present { background: #f0fdf4; color: #15803d; }
.metric-box.late { background: #fefce8; color: #a16207; }
.metric-box.absent { background: #fef2f2; color: #dc2626; }

/* COMING PANEL */
.coming-panel {
  min-height: 350px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px;
}

.coming-panel > svg {
  color: #4051a3;
  margin-bottom: 16px;
}

.coming-panel h2 {
  margin: 0 0 8px;
  color: #202838;
}

.coming-panel p {
  max-width: 500px;
  color: #7d8594;
  font-size: 14px;
  margin-bottom: 24px;
}

/* MODAL */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: 20px;
}

.modal-card {
  background: white;
  border-radius: 14px;
  width: min(640px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.modal-card.modal-card-wide {
  width: min(720px, 100%);
}

.modal-header {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #edf0f4;
}

.modal-header h3 {
  margin: 0;
  color: #202838;
  font-size: 17px;
  font-weight: 700;
}

.close-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #64748b;
  padding: 4px;
  border-radius: 6px;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.modal-body {
  padding: 24px;
}

.field {
  margin-bottom: 16px;
}

.field label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.field input,
.field select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 13px;
  color: #1f2937;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field input:focus,
.field select:focus {
  border-color: #4051a3;
  box-shadow: 0 0 0 3px rgba(64, 81, 163, 0.1);
}

.field input.has-error,
.field select.has-error {
  border-color: #ef4444;
  background-color: #fffaf0;
}

.error-text {
  display: block;
  color: #dc2626;
  font-size: 11px;
  margin-top: 4px;
  font-weight: 500;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.modal-help-text {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 16px;
  line-height: 1.45;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #edf0f4;
}

.cancel-btn {
  padding: 10px 18px;
  border: 1px solid #d1d5db;
  background: white;
  color: #4b5563;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.cancel-btn:hover {
  background: #f9fafb;
}

/* SEARCHABLE PERSON PICKER STYLES */
.selected-person-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0fdf4;
  border: 1.5px solid #86efac;
  border-radius: 10px;
}

.person-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #4051a3;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}

.person-avatar.small {
  width: 32px;
  height: 32px;
  font-size: 12px;
}

.person-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.person-details strong {
  font-size: 13px;
  color: #0f172a;
}

.person-details span {
  font-size: 11px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 6px;
}

.clear-person-btn {
  border: 1px solid #cbd5e1;
  background: white;
  color: #64748b;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.clear-person-btn:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fca5a5;
}

.searchable-picker-container {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  overflow: hidden;
  background: white;
}

.picker-search-bar {
  padding: 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.picker-search-input {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0 10px;
  color: #64748b;
}

.picker-search-input input {
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  padding: 8px 0 !important;
  font-size: 12.5px !important;
  color: #1e293b !important;
  background: transparent !important;
}

.input-clear {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #94a3b8;
  padding: 2px;
}

.input-clear:hover {
  color: #334155;
}

.filter-tabs {
  display: flex;
  gap: 6px;
}

.tab-btn {
  border: none;
  background: #e2e8f0;
  color: #475569;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.tab-btn.active {
  background: #4051a3;
  color: white;
}

.picker-results-list {
  max-height: 180px;
  overflow-y: auto;
}

.picker-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.picker-item:hover {
  background: #f8fafc;
}

.picker-item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-row strong {
  font-size: 13px;
  color: #1e293b;
}

.picker-item-info small {
  font-size: 11px;
  color: #64748b;
}

.picker-select-icon {
  color: transparent;
}

.picker-item:hover .picker-select-icon {
  color: #4051a3;
}

.tag-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
}

.tag-student {
  background: #e0e7ff;
  color: #3730a3;
}

.tag-teacher {
  background: #fef3c7;
  color: #92400e;
}

.picker-no-results {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 12px;
}

/* USER CREATION TYPE TOGGLE */
.user-creation-type-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 18px;
}

.type-toggle-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
  color: #64748b;
}

.type-toggle-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.type-toggle-btn.active {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #1d4ed8;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.12);
}

.toggle-btn-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toggle-btn-text strong {
  font-size: 13px;
  color: #1e293b;
}

.type-toggle-btn.active .toggle-btn-text strong {
  color: #1e40af;
}

.toggle-btn-text small {
  font-size: 11px;
  color: #64748b;
}

.admin-person-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 8px;
}

.user-helper-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  color: #166534;
  font-size: 12px;
  line-height: 1.4;
}

.user-helper-banner .helper-icon {
  flex-shrink: 0;
  color: #16a34a;
}

/* CREDENTIAL PREVIEW */
.credential-preview {
  border: 2px solid #4051a3;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;
  margin-bottom: 16px;
}

.cred-header {
  background: #4051a3;
  color: white;
  padding: 14px 18px;
  text-align: center;
}

.cred-header h4 {
  margin: 0;
  font-size: 15px;
  letter-spacing: 0.5px;
}

.cred-header span {
  font-size: 11px;
  opacity: 0.9;
}

.cred-body {
  display: flex;
  padding: 20px;
  gap: 20px;
  align-items: center;
}

.cred-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.qr-box {
  padding: 10px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #1e293b;
}

.cred-info strong {
  display: block;
  font-size: 16px;
  color: #0f172a;
  margin-bottom: 8px;
}

.cred-info p {
  margin: 4px 0;
  font-size: 12px;
  color: #475569;
}

.cred-badge {
  display: inline-block;
  background: #dcfce7;
  color: #15803d;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 700;
}

.cred-actions {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

@media (max-width: 1100px) {
  .special-grid,
  .concepts-grid,
  .workshop-stats-banner {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .module-content {
    padding: 20px 15px 30px;
  }

  .page-title,
  .toolbar {
    align-items: stretch;
    gap: 12px;
    flex-direction: column;
  }

  .header-actions-group {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box,
  .ws-search {
    width: 100%;
  }

  .secondary-button,
  .primary-button {
    justify-content: center;
  }

  .table-panel {
    overflow-x: auto;
    padding: 12px;
  }

  .table-panel table {
    min-width: 650px;
  }

  .special-panel,
  .coming-panel {
    padding: 18px;
  }

  .special-grid,
  .concepts-grid,
  .report-metrics-grid,
  .workshop-stats-banner {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .form-grid-2,
  .form-grid-3 {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .days-pills-row {
    grid-template-columns: repeat(4, 1fr);
  }

  .course-header-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .course-actions-section {
    width: 100%;
    justify-content: space-between;
  }
}

/* ======================================================== */
/* SCHEDULES (HORARIOS) EXPERIENCE STYLES                   */
/* ======================================================== */
.schedules-stats-banner {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.sc-stat-item {
  background: #ffffff;
  border: 1px solid #e9ecf5;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.sc-stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sc-stat-icon.calendar-icon { background: #eff6ff; color: #3b82f6; }
.sc-stat-icon.group-icon { background: #f5f3ff; color: #8b5cf6; }
.sc-stat-icon.hours-icon { background: #ecfdf5; color: #10b981; }
.sc-stat-icon.peak-icon { background: #fffbeb; color: #f59e0b; }

.sc-stat-info strong {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  display: block;
}

.sc-stat-info span {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.schedules-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.schedules-filters-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}

.filter-select-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0 12px;
  height: 42px;
}

.filter-select-wrapper .select-icon {
  color: #64748b;
  margin-right: 8px;
  flex-shrink: 0;
}

.filter-select {
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  outline: none;
  cursor: pointer;
  padding-right: 8px;
}

.schedules-controls-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.view-mode-toggle {
  display: flex;
  background: #e2e8f0;
  border-radius: 10px;
  padding: 4px;
  gap: 4px;
}

.mode-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 14px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn.active {
  background: #ffffff;
  color: #4051a3;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

/* 1. TIMETABLE GRID */
.calendar-timetable-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(185px, 1fr));
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 20px;
}

.day-column {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e9ecf5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  min-height: 480px;
  transition: all 0.2s;
}

.day-column.is-today {
  border-color: #6366f1;
  box-shadow: 0 4px 18px rgba(99, 102, 241, 0.12);
}

.day-column-header {
  padding: 14px 14px 10px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.day-column.is-today .day-column-header {
  background: #eff6ff;
  border-bottom-color: #dbeafe;
}

.day-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.day-short {
  font-size: 11px;
  font-weight: 800;
  background: #e2e8f0;
  color: #475569;
  padding: 2px 6px;
  border-radius: 6px;
  text-transform: uppercase;
}

.day-column.is-today .day-short {
  background: #3b82f6;
  color: #ffffff;
}

.day-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.today-badge {
  font-size: 10px;
  font-weight: 800;
  background: #dbeafe;
  color: #1d4ed8;
  padding: 2px 6px;
  border-radius: 4px;
}

.day-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sessions-counter {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}

.quick-add-day-btn {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-add-day-btn:hover {
  background: #4051a3;
  color: #ffffff;
  border-color: #4051a3;
}

.day-sessions-container {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.session-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid var(--accent-color, #6366f1);
  border-radius: 10px;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  transition: all 0.2s;
}

.session-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.07);
  border-color: #cbd5e1;
  border-left-color: var(--accent-color, #6366f1);
}

.session-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 4px;
}

.time-range-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
  color: #1e293b;
  background: #f1f5f9;
  padding: 3px 7px;
  border-radius: 6px;
}

.duration-pill {
  font-size: 10px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 2px 5px;
  border-radius: 4px;
}

.session-course-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.25;
  margin-top: 2px;
}

.session-group-name {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}

.session-card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px dashed #f1f5f9;
}

.session-teacher {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 110px;
}

.session-delete-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 3px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.session-delete-btn:hover {
  color: #ef4444;
  background: #fee2e2;
}

.empty-day-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 8px;
  padding: 30px 10px;
  color: #94a3b8;
  flex: 1;
}

.empty-day-icon {
  color: #cbd5e1;
}

.empty-day-state span {
  font-size: 12px;
  color: #94a3b8;
}

.empty-day-add-link {
  font-size: 11px;
  font-weight: 600;
  color: #4051a3;
  background: transparent;
  border: none;
  cursor: pointer;
}

.empty-day-add-link:hover {
  text-decoration: underline;
}

/* 2. GROUP SCHEDULE CARDS */
.groups-schedules-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-schedule-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e9ecf5;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.group-header-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.group-course-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #4051a3;
  background: #eef2ff;
  padding: 3px 10px;
  border-radius: 6px;
  width: fit-content;
}

.group-schedule-card h4 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
}

.group-meta-chips {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 2px;
}

.meta-chip {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
}

.meta-chip.category { background: #f1f5f9; color: #475569; }
.meta-chip.teacher { background: #ecfdf5; color: #047857; }
.meta-chip.students { background: #f5f3ff; color: #6d28d9; }

.add-schedule-to-group-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  color: #4051a3;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.add-schedule-to-group-btn:hover {
  background: #4051a3;
  color: #ffffff;
}

.group-schedules-body {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-slots-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.schedule-slots-title strong {
  color: #1e293b;
}

.schedule-slots-flex {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.schedule-slot-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.03);
}

.slot-day-badge {
  font-size: 11px;
  font-weight: 700;
  background: #e0e7ff;
  color: #3730a3;
  padding: 2px 7px;
  border-radius: 5px;
}

.slot-time-range {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.slot-remove-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slot-remove-btn:hover {
  color: #ef4444;
  background: #fee2e2;
}

.group-no-schedules-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fde68a;
  padding: 10px 14px;
  border-radius: 8px;
}

.assign-now-link {
  font-weight: 700;
  color: #b45309;
  background: transparent;
  border: none;
  text-decoration: underline;
  cursor: pointer;
  margin-left: auto;
}

/* 3. TABLE BADGES & CELLS */
.course-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.group-badge-cell {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 6px;
}

.day-tag-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 6px;
  display: inline-block;
}

.day-tag-badge.monday { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.day-tag-badge.tuesday { background: #f5f3ff; color: #6d28d9; border: 1px solid #ddd6fe; }
.day-tag-badge.wednesday { background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
.day-tag-badge.thursday { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }
.day-tag-badge.friday { background: #fdf2f8; color: #be185d; border: 1px solid #fbcfe8; }
.day-tag-badge.saturday { background: #ecfeff; color: #0e7490; border: 1px solid #a5f3fc; }
.day-tag-badge.sunday { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; }

.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.duration-cell {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.action-button-danger {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-button-danger:hover {
  color: #ef4444;
  background: #fee2e2;
}
</style>
