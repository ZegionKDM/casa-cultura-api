<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import DashboardLayout from '../components/DashboardLayout.vue'
import { clearStoredSession } from '../services/apiService'
import { createStudent, enrollStudent, generateStudentUser, getSupervisorData, updateStudent } from '../services/supervisorService'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeView = ref('resumen')
const data = reactive({ alumnos: [], grupos: [], inscripciones: [] })
const isLoading = ref(true)
const errorMessage = ref('')
const successMessage = ref('')
const showStudentForm = ref(false)
const editingStudent = ref(null)
const selectedStudent = ref('')
const selectedGroup = ref('')
const studentSearch = ref('')
const isSaving = ref(false)
const isGeneratingAccess = ref(false)
const generatedCredentials = ref(null)
const credentialStudent = ref(null)
const initialPassword = ref('')
const savedStudentCredentials = reactive(JSON.parse(localStorage.getItem('casa-cultura-student-credentials') || '{}'))
const currentUser = JSON.parse(localStorage.getItem('casa-cultura-user') || sessionStorage.getItem('casa-cultura-user') || '{}')

const studentForm = reactive({
  nombre: '', apellidoPaterno: '', apellidoMaterno: '', fechaNacimiento: '',
  telefono: '', direccion: '', correo: '', fotoUrl: '', matricula: '',
})

const navItems = [
  { id: 'resumen', label: 'Mi Dashboard', icon: '▦' },
  { id: 'alumnos', label: 'Alumnos', icon: '♙' },
  { id: 'inscripciones', label: 'Inscripciones', icon: '▣' },
]

const stats = computed(() => [
  { value: data.alumnos.length, label: 'Alumnos registrados', icon: '♙', color: 'purple' },
  { value: data.grupos.length, label: 'Cursos disponibles', icon: '▣', color: 'blue' },
  { value: data.inscripciones.length, label: 'Inscripciones', icon: '✓', color: 'green' },
])

const filteredStudents = computed(() => {
  const query = studentSearch.value.trim().toLowerCase()
  if (!query) return data.alumnos
  return data.alumnos.filter((student) =>
    `${student.nombre} ${student.apellidoPaterno} ${student.apellidoMaterno || ''} ${student.matricula}`
      .toLowerCase()
      .includes(query),
  )
})

onMounted(loadData)

async function loadData() {
  isLoading.value = true
  try {
    Object.assign(data, await getSupervisorData())
  } catch (error) {
    errorMessage.value = error.message
    if (error.message.includes('401') || error.message.includes('403')) {
      clearStoredSession()
      router.push('/')
    }
  } finally {
    isLoading.value = false
  }
}

function resetForm() {
  Object.keys(studentForm).forEach((key) => { studentForm[key] = '' })
  editingStudent.value = null
}

function editStudent(student) {
  Object.assign(studentForm, {
    nombre: student.nombre,
    apellidoPaterno: student.apellidoPaterno,
    apellidoMaterno: student.apellidoMaterno || '',
    fechaNacimiento: student.fechaNacimiento || '',
    telefono: student.telefono || '',
    direccion: student.direccion || '',
    correo: student.correo || '',
    fotoUrl: student.fotoUrl || '',
    matricula: student.matricula,
  })
  editingStudent.value = student
  showStudentForm.value = true
}

async function saveStudent() {
  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    if (editingStudent.value) {
      await updateStudent(editingStudent.value.id, studentForm)
      successMessage.value = 'Alumno actualizado correctamente.'
    } else {
      const response = await createStudent(studentForm)
      successMessage.value = response?.qrCredential
        ? `Alumno creado. Credencial QR: ${response.qrCredential}`
        : 'Alumno creado correctamente.'
    }
    showStudentForm.value = false
    resetForm()
    await loadData()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isSaving.value = false
  }
}

async function saveEnrollment() {
  if (!selectedStudent.value || !selectedGroup.value) return
  try {
    await enrollStudent(Number(selectedStudent.value), Number(selectedGroup.value))
    successMessage.value = 'Alumno asignado al curso correctamente.'
    selectedStudent.value = ''
    selectedGroup.value = ''
    studentSearch.value = ''
    await loadData()
  } catch (error) {
    errorMessage.value = error.message
  }
  }

  function selectStudent(student) {
    selectedStudent.value = String(student.id)
    studentSearch.value = `${student.nombre} ${student.apellidoPaterno} · ${student.matricula}`
}

function startGenerateAccess(student) {
  credentialStudent.value = student
  initialPassword.value = ''
}

function cancelGenerateAccess() {
  credentialStudent.value = null
  initialPassword.value = ''
}

async function generateAccess() {
  if (!credentialStudent.value || initialPassword.value.length < 8) {
    errorMessage.value = 'La contraseña debe tener al menos 8 caracteres.'
    return
  }
  isGeneratingAccess.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    generatedCredentials.value = await generateStudentUser(credentialStudent.value.id, initialPassword.value)
    savedStudentCredentials[credentialStudent.value.id] = generatedCredentials.value
    localStorage.setItem('casa-cultura-student-credentials', JSON.stringify(savedStudentCredentials))
    cancelGenerateAccess()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isGeneratingAccess.value = false
  }
}

function viewAccess(student) {
  generatedCredentials.value = savedStudentCredentials[student.id]
}
</script>

<template>
  <DashboardLayout role="SUPERVISOR" :username="currentUser.nombreUsuario || 'Supervisor'" :items="navItems" :active="activeView" @navigate="activeView = $event">
    <section class="page-heading">
      <div><span class="eyebrow">PANEL DEL SUPERVISOR</span><h1>{{ navItems.find((item) => item.id === activeView)?.label }}</h1><p>Apoya la administración escolar de la Casa de la Cultura.</p></div>
      <div class="heading-icon">♙</div>
    </section>

    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="feedback success">{{ successMessage }}</p>
    <div v-if="isLoading" class="state-card">Cargando información...</div>

    <template v-else>
      <template v-if="activeView === 'resumen'">
        <div class="stat-grid">
          <article v-for="stat in stats" :key="stat.label"><span class="stat-icon" :class="stat.color">{{ stat.icon }}</span><strong>{{ stat.value }}</strong><small>{{ stat.label }}</small></article>
        </div>
        <div class="content-grid">
          <section class="panel"><div class="panel-heading"><div><h2>Acciones rápidas</h2><p>Operaciones permitidas para tu rol.</p></div></div><div class="quick-actions"><button type="button" @click="activeView = 'alumnos'; showStudentForm = true">＋ Registrar alumno</button><button type="button" @click="activeView = 'inscripciones'">▣ Asignar alumno a curso</button><button type="button" @click="activeView = 'alumnos'">♙ Consultar alumnos</button></div></section>
          <section class="panel"><div class="panel-heading"><div><h2>Últimos alumnos</h2><p>Registros más recientes.</p></div><button type="button" @click="activeView = 'alumnos'">Ver todos</button></div><div class="course-list"><div v-for="student in data.alumnos.slice(-4).reverse()" :key="student.id" class="course-row"><span class="course-icon">♙</span><div><strong>{{ student.nombre }} {{ student.apellidoPaterno }}</strong><small>{{ student.matricula }}</small></div><span class="status active">{{ student.estado }}</span></div></div></section>
        </div>
      </template>

      <section v-else-if="activeView === 'alumnos'" class="panel full-panel">
        <div class="panel-heading"><div><h2>Alumnos</h2><p>Registra y actualiza la información de los alumnos.</p></div><button class="primary-button" type="button" @click="resetForm(); showStudentForm = true">+ Nuevo alumno</button></div>
        <div v-if="showStudentForm" class="form-card"><h3>{{ editingStudent ? 'Actualizar alumno' : 'Registrar alumno' }}</h3><div class="form-grid"><input v-model="studentForm.nombre" placeholder="Nombre" /><input v-model="studentForm.apellidoPaterno" placeholder="Apellido paterno" /><input v-model="studentForm.apellidoMaterno" placeholder="Apellido materno" /><input v-model="studentForm.matricula" placeholder="Matrícula" /><input v-model="studentForm.fechaNacimiento" type="date" /><input v-model="studentForm.telefono" placeholder="Teléfono" /><input v-model="studentForm.correo" type="email" placeholder="Correo electrónico" /><input v-model="studentForm.direccion" placeholder="Dirección" /></div><div class="form-actions"><button type="button" @click="showStudentForm = false">Cancelar</button><button class="primary-button" type="button" :disabled="isSaving" @click="saveStudent">{{ isSaving ? 'Guardando...' : 'Guardar alumno' }}</button></div></div>
        <div class="table-wrap"><table><thead><tr><th>Alumno</th><th>Matrícula</th><th>Correo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody><tr v-for="student in data.alumnos" :key="student.id"><td>{{ student.nombre }} {{ student.apellidoPaterno }}</td><td>{{ student.matricula }}</td><td>{{ student.correo || '-' }}</td><td><span class="status active">{{ student.estado }}</span></td><td class="actions-cell"><button class="table-action" type="button" @click="editStudent(student)">Editar</button><button v-if="savedStudentCredentials[student.id]" class="table-action" type="button" @click="viewAccess(student)">Ver acceso</button><button v-else class="table-action" type="button" :disabled="isGeneratingAccess" @click="startGenerateAccess(student)">Generar acceso</button></td></tr></tbody></table></div>
      </section>

      <section v-else class="panel full-panel">
        <div class="panel-heading"><div><h2>Asignar alumnos a cursos</h2><p>Consulta inscripciones y registra nuevas asignaciones.</p></div></div>
        <div class="enrollment-form">
          <div class="student-picker">
            <input v-model="studentSearch" placeholder="Buscar alumno por nombre o matrícula" autocomplete="off" @input="selectedStudent = ''" />
            <div v-if="studentSearch && !selectedStudent" class="student-results">
              <button v-for="student in filteredStudents" :key="student.id" type="button" @click="selectStudent(student)">
                <strong>{{ student.nombre }} {{ student.apellidoPaterno }}</strong>
                <small>{{ student.matricula }}</small>
              </button>
              <span v-if="!filteredStudents.length" class="empty-results">No se encontraron alumnos.</span>
            </div>
          </div>
          <select v-model="selectedGroup"><option value="">Selecciona un curso o grupo</option><option v-for="group in data.grupos" :key="group.id" :value="group.id">{{ group.nombreGrupo }}</option></select>
          <button class="primary-button" type="button" :disabled="!selectedStudent || !selectedGroup" @click="saveEnrollment">Asignar</button>
        </div>
        <div class="table-wrap"><table><thead><tr><th>Alumno</th><th>Grupo</th><th>Fecha</th><th>Estado</th></tr></thead><tbody><tr v-for="enrollment in data.inscripciones" :key="enrollment.id"><td>{{ enrollment.matricula }}</td><td>{{ enrollment.grupo }}</td><td>{{ enrollment.fechaInscripcion }}</td><td><span class="status active">{{ enrollment.estado }}</span></td></tr></tbody></table></div>
      </section>
    </template>

    <div v-if="generatedCredentials" class="modal-backdrop" @click.self="generatedCredentials = null">
      <section class="credentials-modal">
        <button class="modal-close" type="button" aria-label="Cerrar" @click="generatedCredentials = null">×</button>
        <span class="eyebrow">ACCESO DEL ALUMNO</span>
        <h2>Credenciales generadas</h2>
        <p>Comparte estos datos con el alumno. La contraseña temporal se muestra únicamente en este momento.</p>
        <div class="credential-row"><span>Usuario</span><strong>{{ generatedCredentials.nombreUsuario }}</strong></div>
        <div class="credential-row"><span>Contraseña temporal</span><strong>{{ generatedCredentials.passwordTemporal }}</strong></div>
        <button class="primary-button modal-button" type="button" @click="generatedCredentials = null">Entendido</button>
      </section>
    </div>

    <div v-if="credentialStudent" class="modal-backdrop" @click.self="cancelGenerateAccess">
      <section class="credentials-modal">
        <button class="modal-close" type="button" aria-label="Cerrar" @click="cancelGenerateAccess">×</button>
        <span class="eyebrow">NUEVO ACCESO</span>
        <h2>Crear contraseña inicial</h2>
        <p>Define la contraseña que entregarás al alumno. Al iniciar sesión, deberá cambiarla.</p>
        <label for="initial-password">Contraseña inicial</label>
        <input id="initial-password" v-model="initialPassword" type="password" minlength="8" autocomplete="new-password" placeholder="Mínimo 8 caracteres" />
        <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
        <div class="form-actions">
          <button type="button" @click="cancelGenerateAccess">Cancelar</button>
          <button class="primary-button" type="button" :disabled="isGeneratingAccess" @click="generateAccess">{{ isGeneratingAccess ? 'Generando...' : 'Crear acceso' }}</button>
        </div>
      </section>
    </div>
  </DashboardLayout>
</template>

<style scoped>
.page-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; } .eyebrow { color: #5363e5; font-size: 9px; font-weight: 800; } h1, h2, h3, p { margin: 0; } h1 { margin-top: 3px; font-size: 23px; } .page-heading p, .panel-heading p { margin-top: 4px; color: #9299aa; font-size: 10px; } .heading-icon { width: 43px; height: 43px; display: grid; place-items: center; border-radius: 10px; color: #5664e1; background: #eef0ff; font-size: 22px; }
.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 13px; margin-bottom: 16px; } .stat-grid article { display: grid; grid-template-columns: 37px 1fr; column-gap: 10px; align-items: center; padding: 15px; border: 1px solid #e4e7ef; border-radius: 8px; background: #fff; } .stat-icon { grid-row: span 2; width: 37px; height: 37px; display: grid; place-items: center; border-radius: 8px; font-size: 19px; } .stat-icon.purple { color: #636fe4; background: #eeefff; } .stat-icon.blue { color: #3a91d4; background: #e9f5ff; } .stat-icon.green { color: #36a378; background: #e8f8f1; } .stat-grid strong { font-size: 20px; } .stat-grid small { color: #9299aa; font-size: 9px; }
.content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; } .panel, .state-card { border: 1px solid #e2e5ed; border-radius: 8px; background: #fff; } .panel { padding: 18px 16px; } .full-panel { min-height: 280px; } .panel-heading { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 15px; } .panel-heading h2 { font-size: 13px; } .panel-heading button { border: 0; color: #5664df; background: transparent; font-size: 10px; cursor: pointer; }
.quick-actions { display: grid; gap: 9px; } .quick-actions button { padding: 12px; border: 1px solid #edf0f5; border-radius: 6px; color: #4c5871; background: #fafbfe; text-align: left; font-size: 10px; cursor: pointer; } .quick-actions button:hover { border-color: #cdd2ff; color: #5664df; }
.course-list { display: grid; gap: 10px; } .course-row { display: flex; align-items: center; gap: 10px; padding: 10px; border: 1px solid #edf0f5; border-radius: 6px; } .course-icon { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 7px; color: #5967df; background: #eff0ff; } .course-row div { flex: 1; } .course-row strong { display: block; font-size: 11px; } .course-row small { color: #969dad; font-size: 9px; }
.status { padding: 4px 7px; border-radius: 10px; font-size: 8px; font-weight: 800; text-transform: capitalize; } .status.active { color: #19865d; background: #e5f7ef; } .feedback { margin: 0 0 12px; font-size: 11px; } .feedback.error { color: #a82d43; } .feedback.success { color: #19865d; } .state-card { padding: 25px; color: #9299aa; font-size: 11px; }
.primary-button { padding: 9px 13px !important; border: 0 !important; border-radius: 5px !important; color: #fff !important; background: #5363e5 !important; font-size: 10px !important; cursor: pointer; } .primary-button:disabled { opacity: .6; }
.form-card { margin-bottom: 18px; padding: 15px; border: 1px solid #e3e6ff; border-radius: 7px; background: #fafbff; } .form-card h3 { margin-bottom: 12px; font-size: 12px; } .form-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 9px; } input, select { min-width: 0; height: 34px; padding: 0 9px; border: 1px solid #dfe3ed; border-radius: 5px; outline: none; color: #4c556c; background: #fff; font: inherit; font-size: 10px; } input:focus, select:focus { border-color: #5363e5; } .form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; } .form-actions button:first-child { border: 0; color: #737d93; background: transparent; font-size: 10px; cursor: pointer; }
.table-wrap { overflow-x: auto; } table { width: 100%; border-collapse: collapse; font-size: 10px; } th { color: #8d95a7; font-size: 9px; text-align: left; } th, td { padding: 12px 8px; border-bottom: 1px solid #eef0f5; white-space: nowrap; } td { color: #4c556c; } .table-action { border: 0; color: #5664df; background: transparent; font-size: 10px; cursor: pointer; }
.actions-cell { display: flex; gap: 8px; } .table-action:disabled { opacity: .5; cursor: wait; }
.enrollment-form { display: grid; grid-template-columns: 1fr 1fr auto; gap: 9px; margin-bottom: 20px; } .enrollment-form .primary-button { height: 34px; } .student-picker { position: relative; } .student-results { position: absolute; z-index: 5; top: 39px; left: 0; right: 0; max-height: 190px; overflow-y: auto; border: 1px solid #dfe3ed; border-radius: 5px; background: #fff; box-shadow: 0 8px 18px rgba(41, 49, 88, .12); } .student-results button { display: block; width: 100%; padding: 9px 10px; border: 0; border-bottom: 1px solid #eef0f5; color: #4c556c; background: #fff; text-align: left; cursor: pointer; } .student-results button:hover { background: #f5f6ff; } .student-results strong, .student-results small { display: block; } .student-results strong { font-size: 10px; } .student-results small { margin-top: 2px; color: #9299aa; font-size: 9px; } .empty-results { display: block; padding: 10px; color: #9299aa; font-size: 10px; }
.modal-backdrop { position: fixed; inset: 0; z-index: 20; display: grid; place-items: center; padding: 20px; background: rgba(20, 27, 53, .42); } .credentials-modal { position: relative; width: min(390px, 100%); padding: 25px; border-radius: 12px; background: #fff; box-shadow: 0 18px 50px rgba(27, 35, 79, .2); } .credentials-modal h2 { margin: 6px 0; font-size: 18px; } .credentials-modal p { margin: 0 0 18px; color: #9299aa; font-size: 10px; line-height: 1.5; } .modal-close { position: absolute; top: 10px; right: 12px; border: 0; color: #8d95a7; background: transparent; font-size: 22px; cursor: pointer; } .credential-row { display: flex; justify-content: space-between; gap: 12px; margin-top: 9px; padding: 11px 12px; border-radius: 6px; background: #f5f6ff; font-size: 10px; } .credential-row span { color: #737d93; } .credential-row strong { color: #3946b8; } .modal-button { width: 100%; margin-top: 18px; }
.credentials-modal label { display: block; margin-bottom: 6px; color: #737d93; font-size: 10px; }
@media (max-width: 900px) { .content-grid { grid-template-columns: 1fr; } .form-grid { grid-template-columns: repeat(2, 1fr); } } @media (max-width: 580px) { .stat-grid { grid-template-columns: 1fr; } .form-grid, .enrollment-form { grid-template-columns: 1fr; } }
</style>
