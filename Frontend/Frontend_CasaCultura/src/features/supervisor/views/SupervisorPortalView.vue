<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import DashboardLayout from '../../../components/DashboardLayout.vue'
import { clearStoredSession } from '../../../services/apiService'
import {
  createStudent,
  enrollStudent,
  generateStudentUser,
  getSupervisorData,
  updateStudent
} from '../../../services/supervisorService'
import { useRouter } from 'vue-router'
import {
  LayoutDashboard,
  GraduationCap,
  ClipboardList,
  Users,
  BookOpen,
  Plus,
  Search,
  Edit3,
  KeyRound,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  X,
  Phone,
  Mail,
  Calendar,
  MapPin,
  Sparkles,
  ShieldCheck,
  ArrowRight,
  Clock,
  Filter,
  RotateCcw,
  Copy,
  Check,
  UserCheck,
  Camera,
  Trash2,
  User
} from 'lucide-vue-next'
import CameraCaptureModal from '../../../components/common/CameraCaptureModal.vue'

const router = useRouter()
const activeView = ref('resumen')
const data = reactive({ alumnos: [], grupos: [], inscripciones: [] })
const isLoading = ref(true)
const errorMessage = ref('')
const successMessage = ref('')
const formModalError = ref('')
const showStudentForm = ref(false)
const showCameraModal = ref(false)

function onPhotoSaved(eventData) {
  studentForm.fotoUrl = eventData.url
  successMessage.value = 'Fotografía del alumno capturada y asignada con éxito.'
}
const editingStudent = ref(null)
const selectedStudent = ref('')
const selectedGroup = ref('')
const studentSearch = ref('')
const isSaving = ref(false)
const isGeneratingAccess = ref(false)
const generatedCredentials = ref(null)
const credentialStudent = ref(null)
const initialPassword = ref('')
const showInitialPassword = ref(false)
const copied = ref(false)

const savedStudentCredentials = reactive(
  JSON.parse(localStorage.getItem('casa-cultura-student-credentials') || '{}')
)
const currentUser = JSON.parse(
  localStorage.getItem('casa-cultura-user') || sessionStorage.getItem('casa-cultura-user') || '{}'
)

// Supervisor display name
const supervisorName = computed(() => {
  if (currentUser.persona?.nombre) {
    return `${currentUser.persona.nombre} ${currentUser.persona.apellidoPaterno || ''}`.trim()
  }
  return currentUser.nombreUsuario || 'Supervisor'
})

// Current date formatted
const formattedDate = computed(() => {
  return new Date().toLocaleDateString('es-MX', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
})

// Nav items for sidebar
const navItems = [
  { id: 'resumen', label: 'Mi Dashboard', icon: LayoutDashboard },
  { id: 'alumnos', label: 'Gestión de Alumnos', icon: GraduationCap },
  { id: 'inscripciones', label: 'Asignar a Cursos', icon: ClipboardList },
]

// ==========================================
// FORM DATA & VALIDATION RULES
// ==========================================
const studentForm = reactive({
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  fechaNacimiento: '',
  telefono: '',
  direccion: '',
  correo: '',
  fotoUrl: '',
  matricula: ''
})

const touched = reactive({
  nombre: false,
  apellidoPaterno: false,
  apellidoMaterno: false,
  fechaNacimiento: false,
  telefono: false,
  direccion: false,
  correo: false,
  matricula: false
})

// Date limits (3 years old minimum, 100 years maximum)
const maxBirthDate = computed(() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() - 3)
  return d.toISOString().split('T')[0]
})

const minBirthDate = computed(() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() - 100)
  return d.toISOString().split('T')[0]
})

// Validation rules evaluation
const errors = computed(() => {
  const errs = {}

  // 1. Nombre
  const n = studentForm.nombre.trim()
  if (!n) {
    errs.nombre = 'El nombre es obligatorio.'
  } else if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(n)) {
    errs.nombre = 'Solo se permiten letras y acentos (sin números ni símbolos).'
  } else if (n.length < 2) {
    errs.nombre = 'Debe tener al menos 2 letras.'
  } else if (n.length > 80) {
    errs.nombre = 'Máximo 80 caracteres.'
  }

  // 2. Apellido Paterno
  const ap = studentForm.apellidoPaterno.trim()
  if (!ap) {
    errs.apellidoPaterno = 'El apellido paterno es obligatorio.'
  } else if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(ap)) {
    errs.apellidoPaterno = 'Solo se permiten letras y acentos.'
  } else if (ap.length < 2) {
    errs.apellidoPaterno = 'Debe tener al menos 2 letras.'
  } else if (ap.length > 80) {
    errs.apellidoPaterno = 'Máximo 80 caracteres.'
  }

  // 3. Apellido Materno (opcional)
  const am = studentForm.apellidoMaterno.trim()
  if (am) {
    if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(am)) {
      errs.apellidoMaterno = 'Solo se permiten letras y acentos.'
    } else if (am.length > 80) {
      errs.apellidoMaterno = 'Máximo 80 caracteres.'
    }
  }

  // 4. Fecha de Nacimiento
  if (!studentForm.fechaNacimiento) {
    errs.fechaNacimiento = 'La fecha de nacimiento es obligatoria.'
  } else {
    const selectedDate = new Date(studentForm.fechaNacimiento)
    const now = new Date()
    const age = (now - selectedDate) / (1000 * 60 * 60 * 24 * 365.25)
    if (selectedDate > now) {
      errs.fechaNacimiento = 'La fecha no puede ser en el futuro.'
    } else if (age < 3) {
      errs.fechaNacimiento = 'El alumno debe tener al menos 3 años cumplidos.'
    } else if (age > 100) {
      errs.fechaNacimiento = 'Ingresa una fecha de nacimiento válida.'
    }
  }

  // 5. Teléfono
  const tel = studentForm.telefono.replace(/\D/g, '')
  if (!tel) {
    errs.telefono = 'El teléfono es obligatorio.'
  } else if (tel.length !== 10) {
    errs.telefono = `Debe tener exactamente 10 dígitos (actualmente: ${tel.length}).`
  }

  // 6. Correo electrónico (Obligatorio)
  const em = studentForm.correo.trim()
  if (!em) {
    errs.correo = 'El correo electrónico es obligatorio.'
  } else if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(em)) {
    errs.correo = 'Ingresa un correo electrónico válido (ej. alumno@gmail.com).'
  } else if (em.length > 160) {
    errs.correo = 'Máximo 160 caracteres.'
  }

  // 7. Matrícula (Obligatorio)
  const mat = studentForm.matricula.trim()
  if (!mat) {
    errs.matricula = 'La matrícula institucional es obligatoria.'
  } else if (mat.length < 3) {
    errs.matricula = 'La matrícula debe tener al menos 3 caracteres.'
  } else if (mat.length > 30) {
    errs.matricula = 'Máximo 30 caracteres.'
  }

  // 8. Dirección / Domicilio (Obligatorio)
  const dir = studentForm.direccion.trim()
  if (!dir) {
    errs.direccion = 'El domicilio / dirección es obligatorio.'
  } else if (dir.length < 5) {
    errs.direccion = 'Ingresa un domicilio completo (calle, número o referencia, mín. 5 letras).'
  } else if (dir.length > 255) {
    errs.direccion = 'Máximo 255 caracteres.'
  }

  return errs
})

const isFormValid = computed(() => {
  const nom = (studentForm.nombre || '').trim()
  const ap = (studentForm.apellidoPaterno || '').trim()
  const fn = studentForm.fechaNacimiento
  const tel = (studentForm.telefono || '').replace(/\D/g, '')
  const em = (studentForm.correo || '').trim()
  const dir = (studentForm.direccion || '').trim()
  const mat = (studentForm.matricula || '').trim()

  const hasEssentials = Boolean(
    nom.length >= 2 &&
    ap.length >= 2 &&
    fn &&
    tel.length === 10 &&
    em.length > 0 &&
    dir.length >= 5 &&
    mat.length >= 3
  )
  return hasEssentials && Object.keys(errors.value).length === 0
})

// Input formatters & filters
function onNameInput(field, e) {
  studentForm[field] = e.target.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '')
  touched[field] = true
}

function onPhoneInput(e) {
  studentForm.telefono = e.target.value.replace(/\D/g, '').slice(0, 10)
  touched.telefono = true
}

function onMatriculaInput(e) {
  studentForm.matricula = e.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, '')
  touched.matricula = true
}

function autoSuggestMatricula() {
  const year = new Date().getFullYear()
  const count = (data.alumnos.length + 1).toString().padStart(3, '0')
  studentForm.matricula = `ALU-${year}-${count}`
  touched.matricula = true
}

// Stats for dashboard
const stats = computed(() => [
  {
    value: data.alumnos.length,
    label: 'Alumnos registrados',
    subtitle: 'Padrón escolar activo',
    icon: Users,
    gradient: 'from-blue-600 to-indigo-600',
    color: '#4f46e5'
  },
  {
    value: data.grupos.length,
    label: 'Cursos y talleres',
    subtitle: 'Grupos con apertura',
    icon: BookOpen,
    gradient: 'from-emerald-600 to-teal-600',
    color: '#059669'
  },
  {
    value: data.inscripciones.length,
    label: 'Inscripciones activas',
    subtitle: 'Asignaciones ciclo actual',
    icon: ClipboardList,
    gradient: 'from-amber-600 to-orange-600',
    color: '#d97706'
  }
])

// Filters & Search for student table
const studentFilter = ref('ALL') // 'ALL' | 'ACTIVE' | 'INACTIVE'
const filteredStudents = computed(() => {
  let list = data.alumnos
  if (studentFilter.value === 'ACTIVE') {
    list = list.filter((s) => s.estado === 'ACTIVO' || !s.estado)
  } else if (studentFilter.value === 'INACTIVE') {
    list = list.filter((s) => s.estado === 'INACTIVO')
  }

  const q = studentSearch.value.trim().toLowerCase()
  if (!q) return list

  return list.filter((student) =>
    `${student.nombre} ${student.apellidoPaterno} ${student.apellidoMaterno || ''} ${student.matricula} ${student.correo || ''} ${student.telefono || ''}`
      .toLowerCase()
      .includes(q)
  )
})

// Check if student is already enrolled in selected group
const isAlreadyEnrolled = computed(() => {
  if (!selectedStudent.value || !selectedGroup.value) return false
  const sId = Number(selectedStudent.value)
  const gId = Number(selectedGroup.value)
  return data.inscripciones.some(
    (ins) => Number(ins.alumnoId) === sId && Number(ins.grupoId) === gId
  )
})

onMounted(loadData)

async function loadData() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const res = await getSupervisorData()
    data.alumnos = res.alumnos || []
    data.grupos = res.grupos || []
    data.inscripciones = res.inscripciones || []
  } catch (error) {
    errorMessage.value = error.message
    if (error.message && (error.message.includes('401') || error.message.includes('403'))) {
      clearStoredSession()
      router.push('/')
    }
  } finally {
    isLoading.value = false
  }
}

function resetForm() {
  Object.keys(studentForm).forEach((key) => { studentForm[key] = '' })
  Object.keys(touched).forEach((key) => { touched[key] = false })
  editingStudent.value = null
  errorMessage.value = ''
  formModalError.value = ''
}

function editStudent(student) {
  resetForm()
  Object.assign(studentForm, {
    nombre: student.nombre || '',
    apellidoPaterno: student.apellidoPaterno || '',
    apellidoMaterno: student.apellidoMaterno || '',
    fechaNacimiento: student.fechaNacimiento || '',
    telefono: student.telefono || '',
    direccion: student.direccion || '',
    correo: student.correo || '',
    fotoUrl: student.fotoUrl || '',
    matricula: student.matricula || '',
  })
  editingStudent.value = student
  formModalError.value = ''
  // Touch fields so existing missing values (like domicilio) immediately display warnings
  Object.keys(touched).forEach((key) => { touched[key] = true })
  showStudentForm.value = true
}

async function saveStudent() {
  // Mark all fields as touched to display errors if any
  Object.keys(touched).forEach((key) => { touched[key] = true })

  if (!isFormValid.value) {
    const missing = []
    if (!studentForm.nombre?.trim() || errors.value.nombre) missing.push('Nombre(s)')
    if (!studentForm.apellidoPaterno?.trim() || errors.value.apellidoPaterno) missing.push('Apellido Paterno')
    if (!studentForm.fechaNacimiento || errors.value.fechaNacimiento) missing.push('Fecha de Nacimiento')
    if (!studentForm.telefono || studentForm.telefono.replace(/\D/g, '').length !== 10 || errors.value.telefono) missing.push('Teléfono (10 dígitos)')
    if (!studentForm.correo?.trim() || errors.value.correo) missing.push('Correo electrónico')
    if (!studentForm.direccion?.trim() || studentForm.direccion.trim().length < 5 || errors.value.direccion) missing.push('Dirección / Domicilio')
    if (!studentForm.matricula?.trim() || errors.value.matricula) missing.push('Matrícula institucional')

    formModalError.value = `No se puede registrar: faltan datos esenciales obligatorios (${missing.join(', ')}).`
    errorMessage.value = formModalError.value
    return
  }

  formModalError.value = ''
  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const payload = {
      nombre: studentForm.nombre.trim(),
      apellidoPaterno: studentForm.apellidoPaterno.trim(),
      apellidoMaterno: studentForm.apellidoMaterno?.trim() || null,
      fechaNacimiento: studentForm.fechaNacimiento,
      telefono: studentForm.telefono.replace(/\D/g, ''),
      direccion: studentForm.direccion.trim(),
      correo: studentForm.correo.trim(),
      fotoUrl: studentForm.fotoUrl?.trim() || null,
      matricula: studentForm.matricula.trim().toUpperCase()
    }

    if (editingStudent.value) {
      await updateStudent(editingStudent.value.id, payload)
      successMessage.value = `Alumno "${payload.nombre} ${payload.apellidoPaterno}" actualizado correctamente.`
    } else {
      const response = await createStudent(payload)
      successMessage.value = response?.qrCredential
        ? `Alumno creado exitosamente. Matrícula: ${payload.matricula} · Credencial QR generada.`
        : `Alumno "${payload.nombre} ${payload.apellidoPaterno}" registrado correctamente.`
    }

    showStudentForm.value = false
    resetForm()
    await loadData()
  } catch (error) {
    formModalError.value = error.message || 'Error al guardar el alumno en el servidor.'
    errorMessage.value = formModalError.value
  } finally {
    isSaving.value = false
  }
}

async function saveEnrollment() {
  if (!selectedStudent.value || !selectedGroup.value) return
  if (isAlreadyEnrolled.value) {
    errorMessage.value = 'Este alumno ya está inscrito en el grupo seleccionado.'
    return
  }

  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await enrollStudent(Number(selectedStudent.value), Number(selectedGroup.value))
    successMessage.value = 'Inscripción realizada con éxito.'
    selectedStudent.value = ''
    selectedGroup.value = ''
    studentSearch.value = ''
    await loadData()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSaving.value = false
  }
}

function selectStudent(student) {
  selectedStudent.value = String(student.id)
  studentSearch.value = `${student.nombre} ${student.apellidoPaterno} (${student.matricula})`
}

function startGenerateAccess(student) {
  credentialStudent.value = student
  initialPassword.value = 'Alumno123!'
  showInitialPassword.value = false
  errorMessage.value = ''
}

function cancelGenerateAccess() {
  credentialStudent.value = null
  initialPassword.value = ''
  showInitialPassword.value = false
}

async function generateAccess() {
  if (!credentialStudent.value || initialPassword.value.length < 8) {
    errorMessage.value = 'La contraseña inicial debe tener al menos 8 caracteres.'
    return
  }

  isGeneratingAccess.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    generatedCredentials.value = await generateStudentUser(
      credentialStudent.value.id,
      initialPassword.value
    )
    savedStudentCredentials[credentialStudent.value.id] = generatedCredentials.value
    localStorage.setItem(
      'casa-cultura-student-credentials',
      JSON.stringify(savedStudentCredentials)
    )
    cancelGenerateAccess()
    successMessage.value = 'Acceso generado correctamente.'
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isGeneratingAccess.value = false
  }
}

function viewAccess(student) {
  generatedCredentials.value = savedStudentCredentials[student.id]
}

function copyCredentials() {
  if (!generatedCredentials.value) return
  const text = `Acceso Casa de la Cultura\nUsuario: ${generatedCredentials.value.nombreUsuario}\nContraseña Temporal: ${generatedCredentials.value.passwordTemporal}`
  navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2500)
}

function getInitials(name, surname) {
  return `${(name || 'A')[0]}${(surname || 'L')[0]}`.toUpperCase()
}
</script>

<template>
  <DashboardLayout
    role="SUPERVISOR"
    :username="supervisorName"
    :items="navItems"
    :active="activeView"
    @navigate="activeView = $event"
  >
    <!-- HERO GREETING HEADER -->
    <header class="supervisor-hero">
      <div class="hero-left">
        <div class="role-pill">
          <span class="pulse-dot"></span>
          <span>Panel del Supervisor</span>
        </div>
        <h1>Hola, {{ supervisorName }}</h1>
        <p class="hero-date">
          <Calendar :size="14" />
          <span>{{ formattedDate }}</span>
          <span class="hero-divider">·</span>
          <span>Gestión de Alumnos e Inscripciones</span>
        </p>
      </div>

      <div class="hero-actions">
        <button
          type="button"
          class="btn-action primary"
          @click="resetForm(); showStudentForm = true; activeView = 'alumnos'"
        >
          <Plus :size="16" />
          <span>Registrar Alumno</span>
        </button>

        <button
          type="button"
          class="btn-action secondary"
          @click="activeView = 'inscripciones'"
        >
          <ClipboardList :size="16" />
          <span>Nueva Inscripción</span>
        </button>
      </div>
    </header>

    <!-- ALERTS FEEDBACK -->
    <Transition name="fade">
      <div v-if="errorMessage" class="alert-banner error" role="alert">
        <AlertCircle :size="18" class="alert-icon" />
        <div class="alert-content">
          <strong>Atención:</strong>
          <span>{{ errorMessage }}</span>
        </div>
        <button type="button" class="alert-close" @click="errorMessage = ''">
          <X :size="16" />
        </button>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="successMessage" class="alert-banner success" role="alert">
        <CheckCircle2 :size="18" class="alert-icon" />
        <div class="alert-content">
          <strong>Operación Exitosa:</strong>
          <span>{{ successMessage }}</span>
        </div>
        <button type="button" class="alert-close" @click="successMessage = ''">
          <X :size="16" />
        </button>
      </div>
    </Transition>

    <!-- LOADING STATE -->
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>Cargando información del sistema...</p>
    </div>

    <!-- MAIN VIEWS -->
    <main v-else class="supervisor-main">
      <!-- ============================================== -->
      <!-- VIEW 1: RESUMEN / DASHBOARD                    -->
      <!-- ============================================== -->
      <section v-if="activeView === 'resumen'" class="view-content">
        <!-- KPI STATS CARDS -->
        <div class="stats-grid">
          <article
            v-for="stat in stats"
            :key="stat.label"
            class="stat-card"
          >
            <div class="stat-icon-wrap" :style="{ backgroundColor: stat.color }">
              <component :is="stat.icon" :size="24" />
            </div>
            <div class="stat-details">
              <span class="stat-label">{{ stat.label }}</span>
              <strong class="stat-value">{{ stat.value }}</strong>
              <small class="stat-subtitle">{{ stat.subtitle }}</small>
            </div>
          </article>
        </div>

        <!-- TWO COLUMNS GRID -->
        <div class="dashboard-grid">
          <!-- Quick Operations Panel -->
          <section class="panel">
            <div class="panel-header">
              <div class="header-text">
                <h3>Acciones Rápidas</h3>
                <p>Operaciones frecuentes para tu perfil</p>
              </div>
              <Sparkles :size="20" class="header-deco-icon" />
            </div>

            <div class="quick-cards-grid">
              <button
                type="button"
                class="quick-card"
                @click="resetForm(); showStudentForm = true; activeView = 'alumnos'"
              >
                <div class="quick-card-icon blue">
                  <Plus :size="20" />
                </div>
                <div class="quick-card-info">
                  <strong>Nuevo Alumno</strong>
                  <span>Dar de alta a un estudiante con reglas de validación</span>
                </div>
                <ArrowRight :size="16" class="quick-arrow" />
              </button>

              <button
                type="button"
                class="quick-card"
                @click="activeView = 'inscripciones'"
              >
                <div class="quick-card-icon emerald">
                  <ClipboardList :size="20" />
                </div>
                <div class="quick-card-info">
                  <strong>Inscribir a Taller</strong>
                  <span>Asignar alumno a cursos y horarios disponibles</span>
                </div>
                <ArrowRight :size="16" class="quick-arrow" />
              </button>

              <button
                type="button"
                class="quick-card"
                @click="activeView = 'alumnos'"
              >
                <div class="quick-card-icon indigo">
                  <Users :size="20" />
                </div>
                <div class="quick-card-info">
                  <strong>Padrón de Alumnos</strong>
                  <span>Consultar, editar y generar credenciales</span>
                </div>
                <ArrowRight :size="16" class="quick-arrow" />
              </button>
            </div>
          </section>

          <!-- Recent Students Registered -->
          <section class="panel">
            <div class="panel-header">
              <div class="header-text">
                <h3>Últimos Alumnos Registrados</h3>
                <p>Estudiantes dados de alta más recientemente</p>
              </div>
              <button
                type="button"
                class="link-button"
                @click="activeView = 'alumnos'"
              >
                Ver todos
                <ArrowRight :size="14" />
              </button>
            </div>

            <div v-if="data.alumnos.length" class="recent-students-list">
              <div
                v-for="student in data.alumnos.slice(-4).reverse()"
                :key="student.id"
                class="recent-student-row"
              >
                <div class="student-avatar-badge">
                  <img v-if="student.fotoUrl" :src="student.fotoUrl" alt="" class="avatar-thumbnail" />
                  <span v-else>{{ getInitials(student.nombre, student.apellidoPaterno) }}</span>
                </div>
                <div class="student-row-info">
                  <strong>{{ student.nombre }} {{ student.apellidoPaterno }}</strong>
                  <span>{{ student.matricula }} · {{ student.correo || 'Sin correo' }}</span>
                </div>
                <span class="status-pill active">
                  {{ student.estado || 'Activo' }}
                </span>
              </div>
            </div>

            <div v-else class="empty-state">
              <Users :size="32" />
              <p>No hay alumnos registrados en el sistema todavía.</p>
            </div>
          </section>
        </div>
      </section>

      <!-- ============================================== -->
      <!-- VIEW 2: GESTIÓN DE ALUMNOS                     -->
      <!-- ============================================== -->
      <section v-else-if="activeView === 'alumnos'" class="view-content">
        <!-- MODAL OR COLLAPSIBLE REGISTRATION FORM -->
        <Transition name="slide-down">
          <div v-if="showStudentForm" class="student-form-modal-container">
            <div class="student-form-card">
              <div class="form-header">
                <div>
                  <h3>{{ editingStudent ? 'Actualizar Información del Alumno' : 'Registrar Nuevo Alumno' }}</h3>
                  <p>Todos los campos con asterisco (*) son obligatorios y cuentan con validación en tiempo real.</p>
                </div>
                <button
                  type="button"
                  class="btn-close-form"
                  @click="showStudentForm = false"
                >
                  <X :size="20" />
                </button>
              </div>

              <!-- Inline Form Error Alert Banner -->
              <div v-if="formModalError" class="modal-form-alert error" role="alert">
                <AlertCircle :size="18" class="flex-shrink-0" />
                <div class="alert-content">
                  <strong>Datos incompletos o inválidos:</strong>
                  <span>{{ formModalError }}</span>
                </div>
                <button type="button" class="btn-close-alert" @click="formModalError = ''">
                  <X :size="14" />
                </button>
              </div>

              <!-- FORM CONTENT WITH RULES -->
              <form @submit.prevent="saveStudent" novalidate class="student-form-body">
                <!-- Fotografía del Alumno -->
                <div class="student-photo-section">
                  <div class="photo-avatar-box">
                    <img
                      v-if="studentForm.fotoUrl"
                      :src="studentForm.fotoUrl"
                      alt="Foto del alumno"
                      class="photo-preview-img"
                    />
                    <div v-else class="photo-placeholder">
                      <User :size="32" />
                      <span>Sin foto</span>
                    </div>
                  </div>

                  <div class="photo-info-box">
                    <div class="photo-title">Fotografía Oficial del Alumno</div>
                    <p class="photo-desc">
                      Captura directamente con la cámara del dispositivo o sube una imagen para su credencial y asistencias.
                    </p>
                    <div class="photo-action-buttons">
                      <button
                        type="button"
                        class="btn-photo-capture"
                        @click="showCameraModal = true"
                      >
                        <Camera :size="16" />
                        <span>{{ studentForm.fotoUrl ? 'Cambiar / Tomar Nueva Foto' : 'Tomar Foto con Cámara' }}</span>
                      </button>

                      <button
                        v-if="studentForm.fotoUrl"
                        type="button"
                        class="btn-photo-remove"
                        title="Quitar foto"
                        @click="studentForm.fotoUrl = ''"
                      >
                        <Trash2 :size="15" />
                        <span>Quitar</span>
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Group 1: Datos Personales -->
                <div class="form-section-title">
                  <UserCheck :size="16" />
                  <span>1. Datos Personales</span>
                </div>

                <div class="form-grid-3">
                  <!-- Nombre -->
                  <div class="input-field" :class="{ 'has-error': touched.nombre && errors.nombre, 'is-valid': touched.nombre && !errors.nombre }">
                    <label for="nombre">Nombre(s) <span class="required">*</span></label>
                    <div class="input-box">
                      <input
                        id="nombre"
                        v-model="studentForm.nombre"
                        type="text"
                        placeholder="Ej. María Elena"
                        autocomplete="off"
                        @input="onNameInput('nombre', $event)"
                        @blur="touched.nombre = true"
                      />
                      <CheckCircle2 v-if="touched.nombre && !errors.nombre" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.nombre && errors.nombre" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.nombre && errors.nombre" class="error-msg">{{ errors.nombre }}</span>
                  </div>

                  <!-- Apellido Paterno -->
                  <div class="input-field" :class="{ 'has-error': touched.apellidoPaterno && errors.apellidoPaterno, 'is-valid': touched.apellidoPaterno && !errors.apellidoPaterno }">
                    <label for="apellidoPaterno">Apellido Paterno <span class="required">*</span></label>
                    <div class="input-box">
                      <input
                        id="apellidoPaterno"
                        v-model="studentForm.apellidoPaterno"
                        type="text"
                        placeholder="Ej. Hernández"
                        autocomplete="off"
                        @input="onNameInput('apellidoPaterno', $event)"
                        @blur="touched.apellidoPaterno = true"
                      />
                      <CheckCircle2 v-if="touched.apellidoPaterno && !errors.apellidoPaterno" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.apellidoPaterno && errors.apellidoPaterno" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.apellidoPaterno && errors.apellidoPaterno" class="error-msg">{{ errors.apellidoPaterno }}</span>
                  </div>

                  <!-- Apellido Materno -->
                  <div class="input-field" :class="{ 'has-error': touched.apellidoMaterno && errors.apellidoMaterno, 'is-valid': touched.apellidoMaterno && !errors.apellidoMaterno && studentForm.apellidoMaterno }">
                    <label for="apellidoMaterno">Apellido Materno</label>
                    <div class="input-box">
                      <input
                        id="apellidoMaterno"
                        v-model="studentForm.apellidoMaterno"
                        type="text"
                        placeholder="Ej. López (opcional)"
                        autocomplete="off"
                        @input="onNameInput('apellidoMaterno', $event)"
                        @blur="touched.apellidoMaterno = true"
                      />
                      <CheckCircle2 v-if="touched.apellidoMaterno && !errors.apellidoMaterno && studentForm.apellidoMaterno" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.apellidoMaterno && errors.apellidoMaterno" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.apellidoMaterno && errors.apellidoMaterno" class="error-msg">{{ errors.apellidoMaterno }}</span>
                  </div>
                </div>

                <!-- Group 2: Control Escolar y Fechas -->
                <div class="form-section-title">
                  <Calendar :size="16" />
                  <span>2. Control Escolar y Nacimiento</span>
                </div>

                <div class="form-grid-2">
                  <!-- Fecha de Nacimiento -->
                  <div class="input-field" :class="{ 'has-error': touched.fechaNacimiento && errors.fechaNacimiento, 'is-valid': touched.fechaNacimiento && !errors.fechaNacimiento }">
                    <label for="fechaNacimiento">Fecha de Nacimiento <span class="required">*</span></label>
                    <div class="input-box">
                      <input
                        id="fechaNacimiento"
                        v-model="studentForm.fechaNacimiento"
                        type="date"
                        :max="maxBirthDate"
                        :min="minBirthDate"
                        @change="touched.fechaNacimiento = true"
                      />
                      <CheckCircle2 v-if="touched.fechaNacimiento && !errors.fechaNacimiento" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.fechaNacimiento && errors.fechaNacimiento" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.fechaNacimiento && errors.fechaNacimiento" class="error-msg">{{ errors.fechaNacimiento }}</span>
                  </div>

                  <!-- Matrícula -->
                  <div class="input-field" :class="{ 'has-error': touched.matricula && errors.matricula, 'is-valid': touched.matricula && !errors.matricula }">
                    <div class="label-with-action">
                      <label for="matricula">Matrícula Institucional <span class="required">*</span></label>
                      <button
                        type="button"
                        class="btn-suggest"
                        title="Generar formato sugerido"
                        @click="autoSuggestMatricula"
                      >
                        <Sparkles :size="13" />
                        <span>Sugerir matrícula</span>
                      </button>
                    </div>
                    <div class="input-box">
                      <input
                        id="matricula"
                        v-model="studentForm.matricula"
                        type="text"
                        placeholder="Ej. ALU-2026-001"
                        autocomplete="off"
                        @input="onMatriculaInput($event)"
                        @blur="touched.matricula = true"
                      />
                      <CheckCircle2 v-if="touched.matricula && !errors.matricula" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.matricula && errors.matricula" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.matricula && errors.matricula" class="error-msg">{{ errors.matricula }}</span>
                  </div>
                </div>

                <!-- Group 3: Contacto y Ubicación -->
                <div class="form-section-title">
                  <Phone :size="16" />
                  <span>3. Datos de Contacto</span>
                </div>

                <div class="form-grid-3">
                  <!-- Teléfono -->
                  <div class="input-field" :class="{ 'has-error': touched.telefono && errors.telefono, 'is-valid': touched.telefono && !errors.telefono }">
                    <label for="telefono">Teléfono (10 dígitos) <span class="required">*</span></label>
                    <div class="input-box">
                      <input
                        id="telefono"
                        v-model="studentForm.telefono"
                        type="tel"
                        maxlength="10"
                        placeholder="Ej. 9531234567"
                        autocomplete="off"
                        @input="onPhoneInput($event)"
                        @blur="touched.telefono = true"
                      />
                      <CheckCircle2 v-if="touched.telefono && !errors.telefono" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.telefono && errors.telefono" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.telefono && errors.telefono" class="error-msg">{{ errors.telefono }}</span>
                  </div>

                  <!-- Correo -->
                  <div class="input-field" :class="{ 'has-error': touched.correo && errors.correo, 'is-valid': touched.correo && !errors.correo }">
                    <label for="correo">Correo Electrónico <span class="required">*</span></label>
                    <div class="input-box">
                      <input
                        id="correo"
                        v-model="studentForm.correo"
                        type="email"
                        placeholder="ejemplo@correo.com"
                        autocomplete="off"
                        @input="touched.correo = true"
                        @blur="touched.correo = true"
                      />
                      <CheckCircle2 v-if="touched.correo && !errors.correo" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.correo && errors.correo" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.correo && errors.correo" class="error-msg">{{ errors.correo }}</span>
                  </div>

                  <!-- Dirección / Domicilio -->
                  <div class="input-field" :class="{ 'has-error': touched.direccion && errors.direccion, 'is-valid': touched.direccion && !errors.direccion }">
                    <label for="direccion">Dirección / Domicilio <span class="required">*</span></label>
                    <div class="input-box">
                      <input
                        id="direccion"
                        v-model="studentForm.direccion"
                        type="text"
                        placeholder="Ej. Calle Morelos #12, Centro"
                        autocomplete="off"
                        @input="touched.direccion = true"
                        @blur="touched.direccion = true"
                      />
                      <CheckCircle2 v-if="touched.direccion && !errors.direccion" :size="16" class="valid-icon" />
                      <AlertCircle v-if="touched.direccion && errors.direccion" :size="16" class="error-icon" />
                    </div>
                    <span v-if="touched.direccion && errors.direccion" class="error-msg">{{ errors.direccion }}</span>
                  </div>
                </div>

                <!-- Form Footer Actions -->
                <div class="form-footer-bar">
                  <div class="rules-summary">
                    <span class="dot-indicator" :class="{ ok: isFormValid }"></span>
                    <span>{{ isFormValid ? 'Todos los datos esenciales son válidos' : 'Completa los datos esenciales obligatorios (*) para guardar' }}</span>
                  </div>

                  <div class="form-action-btns">
                    <button
                      type="button"
                      class="btn-cancel"
                      @click="showStudentForm = false"
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      class="btn-submit"
                      :disabled="isSaving"
                      :class="{ 'btn-submit-disabled': !isFormValid }"
                      :title="!isFormValid ? 'Completa los campos esenciales obligatorios marcados con asterisco (*)' : 'Guardar alumno'"
                    >
                      <CheckCircle2 :size="16" />
                      <span>{{ isSaving ? 'Guardando...' : (editingStudent ? 'Actualizar Alumno' : 'Guardar Alumno') }}</span>
                    </button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </Transition>

        <!-- TABLE SECTION -->
        <section class="panel">
          <div class="panel-header table-header-flex">
            <div>
              <h3>Padrón Escolar de Alumnos</h3>
              <p>Total: {{ data.alumnos.length }} alumnos registrados en la institución</p>
            </div>

            <div class="header-tools">
              <!-- Search Input -->
              <div class="search-box">
                <Search :size="16" class="search-icon" />
                <input
                  v-model="studentSearch"
                  type="text"
                  placeholder="Buscar por nombre, matrícula o teléfono..."
                  autocomplete="off"
                />
                <button
                  v-if="studentSearch"
                  type="button"
                  class="clear-search"
                  @click="studentSearch = ''"
                >
                  <X :size="14" />
                </button>
              </div>

              <!-- Filter Selector -->
              <select v-model="studentFilter" class="filter-select">
                <option value="ALL">Todos los estados</option>
                <option value="ACTIVE">Solo Activos</option>
                <option value="INACTIVE">Solo Inactivos</option>
              </select>

              <!-- New Student Button -->
              <button
                type="button"
                class="btn-action primary"
                @click="resetForm(); showStudentForm = true"
              >
                <Plus :size="16" />
                <span>Nuevo Alumno</span>
              </button>
            </div>
          </div>

          <!-- Students Table -->
          <div class="table-responsive">
            <table v-if="filteredStudents.length">
              <thead>
                <tr>
                  <th>Alumno</th>
                  <th>Matrícula</th>
                  <th>Contacto</th>
                  <th>Fecha Nacimiento</th>
                  <th>Estado</th>
                  <th class="text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="student in filteredStudents" :key="student.id">
                  <td>
                    <div class="student-cell">
                      <div class="table-avatar">
                        <img v-if="student.fotoUrl" :src="student.fotoUrl" alt="" class="avatar-thumbnail" />
                        <span v-else>{{ getInitials(student.nombre, student.apellidoPaterno) }}</span>
                      </div>
                      <div class="student-names">
                        <strong>{{ student.nombre }} {{ student.apellidoPaterno }} {{ student.apellidoMaterno || '' }}</strong>
                        <small>{{ student.direccion || 'Sin dirección registrada' }}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span class="matricula-badge">{{ student.matricula }}</span>
                  </td>
                  <td>
                    <div class="contact-cell">
                      <span v-if="student.telefono" class="contact-item">
                        <Phone :size="12" />
                        <span>{{ student.telefono }}</span>
                      </span>
                      <span v-if="student.correo" class="contact-item">
                        <Mail :size="12" />
                        <span>{{ student.correo }}</span>
                      </span>
                      <span v-if="!student.telefono && !student.correo" class="contact-muted">-</span>
                    </div>
                  </td>
                  <td>
                    <span class="date-cell">{{ student.fechaNacimiento || '-' }}</span>
                  </td>
                  <td>
                    <span class="status-pill active">{{ student.estado || 'Activo' }}</span>
                  </td>
                  <td class="text-right">
                    <div class="actions-group">
                      <button
                        type="button"
                        class="btn-table-icon edit"
                        title="Editar información"
                        @click="editStudent(student)"
                      >
                        <Edit3 :size="15" />
                        <span>Editar</span>
                      </button>

                      <button
                        v-if="savedStudentCredentials[student.id]"
                        type="button"
                        class="btn-table-icon view-access"
                        title="Ver credenciales de acceso"
                        @click="viewAccess(student)"
                      >
                        <KeyRound :size="15" />
                        <span>Ver Acceso</span>
                      </button>

                      <button
                        v-else
                        type="button"
                        class="btn-table-icon generate-access"
                        title="Generar usuario y contraseña"
                        :disabled="isGeneratingAccess"
                        @click="startGenerateAccess(student)"
                      >
                        <Plus :size="14" />
                        <span>Crear Acceso</span>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-else class="empty-state">
              <Users :size="36" />
              <p>No se encontraron alumnos que coincidan con la búsqueda.</p>
              <button
                v-if="studentSearch || studentFilter !== 'ALL'"
                type="button"
                class="btn-reset-filter"
                @click="studentSearch = ''; studentFilter = 'ALL'"
              >
                <RotateCcw :size="14" />
                <span>Restablecer filtros</span>
              </button>
            </div>
          </div>
        </section>
      </section>

      <!-- ============================================== -->
      <!-- VIEW 3: ASIGNACIÓN E INSCRIPCIONES             -->
      <!-- ============================================== -->
      <section v-else-if="activeView === 'inscripciones'" class="view-content">
        <!-- ENROLLMENT BOX -->
        <div class="panel enrollment-panel">
          <div class="panel-header">
            <div>
              <h3>Asignar Alumno a un Curso / Taller</h3>
              <p>Selecciona un alumno del catálogo y asígnalo al grupo correspondiente.</p>
            </div>
          </div>

          <div class="enrollment-grid-picker">
            <!-- Student Picker -->
            <div class="picker-column">
              <label for="student-search">1. Buscar y Seleccionar Alumno <span class="required">*</span></label>
              <div class="picker-input-wrap">
                <Search :size="16" class="picker-icon" />
                <input
                  id="student-search"
                  v-model="studentSearch"
                  type="text"
                  placeholder="Escribe el nombre o matrícula del alumno..."
                  autocomplete="off"
                  @input="selectedStudent = ''"
                />
                <button
                  v-if="studentSearch"
                  type="button"
                  class="clear-picker"
                  @click="studentSearch = ''; selectedStudent = ''"
                >
                  <X :size="14" />
                </button>
              </div>

              <!-- Student Results Dropdown -->
              <div v-if="studentSearch && !selectedStudent" class="picker-dropdown">
                <button
                  v-for="student in filteredStudents"
                  :key="student.id"
                  type="button"
                  class="picker-result-btn"
                  @click="selectStudent(student)"
                >
                  <div class="picker-avatar">
                    <img v-if="student.fotoUrl" :src="student.fotoUrl" alt="" class="avatar-thumbnail" />
                    <span v-else>{{ getInitials(student.nombre, student.apellidoPaterno) }}</span>
                  </div>
                  <div>
                    <strong>{{ student.nombre }} {{ student.apellidoPaterno }}</strong>
                    <small>Matrícula: {{ student.matricula }} · Tel: {{ student.telefono || 'S/N' }}</small>
                  </div>
                </button>
                <div v-if="!filteredStudents.length" class="empty-picker-result">
                  No se encontró ningún alumno con ese nombre o matrícula.
                </div>
              </div>
            </div>

            <!-- Course / Group Selector -->
            <div class="picker-column">
              <label for="group-select">2. Seleccionar Taller y Grupo <span class="required">*</span></label>
              <div class="select-wrap">
                <select id="group-select" v-model="selectedGroup">
                  <option value="">-- Elige un curso o taller --</option>
                  <option
                    v-for="group in data.grupos"
                    :key="group.id"
                    :value="group.id"
                  >
                    {{ group.nombreGrupo }} · {{ group.nombreCurso || 'Taller Cultural' }}
                  </option>
                </select>
              </div>
            </div>

            <!-- Action Button -->
            <div class="picker-action">
              <button
                type="button"
                class="btn-enroll-submit"
                :disabled="!selectedStudent || !selectedGroup || isAlreadyEnrolled || isSaving"
                @click="saveEnrollment"
              >
                <CheckCircle2 :size="16" />
                <span>{{ isSaving ? 'Inscribiendo...' : 'Confirmar Inscripción' }}</span>
              </button>
            </div>
          </div>

          <p v-if="isAlreadyEnrolled" class="warning-banner">
            <AlertCircle :size="15" />
            <span>Este estudiante ya se encuentra inscrito en este taller.</span>
          </p>
        </div>

        <!-- ENROLLMENTS LIST TABLE -->
        <section class="panel">
          <div class="panel-header">
            <div>
              <h3>Historial de Inscripciones Activas</h3>
              <p>Padrón de asignaciones registradas en el ciclo</p>
            </div>
            <span class="counter-badge">{{ data.inscripciones.length }} inscripciones</span>
          </div>

          <div class="table-responsive">
            <table v-if="data.inscripciones.length">
              <thead>
                <tr>
                  <th>Alumno (Matrícula)</th>
                  <th>Taller / Grupo</th>
                  <th>Fecha de Registro</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="enrollment in data.inscripciones" :key="enrollment.id">
                  <td>
                    <span class="matricula-badge">{{ enrollment.matricula || enrollment.alumnoNombre || 'Alumno' }}</span>
                  </td>
                  <td>
                    <strong>{{ enrollment.grupo || 'Grupo Taller' }}</strong>
                  </td>
                  <td>
                    <span class="date-cell">{{ enrollment.fechaInscripcion || '-' }}</span>
                  </td>
                  <td>
                    <span class="status-pill active">{{ enrollment.estado || 'Activa' }}</span>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-else class="empty-state">
              <ClipboardList :size="36" />
              <p>No hay inscripciones registradas todavía.</p>
            </div>
          </div>
        </section>
      </section>
    </main>

    <!-- ============================================== -->
    <!-- MODAL 1: CREDENCIALES GENERADAS                -->
    <!-- ============================================== -->
    <div
      v-if="generatedCredentials"
      class="modal-backdrop"
      @click.self="generatedCredentials = null"
    >
      <div class="modal-card">
        <button
          type="button"
          class="btn-modal-close"
          aria-label="Cerrar modal"
          @click="generatedCredentials = null"
        >
          <X :size="18" />
        </button>

        <div class="modal-badge-icon green">
          <ShieldCheck :size="28" />
        </div>

        <h2>Credenciales de Acceso</h2>
        <p class="modal-desc">
          Comparte estas credenciales con el alumno para que ingrese al portal. En su primer inicio de sesión, el sistema le solicitará cambiar la contraseña por seguridad.
        </p>

        <div class="credential-box">
          <div class="credential-item">
            <span class="cred-label">Usuario</span>
            <strong class="cred-value">{{ generatedCredentials.nombreUsuario }}</strong>
          </div>
          <div class="credential-item">
            <span class="cred-label">Contraseña Temporal</span>
            <strong class="cred-value password">{{ generatedCredentials.passwordTemporal }}</strong>
          </div>
        </div>

        <div class="modal-actions">
          <button
            type="button"
            class="btn-action secondary w-full"
            @click="copyCredentials"
          >
            <Check v-if="copied" :size="16" />
            <Copy v-else :size="16" />
            <span>{{ copied ? '¡Copiado al portapapeles!' : 'Copiar Credenciales' }}</span>
          </button>

          <button
            type="button"
            class="btn-action primary w-full"
            @click="generatedCredentials = null"
          >
            Entendido
          </button>
        </div>
      </div>
    </div>

    <!-- ============================================== -->
    <!-- MODAL 2: CREAR CONTRASEÑA INICIAL              -->
    <!-- ============================================== -->
    <div
      v-if="credentialStudent"
      class="modal-backdrop"
      @click.self="cancelGenerateAccess"
    >
      <div class="modal-card">
        <button
          type="button"
          class="btn-modal-close"
          aria-label="Cerrar modal"
          @click="cancelGenerateAccess"
        >
          <X :size="18" />
        </button>

        <div class="modal-badge-icon indigo">
          <KeyRound :size="28" />
        </div>

        <h2>Generar Acceso para Alumno</h2>
        <p class="modal-desc">
          Estás creando la cuenta de acceso para <strong>{{ credentialStudent.nombre }} {{ credentialStudent.apellidoPaterno }}</strong> (Matrícula: {{ credentialStudent.matricula }}).
        </p>

        <div class="input-field mt-4">
          <label for="initial-pass">Contraseña Temporal Asignada <span class="required">*</span></label>
          <div class="input-box">
            <input
              id="initial-pass"
              v-model="initialPassword"
              :type="showInitialPassword ? 'text' : 'password'"
              minlength="8"
              placeholder="Mínimo 8 caracteres"
            />
            <button
              type="button"
              class="pass-toggle"
              @click="showInitialPassword = !showInitialPassword"
            >
              <EyeOff v-if="showInitialPassword" :size="16" />
              <Eye v-else :size="16" />
            </button>
          </div>
          <small class="help-text">La contraseña debe tener un mínimo de 8 caracteres.</small>
        </div>

        <p v-if="errorMessage" class="error-msg-banner">{{ errorMessage }}</p>

        <div class="modal-actions mt-6">
          <button
            type="button"
            class="btn-cancel"
            @click="cancelGenerateAccess"
          >
            Cancelar
          </button>

          <button
            type="button"
            class="btn-action primary"
            :disabled="isGeneratingAccess || initialPassword.length < 8"
            @click="generateAccess"
          >
            <KeyRound :size="16" />
            <span>{{ isGeneratingAccess ? 'Generando...' : 'Crear Usuario' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de Captura de Fotografía con Cámara Web / Celular -->
    <CameraCaptureModal
      :show="showCameraModal"
      title="Tomar Foto del Alumno"
      @close="showCameraModal = false"
      @photo-saved="onPhotoSaved"
    />
  </DashboardLayout>
</template>

<style scoped src="../../../assets/styles/supervisor.css"></style>
