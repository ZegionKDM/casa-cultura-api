<script setup>
import { reactive, watch, ref } from 'vue'
import {
  X,
  CalendarDays,
  Trash2,
  Plus,
  Sparkles
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
  grupos: {
    type: Array,
    default: () => []
  },
  defaultGroupId: {
    type: [Number, String],
    default: ''
  },
  defaultDay: {
    type: String,
    default: 'MONDAY'
  }
})

const emit = defineEmits(['close', 'save'])

const form = reactive({
  grupoId: '',
  slots: [
    { dias: ['MONDAY', 'WEDNESDAY', 'FRIDAY'], horaInicio: '16:00', horaFin: '18:00' }
  ]
})

const errors = ref({})

const weekDaysList = [
  { key: 'MONDAY', label: 'Lunes', short: 'Lun' },
  { key: 'TUESDAY', label: 'Martes', short: 'Mar' },
  { key: 'WEDNESDAY', label: 'Miércoles', short: 'Mié' },
  { key: 'THURSDAY', label: 'Jueves', short: 'Jue' },
  { key: 'FRIDAY', label: 'Viernes', short: 'Vie' },
  { key: 'SATURDAY', label: 'Sábado', short: 'Sáb' },
  { key: 'SUNDAY', label: 'Domingo', short: 'Dom' }
]

watch(
  () => props.show,
  (val) => {
    if (val) {
      errors.value = {}
      form.grupoId = props.defaultGroupId || props.grupos[0]?.id || ''
      const initDay = props.defaultDay || 'MONDAY'
      form.slots = [
        { dias: [initDay], horaInicio: '16:00', horaFin: '18:00' }
      ]
    }
  }
)

function toggleSlotDay(slot, dayKey) {
  if (!slot.dias) slot.dias = []
  const idx = slot.dias.indexOf(dayKey)
  if (idx >= 0) {
    if (slot.dias.length > 1) {
      slot.dias.splice(idx, 1)
    }
  } else {
    slot.dias.push(dayKey)
  }
}

function applySlotPreset(slot, preset) {
  if (preset === 'MWF') {
    slot.dias = ['MONDAY', 'WEDNESDAY', 'FRIDAY']
  } else if (preset === 'TTH') {
    slot.dias = ['TUESDAY', 'THURSDAY']
  } else if (preset === 'WEEKDAYS') {
    slot.dias = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
  } else if (preset === 'WEEKEND') {
    slot.dias = ['SATURDAY', 'SUNDAY']
  } else if (preset === 'SAT') {
    slot.dias = ['SATURDAY']
  }
}

function addSlot() {
  form.slots.push({
    dias: ['SATURDAY'],
    horaInicio: '10:00',
    horaFin: '12:00'
  })
}

function removeSlot(index) {
  if (form.slots.length > 1) {
    form.slots.splice(index, 1)
  }
}

function countTotalSessions(slots) {
  if (!slots || !slots.length) return 0
  return slots.reduce((acc, s) => acc + (s.dias ? s.dias.length : 0), 0)
}

function validate() {
  errors.value = {}
  if (!form.grupoId) errors.value.grupoId = 'Selecciona un grupo.'
  if (!form.slots || !form.slots.length) {
    errors.value.slots = 'Agrega al menos un bloque de horario.'
  } else {
    form.slots.forEach((s, idx) => {
      if (!s.dias || !s.dias.length) errors.value[`slot_${idx}_dias`] = 'Selecciona al menos un día.'
      if (!s.horaInicio) errors.value[`slot_${idx}_inicio`] = 'Hora de inicio requerida.'
      if (!s.horaFin) errors.value[`slot_${idx}_fin`] = 'Hora de fin requerida.'
      if (s.horaInicio && s.horaFin && s.horaFin <= s.horaInicio) {
        errors.value[`slot_${idx}_fin`] = 'La hora de fin debe ser posterior a la de inicio.'
      }
    })
  }
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    grupoId: form.grupoId,
    slots: form.slots
  })
}
</script>

<template>
  <div
    v-if="show"
    class="modal-backdrop"
    @click.self="emit('close')"
  >
    <div class="modal-card modal-card-wide">
      <div class="modal-header">
        <h3>Programar Horario Semanal</h3>
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
            <label>Grupo / Taller Académico *</label>
            <select
              v-model="form.grupoId"
              :class="{ 'has-error': errors.grupoId }"
              @change="delete errors.grupoId"
            >
              <option value="">Selecciona un grupo</option>
              <option
                v-for="g in grupos"
                :key="g.id"
                :value="g.id"
              >
                {{ g.nombreGrupo }} ({{ g.nombreCurso || g.curso || 'Taller' }})
              </option>
            </select>
            <span v-if="errors.grupoId" class="error-text">{{ errors.grupoId }}</span>
          </div>

          <div class="schedule-batch-container">
            <div
              v-for="(slot, idx) in form.slots"
              :key="idx"
              class="schedule-slot-card"
            >
              <div class="slot-card-header">
                <div class="slot-badge">
                  <CalendarDays :size="15" />
                  <span>Bloque de Horario #{{ idx + 1 }}</span>
                </div>
                <button
                  v-if="form.slots.length > 1"
                  type="button"
                  class="btn-remove-slot"
                  title="Eliminar este bloque"
                  @click="removeSlot(idx)"
                >
                  <Trash2 :size="14" />
                  <span>Quitar bloque</span>
                </button>
              </div>

              <!-- Presets chips -->
              <div class="slot-presets">
                <span class="preset-label">Atajos rápidos:</span>
                <button
                  type="button"
                  class="preset-chip"
                  @click="applySlotPreset(slot, 'MWF')"
                >
                  Lun, Mié, Vie
                </button>
                <button
                  type="button"
                  class="preset-chip"
                  @click="applySlotPreset(slot, 'TTH')"
                >
                  Mar, Jue
                </button>
                <button
                  type="button"
                  class="preset-chip"
                  @click="applySlotPreset(slot, 'WEEKDAYS')"
                >
                  Lun a Vie
                </button>
                <button
                  type="button"
                  class="preset-chip"
                  @click="applySlotPreset(slot, 'WEEKEND')"
                >
                  Sáb y Dom
                </button>
                <button
                  type="button"
                  class="preset-chip"
                  @click="applySlotPreset(slot, 'SAT')"
                >
                  Sábados
                </button>
              </div>

              <!-- Day selector pills -->
              <div class="slot-days-selector">
                <label>Días de este bloque:</label>
                <div class="days-pills-row">
                  <button
                    v-for="d in weekDaysList"
                    :key="d.key"
                    type="button"
                    class="day-pill-btn"
                    :class="{ selected: slot.dias && slot.dias.includes(d.key) }"
                    @click="toggleSlotDay(slot, d.key)"
                  >
                    {{ d.label }}
                  </button>
                </div>
                <span
                  v-if="errors[`slot_${idx}_dias`]"
                  class="error-text"
                >{{ errors[`slot_${idx}_dias`] }}</span>
              </div>

              <!-- Time Inputs -->
              <div class="form-grid-2">
                <div class="field">
                  <label>Hora de Inicio *</label>
                  <input
                    v-model="slot.horaInicio"
                    type="time"
                    :class="{ 'has-error': errors[`slot_${idx}_inicio`] }"
                  />
                  <span
                    v-if="errors[`slot_${idx}_inicio`]"
                    class="error-text"
                  >{{ errors[`slot_${idx}_inicio`] }}</span>
                </div>

                <div class="field">
                  <label>Hora de Fin *</label>
                  <input
                    v-model="slot.horaFin"
                    type="time"
                    :class="{ 'has-error': errors[`slot_${idx}_fin`] }"
                  />
                  <span
                    v-if="errors[`slot_${idx}_fin`]"
                    class="error-text"
                  >{{ errors[`slot_${idx}_fin`] }}</span>
                </div>
              </div>
            </div>

            <!-- Add another slot button & summary -->
            <div class="schedule-batch-footer">
              <button
                type="button"
                class="btn-add-slot"
                @click="addSlot"
              >
                <Plus :size="16" />
                <span>Agregar otro bloque de horario (ej. fines de semana u otro turno)</span>
              </button>

              <div class="batch-summary-info">
                <Sparkles :size="16" />
                <span>
                  Se programarán <strong>{{ countTotalSessions(form.slots) }} sesión(es)</strong> en la base de datos en una sola operación.
                </span>
              </div>
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
            {{ isSaving ? 'Guardando...' : 'Guardar Todos los Horarios' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
