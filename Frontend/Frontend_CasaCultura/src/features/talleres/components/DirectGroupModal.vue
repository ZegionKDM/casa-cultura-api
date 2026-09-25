<script setup>
import { reactive, watch, ref } from 'vue'
import { X, BookOpen } from 'lucide-vue-next'

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
  },
  categorias: {
    type: Array,
    default: () => []
  },
  docentes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'save'])

const form = reactive({
  cursoId: '',
  ofertaId: '',
  nombreCurso: '',
  nombreGrupo: '',
  categoriaId: '',
  docenteId: '',
  agregarHorarioInicial: true,
  dia: 'MONDAY',
  horaInicio: '16:00',
  horaFin: '17:30'
})

const errors = ref({})

watch(
  () => props.course,
  (c) => {
    if (c) {
      let ofertaId = c.ofertaActual?.id || ''
      if (!ofertaId && c.ofertas?.length > 0) {
        ofertaId = c.ofertas[0].id
      }

      Object.assign(form, {
        cursoId: c.id,
        ofertaId: ofertaId,
        nombreCurso: c.nombre,
        nombreGrupo: `Grupo ${String.fromCharCode(65 + (c.grupos?.length || 0))}`,
        categoriaId: props.categorias[0]?.id || '',
        docenteId: props.docentes[0]?.id || '',
        agregarHorarioInicial: true,
        dia: 'MONDAY',
        horaInicio: '16:00',
        horaFin: '17:30'
      })
      errors.value = {}
    }
  },
  { immediate: true }
)

function validate() {
  errors.value = {}
  if (!form.nombreGrupo?.trim()) errors.value.nombreGrupo = 'El nombre del grupo es obligatorio.'
  if (!form.categoriaId) errors.value.categoriaId = 'Selecciona una categoría de edad.'
  if (form.agregarHorarioInicial) {
    if (form.horaInicio && form.horaFin && form.horaFin <= form.horaInicio) {
      errors.value.horaFin = 'La hora de fin debe ser posterior a la de inicio.'
    }
  }
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', { ...form })
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
        <h3>Agregar Grupo a Taller: {{ form.nombreCurso }}</h3>
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
            <BookOpen :size="18" />
            <span>Agregando grupo para el taller: <strong>{{ form.nombreCurso }}</strong></span>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Nombre del Grupo *</label>
              <input
                v-model="form.nombreGrupo"
                type="text"
                maxlength="100"
                placeholder="Ej. Grupo B - Jóvenes y Adultos"
                :class="{ 'has-error': errors.nombreGrupo }"
              />
              <span v-if="errors.nombreGrupo" class="error-text">{{ errors.nombreGrupo }}</span>
            </div>

            <div class="field">
              <label>Categoría de Edad *</label>
              <select
                v-model="form.categoriaId"
                :class="{ 'has-error': errors.categoriaId }"
              >
                <option value="">Selecciona una categoría</option>
                <option
                  v-for="cat in categorias"
                  :key="cat.id"
                  :value="cat.id"
                >
                  {{ cat.nombre }}
                </option>
              </select>
              <span v-if="errors.categoriaId" class="error-text">{{ errors.categoriaId }}</span>
            </div>
          </div>

          <div class="field">
            <label>Instructor / Docente Asignado (Opcional)</label>
            <select v-model="form.docenteId">
              <option value="">-- Sin docente asignado por ahora --</option>
              <option
                v-for="d in docentes"
                :key="d.id"
                :value="d.id"
              >
                {{ d.nombre }} {{ d.apellidoPaterno }} {{ d.apellidoMaterno || '' }} ({{ d.especialidad || 'General' }})
              </option>
            </select>
          </div>

          <div class="checkbox-field-box">
            <label class="custom-checkbox">
              <input
                v-model="form.agregarHorarioInicial"
                type="checkbox"
              />
              <span class="checkmark"></span>
              <span class="checkbox-label-text">
                <strong>Programar horario inicial para este grupo</strong>
              </span>
            </label>
          </div>

          <template v-if="form.agregarHorarioInicial">
            <div class="form-grid-3">
              <div class="field">
                <label>Día</label>
                <select v-model="form.dia">
                  <option value="MONDAY">Lunes</option>
                  <option value="TUESDAY">Martes</option>
                  <option value="WEDNESDAY">Miércoles</option>
                  <option value="THURSDAY">Jueves</option>
                  <option value="FRIDAY">Viernes</option>
                  <option value="SATURDAY">Sábado</option>
                  <option value="SUNDAY">Domingo</option>
                </select>
              </div>
              <div class="field">
                <label>Hora Inicio</label>
                <input
                  v-model="form.horaInicio"
                  type="time"
                />
              </div>
              <div class="field">
                <label>Hora Fin</label>
                <input
                  v-model="form.horaFin"
                  type="time"
                />
              </div>
            </div>
          </template>
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
            {{ isSaving ? 'Guardando...' : 'Crear Grupo' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
