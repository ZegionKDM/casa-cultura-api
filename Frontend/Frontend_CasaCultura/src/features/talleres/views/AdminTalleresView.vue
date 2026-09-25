<script setup>
import { ref, computed } from 'vue'
import {
  BookOpen,
  Tag,
  Plus,
  Search
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import TallerStatsBanner from '../components/TallerStatsBanner.vue'
import TallerTreeView from '../components/TallerTreeView.vue'
import TallerWizardModal from '../components/TallerWizardModal.vue'
import DirectGroupModal from '../components/DirectGroupModal.vue'
import ScheduleGroupModal from '../components/ScheduleGroupModal.vue'
import AssignTeacherGroupModal from '../components/AssignTeacherGroupModal.vue'
import CategoriaModal from '../components/CategoriaModal.vue'
import CursoEditModal from '../components/CursoEditModal.vue'
import {
  createCourse,
  updateCourse,
  createOffer,
  createGroup,
  createSchedule,
  createSchedulesBatch,
  assignTeacherToGroup,
  createAgeCategory
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const treeViewRef = ref(null)

// Modals state
const showWizardModal = ref(false)
const showGroupModal = ref(false)
const selectedCourseForGroup = ref(null)

const showScheduleModal = ref(false)
const selectedGroupForSchedule = ref(null)
const selectedCourseNameForSchedule = ref('')

const showAssignModal = ref(false)
const selectedGroupForAssign = ref(null)
const selectedCourseNameForAssign = ref('')

const showCategoryModal = ref(false)
const showEditCourseModal = ref(false)
const selectedCourseForEdit = ref(null)

// Quick stats for workshops view
const workshopStats = computed(() => {
  const totalCourses = data.cursos?.length || 0
  const totalGroups = data.grupos?.length || 0
  const assignedTeachersCount = new Set((data.asignacionesDocentes || []).map(a => a.docenteId)).size
  const totalStudents = data.inscripciones?.length || 0
  return { totalCourses, totalGroups, assignedTeachersCount, totalStudents }
})

// Hierarchical structure computed
const workshopsWithHierarchy = computed(() => {
  const q = search.value.trim().toLowerCase()

  return (data.cursos || []).map(curso => {
    const ofertas = (data.ofertas || []).filter(o => o.cursoId === curso.id || o.curso === curso.nombre)
    const ofertaIds = new Set(ofertas.map(o => o.id))

    const grupos = (data.grupos || []).filter(g =>
      (g.ofertaId && ofertaIds.has(g.ofertaId)) ||
      g.cursoId === curso.id ||
      g.nombreCurso === curso.nombre ||
      g.curso === curso.nombre
    ).map(grupo => {
      const categoria = (data.categorias || []).find(c => c.id === grupo.categoriaId) || { nombre: grupo.categoria || 'General' }

      const asignacion = (data.asignacionesDocentes || [])
        .filter(a => a.grupoId === grupo.id)
        .sort((a, b) => b.id - a.id)[0]

      let docente = null
      if (asignacion) {
        docente = (data.docentes || []).find(d => d.id === asignacion.docenteId) || {
          id: asignacion.docenteId,
          nombre: asignacion.docente || asignacion.nombreDocente || 'Docente Asignado'
        }
      }

      const horarios = (data.horarios || []).filter(h => h.grupoId === grupo.id)
      const inscritos = (data.inscripciones || []).filter(i => i.grupoId === grupo.id)

      return {
        ...grupo,
        categoriaObj: categoria,
        categoriaNombre: categoria.nombre || 'General',
        docenteAsignado: docente,
        asignacionActual: asignacion,
        horariosList: horarios,
        totalInscritos: inscritos.length,
        inscripcionesList: inscritos
      }
    })

    const totalAlumnos = grupos.reduce((acc, g) => acc + g.totalInscritos, 0)
    const ofertaActual = ofertas[ofertas.length - 1] || null

    return {
      ...curso,
      ofertas,
      ofertaActual,
      grupos,
      totalGrupos: grupos.length,
      totalAlumnos
    }
  }).filter(c => {
    if (!q) return true
    const matchCourse = c.nombre.toLowerCase().includes(q)
    const matchGroup = c.grupos.some(g =>
      g.nombreGrupo?.toLowerCase().includes(q) ||
      g.categoriaNombre?.toLowerCase().includes(q) ||
      (g.docenteAsignado && `${g.docenteAsignado.nombre} ${g.docenteAsignado.apellidoPaterno || ''}`.toLowerCase().includes(q))
    )
    return matchCourse || matchGroup
  })
})

function expandAll() {
  if (treeViewRef.value) treeViewRef.value.expandAll()
}

function collapseAll() {
  if (treeViewRef.value) treeViewRef.value.collapseAll()
}

// Modal open handlers
function openAddGroup(course) {
  selectedCourseForGroup.value = course
  showGroupModal.value = true
}

function openEditCourse(course) {
  selectedCourseForEdit.value = course
  showEditCourseModal.value = true
}

function openAddSchedule(group, courseName) {
  selectedGroupForSchedule.value = group
  selectedCourseNameForSchedule.value = courseName
  showScheduleModal.value = true
}

function openAssignTeacher(group, courseName) {
  selectedGroupForAssign.value = group
  selectedCourseNameForAssign.value = courseName
  showAssignModal.value = true
}

// Form submit handlers
async function handleCreateFullWorkshop(payload) {
  isSaving.value = true
  try {
    const createdCourse = await createCourse({ nombre: payload.nombreCurso.trim() })
    const courseId = createdCourse.id || createdCourse.data?.id

    const createdOffer = await createOffer({
      cursoId: Number(courseId),
      tipo: payload.tipoOferta.trim(),
      fechaInicio: payload.fechaInicio,
      fechaFin: payload.fechaFin
    })
    const offerId = createdOffer.id || createdOffer.data?.id

    if (payload.crearGrupoInicial) {
      const createdGroup = await createGroup({
        ofertaId: Number(offerId),
        categoriaId: Number(payload.categoriaId),
        nombreGrupo: payload.nombreGrupo.trim()
      })
      const groupId = createdGroup.id || createdGroup.data?.id

      if (payload.docenteId) {
        await assignTeacherToGroup({
          docenteId: Number(payload.docenteId),
          grupoId: Number(groupId),
          fechaInicio: payload.fechaInicio,
          fechaFin: payload.fechaFin || null
        })
      }

      if (payload.dia && payload.horaInicio && payload.horaFin) {
        await createSchedule({
          grupoId: Number(groupId),
          dia: payload.dia,
          horaInicio: payload.horaInicio.length === 5 ? `${payload.horaInicio}:00` : payload.horaInicio,
          horaFin: payload.horaFin.length === 5 ? `${payload.horaFin}:00` : payload.horaFin
        })
      }
    }
    showToast('¡Taller cultural y grupo estructurados exitosamente!')
    showWizardModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al estructurar taller', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleCreateDirectGroup(payload) {
  isSaving.value = true
  try {
    let ofertaId = payload.ofertaId
    if (!ofertaId) {
      const newOff = await createOffer({
        cursoId: Number(payload.cursoId),
        tipo: `SEMESTRAL ${new Date().getFullYear()}-1`,
        fechaInicio: new Date().toISOString().slice(0, 10),
        fechaFin: new Date(new Date().setMonth(new Date().getMonth() + 5)).toISOString().slice(0, 10)
      })
      ofertaId = newOff.id || newOff.data?.id
    }

    const createdGroup = await createGroup({
      ofertaId: Number(ofertaId),
      categoriaId: Number(payload.categoriaId),
      nombreGrupo: payload.nombreGrupo.trim()
    })
    const groupId = createdGroup.id || createdGroup.data?.id

    if (payload.docenteId) {
      await assignTeacherToGroup({
        docenteId: Number(payload.docenteId),
        grupoId: Number(groupId),
        fechaInicio: new Date().toISOString().slice(0, 10),
        fechaFin: null
      })
    }

    if (payload.agregarHorarioInicial && payload.dia && payload.horaInicio && payload.horaFin) {
      await createSchedule({
        grupoId: Number(groupId),
        dia: payload.dia,
        horaInicio: payload.horaInicio.length === 5 ? `${payload.horaInicio}:00` : payload.horaInicio,
        horaFin: payload.horaFin.length === 5 ? `${payload.horaFin}:00` : payload.horaFin
      })
    }

    showToast('¡Nuevo grupo agregado con éxito al taller!')
    showGroupModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al agregar grupo', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleSaveScheduleBatch({ grupoId, slots }) {
  isSaving.value = true
  try {
    const slotsPayload = slots.map(s => ({
      dias: s.dias,
      horaInicio: s.horaInicio.length === 5 ? `${s.horaInicio}:00` : s.horaInicio,
      horaFin: s.horaFin.length === 5 ? `${s.horaFin}:00` : s.horaFin
    }))
    const created = await createSchedulesBatch({
      grupoId: Number(grupoId),
      slots: slotsPayload
    })
    const count = Array.isArray(created) ? created.length : 1
    showToast(`¡Se asignaron exitosamente ${count} horario(s) al grupo!`)
    showScheduleModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al programar horarios', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleAssignTeacherToGroup(payload) {
  isSaving.value = true
  try {
    await assignTeacherToGroup(payload)
    showToast('Docente instructor asignado al grupo.')
    showAssignModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al asignar docente', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleCreateAgeCategory(payload) {
  isSaving.value = true
  try {
    await createAgeCategory(payload)
    showToast('Nueva categoría de edad registrada.')
    showCategoryModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al crear categoría', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleUpdateCourseName({ id, nombre }) {
  isSaving.value = true
  try {
    await updateCourse(id, { nombre })
    showToast('Nombre del taller actualizado.')
    showEditCourseModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al actualizar taller', 'error')
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <BookOpen :size="28" />
        </div>
        <div>
          <h1>Talleres</h1>
          <p>Cursos y talleres culturales ofertados</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="secondary-button" @click="showCategoryModal = true">
          <Tag :size="16" />
          + Nueva Categoría de Edad
        </button>

        <button class="primary-button" @click="showWizardModal = true">
          <Plus :size="18" />
          Nuevo Taller Cultural
        </button>
      </div>
    </div>

    <!-- Quick Stats Banner Component -->
    <TallerStatsBanner :stats="workshopStats" />

    <!-- Toolbar with Search and Expand Controls -->
    <div class="toolbar ws-toolbar">
      <div class="search-box ws-search">
        <Search :size="18" />
        <input
          v-model="search"
          type="text"
          placeholder="Buscar taller, grupo, categoría de edad, docente..."
        />
      </div>

      <div class="ws-expand-controls">
        <button class="secondary-button small" @click="expandAll">
          Expandir Todo
        </button>
        <button class="secondary-button small" @click="collapseAll">
          Colapsar Todo
        </button>
      </div>
    </div>

    <!-- Hierarchical Tree View Component -->
    <TallerTreeView
      ref="treeViewRef"
      :courses="workshopsWithHierarchy"
      @add-group="openAddGroup"
      @edit-course="openEditCourse"
      @add-schedule="openAddSchedule"
      @assign-teacher="openAssignTeacher"
    />

    <!-- Wizard Modal Component -->
    <TallerWizardModal
      :show="showWizardModal"
      :is-saving="isSaving"
      :categorias="data.categorias || []"
      :docentes="data.docentes || []"
      @close="showWizardModal = false"
      @save="handleCreateFullWorkshop"
    />

    <!-- Direct Group Modal Component -->
    <DirectGroupModal
      :show="showGroupModal"
      :is-saving="isSaving"
      :course="selectedCourseForGroup"
      :categorias="data.categorias || []"
      :docentes="data.docentes || []"
      @close="showGroupModal = false"
      @save="handleCreateDirectGroup"
    />

    <!-- Schedule Group Modal Component -->
    <ScheduleGroupModal
      :show="showScheduleModal"
      :is-saving="isSaving"
      :group="selectedGroupForSchedule"
      :course-name="selectedCourseNameForSchedule"
      @close="showScheduleModal = false"
      @save="handleSaveScheduleBatch"
    />

    <!-- Assign Teacher Group Modal Component -->
    <AssignTeacherGroupModal
      :show="showAssignModal"
      :is-saving="isSaving"
      :group="selectedGroupForAssign"
      :course-name="selectedCourseNameForAssign"
      :docentes="data.docentes || []"
      @close="showAssignModal = false"
      @save="handleAssignTeacherToGroup"
    />

    <!-- Age Category Modal Component -->
    <CategoriaModal
      :show="showCategoryModal"
      :is-saving="isSaving"
      @close="showCategoryModal = false"
      @save="handleCreateAgeCategory"
    />

    <!-- Course Edit Modal Component -->
    <CursoEditModal
      :show="showEditCourseModal"
      :is-saving="isSaving"
      :course="selectedCourseForEdit"
      @close="showEditCourseModal = false"
      @save="handleUpdateCourseName"
    />
  </div>
</template>
