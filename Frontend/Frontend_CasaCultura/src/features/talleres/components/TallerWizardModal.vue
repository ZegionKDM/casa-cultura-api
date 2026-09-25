<script setup>
import { reactive, watch, ref } from 'vue'
import { X, Clock } from 'lucide-vue-next'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  isSaving: {
    type: Boolean,
    default: false
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
  nombreCurso: '',
  tipoOferta: `SEMESTRAL ${new Date().getFullYear()}-1`,
  fechaInicio: new Date().toISOString().slice(0, 10),
  fechaFin: new Date(new Date().setMonth(new Date().getMonth() + 5)).toISOString().slice(0, 10),
  crearGrupoInicial: true,
  nombreGrupo: 'Grupo A - Niños',
  categoriaId: '',
  docenteId: '',
  dia: 'MONDAY',
  horaInicio: '16:00',
  horaFin: '17:30'
})

const errors = ref({})

watch(
  () => props.show,
  (val) => {
    if (val) {
      errors.value = {}
      Object.assign(form, {
        nombreCurso: '',
        tipoOferta: `SEMESTRAL ${new Date().getFullYear()}-1`,
        fechaInicio: new Date().toISOString().slice(0, 10),
        fechaFin: new Date(new Date().setMonth(new Date().getMonth() + 5)).toISOString().slice(0, 10),
        crearGrupoInicial: true,
        nombreGrupo: 'Grupo A - Niños',
        categoriaId: props.categorias[0]?.id || '',
        docenteId: props.docentes[0]?.id || '',
        dia: 'MONDAY',
        horaInicio: '16:00',
        horaFin: '17:30'
      })
    }
  }
)

function validate() {
  errors.value = {}
  if (!form.nombreCurso || !form.nombreCurso.trim()) {
    errors.value.nombreCurso = 'El nombre del taller o curso es obligatorio.'
  }
  if (!form.tipoOferta?.trim()) {
    errors.value.tipoOferta = 'El tipo de ciclo u oferta es obligatorio.'
  }
  if (!form.fechaInicio) errors.value.fechaInicio = 'Fecha de inicio obligatoria.'
  if (!form.fechaFin) errors.value.fechaFin = 'Fecha de fin obligatoria.'
  if (form.fechaInicio && form.fechaFin && form.fechaFin < form.fechaInicio) {
    errors.value.fechaFin = 'La fecha de fin debe ser posterior a la de inicio.'
  }
  if (form.crearGrupoInicial) {
    if (!form.nombreGrupo?.trim()) errors.value.nombreGrupo = 'El nombre del grupo es obligatorio.'
    if (!form.categoriaId) errors.value.categoriaId = 'Selecciona una categoría de edad.'
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
    <div class="modal-card modal-card-wide">
      <div class="modal-header">
        <h3>Nuevo Taller Cultural Integral</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <div class="form-step-banner">
            <span class="step-num">1</span>
            <div>
              <strong>Datos de la Disciplina / Taller Cultural</strong>
              <p>Define la disciplina cultural (ej. Pintura y Artes Plásticas, Danza Folclórica, Guitarra Clásica).</p>
            </div>
          </div>

          <div class="field">
            <label>Nombre del Taller / Curso *</label>
            <input
              v-model="form.nombreCurso"
              type="text"
              maxlength="120"
              placeholder="Ej. Guitarra Clásica y Cuerdas"
              :class="{ 'has-error': errors.nombreCurso }"
            />
            <span v-if="errors.nombreCurso" class="error-text">{{ errors.nombreCurso }}</span>
          </div>

          <div class="form-step-banner">
            <span class="step-num">2</span>
            <div>
              <strong>Ciclo Escolar / Modalidad (Oferta)</strong>
              <p>Establece el periodo y vigencia de este taller.</p>
            </div>
          </div>

          <div class="field">
            <label>Nombre o Tipo de Ciclo *</label>
            <input
              v-model="form.tipoOferta"
              type="text"
              maxlength="80"
              placeholder="Ej. SEMESTRAL 2026-1, TALLER DE VERANO 2026..."
              :class="{ 'has-error': errors.tipoOferta }"
            />
            <span v-if="errors.tipoOferta" class="error-text">{{ errors.tipoOferta }}</span>
          </div>

          <div class="form-grid-2">
            <div class="field">
              <label>Fecha de Inicio *</label>
              <input
                v-model="form.fechaInicio"
                type="date"
                :class="{ 'has-error': errors.fechaInicio }"
              />
              <span v-if="errors.fechaInicio" class="error-text">{{ errors.fechaInicio }}</span>
            </div>
            <div class="field">
              <label>Fecha de Fin *</label>
              <input
                v-model="form.fechaFin"
                type="date"
                :class="{ 'has-error': errors.fechaFin }"
              />
              <span v-if="errors.fechaFin" class="error-text">{{ errors.fechaFin }}</span>
            </div>
          </div>

          <div class="form-step-banner">
            <span class="step-num">3</span>
            <div>
              <strong>Estructura del Primer Grupo</strong>
              <p>Crea el primer grupo para este taller asignando su categoría, docente y horario inicial.</p>
            </div>
          </div>

          <div class="checkbox-field-box">
            <label class="custom-checkbox">
              <input
                v-model="form.crearGrupoInicial"
                type="checkbox"
              />
              <span class="checkmark"></span>
              <span class="checkbox-label-text">
                <strong>Crear el primer grupo de inmediato</strong> (Recomendado)
              </span>
            </label>
          </div>

          <template v-if="form.crearGrupoInicial">
            <div class="form-grid-2">
              <div class="field">
                <label>Nombre del Grupo *</label>
                <input
                  v-model="form.nombreGrupo"
                  type="text"
                  maxlength="100"
                  placeholder="Ej. Grupo A - Niños"
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

            <div class="schedule-subform-box">
              <span class="subform-title"><Clock :size="14" /> Horario Inicial de Clase</span>
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
            {{ isSaving ? 'Guardando...' : 'Crear Taller Completo' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
