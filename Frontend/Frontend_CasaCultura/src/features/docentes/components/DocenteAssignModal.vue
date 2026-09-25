<script setup>
import { reactive, watch, ref } from 'vue'
import { X, UserCheck } from 'lucide-vue-next'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  isSaving: {
    type: Boolean,
    default: false
  },
  docente: {
    type: Object,
    default: null
  },
  grupos: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'assign'])

const form = reactive({
  docenteId: '',
  grupoId: '',
  fechaInicio: new Date().toISOString().slice(0, 10),
  fechaFin: ''
})

const errors = ref({})

watch(
  () => props.docente,
  (d) => {
    if (d) {
      form.docenteId = d.id
      form.grupoId = props.grupos[0]?.id || ''
      form.fechaInicio = new Date().toISOString().slice(0, 10)
      form.fechaFin = ''
    }
    errors.value = {}
  },
  { immediate: true }
)

function validate() {
  errors.value = {}
  if (!form.docenteId) errors.value.docenteId = 'Selecciona un docente.'
  if (!form.grupoId) errors.value.grupoId = 'Selecciona un grupo.'
  if (!form.fechaInicio) errors.value.fechaInicio = 'La fecha de inicio es requerida.'
  if (form.fechaInicio && form.fechaFin && form.fechaFin < form.fechaInicio) {
    errors.value.fechaFin = 'La fecha de fin debe ser posterior a la de inicio.'
  }
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('assign', {
    docenteId: Number(form.docenteId),
    grupoId: Number(form.grupoId),
    fechaInicio: form.fechaInicio,
    fechaFin: form.fechaFin || null
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
        <h3>Asignar Docente a Grupo</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <div v-if="docente" class="context-banner">
            <UserCheck :size="18" />
            <span>Asignando al docente: <strong>{{ docente.nombre }} {{ docente.apellidoPaterno }}</strong></span>
          </div>

          <div class="field">
            <label>Selecciona el Grupo / Taller *</label>
            <select
              v-model="form.grupoId"
              :class="{ 'has-error': errors.grupoId }"
            >
              <option value="" disabled>Selecciona un grupo cultural...</option>
              <option
                v-for="g in grupos"
                :key="g.id"
                :value="g.id"
              >
                {{ g.nombreCurso || g.curso || 'Taller' }} - {{ g.nombreGrupo || ('Grupo #' + g.id) }} ({{ g.categoria || 'General' }})
              </option>
            </select>
            <span v-if="errors.grupoId" class="error-text">{{ errors.grupoId }}</span>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Fecha de Inicio de Asignación *</label>
              <input
                v-model="form.fechaInicio"
                type="date"
                :class="{ 'has-error': errors.fechaInicio }"
              />
              <span v-if="errors.fechaInicio" class="error-text">{{ errors.fechaInicio }}</span>
            </div>
            <div class="field">
              <label>Fecha de Fin (Opcional)</label>
              <input
                v-model="form.fechaFin"
                type="date"
                :class="{ 'has-error': errors.fechaFin }"
              />
              <span v-if="errors.fechaFin" class="error-text">{{ errors.fechaFin }}</span>
            </div>
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
            {{ isSaving ? 'Guardando...' : 'Asignar Docente' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
