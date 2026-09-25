<script setup>
import { reactive, ref, computed, watch } from 'vue'
import {
  X,
  Search,
  Check,
  AlertCircle,
  Users,
  Clock,
  ShieldCheck
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
  alumnos: {
    type: Array,
    default: () => []
  },
  grupos: {
    type: Array,
    default: () => []
  },
  ofertas: {
    type: Array,
    default: () => []
  },
  cursos: {
    type: Array,
    default: () => []
  },
  categorias: {
    type: Array,
    default: () => []
  },
  docentes: {
    type: Array,
    default: () => []
  },
  asignacionesDocentes: {
    type: Array,
    default: () => []
  },
  horarios: {
    type: Array,
    default: () => []
  },
  inscripciones: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'save'])

const studentSearch = ref('')
const selectedStudent = ref(null)

const form = reactive({
  alumnoId: '',
  grupoId: ''
})

const errors = ref({})

const dayNamesMap = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
  LUNES: 'Lunes',
  MARTES: 'Martes',
  MIERCOLES: 'Miércoles',
  JUEVES: 'Jueves',
  VIERNES: 'Viernes',
  SABADO: 'Sábado',
  DOMINGO: 'Domingo'
}

function formatDay(d) {
  return dayNamesMap[d] || d
}

function formatTime(t) {
  if (!t) return ''
  return String(t).substring(0, 5)
}

watch(
  () => props.show,
  (val) => {
    if (val) {
      selectedStudent.value = null
      studentSearch.value = ''
      form.alumnoId = ''
      form.grupoId = props.grupos[0]?.id || ''
      errors.value = {}
    }
  }
)

const filteredStudents = computed(() => {
  const q = studentSearch.value.trim().toLowerCase()
  const all = props.alumnos || []
  if (!q) return all.slice(0, 10)
  return all.filter(a => {
    const fullName = `${a.nombre} ${a.apellidoPaterno || ''} ${a.apellidoMaterno || ''}`.toLowerCase()
    const mat = (a.matricula || '').toLowerCase()
    const tel = (a.telefono || '').replace(/\D/g, '')
    const em = (a.correo || '').toLowerCase()
    return fullName.includes(q) || mat.includes(q) || tel.includes(q) || em.includes(q)
  }).slice(0, 20)
})

const selectedGroupInfo = computed(() => {
  if (!form.grupoId) return null
  const g = props.grupos.find(x => x.id === Number(form.grupoId))
  if (!g) return null
  const oferta = props.ofertas.find(o => o.id === g.ofertaId) || {}
  const curso = props.cursos.find(c => c.id === oferta.cursoId) || {}
  const cat = props.categorias.find(c => c.id === g.categoriaId) || {}

  let docenteName = 'Sin docente asignado'
  const asignacion = (props.asignacionesDocentes || []).find(a => a.grupoId === g.id)
  if (asignacion) {
    const d = props.docentes.find(doc => doc.id === asignacion.docenteId)
    if (d) docenteName = `${d.nombre} ${d.apellidoPaterno || ''}`
    else if (asignacion.docente) docenteName = asignacion.docente
  } else if (g.docente) {
    docenteName = g.docente
  }

  const groupSchedules = (props.horarios || []).filter(h => h.grupoId === g.id).map(h => ({
    diaTexto: formatDay(h.dia),
    rangoHorario: `${formatTime(h.horaInicio)} - ${formatTime(h.horaFin)}`
  }))

  const enrolledCount = (props.inscripciones || []).filter(i => i.grupoId === g.id && i.estado !== 'BAJA').length
  return {
    ...g,
    nombreCurso: curso.nombre || g.curso || 'Taller Cultural',
    nombreCategoria: cat.nombre || 'General',
    docenteName,
    schedules: groupSchedules,
    enrolledCount
  }
})

function selectStudent(alumno) {
  selectedStudent.value = alumno
  form.alumnoId = alumno.id
  if (errors.value.alumnoId) delete errors.value.alumnoId
}

function clearStudent() {
  selectedStudent.value = null
  form.alumnoId = ''
  studentSearch.value = ''
}

function validate() {
  errors.value = {}
  if (!form.alumnoId) errors.value.alumnoId = 'Debes buscar y seleccionar un alumno de la lista.'
  if (!form.grupoId) errors.value.grupoId = 'Selecciona el grupo o taller al que se inscribirá.'
  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('save', {
    alumnoId: Number(form.alumnoId),
    grupoId: Number(form.grupoId)
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
        <h3>Nueva Inscripción a Taller Cultural</h3>
        <button
          class="modal-close"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <!-- Student Picker -->
          <div class="field">
            <label>Alumno a Inscribir *</label>

            <!-- Selected Student Card -->
            <div
              v-if="selectedStudent"
              class="selected-person-card"
            >
              <div class="person-avatar">
                {{ selectedStudent.nombre.charAt(0).toUpperCase() }}
              </div>
              <div class="person-details">
                <div class="name-row">
                  <strong>{{ selectedStudent.nombre }} {{ selectedStudent.apellidoPaterno }} {{ selectedStudent.apellidoMaterno || '' }}</strong>
                  <span class="tag-badge tag-student">{{ selectedStudent.matricula }}</span>
                </div>
                <span>
                  {{ selectedStudent.correo || 'Sin correo registrado' }}
                  <template v-if="selectedStudent.telefono">&bull; Tel: {{ selectedStudent.telefono }}</template>
                </span>
              </div>
              <button
                type="button"
                class="clear-person-btn"
                @click="clearStudent"
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
                    v-model="studentSearch"
                    type="text"
                    placeholder="Buscar por nombre, apellidos, matrícula o teléfono..."
                  />
                  <button
                    v-if="studentSearch"
                    type="button"
                    class="input-clear"
                    @click="studentSearch = ''"
                  >
                    <X :size="13" />
                  </button>
                </div>
              </div>

              <div class="picker-results-list">
                <div
                  v-for="a in filteredStudents"
                  :key="a.id"
                  class="picker-item"
                  @click="selectStudent(a)"
                >
                  <div class="person-avatar small">
                    {{ a.nombre.charAt(0).toUpperCase() }}
                  </div>
                  <div class="picker-item-info">
                    <div class="name-row">
                      <strong>{{ a.nombre }} {{ a.apellidoPaterno }} {{ a.apellidoMaterno || '' }}</strong>
                      <span class="tag-badge tag-student">{{ a.matricula }}</span>
                    </div>
                    <small>
                      {{ a.correo || 'Sin correo' }}
                      <template v-if="a.telefono">&bull; {{ a.telefono }}</template>
                    </small>
                  </div>
                  <Check :size="16" class="picker-select-icon" />
                </div>

                <div
                  v-if="filteredStudents.length === 0"
                  class="picker-no-results"
                >
                  <AlertCircle :size="16" /> No se encontraron alumnos con ese criterio de búsqueda.
                </div>
              </div>
            </div>
            <span v-if="errors.alumnoId" class="error-text">{{ errors.alumnoId }}</span>
          </div>

          <!-- Target Group Selector -->
          <div class="field">
            <label>Grupo / Taller Destino *</label>
            <select
              v-model="form.grupoId"
              :class="{ 'has-error': errors.grupoId }"
              @change="delete errors.grupoId"
            >
              <option value="">-- Selecciona un grupo de taller cultural --</option>
              <option
                v-for="g in grupos"
                :key="g.id"
                :value="g.id"
              >
                {{ g.nombreGrupo }} ({{ g.nombreCurso || g.curso || 'Taller Cultural' }})
              </option>
            </select>
            <span v-if="errors.grupoId" class="error-text">{{ errors.grupoId }}</span>
          </div>

          <!-- Selected Group Preview Card -->
          <div
            v-if="selectedGroupInfo"
            class="enrollment-group-preview"
          >
            <div class="preview-header">
              <div>
                <h4 class="preview-title">{{ selectedGroupInfo.nombreCurso }} - {{ selectedGroupInfo.nombreGrupo }}</h4>
                <span class="preview-cat">Categoría: {{ selectedGroupInfo.nombreCategoria }} &bull; Instructor: {{ selectedGroupInfo.docenteName }}</span>
              </div>
              <span class="preview-capacity-badge">
                <Users :size="13" /> {{ selectedGroupInfo.enrolledCount }} alumnos activos
              </span>
            </div>

            <!-- Schedules Preview -->
            <div class="preview-schedules">
              <span class="sched-label"><Clock :size="13" /> Horarios de clase asignados:</span>
              <div v-if="selectedGroupInfo.schedules.length" class="sched-badges">
                <span
                  v-for="(s, sIdx) in selectedGroupInfo.schedules"
                  :key="sIdx"
                  class="sched-pill"
                >
                  <strong>{{ s.diaTexto }}</strong>: {{ s.rangoHorario }}
                </span>
              </div>
              <span v-else class="text-muted-small">Este grupo aún no tiene horarios semanales programados.</span>
            </div>

            <!-- Collision Warning Notification -->
            <div class="conflict-check-note">
              <ShieldCheck :size="15" />
              <span>Validación automática: El sistema comprobará que no existan empalmes de horario con otras clases del alumno.</span>
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
            {{ isSaving ? 'Guardando...' : 'Completar Inscripción' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
