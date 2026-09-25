<script setup>
import { ref, computed } from 'vue'
import {
  UserCheck,
  CalendarCheck2,
  Plus,
  Search,
  Download,
  Calendar,
  Filter,
  BookOpen
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import AsistenciaStatsBanner from '../components/AsistenciaStatsBanner.vue'
import AsistenciaTable from '../components/AsistenciaTable.vue'
import AsistenciaFormModal from '../components/AsistenciaFormModal.vue'
import {
  registerAttendance,
  generateAbsences
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData, exportToCsv } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showModal = ref(false)

const attendanceStatusFilter = ref('ALL') // 'ALL' | 'PRESENTE' | 'RETARDO' | 'FALTA'
const attendanceCourseFilter = ref('ALL')
const attendanceDateFilter = ref('')

const attendanceStats = computed(() => {
  const total = data.asistencias?.length || 0
  const presentes = (data.asistencias || []).filter(a => a.estado === 'PRESENTE').length
  const retardos = (data.asistencias || []).filter(a => a.estado === 'RETARDO').length
  const faltas = (data.asistencias || []).filter(a => a.estado === 'FALTA' || a.estado === 'JUSTIFICADA').length
  return {
    total,
    presentes,
    presentesPct: total ? Math.round((presentes / total) * 100) : 0,
    retardos,
    retardosPct: total ? Math.round((retardos / total) * 100) : 0,
    faltas,
    faltasPct: total ? Math.round((faltas / total) * 100) : 0
  }
})

const filteredAsistencias = computed(() => {
  const q = search.value.trim().toLowerCase()
  const statusF = attendanceStatusFilter.value
  const courseF = attendanceCourseFilter.value
  const dateF = attendanceDateFilter.value

  let list = (data.asistencias || []).map(a => {
    const insc = (data.inscripciones || []).find(i => i.id === a.inscripcionId)
    const al = insc ? (data.alumnos || []).find(x => x.id === insc.alumnoId) : null
    const grp = insc ? (data.grupos || []).find(g => g.id === insc.grupoId) : null
    const ofr = grp ? (data.ofertas || []).find(o => o.id === grp.ofertaId) : null
    const crs = ofr ? (data.cursos || []).find(c => c.id === ofr.cursoId) : null

    return {
      ...a,
      alumno: a.alumno || (al ? `${al.nombre} ${al.apellidoPaterno} ${al.apellidoMaterno || ''}`.trim() : 'Alumno'),
      matricula: a.matricula || al?.matricula || '',
      grupo: a.grupo || grp?.nombreGrupo || 'Grupo Cultural',
      taller: crs?.nombre || grp?.curso || '',
      cursoId: crs?.id || ofr?.cursoId || '',
      fecha: a.fecha ? a.fecha.slice(0, 10) : ''
    }
  })

  if (statusF !== 'ALL') {
    list = list.filter(a => a.estado === statusF)
  }
  if (courseF !== 'ALL') {
    list = list.filter(a => String(a.cursoId) === String(courseF))
  }
  if (dateF) {
    list = list.filter(a => a.fecha === dateF)
  }

  if (q) {
    list = list.filter(a =>
      a.alumno.toLowerCase().includes(q) ||
      a.matricula.toLowerCase().includes(q) ||
      a.grupo.toLowerCase().includes(q) ||
      (a.taller && a.taller.toLowerCase().includes(q))
    )
  }

  return list
})

async function handleGenerateDailyAbsences() {
  try {
    const count = await generateAbsences()
    showToast(`Se generaron ${count} faltas para el día de hoy.`)
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error generando faltas', 'error')
  }
}

async function handleSaveAttendance(payload) {
  isSaving.value = true
  try {
    await registerAttendance(payload)
    showToast('Asistencia guardada exitosamente.')
    showModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al guardar asistencia', 'error')
  } finally {
    isSaving.value = false
  }
}

function handleExport() {
  const rows = filteredAsistencias.value.map(a => ({
    ID: a.id,
    Alumno: a.alumno,
    Matricula: a.matricula,
    Grupo: a.grupo,
    Taller: a.taller || '',
    Fecha: a.fecha || '',
    Hora: a.hora || '',
    Estado: a.estado
  }))
  exportToCsv('asistencias', rows)
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <UserCheck :size="28" />
        </div>
        <div>
          <h1>Asistencias</h1>
          <p>Registro diario y control de faltas</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="secondary-button" @click="handleGenerateDailyAbsences">
          <CalendarCheck2 :size="16" />
          Generar Faltas de Hoy
        </button>

        <button class="primary-button" @click="showModal = true">
          <Plus :size="18" />
          Registrar Asistencia
        </button>
      </div>
    </div>

    <!-- Attendance Stats Banner -->
    <AsistenciaStatsBanner :stats="attendanceStats" />

    <!-- Controls Bar -->
    <div class="module-content-wrapper">
      <div class="filter-actions-bar">
        <div class="search-input-wrapper">
          <Search class="search-icon" :size="18" />
          <input
            v-model="search"
            type="text"
            placeholder="Buscar por alumno, matrícula o grupo..."
            class="search-input"
          />
        </div>

        <div class="filters-group-right">
          <div class="filter-pill-select">
            <Calendar :size="15" />
            <input
              v-model="attendanceDateFilter"
              type="date"
              class="date-filter-input"
            />
          </div>

          <div class="filter-pill-select">
            <Filter :size="15" />
            <select v-model="attendanceStatusFilter">
              <option value="ALL">Todos los Estados</option>
              <option value="PRESENTE">Presente</option>
              <option value="RETARDO">Retardo</option>
              <option value="FALTA">Falta</option>
              <option value="JUSTIFICADA">Justificada</option>
            </select>
          </div>

          <div class="filter-pill-select">
            <BookOpen :size="15" />
            <select v-model="attendanceCourseFilter">
              <option value="ALL">Todos los Talleres</option>
              <option v-for="c in data.cursos" :key="c.id" :value="c.id">
                {{ c.nombre }}
              </option>
            </select>
          </div>

          <button class="export-button" @click="handleExport">
            <Download :size="16" />
            Exportar Lista
          </button>
        </div>
      </div>

      <!-- Asistencia Table Component -->
      <AsistenciaTable :asistencias="filteredAsistencias" />
    </div>

    <!-- Asistencia Form Modal Component -->
    <AsistenciaFormModal
      :show="showModal"
      :is-saving="isSaving"
      :inscripciones="data.inscripciones || []"
      :alumnos="data.alumnos || []"
      :grupos="data.grupos || []"
      :horarios="data.horarios || []"
      @close="showModal = false"
      @save="handleSaveAttendance"
    />
  </div>
</template>
