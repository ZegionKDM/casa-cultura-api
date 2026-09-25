<script setup>
import { ref, computed } from 'vue'
import {
  GraduationCap,
  Plus,
  Search,
  Download
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import AlumnoTable from '../components/AlumnoTable.vue'
import AlumnoFormModal from '../components/AlumnoFormModal.vue'
import StudentCredentialModal from '../components/StudentCredentialModal.vue'
import StudentUserModal from '../components/StudentUserModal.vue'
import CameraCaptureModal from '../../../components/common/CameraCaptureModal.vue'
import {
  createStudent,
  updateStudent,
  deactivateStudent,
  generateStudentCredential,
  generateStudentUser
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData, exportToCsv } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showModal = ref(false)
const modalMode = ref('create') // 'create' | 'edit'
const editingId = ref(null)
const editingData = ref({})

// Modals
const showCredentialModal = ref(false)
const credentialInfo = ref(null)
const showUserModal = ref(false)
const userStudentId = ref(null)
const userErrorMessage = ref('')
const showCameraModal = ref(false)
const capturedPhotoUrl = ref('')

const filteredAlumnos = computed(() => {
  const q = search.value.trim().toLowerCase()
  const list = data.alumnos || []
  if (!q) return list
  return list.filter(a => {
    const fullName = `${a.nombre} ${a.apellidoPaterno} ${a.apellidoMaterno || ''}`.toLowerCase()
    const matricula = (a.matricula || '').toLowerCase()
    const correo = (a.correo || '').toLowerCase()
    const telefono = (a.telefono || '')
    return fullName.includes(q) || matricula.includes(q) || correo.includes(q) || telefono.includes(q)
  })
})

function openCreateModal() {
  editingId.value = null
  const currentYr = new Date().getFullYear()
  const nextNum = String((data.alumnos?.length || 0) + 1).padStart(3, '0')
  capturedPhotoUrl.value = ''
  editingData.value = {
    nombre: '',
    apellidoPaterno: '',
    apellidoMaterno: '',
    matricula: `ALU-${currentYr}-${nextNum}`,
    fechaNacimiento: '',
    telefono: '',
    direccion: '',
    correo: '',
    fotoUrl: ''
  }
  modalMode.value = 'create'
  showModal.value = true
}

function openEditModal(alumno) {
  editingId.value = alumno.id
  capturedPhotoUrl.value = alumno.fotoUrl || ''
  editingData.value = {
    nombre: alumno.nombre || '',
    apellidoPaterno: alumno.apellidoPaterno || '',
    apellidoMaterno: alumno.apellidoMaterno || '',
    matricula: alumno.matricula || '',
    fechaNacimiento: alumno.fechaNacimiento || '',
    telefono: alumno.telefono || '',
    direccion: alumno.direccion || '',
    correo: alumno.correo || '',
    fotoUrl: alumno.fotoUrl || ''
  }
  modalMode.value = 'edit'
  showModal.value = true
}

async function handleSaveStudent(payload) {
  isSaving.value = true
  try {
    if (modalMode.value === 'edit') {
      await updateStudent(editingId.value, payload)
      showToast('Alumno actualizado correctamente.')
    } else {
      await createStudent(payload)
      showToast('Alumno registrado exitosamente.')
    }
    showModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al guardar el alumno', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleDeactivateStudent(id) {
  if (!confirm('¿Estás seguro de dar de baja a este alumno?')) return
  try {
    await deactivateStudent(id)
    showToast('Alumno dado de baja exitosamente.')
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al desactivar el alumno', 'error')
  }
}

async function handleViewCredential(alumno) {
  try {
    const cred = await generateStudentCredential(alumno.id)
    credentialInfo.value = {
      ...cred,
      nombreCompleto: cred.nombreCompleto || `${alumno.nombre} ${alumno.apellidoPaterno} ${alumno.apellidoMaterno || ''}`.trim(),
      matricula: cred.matricula || alumno.matricula,
      fotoUrl: cred.fotoUrl || alumno.fotoUrl || null
    }
    showCredentialModal.value = true
  } catch (err) {
    showToast(err.message || 'Error al obtener credencial del alumno', 'error')
  }
}

function handleOpenUserModal(alumno) {
  userStudentId.value = alumno.id
  userErrorMessage.value = ''
  showUserModal.value = true
}

async function handleConfirmStudentUser(password) {
  isSaving.value = true
  try {
    await generateStudentUser(userStudentId.value, { password })
    showToast('Acceso y usuario creados exitosamente para el alumno.')
    showUserModal.value = false
    await loadAllData(true)
  } catch (err) {
    userErrorMessage.value = err.message || 'Error al generar acceso de alumno'
    showToast(err.message || 'Error al generar acceso de alumno', 'error')
  } finally {
    isSaving.value = false
  }
}

function onStudentPhotoSaved(eventData) {
  capturedPhotoUrl.value = eventData.url
  editingData.value.fotoUrl = eventData.url
  showToast('Fotografía capturada y asignada al alumno.', 'success')
}

function handleExport() {
  const rows = filteredAlumnos.value.map(a => ({
    ID: a.id,
    Nombre: `${a.nombre} ${a.apellidoPaterno} ${a.apellidoMaterno || ''}`.trim(),
    Matricula: a.matricula,
    Correo: a.correo || '',
    Telefono: a.telefono || '',
    Estado: a.estado || 'ACTIVO'
  }))
  exportToCsv('alumnos', rows)
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <GraduationCap :size="28" />
        </div>
        <div>
          <h1>Alumnos</h1>
          <p>Padrón de estudiantes inscritos</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="openCreateModal">
          <Plus :size="18" />
          Nuevo Alumno
        </button>
      </div>
    </div>

    <!-- Actions & Filter Bar -->
    <div class="module-content-wrapper">
      <div class="filter-actions-bar">
        <div class="search-input-wrapper">
          <Search class="search-icon" :size="18" />
          <input
            v-model="search"
            type="text"
            placeholder="Buscar por nombre, matrícula o contacto..."
            class="search-input"
          />
        </div>

        <button class="export-button" @click="handleExport">
          <Download :size="16" />
          Exportar Lista
        </button>
      </div>

      <!-- Alumnos Table Component -->
      <AlumnoTable
        :alumnos="filteredAlumnos"
        @edit="openEditModal"
        @deactivate="handleDeactivateStudent"
        @view-credential="handleViewCredential"
        @create-user="handleOpenUserModal"
      />
    </div>

    <!-- Form Modal Component -->
    <AlumnoFormModal
      :show="showModal"
      :is-saving="isSaving"
      :editing-id="editingId"
      :initial-data="editingData"
      :photo-url="capturedPhotoUrl"
      @close="showModal = false"
      @save="handleSaveStudent"
      @open-camera="showCameraModal = true"
      @clear-photo="capturedPhotoUrl = ''"
    />

    <!-- Credential Modal Component -->
    <StudentCredentialModal
      :show="showCredentialModal"
      :credential-info="credentialInfo"
      @close="showCredentialModal = false"
    />

    <!-- Student User Modal Component -->
    <StudentUserModal
      :show="showUserModal"
      :is-saving="isSaving"
      :error-message="userErrorMessage"
      @close="showUserModal = false"
      @confirm="handleConfirmStudentUser"
    />

    <!-- Camera Modal Reusable Component -->
    <CameraCaptureModal
      v-if="showCameraModal"
      :show="showCameraModal"
      entity-type="alumno"
      title="Tomar Foto del Alumno"
      @close="showCameraModal = false"
      @photo-saved="onStudentPhotoSaved"
      @saved="onStudentPhotoSaved"
    />
  </div>
</template>
