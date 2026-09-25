<script setup>
import { reactive, ref, computed, watch } from 'vue'
import {
  X,
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
  inscripciones: {
    type: Array,
    default: () => []
  },
  alumnos: {
    type: Array,
    default: () => []
  },
  grupos: {
    type: Array,
    default: () => []
  },
  horarios: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'save'])

const searchStudent = ref('')
const selectedEnrollment = ref(null)

const form = reactive({
  inscripcionId: '',
  horarioId: '',
  fecha: new Date().toISOString().slice(0, 10),
  estado: 'PRESENTE'
})

const errors = ref({})

watch(
  () => props.show,
  (val) => {
    if (val) {
      selectedEnrollment.value = null
      searchStudent.value = ''
      form.inscripcionId = ''
      form.horarioId = ''
      form.fecha = new Date().toISOString().slice(0, 10)
      form.estado = 'PRESENTE'
      errors.value = {}
    }
  }
)

const filteredEnrollments = computed(() => {
  const q = searchStudent.value.trim().toLowerCase()
  const list = props.inscripciones.filter(i => i.estado === 'ACTIVA' || !i.estado)
  if (!q) return list.slice(0, 10)
  return list.filter(i => {
    const al = props.alumnos.find(a => a.id === i.alumnoId)
    const per = al?.persona
    const fullName = `${al?.nombre || ''} ${al?.apellidoPaterno || ''} ${al?.apellidoMaterno || ''} ${per?.nombre || ''} ${i.alumno || ''}`.toLowerCase()
    const mat = (i.matricula || al?.matricula || '').toLowerCase()
    const grp = (i.grupo || '').toLowerCase()
    const tel = (al?.telefono || per?.telefono || '').replace(/\D/g, '')
    const em = (al?.correo || per?.correo || '').toLowerCase()
    return fullName.includes(q) || mat.includes(q) || grp.includes(q) || tel.includes(q) || em.includes(q)
  }).slice(0, 20)
})

function selectEnrollment(item) {
  selectedEnrollment.value = item
  form.inscripcionId = item.id
  if (errors.value.inscripcionId) delete errors.value.inscripcionId
}

function clearEnrollment() {
  selectedEnrollment.value = null
  form.inscripcionId = ''
  searchStudent.value = ''
}

function validate() {
  errors.value = {}
  if (!form.inscripcionId) errors.value.inscripcionId = 'Debes buscar y seleccionar un alumno inscrito de la lista.'
  if (!form.fecha) errors.value.fecha = 'La fecha es obligatoria.'
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    inscripcionId: Number(form.inscripcionId),
    horarioId: form.horarioId ? Number(form.horarioId) : null,
    fecha: form.fecha,
    estado: form.estado
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
        <h3>Registrar Asistencia Manual</h3>
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
            <label>Alumno y Taller Inscrito *</label>

            <!-- Selected Student Preview Card -->
            <div
              v-if="selectedEnrollment"
              class="selected-person-card"
            >
              <div class="person-avatar">
                {{ selectedEnrollment.alumno ? selectedEnrollment.alumno.charAt(0).toUpperCase() : 'A' }}
              </div>
              <div class="person-details">
                <div class="name-row">
                  <strong>{{ selectedEnrollment.alumno }}</strong>
                  <span v-if="selectedEnrollment.matricula" class="tag-badge tag-student">{{ selectedEnrollment.matricula }}</span>
                </div>
                <span>{{ selectedEnrollment.grupo }}</span>
              </div>
              <button
                type="button"
                class="clear-person-btn"
                @click="clearEnrollment"
              >
                <X :size="14" /> Cambiar Alumno
              </button>
            </div>

            <!-- Searchable Student Picker -->
            <div
              v-else
              class="searchable-picker-container"
            >
              <div class="picker-search-bar">
                <div class="picker-search-input">
                  <Search :size="15" />
                  <input
                    v-model="searchStudent"
                    type="text"
                    placeholder="Buscar por nombre, matrícula o taller cultural..."
                  />
                  <button
                    v-if="searchStudent"
                    type="button"
                    class="input-clear"
                    @click="searchStudent = ''"
                  >
                    <X :size="13" />
                  </button>
                </div>
              </div>

              <div class="picker-results-list">
                <div
                  v-for="item in filteredEnrollments"
                  :key="item.id"
                  class="picker-item"
                  @click="selectEnrollment(item)"
                >
                  <div class="person-avatar small">
                    {{ item.alumno ? item.alumno.charAt(0).toUpperCase() : 'A' }}
                  </div>
                  <div class="picker-item-info">
                    <div class="name-row">
                      <strong>{{ item.alumno }}</strong>
                      <span v-if="item.matricula" class="tag-badge tag-student">{{ item.matricula }}</span>
                    </div>
                    <small>{{ item.grupo }}</small>
                  </div>
                  <Check :size="16" class="picker-select-icon" />
                </div>

                <div
                  v-if="filteredEnrollments.length === 0"
                  class="picker-no-results"
                >
                  <span>No se encontraron inscripciones que coincidan con la búsqueda.</span>
                </div>
              </div>
            </div>

            <span v-if="errors.inscripcionId" class="error-text">{{ errors.inscripcionId }}</span>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Fecha de la Clase *</label>
              <input
                v-model="form.fecha"
                type="date"
                :class="{ 'has-error': errors.fecha }"
              />
              <span v-if="errors.fecha" class="error-text">{{ errors.fecha }}</span>
            </div>
            <div class="field">
              <label>Estado de Asistencia *</label>
              <select v-model="form.estado">
                <option value="PRESENTE">PRESENTE</option>
                <option value="RETARDO">RETARDO</option>
                <option value="FALTA">FALTA</option>
                <option value="JUSTIFICADA">JUSTIFICADA</option>
              </select>
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
            {{ isSaving ? 'Guardando...' : 'Guardar Asistencia' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
