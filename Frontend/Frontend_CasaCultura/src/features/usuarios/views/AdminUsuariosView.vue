<script setup>
import { ref, computed } from 'vue'
import {
  Users,
  Plus,
  Search,
  Download
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import UsuarioTable from '../components/UsuarioTable.vue'
import UsuarioFormModal from '../components/UsuarioFormModal.vue'
import ResetPasswordModal from '../components/ResetPasswordModal.vue'
import {
  createUser,
  deactivateUser,
  resetUserPassword
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData, exportToCsv } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showCreateModal = ref(false)

const showPasswordModal = ref(false)
const targetUserId = ref(null)
const passwordErrorMessage = ref('')

const filteredUsuarios = computed(() => {
  const q = search.value.trim().toLowerCase()
  const list = data.usuarios || []
  if (!q) return list
  return list.filter(u => {
    const username = (u.nombreUsuario || '').toLowerCase()
    const personaName = u.persona ? `${u.persona.nombre} ${u.persona.apellidoPaterno || ''}`.toLowerCase() : ''
    const rol = (u.rol || u.nombreRol || '').toLowerCase()
    const correo = (u.correo || '').toLowerCase()
    return username.includes(q) || personaName.includes(q) || rol.includes(q) || correo.includes(q)
  })
})

function openPasswordModal(user) {
  targetUserId.value = user.id
  passwordErrorMessage.value = ''
  showPasswordModal.value = true
}

async function handleConfirmResetPassword(newPassword) {
  isSaving.value = true
  try {
    await resetUserPassword(targetUserId.value, newPassword)
    showToast('Contraseña restablecida correctamente.')
    showPasswordModal.value = false
    await loadAllData(true)
  } catch (err) {
    passwordErrorMessage.value = err.message || 'Error al restablecer la contraseña'
    showToast(err.message || 'Error al restablecer la contraseña', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleSaveUser({ type, payload }) {
  isSaving.value = true
  try {
    await createUser(payload)
    showToast('Usuario creado exitosamente.')
    showCreateModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al crear usuario', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleDeactivateUser(id) {
  if (!confirm('¿Estás seguro de desactivar este usuario?')) return
  try {
    await deactivateUser(id)
    showToast('Usuario desactivado exitosamente.')
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al desactivar usuario', 'error')
  }
}

function handleExport() {
  const rows = filteredUsuarios.value.map(u => ({
    ID: u.id,
    Usuario: u.nombreUsuario,
    Persona: u.persona ? `${u.persona.nombre} ${u.persona.apellidoPaterno || ''}` : '',
    Rol: u.rol || u.nombreRol || 'SUPER_ADMIN',
    Estado: u.estado || 'ACTIVO'
  }))
  exportToCsv('usuarios', rows)
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <Users :size="28" />
        </div>
        <div>
          <h1>Usuarios</h1>
          <p>Gestión de cuentas y accesos del sistema</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="showCreateModal = true">
          <Plus :size="18" />
          Nuevo Usuario
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
            placeholder="Buscar por usuario, nombre o rol..."
            class="search-input"
          />
        </div>

        <button class="export-button" @click="handleExport">
          <Download :size="16" />
          Exportar Lista
        </button>
      </div>

      <!-- Usuario Table Component -->
      <UsuarioTable
        :usuarios="filteredUsuarios"
        @change-password="openPasswordModal"
        @deactivate="handleDeactivateUser"
      />
    </div>

    <!-- Create User Form Modal -->
    <UsuarioFormModal
      :show="showCreateModal"
      :is-saving="isSaving"
      :roles="data.roles || []"
      :alumnos="data.alumnos || []"
      :docentes="data.docentes || []"
      :usuarios="data.usuarios || []"
      @close="showCreateModal = false"
      @save="handleSaveUser"
    />

    <!-- Reset Password Modal -->
    <ResetPasswordModal
      :show="showPasswordModal"
      :is-saving="isSaving"
      :error-message="passwordErrorMessage"
      @close="showPasswordModal = false"
      @confirm="handleConfirmResetPassword"
    />
  </div>
</template>
