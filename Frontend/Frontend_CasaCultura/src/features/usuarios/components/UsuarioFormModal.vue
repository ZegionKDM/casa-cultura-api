<script setup>
import { reactive, ref, computed, watch } from 'vue'
import {
  X,
  ShieldCheck,
  UserCheck,
  Search,
  Check,
  AlertCircle
} from 'lucide-vue-next'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  isSaving: {
    type: Boolean,
    default: false
  },
  roles: {
    type: Array,
    default: () => []
  },
  alumnos: {
    type: Array,
    default: () => []
  },
  docentes: {
    type: Array,
    default: () => []
  },
  usuarios: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'save'])

const userCreationType = ref('admin') // 'admin' | 'existing'
const personSearch = ref('')
const personTypeFilter = ref('ALL') // 'ALL' | 'ALUMNO' | 'DOCENTE'
const selectedPerson = ref(null)

const formAdminPerson = reactive({
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  correo: '',
  telefono: ''
})

const formUser = reactive({
  personaId: '',
  rolId: '',
  nombreUsuario: '',
  password: ''
})

const errors = ref({})

watch(
  () => props.show,
  (val) => {
    if (val) {
      userCreationType.value = 'admin'
      selectedPerson.value = null
      personSearch.value = ''
      errors.value = {}
      Object.assign(formUser, { personaId: '', rolId: '', nombreUsuario: '', password: '' })
      Object.assign(formAdminPerson, { nombre: '', apellidoPaterno: '', apellidoMaterno: '', correo: '', telefono: '' })
      const supRole = props.roles.find(r => r.nombre.toUpperCase().includes('SUPERVISOR'))
      if (supRole) formUser.rolId = supRole.id
    }
  }
)

const allPersons = computed(() => {
  const existingUserPersonaIds = new Set((props.usuarios || []).map(u => Number(u.personaId || u.persona?.id)))
  const list = []
  props.alumnos.forEach(a => {
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
  props.docentes.forEach(d => {
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
    const foundRole = props.roles.find(r => r.nombre.toUpperCase().includes(targetRoleName))
    if (foundRole) formUser.rolId = foundRole.id
  }
}

function clearSelectedPerson() {
  selectedPerson.value = null
  formUser.personaId = ''
}

function setUserCreationType(type) {
  userCreationType.value = type
  errors.value = {}
  if (type === 'admin') {
    selectedPerson.value = null
    formUser.personaId = ''
    if (!formUser.rolId) {
      const supRole = props.roles.find(r => r.nombre.toUpperCase().includes('SUPERVISOR'))
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

function filterUsernameInput(event) {
  const clean = event.target.value.toLowerCase().replace(/[^a-z0-9._-]/g, '').slice(0, 80)
  formUser.nombreUsuario = clean
  if (errors.value.nombreUsuario) delete errors.value.nombreUsuario
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

function validate() {
  errors.value = {}
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

  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return

  if (userCreationType.value === 'admin') {
    emit('save', {
      type: 'admin',
      payload: {
        nombre: formAdminPerson.nombre.trim(),
        apellidoPaterno: formAdminPerson.apellidoPaterno.trim(),
        apellidoMaterno: formAdminPerson.apellidoMaterno?.trim() || null,
        correo: formAdminPerson.correo?.trim() || null,
        telefono: formAdminPerson.telefono ? formAdminPerson.telefono.replace(/[\s-]/g, '') : null,
        rolId: Number(formUser.rolId),
        nombreUsuario: formUser.nombreUsuario.trim(),
        password: formUser.password
      }
    })
  } else {
    emit('save', {
      type: 'existing',
      payload: {
        personaId: Number(formUser.personaId),
        rolId: Number(formUser.rolId),
        nombreUsuario: formUser.nombreUsuario.trim(),
        password: formUser.password
      }
    })
  }
}
</script>

<template>
  <div
    v-if="show"
    class="modal-backdrop"
    @click.self="emit('close')"
  >
    <div class="modal-card">
      <div class="modal-header">
        <h3>Nuevo Usuario del Sistema</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
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
                <span v-if="errors.adminNombre" class="error-text">{{ errors.adminNombre }}</span>
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
                <span v-if="errors.adminApellidoPaterno" class="error-text">{{ errors.adminApellidoPaterno }}</span>
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
                  <Check :size="16" class="picker-select-icon" />
                </div>

                <div
                  v-if="filteredPersons.length === 0"
                  class="picker-no-results"
                >
                  <AlertCircle :size="16" /> No se encontraron personas disponibles sin usuario.
                </div>
              </div>
            </div>
            <span v-if="errors.personaId" class="error-text">{{ errors.personaId }}</span>
          </div>

          <!-- CAMPOS COMUNES: ROL, NOMBRE DE USUARIO Y CONTRASEÑA -->
          <div class="form-grid-2">
            <div class="field">
              <label>Rol Asignado *</label>
              <select
                v-model="formUser.rolId"
                :class="{ 'has-error': errors.rolId }"
              >
                <option value="">Selecciona un rol</option>
                <option
                  v-for="r in roles"
                  :key="r.id"
                  :value="r.id"
                >
                  {{ r.nombre }}
                </option>
              </select>
              <span v-if="errors.rolId" class="error-text">{{ errors.rolId }}</span>
            </div>

            <div class="field">
              <label>Nombre de Usuario *</label>
              <input
                :value="formUser.nombreUsuario"
                type="text"
                maxlength="80"
                placeholder="ej. roberto.gomez"
                :class="{ 'has-error': errors.nombreUsuario }"
                @input="filterUsernameInput"
              />
              <span v-if="errors.nombreUsuario" class="error-text">{{ errors.nombreUsuario }}</span>
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
            <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
          </div>
        </div>

        <div class="modal-footer">
          <button
            type="button"
            class="btn-cancel"
            @click="emit('close')"
          >
            Cancelar
          </button>
          <button
            type="submit"
            class="btn-primary"
            :disabled="isSaving"
          >
            {{ isSaving ? 'Guardando...' : 'Crear Usuario' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
