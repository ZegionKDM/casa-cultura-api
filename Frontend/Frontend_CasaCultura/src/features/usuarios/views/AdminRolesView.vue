<script setup>
import { ref, computed } from 'vue'
import {
  ShieldCheck,
  Plus,
  Search,
  Download
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import RolTable from '../components/RolTable.vue'
import RolFormModal from '../components/RolFormModal.vue'
import {
  createRole,
  updateRole,
  deleteRole
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData, exportToCsv } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showModal = ref(false)
const modalMode = ref('create') // 'create' | 'edit'
const editingId = ref(null)
const editingData = ref({})

const filteredRoles = computed(() => {
  const q = search.value.trim().toLowerCase()
  const list = data.roles || []
  if (!q) return list
  return list.filter(r => {
    const name = (r.nombre || '').toLowerCase()
    const desc = (r.descripcion || '').toLowerCase()
    return name.includes(q) || desc.includes(q)
  })
})

function openCreateModal() {
  editingId.value = null
  editingData.value = {
    nombre: '',
    descripcion: ''
  }
  modalMode.value = 'create'
  showModal.value = true
}

function openEditModal(role) {
  editingId.value = role.id
  editingData.value = {
    nombre: role.nombre || '',
    descripcion: role.descripcion || ''
  }
  modalMode.value = 'edit'
  showModal.value = true
}

async function handleSaveRole(payload) {
  isSaving.value = true
  try {
    if (modalMode.value === 'edit') {
      await updateRole(editingId.value, payload)
      showToast('Rol actualizado correctamente.')
    } else {
      await createRole(payload)
      showToast('Rol creado exitosamente.')
    }
    showModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al guardar el rol', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleDeleteRole(id) {
  if (!confirm('¿Estás seguro de eliminar este rol?')) return
  try {
    await deleteRole(id)
    showToast('Rol eliminado exitosamente.')
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al eliminar el rol', 'error')
  }
}

function handleExport() {
  const rows = filteredRoles.value.map(r => ({
    ID: r.id,
    Rol: r.nombre,
    Descripcion: r.descripcion || '',
    Estado: 'ACTIVO'
  }))
  exportToCsv('roles', rows)
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <ShieldCheck :size="28" />
        </div>
        <div>
          <h1>Roles y Permisos</h1>
          <p>Control de accesos y permisos</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="openCreateModal">
          <Plus :size="18" />
          Nuevo Rol
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
            placeholder="Buscar por rol o descripción..."
            class="search-input"
          />
        </div>

        <button class="export-button" @click="handleExport">
          <Download :size="16" />
          Exportar Lista
        </button>
      </div>

      <!-- Rol Table Component -->
      <RolTable
        :roles="filteredRoles"
        @edit="openEditModal"
        @delete="handleDeleteRole"
      />
    </div>

    <!-- Rol Form Modal Component -->
    <RolFormModal
      :show="showModal"
      :is-saving="isSaving"
      :editing-id="editingId"
      :initial-data="editingData"
      @close="showModal = false"
      @save="handleSaveRole"
    />
  </div>
</template>
