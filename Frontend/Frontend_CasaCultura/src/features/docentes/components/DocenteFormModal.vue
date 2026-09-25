<script setup>
import { reactive, watch, ref } from 'vue'
import {
  X,
  User,
  Camera,
  Trash2
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
  editingId: {
    type: [Number, String, null],
    default: null
  },
  initialData: {
    type: Object,
    default: () => ({})
  },
  photoUrl: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'save', 'open-camera', 'clear-photo'])

const todayDate = new Date().toISOString().slice(0, 10)

const form = reactive({
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  especialidad: '',
  fechaNacimiento: '',
  telefono: '',
  direccion: '',
  correo: '',
  fotoUrl: ''
})

const errors = ref({})

watch(
  () => props.initialData,
  (val) => {
    if (val && Object.keys(val).length > 0) {
      Object.assign(form, {
        nombre: val.nombre || '',
        apellidoPaterno: val.apellidoPaterno || '',
        apellidoMaterno: val.apellidoMaterno || '',
        especialidad: val.especialidad || '',
        fechaNacimiento: val.fechaNacimiento || '',
        telefono: val.telefono || '',
        direccion: val.direccion || '',
        correo: val.correo || '',
        fotoUrl: val.fotoUrl || ''
      })
    }
    errors.value = {}
  },
  { immediate: true, deep: true }
)

watch(
  () => props.photoUrl,
  (url) => {
    if (url) {
      form.fotoUrl = url
    }
  }
)

function filterNameInput(event, key) {
  const clean = event.target.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s.'-]/g, '')
  form[key] = clean
  if (errors.value[key]) delete errors.value[key]
}

function filterPhoneInput(event) {
  const clean = event.target.value.replace(/\D/g, '').slice(0, 10)
  form.telefono = clean
  if (errors.value.telefono) delete errors.value.telefono
}

function validateName(val) {
  if (!val || !val.trim()) return 'Este campo es obligatorio.'
  if (val.trim().length < 2) return 'Debe tener al menos 2 caracteres.'
  if (val.trim().length > 80) return 'No puede exceder los 80 caracteres.'
  if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s.'-]+$/.test(val.trim())) return 'Solo se permiten letras y espacios.'
  return null
}

function validateEmail(val) {
  if (!val || !val.trim()) return 'El correo es obligatorio.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val.trim())) return 'Ingresa un correo electrónico válido.'
  if (val.trim().length > 160) return 'El correo no puede exceder 160 caracteres.'
  return null
}

function validatePhone(val) {
  if (!val || !val.trim()) return 'El teléfono es obligatorio.'
  const clean = val.replace(/[\s-]/g, '')
  if (!/^\d{10}$/.test(clean)) return 'El teléfono debe contener exactamente 10 dígitos numéricos.'
  return null
}

function validate() {
  errors.value = {}

  const nErr = validateName(form.nombre)
  if (nErr) errors.value.nombre = nErr

  const apErr = validateName(form.apellidoPaterno)
  if (apErr) errors.value.apellidoPaterno = apErr

  if (form.apellidoMaterno?.trim()) {
    const amErr = validateName(form.apellidoMaterno)
    if (amErr) errors.value.apellidoMaterno = amErr
  }

  if (!form.especialidad || !form.especialidad.trim()) {
    errors.value.especialidad = 'La especialidad cultural es obligatoria.'
  }

  const emErr = validateEmail(form.correo)
  if (emErr) errors.value.correo = emErr

  const phErr = validatePhone(form.telefono)
  if (phErr) errors.value.telefono = phErr

  if (!form.fechaNacimiento) {
    errors.value.fechaNacimiento = 'La fecha de nacimiento es obligatoria.'
  } else {
    const d = new Date(form.fechaNacimiento)
    if (d > new Date()) errors.value.fechaNacimiento = 'La fecha de nacimiento no puede ser futura.'
  }

  if (!form.direccion || !form.direccion.trim()) {
    errors.value.direccion = 'La dirección es obligatoria.'
  } else if (form.direccion.trim().length < 5) {
    errors.value.direccion = 'Ingresa una dirección válida (mínimo 5 caracteres).'
  }

  return Object.keys(errors.value).length === 0
}

function handleRemovePhoto() {
  form.fotoUrl = ''
  emit('clear-photo')
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    ...form,
    nombre: form.nombre.trim(),
    apellidoPaterno: form.apellidoPaterno.trim(),
    apellidoMaterno: form.apellidoMaterno?.trim() || null,
    especialidad: form.especialidad.trim(),
    fechaNacimiento: form.fechaNacimiento || null,
    telefono: form.telefono ? form.telefono.replace(/[\s-]/g, '') : null,
    correo: form.correo?.trim() || null,
    direccion: form.direccion?.trim() || null,
    fotoUrl: form.fotoUrl?.trim() || null
  })
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
        <h3>{{ editingId ? 'Editar Docente' : 'Nuevo Docente' }}</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <!-- Fotografía del Docente -->
          <div class="student-photo-section">
            <div class="photo-avatar-box">
              <img
                v-if="form.fotoUrl"
                :src="form.fotoUrl"
                alt="Foto del docente"
                class="photo-preview-img"
              />
              <div v-else class="photo-placeholder">
                <User :size="32" />
                <span>Sin foto</span>
              </div>
            </div>

            <div class="photo-info-box">
              <div class="photo-title">Fotografía Oficial del Docente</div>
              <p class="photo-desc">
                Captura con la cámara web o del celular para la credencial y expediente del docente.
              </p>
              <div class="photo-action-buttons">
                <button
                  type="button"
                  class="btn-photo-capture"
                  @click="emit('open-camera')"
                >
                  <Camera :size="15" />
                  <span>{{ form.fotoUrl ? 'Cambiar / Tomar Nueva Foto' : 'Tomar Foto con Cámara' }}</span>
                </button>

                <button
                  v-if="form.fotoUrl"
                  type="button"
                  class="btn-photo-remove"
                  title="Quitar foto"
                  @click="handleRemovePhoto"
                >
                  <Trash2 :size="14" />
                  <span>Quitar</span>
                </button>
              </div>
            </div>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Nombre(s) *</label>
              <input
                :value="form.nombre"
                type="text"
                maxlength="80"
                placeholder="Nombre de pila"
                :class="{ 'has-error': errors.nombre }"
                @input="filterNameInput($event, 'nombre')"
              />
              <span v-if="errors.nombre" class="error-text">{{ errors.nombre }}</span>
            </div>
            <div class="field">
              <label>Apellido Paterno *</label>
              <input
                :value="form.apellidoPaterno"
                type="text"
                maxlength="80"
                placeholder="Primer apellido"
                :class="{ 'has-error': errors.apellidoPaterno }"
                @input="filterNameInput($event, 'apellidoPaterno')"
              />
              <span v-if="errors.apellidoPaterno" class="error-text">{{ errors.apellidoPaterno }}</span>
            </div>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Apellido Materno</label>
              <input
                :value="form.apellidoMaterno"
                type="text"
                maxlength="80"
                placeholder="Segundo apellido (opcional)"
                :class="{ 'has-error': errors.apellidoMaterno }"
                @input="filterNameInput($event, 'apellidoMaterno')"
              />
            </div>
            <div class="field">
              <label>Especialidad Cultural *</label>
              <input
                v-model="form.especialidad"
                type="text"
                maxlength="100"
                placeholder="Ej. Artes Plásticas, Danza Folklórica"
                :class="{ 'has-error': errors.especialidad }"
                @input="delete errors.especialidad"
              />
              <span v-if="errors.especialidad" class="error-text">{{ errors.especialidad }}</span>
            </div>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Correo Electrónico *</label>
              <input
                v-model="form.correo"
                type="email"
                maxlength="160"
                placeholder="docente@ejemplo.com"
                :class="{ 'has-error': errors.correo }"
                @input="delete errors.correo"
              />
              <span v-if="errors.correo" class="error-text">{{ errors.correo }}</span>
            </div>
            <div class="field">
              <label>Teléfono (10 dígitos) *</label>
              <input
                :value="form.telefono"
                type="tel"
                maxlength="10"
                placeholder="9531234567"
                :class="{ 'has-error': errors.telefono }"
                @input="filterPhoneInput"
              />
              <span v-if="errors.telefono" class="error-text">{{ errors.telefono }}</span>
            </div>
          </div>

          <div class="field">
            <label>Fecha de Nacimiento *</label>
            <input
              v-model="form.fechaNacimiento"
              type="date"
              :max="todayDate"
              min="1920-01-01"
              :class="{ 'has-error': errors.fechaNacimiento }"
              @change="delete errors.fechaNacimiento"
            />
            <span v-if="errors.fechaNacimiento" class="error-text">{{ errors.fechaNacimiento }}</span>
          </div>

          <div class="field">
            <label>Dirección *</label>
            <input
              v-model="form.direccion"
              type="text"
              maxlength="255"
              placeholder="Calle, Número, Colonia, Municipio"
              :class="{ 'has-error': errors.direccion }"
              @input="delete errors.direccion"
            />
            <span v-if="errors.direccion" class="error-text">{{ errors.direccion }}</span>
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
            {{ isSaving ? 'Guardando...' : (editingId ? 'Actualizar Docente' : 'Guardar Docente') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
