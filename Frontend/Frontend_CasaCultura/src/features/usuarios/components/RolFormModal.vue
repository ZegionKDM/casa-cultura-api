<script setup>
import { reactive, watch, ref } from 'vue'
import { X, ShieldCheck } from 'lucide-vue-next'

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
  }
})

const emit = defineEmits(['close', 'save'])

const form = reactive({
  nombre: '',
  descripcion: ''
})

const errors = ref({})

watch(
  () => props.initialData,
  (val) => {
    if (val && Object.keys(val).length > 0) {
      Object.assign(form, {
        nombre: val.nombre || '',
        descripcion: val.descripcion || ''
      })
    }
    errors.value = {}
  },
  { immediate: true, deep: true }
)

function filterRoleNameInput(event) {
  const clean = event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '').slice(0, 50)
  form.nombre = clean
  if (errors.value.nombre) delete errors.value.nombre
}

function validate() {
  errors.value = {}
  if (!form.nombre || !form.nombre.trim()) {
    errors.value.nombre = 'El nombre del rol es obligatorio.'
  } else if (form.nombre.trim().length > 50) {
    errors.value.nombre = 'Máximo 50 caracteres.'
  }
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    nombre: form.nombre.trim().toUpperCase(),
    descripcion: form.descripcion?.trim() || ''
  })
}
</script>

<template>
  <div
    v-if="show"
    class="modal-backdrop"
    @click.self="emit('close')"
  >
    <div class="modal-card modal-card-sm">
      <div class="modal-header">
        <div class="modal-header-with-icon">
          <div class="modal-icon-badge">
            <ShieldCheck :size="20" />
          </div>
          <div>
            <h3>{{ editingId ? 'Editar Rol' : 'Nuevo Rol' }}</h3>
            <p class="modal-header-subtitle">Configura privilegios y alcance del perfil</p>
          </div>
        </div>
        <button
          class="modal-close"
          type="button"
          aria-label="Cerrar modal"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <div class="field">
            <label>Nombre del Rol *</label>
            <input
              :value="form.nombre"
              type="text"
              maxlength="50"
              placeholder="Ej. ROLE_COORDINADOR"
              :class="{ 'has-error': errors.nombre }"
              @input="filterRoleNameInput"
            />
            <span v-if="errors.nombre" class="error-text">{{ errors.nombre }}</span>
            <span class="field-hint">Usa mayúsculas, números o guiones bajos (ej. ROLE_SUPERVISOR).</span>
          </div>

          <div class="field">
            <label>Descripción del Rol</label>
            <textarea
              v-model="form.descripcion"
              rows="3"
              placeholder="Define las facultades y alcance de este rol..."
            ></textarea>
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
            {{ isSaving ? 'Guardando...' : (editingId ? 'Actualizar Rol' : 'Guardar Rol') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
