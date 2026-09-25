<script setup>
import { ref, computed } from 'vue'
import {
  UserCheck,
  Plus,
  Search,
  Download
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import DocenteTable from '../components/DocenteTable.vue'
import DocenteFormModal from '../components/DocenteFormModal.vue'
import DocenteAssignModal from '../components/DocenteAssignModal.vue'
import CameraCaptureModal from '../../../components/common/CameraCaptureModal.vue'
import {
  createTeacher,
  updateTeacher,
  deactivateTeacher,
  assignTeacherToGroup
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData, exportToCsv } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showFormModal = ref(false)
const modalMode = ref('create') // 'create' | 'edit'
const editingId = ref(null)
const editingData = ref({})
const showCameraModal = ref(false)
const capturedPhotoUrl = ref('')

const showAssignModal = ref(false)
const selectedDocente = ref(null)

const filteredDocentes = computed(() => {
  const q = search.value.trim().toLowerCase()
  const list = data.docentes || []
  if (!q) return list
  return list.filter(d => {
    const fullName = `${d.nombre} ${d.apellidoPaterno} ${d.apellidoMaterno || ''}`.toLowerCase()
    const especialidad = (d.especialidad || '').toLowerCase()
    const correo = (d.correo || '').toLowerCase()
    const telefono = (d.telefono || '')
    return fullName.includes(q) || especialidad.includes(q) || correo.includes(q) || telefono.includes(q)
  })
})

function openCreateModal() {
  editingId.value = null
  capturedPhotoUrl.value = ''
  editingData.value = {
    nombre: '',
    apellidoPaterno: '',
    apellidoMaterno: '',
    especialidad: '',
    fechaNacimiento: '',
    telefono: '',
    direccion: '',
    correo: '',
    fotoUrl: ''
  }
  modalMode.value = 'create'
  showFormModal.value = true
}

function openEditModal(docente) {
  editingId.value = docente.id
  capturedPhotoUrl.value = docente.fotoUrl || ''
  editingData.value = {
    nombre: docente.nombre || '',
    apellidoPaterno: docente.apellidoPaterno || '',
    apellidoMaterno: docente.apellidoMaterno || '',
    especialidad: docente.especialidad || '',
    fechaNacimiento: docente.fechaNacimiento || '',
    telefono: docente.telefono || '',
    direccion: docente.direccion || '',
    correo: docente.correo || '',
    fotoUrl: docente.fotoUrl || ''
  }
  modalMode.value = 'edit'
  showFormModal.value = true
}

function onDocentePhotoSaved(eventData) {
  capturedPhotoUrl.value = eventData.url
  editingData.value.fotoUrl = eventData.url
  showToast('Fotografía capturada y asignada al docente.', 'success')
}

function openAssignModal(docente) {
  selectedDocente.value = docente
  showAssignModal.value = true
}

async function handleSaveTeacher(payload) {
  isSaving.value = true
  try {
    if (modalMode.value === 'edit') {
      await updateTeacher(editingId.value, payload)
      showToast('Docente actualizado correctamente.')
    } else {
      await createTeacher(payload)
      showToast('Docente registrado exitosamente.')
    }
    showFormModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al guardar el docente', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleAssignTeacher(payload) {
  isSaving.value = true
  try {
    await assignTeacherToGroup(payload)
    showToast('Docente asignado al grupo exitosamente.')
    showAssignModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al asignar el docente', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleDeactivateTeacher(id) {
  if (!confirm('¿Estás seguro de dar de baja a este docente?')) return
  try {
    await deactivateTeacher(id)
    showToast('Docente dado de baja exitosamente.')
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al desactivar el docente', 'error')
  }
}

function handleExport() {
  const rows = filteredDocentes.value.map(d => ({
    ID: d.id,
    Nombre: `${d.nombre} ${d.apellidoPaterno} ${d.apellidoMaterno || ''}`.trim(),
    Especialidad: d.especialidad || '',
    Correo: d.correo || '',
    Telefono: d.telefono || '',
    Estado: d.estado || 'ACTIVO'
  }))
  exportToCsv('docentes', rows)
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <UserCheck :size="28" />
        </div>
        <div>
          <h1>Docentes</h1>
          <p>Plantilla de instructores y talleristas</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="openCreateModal">
          <Plus :size="18" />
          Nuevo Docente
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
            placeholder="Buscar por nombre, especialidad o contacto..."
            class="search-input"
          />
        </div>

        <button class="export-button" @click="handleExport">
          <Download :size="16" />
          Exportar Lista
        </button>
      </div>

      <!-- Docente Table Component -->
      <DocenteTable
        :docentes="filteredDocentes"
        @edit="openEditModal"
        @assign="openAssignModal"
        @deactivate="handleDeactivateTeacher"
      />
    </div>

    <!-- Docente Form Modal Component -->
    <DocenteFormModal
      :show="showFormModal"
      :is-saving="isSaving"
      :editing-id="editingId"
      :initial-data="editingData"
      :photo-url="capturedPhotoUrl"
      @close="showFormModal = false"
      @save="handleSaveTeacher"
      @open-camera="showCameraModal = true"
      @clear-photo="capturedPhotoUrl = ''"
    />

    <!-- Assign Modal Component -->
    <DocenteAssignModal
      :show="showAssignModal"
      :is-saving="isSaving"
      :docente="selectedDocente"
      :grupos="data.grupos || []"
      @close="showAssignModal = false"
      @assign="handleAssignTeacher"
    />

    <!-- Camera Modal Reusable Component -->
    <CameraCaptureModal
      v-if="showCameraModal"
      :show="showCameraModal"
      entity-type="docente"
      title="Tomar Foto del Docente"
      @close="showCameraModal = false"
      @photo-saved="onDocentePhotoSaved"
      @saved="onDocentePhotoSaved"
    />
  </div>
</template>
