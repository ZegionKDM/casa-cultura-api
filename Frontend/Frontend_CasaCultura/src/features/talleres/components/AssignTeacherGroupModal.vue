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
  group: {
    type: Object,
    default: null
  },
  courseName: {
    type: String,
    default: ''
  },
  docentes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'save'])

const form = reactive({
  grupoId: '',
  nombreGrupo: '',
  docenteId: '',
  fechaInicio: new Date().toISOString().slice(0, 10),
  fechaFin: ''
})

const errors = ref({})

watch(
  () => props.group,
  (g) => {
    if (g) {
      form.grupoId = g.id
      form.nombreGrupo = `${props.courseName ? props.courseName + ' - ' : ''}${g.nombreGrupo || ''}`
      form.docenteId = g.docenteAsignado?.id || ''
      form.fechaInicio = new Date().toISOString().slice(0, 10)
      form.fechaFin = ''
      errors.value = {}
    }
  },
  { immediate: true }
)

function validate() {
  errors.value = {}
  if (!form.grupoId) errors.value.grupoId = 'El grupo es obligatorio.'
  if (!form.docenteId) errors.value.docenteId = 'Selecciona un instructor/docente.'
  if (!form.fechaInicio) errors.value.fechaInicio = 'La fecha de inicio es requerida.'
  if (form.fechaInicio && form.fechaFin && form.fechaFin < form.fechaInicio) {
    errors.value.fechaFin = 'La fecha de fin debe ser posterior a la de inicio.'
  }
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    grupoId: Number(form.grupoId),
    docenteId: Number(form.docenteId),
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
        <h3>Asignar Instructor: {{ form.nombreGrupo }}</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <div class="context-banner">
            <UserCheck :size="18" />
            <span>Asignando instructor a: <strong>{{ form.nombreGrupo }}</strong></span>
          </div>

          <div class="field">
            <label>Selecciona al Docente / Instructor *</label>
            <select
              v-model="form.docenteId"
              :class="{ 'has-error': errors.docenteId }"
            >
              <option value="">Selecciona al docente para este grupo...</option>
              <option
                v-for="d in docentes"
                :key="d.id"
                :value="d.id"
              >
                {{ d.nombre }} {{ d.apellidoPaterno }} {{ d.apellidoMaterno || '' }} ({{ d.especialidad || 'General' }})
              </option>
            </select>
            <span v-if="errors.docenteId" class="error-text">{{ errors.docenteId }}</span>
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
              />
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
