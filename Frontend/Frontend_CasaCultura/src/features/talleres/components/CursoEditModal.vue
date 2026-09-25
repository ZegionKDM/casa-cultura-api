<script setup>
import { reactive, watch, ref } from 'vue'
import { X } from 'lucide-vue-next'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  isSaving: {
    type: Boolean,
    default: false
  },
  course: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'save'])

const form = reactive({
  id: null,
  nombre: ''
})

const errors = ref({})

watch(
  () => props.course,
  (c) => {
    if (c) {
      form.id = c.id
      form.nombre = c.nombre || ''
      errors.value = {}
    }
  },
  { immediate: true }
)

function validate() {
  errors.value = {}
  if (!form.nombre || !form.nombre.trim()) {
    errors.value.nombre = 'El nombre del curso es obligatorio.'
  }
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    id: form.id,
    nombre: form.nombre.trim()
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
        <h3>Editar Nombre del Taller</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <div class="field">
            <label>Nombre del Taller / Curso *</label>
            <input
              v-model="form.nombre"
              type="text"
              maxlength="120"
              :class="{ 'has-error': errors.nombre }"
            />
            <span v-if="errors.nombre" class="error-text">{{ errors.nombre }}</span>
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
            {{ isSaving ? 'Guardando...' : 'Actualizar Nombre' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
