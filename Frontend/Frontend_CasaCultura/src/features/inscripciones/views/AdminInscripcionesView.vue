<script setup>
import { ref, computed } from 'vue'
import {
  ClipboardList,
  Plus,
  Search,
  Download,
  Filter,
  BookOpen
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import InscripcionStatsBanner from '../components/InscripcionStatsBanner.vue'
import InscripcionTable from '../components/InscripcionTable.vue'
import InscripcionFormModal from '../components/InscripcionFormModal.vue'
import {
  createEnrollment,
  deactivateEnrollment
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData, exportToCsv } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showModal = ref(false)
const registrationStatusFilter = ref('ALL') // 'ALL' | 'ACTIVA' | 'BAJA'
const registrationCourseFilter = ref('ALL')

const registrationStats = computed(() => {
  const total = data.inscripciones?.length || 0
  const active = (data.inscripciones || []).filter(i => i.estado === 'ACTIVA' || !i.estado).length
  const bajas = (data.inscripciones || []).filter(i => i.estado === 'BAJA').length
  const uniqueStudents = new Set((data.inscripciones || []).map(i => i.alumnoId)).size
  return { total, active, bajas, uniqueStudents }
})

const filteredInscripciones = computed(() => {
  const q = search.value.trim().toLowerCase()
  const statusF = registrationStatusFilter.value
  const courseF = registrationCourseFilter.value

  let list = (data.inscripciones || []).map(i => {
    const al = (data.alumnos || []).find(a => a.id === i.alumnoId)
    const grp = (data.grupos || []).find(g => g.id === i.grupoId)
    const ofr = grp ? (data.ofertas || []).find(o => o.id === grp.ofertaId) : null
    const crs = ofr ? (data.cursos || []).find(c => c.id === ofr.cursoId) : null

    return {
      ...i,
      alumno: i.alumno || (al ? `${al.nombre} ${al.apellidoPaterno} ${al.apellidoMaterno || ''}`.trim() : 'Alumno'),
      matricula: i.matricula || al?.matricula || '',
      grupo: i.grupo || grp?.nombreGrupo || 'Grupo Cultural',
      taller: crs?.nombre || grp?.curso || '',
      cursoId: crs?.id || ofr?.cursoId || '',
      estado: i.estado || 'ACTIVA',
      fechaInscripcion: i.fechaInscripcion ? i.fechaInscripcion.slice(0, 10) : ''
    }
  })

  if (statusF !== 'ALL') {
    list = list.filter(i => (statusF === 'ACTIVA' ? (i.estado === 'ACTIVA' || !i.estado) : i.estado === statusF))
  }
  if (courseF !== 'ALL') {
    list = list.filter(i => String(i.cursoId) === String(courseF))
  }

  if (q) {
    list = list.filter(i =>
      i.alumno.toLowerCase().includes(q) ||
      i.matricula.toLowerCase().includes(q) ||
      i.grupo.toLowerCase().includes(q) ||
      (i.taller && i.taller.toLowerCase().includes(q))
    )
  }

  return list
})

async function handleCreateEnrollment(payload) {
  isSaving.value = true
  try {
    await createEnrollment(payload)
    showToast('Inscripción registrada correctamente.')
    showModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al registrar inscripción', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleDeactivateEnrollment(id) {
  if (!confirm('¿Estás seguro de dar de baja esta inscripción?')) return
  try {
    await deactivateEnrollment(id)
    showToast('Inscripción dada de baja correctamente.')
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al dar de baja la inscripción', 'error')
  }
}

function handleExport() {
  const rows = filteredInscripciones.value.map(i => ({
    ID: i.id,
    Alumno: i.alumno,
    Matricula: i.matricula,
    Grupo: i.grupo,
    Taller: i.taller || '',
    Fecha: i.fechaInscripcion || '',
    Estado: i.estado
  }))
  exportToCsv('inscripciones', rows)
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <ClipboardList :size="28" />
        </div>
        <div>
          <h1>Inscripciones</h1>
          <p>Control de matrículas en grupos</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="showModal = true">
          <Plus :size="18" />
          Nueva Inscripción
        </button>
      </div>
    </div>

    <!-- Stats Banner -->
    <InscripcionStatsBanner :stats="registrationStats" />

    <!-- Filters & Actions Toolbar -->
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
            <Filter :size="15" />
            <select v-model="registrationStatusFilter">
              <option value="ALL">Todas las inscripciones</option>
              <option value="ACTIVA">Solo Activas</option>
              <option value="BAJA">Solo Bajas</option>
            </select>
          </div>

          <div class="filter-pill-select">
            <BookOpen :size="15" />
            <select v-model="registrationCourseFilter">
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

      <!-- Inscripcion Table Component -->
      <InscripcionTable
        :inscripciones="filteredInscripciones"
        @deactivate="handleDeactivateEnrollment"
      />
    </div>

    <!-- Form Modal Component -->
    <InscripcionFormModal
      :show="showModal"
      :is-saving="isSaving"
      :alumnos="data.alumnos || []"
      :grupos="data.grupos || []"
      :ofertas="data.ofertas || []"
      :cursos="data.cursos || []"
      :categorias="data.categorias || []"
      :docentes="data.docentes || []"
      :asignaciones-docentes="data.asignacionesDocentes || []"
      :horarios="data.horarios || []"
      :inscripciones="data.inscripciones || []"
      @close="showModal = false"
      @save="handleCreateEnrollment"
    />
  </div>
</template>
