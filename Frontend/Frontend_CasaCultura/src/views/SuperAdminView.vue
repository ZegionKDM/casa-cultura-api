<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  BarChart3,
  BookOpen,
  CalendarCheck2,
  CreditCard,
  GraduationCap,
  LayoutDashboard,
  PencilLine,
  Plus,
  RotateCcw,
  Save,
  Search,
  ShieldCheck,
  SquareUserRound,
  Trash2,
  Users,
} from 'lucide-vue-next'
import DashboardLayout from '../components/DashboardLayout.vue'
import { clearStoredSession } from '../services/apiService'
import { createStudent as createAlumno, updateStudent } from '../services/supervisorService'
import {
  createCourse, createGroup, createPayment, createTeacher, createUser, deactivateTeacher,
  deactivateUser, generateAbsences, getAttendanceReport, getSuperAdminData, registerAttendance,
  resetUserPassword, updateCourse, updateGroup, updateTeacher,
} from '../services/superAdminService'

const router = useRouter()
const activeView = ref('resumen')
const isLoading = ref(true)
const isSaving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const data = reactive({
  alumnos: [], docentes: [], cursos: [], ofertas: [], grupos: [], categorias: [], inscripciones: [],
  pagos: [], asistencias: [], usuarios: [], roles: [],
})

const currentUser = JSON.parse(localStorage.getItem('casa-cultura-user')
  || sessionStorage.getItem('casa-cultura-user') || '{}')
const navItems = [
  { id: 'resumen', label: 'Dashboard general', icon: LayoutDashboard },
  { id: 'docentes', label: 'Docentes', icon: Users },
  { id: 'alumnos', label: 'Alumnos', icon: GraduationCap },
  { id: 'cursos', label: 'Cursos y grupos', icon: BookOpen },
  { id: 'inscripciones', label: 'Inscripciones', icon: SquareUserRound },
  { id: 'asistencias', label: 'Asistencias', icon: CalendarCheck2 },
  { id: 'pagos', label: 'Pagos', icon: CreditCard },
  { id: 'usuarios', label: 'Usuarios y accesos', icon: ShieldCheck },
  { id: 'reportes', label: 'Reportes', icon: BarChart3 },
]

const forms = reactive({
  docente: { nombre: '', apellidoPaterno: '', apellidoMaterno: '', telefono: '', direccion: '', correo: '', especialidad: '' },
  alumno: { nombre: '', apellidoPaterno: '', apellidoMaterno: '', fechaNacimiento: '', telefono: '', direccion: '', correo: '', matricula: '' },
  curso: { nombre: '' },
  grupo: { ofertaId: '', categoriaId: '', nombreGrupo: '' },
  pago: { inscripcionId: '', tipoPago: 'INSCRIPCION', periodo: '', fechaVencimiento: '', fechaPago: '', estado: 'PENDIENTE' },
  usuario: { personaId: '', rolId: '', nombreUsuario: '', password: '' },
  asistencia: { inscripcionId: '', horarioId: '', fecha: new Date().toISOString().slice(0, 10), estado: 'PRESENTE' },
})
const editing = reactive({ docente: null, alumno: null, curso: null, grupo: null })
const touched = reactive({ docente: {}, alumno: {} })
const report = ref(null)
const reportDates = reactive({ from: '', to: '' })
const search = ref('')

const namePattern = /^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+(?:[ '-][A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$/
const emailPattern = /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/
const phonePattern = /^[0-9+()\s-]{7,20}$/
const matriculaPattern = /^[A-Za-z0-9-]{3,30}$/

const formErrors = computed(() => ({
  docente: {
    nombre: validateName(forms.docente.nombre, 'El nombre es obligatorio.'),
    apellidoPaterno: validateName(forms.docente.apellidoPaterno, 'El apellido paterno es obligatorio.'),
    apellidoMaterno: validateOptionalName(forms.docente.apellidoMaterno),
    telefono: validateRequiredPhone(forms.docente.telefono),
    correo: validateRequiredEmail(forms.docente.correo),
    especialidad: validateLength(forms.docente.especialidad, 120),
    direccion: validateRequiredAddress(forms.docente.direccion),
  },
  alumno: {
    nombre: validateName(forms.alumno.nombre, 'El nombre es obligatorio.'),
    apellidoPaterno: validateName(forms.alumno.apellidoPaterno, 'El apellido paterno es obligatorio.'),
    apellidoMaterno: validateOptionalName(forms.alumno.apellidoMaterno),
    matricula: validateMatricula(forms.alumno.matricula),
    fechaNacimiento: validateBirthDate(forms.alumno.fechaNacimiento),
    telefono: validateRequiredPhone(forms.alumno.telefono),
    correo: validateRequiredEmail(forms.alumno.correo),
    direccion: validateRequiredAddress(forms.alumno.direccion),
  },
}))

const docenteFormValid = computed(() => Object.values(formErrors.value.docente).every((error) => !error))
const alumnoFormValid = computed(() => Object.values(formErrors.value.alumno).every((error) => !error))

const stats = computed(() => [
  { value: data.alumnos.length, label: 'Alumnos registrados', icon: GraduationCap, color: 'purple' },
  { value: data.docentes.length, label: 'Docentes activos', icon: Users, color: 'blue' },
  { value: data.grupos.length, label: 'Cursos y grupos', icon: BookOpen, color: 'green' },
  { value: data.inscripciones.length, label: 'Inscripciones', icon: SquareUserRound, color: 'orange' },
])
const filteredStudents = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) return data.alumnos
  return data.alumnos.filter((item) => `${item.nombre} ${item.apellidoPaterno} ${item.matricula}`.toLowerCase().includes(query))
})

onMounted(loadData)

function validateName(value, requiredMessage) {
  const normalized = String(value || '').trim()
  if (!normalized) return requiredMessage
  if (normalized.length > 80) return 'No puede superar 80 caracteres.'
  return namePattern.test(normalized) ? '' : 'Usa solo letras, espacios, guiones o apóstrofes.'
}

function validateOptionalName(value) {
  if (!String(value || '').trim()) return ''
  return validateName(value, '')
}

function validateRequiredEmail(value) {
  const normalized = String(value || '').trim()
  if (!normalized) return 'El correo electrónico es obligatorio.'
  if (normalized.length > 160) return 'No puede superar 160 caracteres.'
  return emailPattern.test(normalized) ? '' : 'Escribe un correo válido, por ejemplo nombre@dominio.com.'
}

function validateRequiredPhone(value) {
  const clean = String(value || '').replace(/\D/g, '')
  if (!clean) return 'El teléfono es obligatorio.'
  if (clean.length !== 10) return 'El teléfono debe tener exactamente 10 dígitos numéricos.'
  return ''
}

function validateRequiredAddress(value) {
  const normalized = String(value || '').trim()
  if (!normalized) return 'El domicilio / dirección es obligatorio.'
  if (normalized.length < 5) return 'Ingresa un domicilio completo (mínimo 5 caracteres).'
  if (normalized.length > 255) return 'No puede superar 255 caracteres.'
  return ''
}

function validateOptionalEmail(value) {
  const normalized = String(value || '').trim()
  if (!normalized) return ''
  if (normalized.length > 160) return 'No puede superar 160 caracteres.'
  return emailPattern.test(normalized) ? '' : 'Escribe un correo válido, por ejemplo nombre@dominio.com.'
}

function validateOptionalPhone(value) {
  const normalized = String(value || '').trim()
  if (!normalized) return ''
  return phonePattern.test(normalized) ? '' : 'Usa entre 7 y 20 caracteres numéricos.'
}

function validateLength(value, max) {
  return String(value || '').length > max ? `No puede superar ${max} caracteres.` : ''
}

function validateMatricula(value) {
  const normalized = String(value || '').trim()
  if (!normalized) return 'La matrícula es obligatoria.'
  return matriculaPattern.test(normalized) ? '' : 'Usa de 3 a 30 caracteres: letras, números o guiones.'
}

function validateBirthDate(value) {
  if (!value) return 'La fecha de nacimiento es obligatoria.'
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return 'Selecciona una fecha válida.'
  return date > new Date() ? 'La fecha no puede ser futura.' : ''
}

function markFormTouched(name) {
  Object.keys(formErrors.value[name]).forEach((field) => { touched[name][field] = true })
}

function fieldError(name, field) {
  return touched[name]?.[field] ? formErrors.value[name][field] : ''
}

function touchField(name, field) {
  touched[name][field] = true
}

async function loadData() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    Object.assign(data, await getSuperAdminData())
  } catch (error) {
    errorMessage.value = error.message
    if (error.message.includes('401') || error.message.includes('403')) {
      clearStoredSession()
      await router.push('/')
    }
  } finally {
    isLoading.value = false
  }
}

function resetForm(name) {
  const defaults = {
    docente: { nombre: '', apellidoPaterno: '', apellidoMaterno: '', telefono: '', direccion: '', correo: '', especialidad: '' },
    alumno: { nombre: '', apellidoPaterno: '', apellidoMaterno: '', fechaNacimiento: '', telefono: '', direccion: '', correo: '', matricula: '' },
    curso: { nombre: '' },
    grupo: { ofertaId: '', categoriaId: '', nombreGrupo: '' },
  }
  Object.assign(forms[name], defaults[name])
  editing[name] = null
  if (touched[name]) Object.keys(touched[name]).forEach((field) => { touched[name][field] = false })
}

function editRecord(name, record) {
  Object.assign(forms[name], record)
  editing[name] = record
  activeView.value = name === 'grupo' ? 'cursos' : name
}

async function saveRecord(name, create, update) {
  if (name === 'docente' || name === 'alumno') {
    markFormTouched(name)
    if (!(name === 'docente' ? docenteFormValid.value : alumnoFormValid.value)) {
      errorMessage.value = 'Corrige los campos marcados antes de guardar.'
      return
    }
  }
  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const payload = { ...forms[name] }
    Object.keys(payload).forEach((key) => {
      if (typeof payload[key] === 'string') payload[key] = payload[key].trim()
      if (payload[key] === '') payload[key] = null
    })
    if (editing[name]) await update(editing[name].id, payload)
    else await create(payload)
    successMessage.value = 'Información guardada correctamente.'
    resetForm(name)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSaving.value = false
  }
}

async function runAction(action, message = 'Operación realizada correctamente.') {
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await action()
    successMessage.value = message
    await loadData()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function loadReport() {
  await runAction(async () => { report.value = await getAttendanceReport(reportDates.from, reportDates.to) })
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium' }).format(new Date(`${value}T00:00:00`))
}
</script>

<template>
  <DashboardLayout role="SUPER_ADMIN" :username="currentUser.nombreUsuario || 'Super administrador'" :items="navItems" :active="activeView" @navigate="activeView = $event">
    <section class="page-heading">
      <div>
        <span class="eyebrow">SUPER ADMINISTRACIÓN</span>
        <h1>{{ navItems.find((item) => item.id === activeView)?.label }}</h1>
        <p>Control total de la plataforma de la Casa de la Cultura.</p>
      </div>
      <div class="heading-icon"><ShieldCheck class="heading-icon-svg" /></div>
    </section>
    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="feedback success">{{ successMessage }}</p>
    <div v-if="isLoading" class="state-card">Cargando información administrativa...</div>

    <template v-else>
      <template v-if="activeView === 'resumen'">
        <div class="stat-grid"><article v-for="stat in stats" :key="stat.label"><span class="stat-icon" :class="stat.color"><component :is="stat.icon" class="stat-icon-svg" /></span><strong>{{ stat.value }}</strong><small>{{ stat.label }}</small></article></div>
        <div class="content-grid">
          <section class="panel"><div class="panel-heading"><div><h2>Acciones rápidas</h2><p>Operaciones principales del super administrador.</p></div></div><div class="quick-actions"><button type="button" @click="activeView = 'docentes'"><Plus class="action-icon" />Registrar docente</button><button type="button" @click="activeView = 'alumnos'"><Plus class="action-icon" />Registrar alumno</button><button type="button" @click="activeView = 'cursos'"><BookOpen class="action-icon" />Administrar cursos</button><button type="button" @click="activeView = 'usuarios'"><ShieldCheck class="action-icon" />Crear usuario y contraseña</button></div></section>
          <section class="panel"><div class="panel-heading"><div><h2>Actividad reciente</h2><p>Últimos alumnos registrados.</p></div><button type="button" @click="activeView = 'alumnos'">Ver todos</button></div><div class="record-list"><div v-for="item in data.alumnos.slice(-5).reverse()" :key="item.id" class="record-row"><span class="record-icon"><img v-if="item.fotoUrl" :src="item.fotoUrl" class="record-avatar-img" /><GraduationCap v-else class="record-icon-svg" /></span><div><strong>{{ item.nombre }} {{ item.apellidoPaterno }}</strong><small>{{ item.matricula || 'Sin matrícula' }}</small></div><span class="status active">{{ item.estado || 'ACTIVO' }}</span></div></div></section>
        </div>
      </template>

      <section v-else-if="activeView === 'docentes'" class="section-grid">
        <div class="panel">
          <div class="panel-heading"><div><h2>Registrar docente</h2><p>Captura los datos del docente con formato correcto.</p></div><button class="primary-button" type="button" @click="resetForm('docente')">Nuevo docente</button></div>
        <div class="form-card">
          <p class="form-note"><strong>*</strong> Campos obligatorios. Los nombres deben contener solo letras.</p>
          <div class="form-grid">
            <div class="field"><label for="teacher-name">Nombre <span>*</span></label><input id="teacher-name" v-model.trim="forms.docente.nombre" :class="{ invalid: fieldError('docente', 'nombre') }" maxlength="80" placeholder="Ej. María Fernanda" @input="touchField('docente', 'nombre')" /><small v-if="fieldError('docente', 'nombre')" class="field-error">{{ fieldError('docente', 'nombre') }}</small></div>
            <div class="field"><label for="teacher-last-name">Apellido paterno <span>*</span></label><input id="teacher-last-name" v-model.trim="forms.docente.apellidoPaterno" :class="{ invalid: fieldError('docente', 'apellidoPaterno') }" maxlength="80" placeholder="Ej. López" @input="touchField('docente', 'apellidoPaterno')" /><small v-if="fieldError('docente', 'apellidoPaterno')" class="field-error">{{ fieldError('docente', 'apellidoPaterno') }}</small></div>
            <div class="field"><label for="teacher-maternal">Apellido materno</label><input id="teacher-maternal" v-model.trim="forms.docente.apellidoMaterno" :class="{ invalid: fieldError('docente', 'apellidoMaterno') }" maxlength="80" placeholder="Ej. García" @input="touchField('docente', 'apellidoMaterno')" /><small v-if="fieldError('docente', 'apellidoMaterno')" class="field-error">{{ fieldError('docente', 'apellidoMaterno') }}</small></div>
            <div class="field"><label for="teacher-specialty">Especialidad</label><input id="teacher-specialty" v-model.trim="forms.docente.especialidad" :class="{ invalid: fieldError('docente', 'especialidad') }" maxlength="120" placeholder="Ej. Música" @input="touchField('docente', 'especialidad')" /><small v-if="fieldError('docente', 'especialidad')" class="field-error">{{ fieldError('docente', 'especialidad') }}</small></div>
            <div class="field"><label for="teacher-phone">Teléfono</label><input id="teacher-phone" v-model.trim="forms.docente.telefono" :class="{ invalid: fieldError('docente', 'telefono') }" inputmode="tel" maxlength="20" placeholder="Ej. 9511234567" @input="touchField('docente', 'telefono')" /><small v-if="fieldError('docente', 'telefono')" class="field-error">{{ fieldError('docente', 'telefono') }}</small></div>
            <div class="field"><label for="teacher-email">Correo electrónico</label><input id="teacher-email" v-model.trim="forms.docente.correo" :class="{ invalid: fieldError('docente', 'correo') }" type="email" inputmode="email" maxlength="160" placeholder="nombre@dominio.com" @input="touchField('docente', 'correo')" /><small v-if="fieldError('docente', 'correo')" class="field-error">{{ fieldError('docente', 'correo') }}</small></div>
            <div class="field field-wide"><label for="teacher-address">Dirección</label><input id="teacher-address" v-model.trim="forms.docente.direccion" :class="{ invalid: fieldError('docente', 'direccion') }" maxlength="255" placeholder="Calle, número, colonia y municipio" @input="touchField('docente', 'direccion')" /><small v-if="fieldError('docente', 'direccion')" class="field-error">{{ fieldError('docente', 'direccion') }}</small></div>
          </div>
          <div class="form-actions"><button type="button" @click="resetForm('docente')">Limpiar</button><button class="primary-button" type="button" :disabled="isSaving || !docenteFormValid" @click="saveRecord('docente', createTeacher, updateTeacher)">{{ editing.docente ? 'Actualizar' : 'Guardar docente' }}</button></div>
        </div>
        </div>
        <div class="panel">
          <div class="panel-heading"><div><h2>Listado de docentes</h2><p>Vista compacta como en el panel de referencia.</p></div></div>
          <div class="table-wrap table-card"><table class="compact-table"><thead><tr><th>Docente</th><th>Especialidad</th><th>Correo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody><tr v-for="item in data.docentes" :key="item.id"><td>{{ item.nombre }} {{ item.apellidoPaterno }}</td><td>{{ item.especialidad || '-' }}</td><td>{{ item.correo || '-' }}</td><td><span class="status active">{{ item.estado || 'ACTIVO' }}</span></td><td><button class="table-action" type="button" @click="editRecord('docente', item)"><PencilLine class="action-icon small" />Editar</button><button class="table-action danger" type="button" @click="runAction(() => deactivateTeacher(item.id), 'Docente desactivado.')"><Trash2 class="action-icon small" />Dar de baja</button></td></tr></tbody></table></div>
        </div>
      </section>

      <section v-else-if="activeView === 'alumnos'" class="section-grid">
        <div class="panel">
          <div class="panel-heading"><div><h2>Registrar alumno</h2><p>Completa los datos tal como aparecen en la referencia visual.</p></div></div>
        <div class="form-card">
          <p class="form-note"><strong>*</strong> Campos obligatorios. Verifica la matrícula y los datos de contacto antes de guardar.</p>
          <div class="form-grid">
            <div class="field"><label for="student-name">Nombre <span>*</span></label><input id="student-name" v-model.trim="forms.alumno.nombre" :class="{ invalid: fieldError('alumno', 'nombre') }" maxlength="80" placeholder="Ej. Juan Carlos" @input="touchField('alumno', 'nombre')" /><small v-if="fieldError('alumno', 'nombre')" class="field-error">{{ fieldError('alumno', 'nombre') }}</small></div>
            <div class="field"><label for="student-last-name">Apellido paterno <span>*</span></label><input id="student-last-name" v-model.trim="forms.alumno.apellidoPaterno" :class="{ invalid: fieldError('alumno', 'apellidoPaterno') }" maxlength="80" placeholder="Ej. Hernández" @input="touchField('alumno', 'apellidoPaterno')" /><small v-if="fieldError('alumno', 'apellidoPaterno')" class="field-error">{{ fieldError('alumno', 'apellidoPaterno') }}</small></div>
            <div class="field"><label for="student-maternal">Apellido materno</label><input id="student-maternal" v-model.trim="forms.alumno.apellidoMaterno" :class="{ invalid: fieldError('alumno', 'apellidoMaterno') }" maxlength="80" placeholder="Ej. Martínez" @input="touchField('alumno', 'apellidoMaterno')" /><small v-if="fieldError('alumno', 'apellidoMaterno')" class="field-error">{{ fieldError('alumno', 'apellidoMaterno') }}</small></div>
            <div class="field"><label for="student-enrollment">Matrícula <span>*</span></label><input id="student-enrollment" v-model.trim="forms.alumno.matricula" :class="{ invalid: fieldError('alumno', 'matricula') }" maxlength="30" placeholder="Ej. ALU-2026-001" @input="touchField('alumno', 'matricula')" /><small v-if="fieldError('alumno', 'matricula')" class="field-error">{{ fieldError('alumno', 'matricula') }}</small></div>
            <div class="field"><label for="student-birth-date">Fecha de nacimiento</label><input id="student-birth-date" v-model="forms.alumno.fechaNacimiento" :class="{ invalid: fieldError('alumno', 'fechaNacimiento') }" type="date" :max="new Date().toISOString().slice(0, 10)" @input="touchField('alumno', 'fechaNacimiento')" /><small v-if="fieldError('alumno', 'fechaNacimiento')" class="field-error">{{ fieldError('alumno', 'fechaNacimiento') }}</small></div>
            <div class="field"><label for="student-phone">Teléfono</label><input id="student-phone" v-model.trim="forms.alumno.telefono" :class="{ invalid: fieldError('alumno', 'telefono') }" inputmode="tel" maxlength="20" placeholder="Ej. 9511234567" @input="touchField('alumno', 'telefono')" /><small v-if="fieldError('alumno', 'telefono')" class="field-error">{{ fieldError('alumno', 'telefono') }}</small></div>
            <div class="field"><label for="student-email">Correo electrónico</label><input id="student-email" v-model.trim="forms.alumno.correo" :class="{ invalid: fieldError('alumno', 'correo') }" type="email" inputmode="email" maxlength="160" placeholder="nombre@dominio.com" @input="touchField('alumno', 'correo')" /><small v-if="fieldError('alumno', 'correo')" class="field-error">{{ fieldError('alumno', 'correo') }}</small></div>
            <div class="field field-wide"><label for="student-address">Dirección</label><input id="student-address" v-model.trim="forms.alumno.direccion" :class="{ invalid: fieldError('alumno', 'direccion') }" maxlength="255" placeholder="Calle, número, colonia y municipio" @input="touchField('alumno', 'direccion')" /><small v-if="fieldError('alumno', 'direccion')" class="field-error">{{ fieldError('alumno', 'direccion') }}</small></div>
          </div>
          <div class="form-actions"><button type="button" @click="resetForm('alumno')">Limpiar</button><button class="primary-button" type="button" :disabled="isSaving || !alumnoFormValid" @click="saveRecord('alumno', createAlumno, updateStudent)">{{ editing.alumno ? 'Actualizar' : 'Guardar alumno' }}</button></div>
        </div>
        </div>
        <div class="panel">
          <div class="panel-heading"><div><h2>Listado de alumnos</h2><p>Filas limpias con acciones de edición y baja.</p></div></div>
          <input v-model="search" class="search-input" placeholder="Buscar por nombre o matrícula" />
          <div class="table-wrap table-card"><table class="compact-table"><thead><tr><th>Alumno</th><th>Matrícula</th><th>Correo</th><th>Estado</th><th>Acción</th></tr></thead><tbody><tr v-for="item in filteredStudents" :key="item.id"><td><div class="user-name-cell"><img v-if="item.fotoUrl" :src="item.fotoUrl" class="table-avatar-img" /><span>{{ item.nombre }} {{ item.apellidoPaterno }}</span></div></td><td>{{ item.matricula || '-' }}</td><td>{{ item.correo || '-' }}</td><td><span class="status active">{{ item.estado || 'ACTIVO' }}</span></td><td><button class="table-action" type="button" @click="editRecord('alumno', item)"><PencilLine class="action-icon small" />Editar</button></td></tr></tbody></table></div>
        </div>
      </section>

      <section v-else-if="activeView === 'cursos'" class="panel full-panel section-single">
        <div class="panel-heading"><div><h2>Cursos y grupos</h2><p>Administra la oferta académica y sus grupos.</p></div></div>
        <div class="form-card"><h3>Curso</h3><div class="inline-form"><input v-model="forms.curso.nombre" placeholder="Nombre del curso" /><button class="primary-button" type="button" :disabled="isSaving" @click="saveRecord('curso', createCourse, updateCourse)">Guardar curso</button></div></div>
        <div class="form-card"><h3>Grupo</h3><div class="form-grid"><select v-model="forms.grupo.ofertaId"><option value="">Oferta</option><option v-for="item in data.ofertas" :key="item.id" :value="item.id">{{ item.tipo || item.nombre }}</option></select><select v-model="forms.grupo.categoriaId"><option value="">Categoría de edad</option><option v-for="item in data.categorias" :key="item.id" :value="item.id">{{ item.nombre }}</option></select><input v-model="forms.grupo.nombreGrupo" placeholder="Nombre del grupo" /><button class="primary-button" type="button" :disabled="isSaving" @click="saveRecord('grupo', createGroup, updateGroup)">Guardar grupo</button></div></div>
        <div class="table-wrap"><table><thead><tr><th>Grupo</th><th>Curso</th><th>Estado</th><th>Acción</th></tr></thead><tbody><tr v-for="item in data.grupos" :key="item.id"><td>{{ item.nombreGrupo }}</td><td>{{ item.curso || item.nombreCurso || '-' }}</td><td><span class="status active">{{ item.estado || 'ACTIVO' }}</span></td><td><button class="table-action" type="button" @click="editRecord('grupo', item)">Editar</button></td></tr></tbody></table></div>
      </section>

      <section v-else-if="activeView === 'inscripciones'" class="panel full-panel section-single"><div class="panel-heading"><div><h2>Inscripciones</h2><p>Consulta las asignaciones de alumnos a cursos.</p></div></div><div class="table-wrap table-card"><table class="compact-table"><thead><tr><th>Alumno</th><th>Grupo</th><th>Fecha</th><th>Estado</th></tr></thead><tbody><tr v-for="item in data.inscripciones" :key="item.id"><td>{{ item.matricula || item.alumno || '-' }}</td><td>{{ item.grupo || item.nombreGrupo || '-' }}</td><td>{{ formatDate(item.fechaInscripcion) }}</td><td><span class="status active">{{ item.estado }}</span></td></tr></tbody></table></div></section>

      <section v-else-if="activeView === 'asistencias'" class="panel full-panel section-single"><div class="panel-heading"><div><h2>Asistencias</h2><p>Registra asistencias manuales y consulta el historial.</p></div><button class="primary-button" type="button" @click="runAction(() => generateAbsences(), 'Faltas generadas correctamente.')"><CalendarCheck2 class="btn-icon" />Generar faltas de hoy</button></div><div class="form-card"><div class="form-grid"><select v-model="forms.asistencia.inscripcionId"><option value="">Inscripción</option><option v-for="item in data.inscripciones" :key="item.id" :value="item.id">{{ item.matricula || item.alumno }} · {{ item.grupo }}</option></select><input v-model="forms.asistencia.horarioId" type="number" placeholder="ID del horario" /><input v-model="forms.asistencia.fecha" type="date" /><select v-model="forms.asistencia.estado"><option>PRESENTE</option><option>RETARDO</option><option>FALTA</option></select><button class="primary-button" type="button" @click="runAction(() => registerAttendance(forms.asistencia), 'Asistencia registrada.')"><Save class="btn-icon" />Registrar asistencia</button></div></div><div class="table-wrap table-card"><table class="compact-table"><thead><tr><th>Fecha</th><th>Alumno</th><th>Grupo</th><th>Estado</th></tr></thead><tbody><tr v-for="item in data.asistencias" :key="item.id"><td>{{ formatDate(item.fecha) }}</td><td>{{ item.matricula || item.alumno || '-' }}</td><td>{{ item.grupo || '-' }}</td><td><span class="status" :class="String(item.estado).toLowerCase()">{{ item.estado }}</span></td></tr></tbody></table></div></section>

      <section v-else-if="activeView === 'pagos'" class="panel full-panel section-single"><div class="panel-heading"><div><h2>Pagos</h2><p>Registra y consulta pagos de inscripciones.</p></div></div><div class="form-card"><div class="form-grid"><select v-model="forms.pago.inscripcionId"><option value="">Inscripción</option><option v-for="item in data.inscripciones" :key="item.id" :value="item.id">{{ item.matricula || item.alumno }} · {{ item.grupo }}</option></select><select v-model="forms.pago.tipoPago"><option>INSCRIPCION</option><option>MENSUALIDAD</option><option>RECARGO</option></select><input v-model="forms.pago.periodo" placeholder="Periodo" /><input v-model="forms.pago.fechaVencimiento" type="date" /><input v-model="forms.pago.fechaPago" type="date" /><select v-model="forms.pago.estado"><option>PENDIENTE</option><option>PAGADO</option><option>VENCIDO</option></select><button class="primary-button" type="button" @click="runAction(() => createPayment(forms.pago), 'Pago registrado.')"><CreditCard class="btn-icon" />Registrar pago</button></div></div><div class="table-wrap table-card"><table class="compact-table"><thead><tr><th>Concepto</th><th>Periodo</th><th>Vencimiento</th><th>Pago</th><th>Estado</th></tr></thead><tbody><tr v-for="item in data.pagos" :key="item.id"><td>{{ item.tipoPago }}</td><td>{{ item.periodo || '-' }}</td><td>{{ formatDate(item.fechaVencimiento) }}</td><td>{{ formatDate(item.fechaPago) }}</td><td><span class="status active">{{ item.estado }}</span></td></tr></tbody></table></div></section>

      <section v-else-if="activeView === 'usuarios'" class="panel full-panel section-single"><div class="panel-heading"><div><h2>Usuarios y contraseñas</h2><p>Crea accesos y administra altas y bajas.</p></div></div><div class="form-card"><div class="form-grid"><select v-model="forms.usuario.personaId"><option value="">Persona</option><option v-for="item in [...data.alumnos, ...data.docentes]" :key="item.id" :value="item.id">{{ item.nombre }} {{ item.apellidoPaterno }}</option></select><select v-model="forms.usuario.rolId"><option value="">Rol</option><option v-for="item in data.roles" :key="item.id" :value="item.id">{{ item.nombre }}</option></select><input v-model="forms.usuario.nombreUsuario" placeholder="Nombre de usuario" /><input v-model="forms.usuario.password" type="password" placeholder="Contraseña inicial" /><button class="primary-button" type="button" @click="runAction(() => createUser(forms.usuario), 'Usuario creado correctamente.')"><ShieldCheck class="btn-icon" />Crear usuario</button></div></div><div class="table-wrap table-card"><table class="compact-table"><thead><tr><th>Usuario</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead><tbody><tr v-for="item in data.usuarios" :key="item.id"><td>{{ item.nombreUsuario }}</td><td>{{ item.rol || item.nombreRol || '-' }}</td><td><span class="status active">{{ item.estado || 'ACTIVO' }}</span></td><td><button class="table-action danger" type="button" @click="runAction(() => deactivateUser(item.id), 'Usuario dado de baja.')"><Trash2 class="action-icon small" />Dar de baja</button><button class="table-action" type="button" @click="runAction(() => resetUserPassword(item.id, 'CasaCultura123!'), 'Contraseña restablecida.')"><RotateCcw class="action-icon small" />Restablecer contraseña</button></td></tr></tbody></table></div></section>

      <section v-else class="panel full-panel section-single"><div class="panel-heading"><div><h2>Reportes y estadísticas</h2><p>Consulta el resumen de asistencias por periodo.</p></div></div><div class="report-filter"><input v-model="reportDates.from" type="date" /><input v-model="reportDates.to" type="date" /><button class="primary-button" type="button" @click="loadReport"><BarChart3 class="btn-icon" />Consultar reporte</button></div><div v-if="report" class="report-grid"><article><strong>{{ report.total }}</strong><small>Total de registros</small></article><article><strong>{{ report.presentes }}</strong><small>Presentes</small></article><article><strong>{{ report.retardos }}</strong><small>Retardos</small></article><article><strong>{{ report.faltas }}</strong><small>Faltas</small></article></div><p v-else class="empty-text">Selecciona un periodo para consultar estadísticas.</p></section>
    </template>
  </DashboardLayout>
</template>

<style scoped>
.page-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}.eyebrow{color:#5363e5;font-size:9px;font-weight:800;letter-spacing:.03em}h1,h2,h3,p{margin:0}h1{margin-top:3px;color:#252b44;font-size:23px}.page-heading p,.panel-heading p{margin-top:4px;color:#9299aa;font-size:10px}.heading-icon{width:43px;height:43px;display:grid;place-items:center;border-radius:10px;color:#5664e1;background:#eef0ff}.heading-icon-svg,.stat-icon-svg{width:18px;height:18px}.stat-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:13px;margin-bottom:16px}.stat-grid article{display:grid;grid-template-columns:37px 1fr;column-gap:10px;align-items:center;padding:15px;border:1px solid #e4e7ef;border-radius:8px;background:#fff}.stat-icon{grid-row:span 2;width:37px;height:37px;display:grid;place-items:center;border-radius:8px;font-size:19px}.stat-icon.purple{color:#636fe4;background:#eeefff}.stat-icon.blue{color:#3a91d4;background:#e9f5ff}.stat-icon.green{color:#36a378;background:#e8f8f1}.stat-icon.orange{color:#d58b46;background:#fff3e7}.stat-grid strong{font-size:20px}.stat-grid small{color:#9299aa;font-size:9px}.content-grid,.section-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.section-single{grid-template-columns:1fr}.panel,.state-card{border:1px solid #e2e5ed;border-radius:8px;background:#fff}.panel{padding:18px 16px}.full-panel{min-height:280px}.panel-heading{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:15px}.panel-heading h2{font-size:13px}.panel-heading button{border:0;color:#5664df;background:transparent;font-size:10px;cursor:pointer}.quick-actions{display:grid;gap:9px}.quick-actions button{display:flex;align-items:center;gap:8px;padding:12px;border:1px solid #edf0f5;border-radius:6px;color:#4c5871;background:#fafbfe;text-align:left;font-size:10px;cursor:pointer}.record-list{display:grid;gap:10px}.record-row{display:flex;align-items:center;gap:10px;padding:10px;border:1px solid #edf0f5;border-radius:6px}.record-row div{flex:1}.record-row strong,.record-row small{display:block}.record-row strong{font-size:11px}.record-row small{color:#969dad;font-size:9px}.record-icon{width:30px;height:30px;display:grid;place-items:center;border-radius:7px;color:#5967df;background:#eff0ff}.record-icon-svg{width:16px;height:16px}.status{padding:4px 7px;border-radius:10px;font-size:8px;font-weight:800;text-transform:capitalize}.status.active,.status.presente{color:#19865d;background:#e5f7ef}.status.retardo,.status.pendiente{color:#b2782d;background:#fff3dd}.status.falta,.status.vencido{color:#bc5361;background:#ffebee}.feedback{margin:0 0 12px;font-size:11px}.feedback.error,.danger{color:#a82d43}.feedback.success{color:#19865d}.state-card{padding:25px;color:#9299aa;font-size:11px}.primary-button{display:inline-flex;align-items:center;justify-content:center;gap:7px;padding:9px 13px!important;border:0!important;border-radius:5px!important;color:#fff!important;background:#5363e5!important;font-size:10px!important;cursor:pointer}.btn-icon,.action-icon{width:14px;height:14px;flex:0 0 auto}.action-icon.small{width:12px;height:12px;margin-right:4px}.primary-button:disabled{opacity:.6}.form-card{margin-bottom:18px;padding:15px;border:1px solid #e3e6ff;border-radius:7px;background:#fafbff}.form-card h3{margin-bottom:10px;font-size:12px}.form-note{margin-bottom:12px;color:#737d93;font-size:10px}.form-note strong,label span{color:#a82d43}.form-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:9px}.field{min-width:0}.field-wide{grid-column:span 2}.field label{display:block;margin-bottom:5px;color:#4c556c;font-size:10px;font-weight:700}.field input{width:100%;box-sizing:border-box}.field-error{display:block;margin-top:4px;color:#b23a4c;font-size:9px;line-height:1.3}.invalid{border-color:#cf5365!important;background:#fff8f9!important}.inline-form,.report-filter{display:flex;gap:9px}.inline-form input{flex:1}input,select{min-width:0;height:34px;padding:0 9px;border:1px solid #dfe3ed;border-radius:5px;outline:none;color:#4c556c;background:#fff;font:inherit;font-size:10px}input:focus,select:focus{border-color:#5363e5}.form-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:12px}.form-actions button:first-child{border:0;color:#737d93;background:transparent;font-size:10px;cursor:pointer}.table-wrap{overflow-x:auto}.table-card{border:1px solid #edf0f5;border-radius:7px}.compact-table{width:100%;border-collapse:collapse;font-size:10px}.compact-table thead th{padding:11px 10px;color:#9aa1b3;font-size:9px;text-align:left;font-weight:700;letter-spacing:.01em}.compact-table td{padding:13px 10px;border-top:1px solid #f1f3f8;white-space:nowrap;color:#4c556c}.table-action{display:inline-flex;align-items:center;gap:0;margin-right:8px;border:0;color:#5664df;background:transparent;font-size:10px;cursor:pointer}.search-input{width:260px;margin-bottom:12px}.report-filter{margin-bottom:20px}.report-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.report-grid article{padding:20px;border:1px solid #edf0f5;border-radius:7px;background:#fff}.report-grid strong,.report-grid small{display:block}.report-grid strong{font-size:25px;color:#5363e5}.report-grid small,.empty-text{margin-top:5px;color:#9299aa;font-size:10px}.record-avatar-img{width:100%;height:100%;border-radius:7px;object-fit:cover;display:block}.table-avatar-img{width:26px;height:26px;border-radius:50%;object-fit:cover;display:block;flex-shrink:0}.user-name-cell{display:flex;align-items:center;gap:8px}@media(max-width:900px){.stat-grid,.content-grid,.section-grid{grid-template-columns:repeat(2,1fr)}.section-single{grid-template-columns:1fr}.form-grid{grid-template-columns:repeat(2,1fr)}.report-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:580px){.stat-grid,.form-grid,.report-grid,.content-grid,.section-grid{grid-template-columns:1fr}.field-wide{grid-column:auto}.inline-form,.report-filter{display:grid}.search-input{width:100%}}
</style>
