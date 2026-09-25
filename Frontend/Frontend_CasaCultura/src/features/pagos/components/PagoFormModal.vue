<script setup>
import { reactive, ref, computed, watch } from 'vue'
import {
  X,
  Search,
  Check
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
  }
})

const emit = defineEmits(['close', 'save'])

const searchStudent = ref('')
const selectedEnrollment = ref(null)

const form = reactive({
  inscripcionId: '',
  tipoPago: 'INSCRIPCION',
  periodo: `${new Date().getFullYear()}-1`,
  fechaVencimiento: '',
  fechaPago: new Date().toISOString().slice(0, 10),
  estado: 'PAGADO',
  monto: 500
})

const errors = ref({})

watch(
  () => props.show,
  (val) => {
    if (val) {
      selectedEnrollment.value = null
      searchStudent.value = ''
      errors.value = {}
      Object.assign(form, {
        inscripcionId: '',
        tipoPago: 'INSCRIPCION',
        periodo: `${new Date().getFullYear()}-1`,
        fechaVencimiento: '',
        fechaPago: new Date().toISOString().slice(0, 10),
        estado: 'PAGADO',
        monto: 500
      })
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
  onPaymentConceptChange()
}

function clearEnrollment() {
  selectedEnrollment.value = null
  form.inscripcionId = ''
  searchStudent.value = ''
}

function onPaymentConceptChange() {
  if (form.tipoPago === 'INSCRIPCION') form.monto = 500
  else if (form.tipoPago === 'MENSUALIDAD') form.monto = 400
  else if (form.tipoPago === 'RECARGO') form.monto = 150
}

function blockInvalidNumberKeys(event) {
  if (['e', 'E', '+', '-'].includes(event.key)) {
    event.preventDefault()
  }
}

function validate() {
  errors.value = {}
  if (!form.inscripcionId) errors.value.inscripcionId = 'Debes buscar y seleccionar un alumno inscrito de la lista.'
  if (!form.tipoPago) errors.value.tipoPago = 'Selecciona un tipo de pago.'
  if (!form.periodo?.trim()) errors.value.periodo = 'El periodo es obligatorio.'
  if (!form.monto || Number(form.monto) <= 0) errors.value.monto = 'Ingresa un monto mayor a 0.'
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    inscripcionId: Number(form.inscripcionId),
    tipoPago: form.tipoPago,
    periodo: form.periodo.trim(),
    fechaVencimiento: form.fechaVencimiento || null,
    fechaPago: form.fechaPago || null,
    estado: form.estado,
    monto: Number(form.monto)
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
        <h3>Registrar Cobro / Pago de Alumno</h3>
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
            <label>Alumno y Taller a Cobrar *</label>

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
              <label>Concepto de Pago *</label>
              <select v-model="form.tipoPago" @change="onPaymentConceptChange">
                <option value="INSCRIPCION">INSCRIPCIÓN ($500.00 MXN)</option>
                <option value="MENSUALIDAD">MENSUALIDAD ($400.00 MXN)</option>
                <option value="RECARGO">RECARGO / TRÁMITE ($150.00 MXN)</option>
              </select>
            </div>
            <div class="field">
              <label>Monto a Cobrar ($ MXN) *</label>
              <input
                v-model="form.monto"
                type="number"
                min="1"
                max="100000"
                step="0.01"
                placeholder="500"
                :class="{ 'has-error': errors.monto }"
                @keydown="blockInvalidNumberKeys"
              />
              <span v-if="errors.monto" class="error-text">{{ errors.monto }}</span>
            </div>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Periodo Escolar *</label>
              <input
                v-model="form.periodo"
                type="text"
                maxlength="50"
                placeholder="Ej. 2026-1, Mayo 2026..."
                :class="{ 'has-error': errors.periodo }"
              />
              <span v-if="errors.periodo" class="error-text">{{ errors.periodo }}</span>
            </div>
            <div class="field">
              <label>Estado del Pago *</label>
              <select v-model="form.estado">
                <option value="PAGADO">PAGADO (Completado)</option>
                <option value="PENDIENTE">PENDIENTE (Por Cobrar)</option>
                <option value="VENCIDO">VENCIDO (En Mora)</option>
              </select>
            </div>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Fecha de Pago</label>
              <input
                v-model="form.fechaPago"
                type="date"
              />
            </div>
            <div class="field">
              <label>Fecha de Vencimiento (Opcional)</label>
              <input
                v-model="form.fechaVencimiento"
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
            {{ isSaving ? 'Guardando...' : 'Registrar Pago' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
