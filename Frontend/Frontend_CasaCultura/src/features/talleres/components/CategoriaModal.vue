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
  }
})

const emit = defineEmits(['close', 'save'])

const form = reactive({
  nombre: ''
})

const errors = ref({})

watch(
  () => props.show,
  (val) => {
    if (val) {
      form.nombre = ''
      errors.value = {}
    }
  }
)

function validate() {
  errors.value = {}
  if (!form.nombre || !form.nombre.trim()) {
    errors.value.nombre = 'El nombre de la categoría es obligatorio.'
  }
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', { nombre: form.nombre.trim() })
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
        <h3>Nueva Categoría de Edad</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <p class="modal-help-text">
            Las categorías de edad permiten clasificar los grupos en los talleres (ej. Niños, Adolescentes, Adultos, Tercera Edad).
          </p>
          <div class="field">
            <label>Nombre de la Categoría de Edad *</label>
            <input
              v-model="form.nombre"
              type="text"
              maxlength="100"
              placeholder="Ej. Infantil (6 a 12 años)"
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
            {{ isSaving ? 'Guardando...' : 'Crear Categoría' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
