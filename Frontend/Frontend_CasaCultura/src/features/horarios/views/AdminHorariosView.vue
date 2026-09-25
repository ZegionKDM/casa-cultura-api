<script setup>
import { ref, computed } from 'vue'
import {
  CalendarDays,
  Plus,
  Search,
  Calendar,
  BookOpen,
  Layers,
  List
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import HorarioStatsBanner from '../components/HorarioStatsBanner.vue'
import HorarioCalendarView from '../components/HorarioCalendarView.vue'
import HorarioGroupsView from '../components/HorarioGroupsView.vue'
import HorarioTableView from '../components/HorarioTableView.vue'
import HorarioModal from '../components/HorarioModal.vue'
import {
  createSchedulesBatch,
  deleteSchedule
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showModal = ref(false)
const defaultModalGroupId = ref('')
const defaultModalDay = ref('MONDAY')

const scheduleViewMode = ref('calendar') // 'calendar' | 'groups' | 'list'
const scheduleDayFilter = ref('ALL')
const scheduleCourseFilter = ref('ALL')

const weekDays = [
  { key: 'MONDAY', name: 'Lunes', short: 'LUN' },
  { key: 'TUESDAY', name: 'Martes', short: 'MAR' },
  { key: 'WEDNESDAY', name: 'Miércoles', short: 'MIÉ' },
  { key: 'THURSDAY', name: 'Jueves', short: 'JUE' },
  { key: 'FRIDAY', name: 'Viernes', short: 'VIE' },
  { key: 'SATURDAY', name: 'Sábado', short: 'SÁB' },
  { key: 'SUNDAY', name: 'Domingo', short: 'DOM' }
]

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

const enrichedSchedules = computed(() => {
  const q = search.value.trim().toLowerCase()
  const dayFilter = scheduleDayFilter.value
  const courseFilter = scheduleCourseFilter.value

  return (data.horarios || []).map(h => {
    const grupo = (data.grupos || []).find(g => g.id === h.grupoId) || {}
    const oferta = (data.ofertas || []).find(o => o.id === grupo.ofertaId) || {}
    const curso = (data.cursos || []).find(c => c.id === oferta.cursoId) || {}
    const categoria = (data.categorias || []).find(cat => cat.id === grupo.categoriaId) || {}

    let docenteName = 'Sin docente asignado'
    const asignacion = (data.asignacionesDocentes || []).find(a => a.grupoId === grupo.id)
    if (asignacion) {
      const d = (data.docentes || []).find(doc => doc.id === asignacion.docenteId)
      if (d) docenteName = `${d.nombre} ${d.apellidoPaterno || ''}`
      else if (asignacion.docente) docenteName = asignacion.docente
    }

    let durationText = ''
    let durationMinutes = 0
    if (h.horaInicio && h.horaFin) {
      const [sh, sm] = String(h.horaInicio).split(':').map(Number)
      const [eh, em] = String(h.horaFin).split(':').map(Number)
      const diffMins = (eh * 60 + (em || 0)) - (sh * 60 + (sm || 0))
      if (diffMins > 0) {
        durationMinutes = diffMins
        const hours = Math.floor(diffMins / 60)
        const mins = diffMins % 60
        durationText = hours > 0 ? (mins > 0 ? `${hours}h ${mins}m` : `${hours}h`) : `${mins}m`
      }
    }

    const colors = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4', '#3b82f6', '#14b8a6']
    const colorIndex = (curso.id || h.grupoId || h.id || 0) % colors.length
    const themeColor = colors[colorIndex]

    return {
      ...h,
      grupo,
      nombreGrupo: grupo.nombreGrupo || h.nombreGrupo || 'Grupo Cultural',
      curso,
      cursoId: curso.id,
      nombreCurso: curso.nombre || h.nombre || 'Taller Cultural',
      categoriaNombre: categoria.nombre || 'General',
      docenteName,
      diaTexto: formatDay(h.dia),
      diaKey: String(h.dia).toUpperCase(),
      horaInicioFormateada: formatTime(h.horaInicio),
      horaFinFormateada: formatTime(h.horaFin),
      rangoHorario: `${formatTime(h.horaInicio)} - ${formatTime(h.horaFin)}`,
      durationText,
      durationMinutes,
      themeColor
    }
  }).filter(item => {
    if (dayFilter !== 'ALL' && item.diaKey !== dayFilter) return false
    if (courseFilter !== 'ALL' && String(item.cursoId) !== String(courseFilter)) return false
    if (q) {
      return item.nombreCurso.toLowerCase().includes(q) ||
             item.nombreGrupo.toLowerCase().includes(q) ||
             item.docenteName.toLowerCase().includes(q) ||
             item.diaTexto.toLowerCase().includes(q) ||
             item.rangoHorario.includes(q)
    }
    return true
  }).sort((a, b) => (a.horaInicio || '').localeCompare(b.horaInicio || ''))
})

const scheduleStats = computed(() => {
  const all = data.horarios || []
  const groupIdsWithSched = new Set(all.map(h => h.grupoId))

  let totalMinutes = 0
  const dayCounts = {}
  all.forEach(h => {
    const dKey = String(h.dia).toUpperCase()
    dayCounts[dKey] = (dayCounts[dKey] || 0) + 1
    if (h.horaInicio && h.horaFin) {
      const [sh, sm] = String(h.horaInicio).split(':').map(Number)
      const [eh, em] = String(h.horaFin).split(':').map(Number)
      const diff = (eh * 60 + (em || 0)) - (sh * 60 + (sm || 0))
      if (diff > 0) totalMinutes += diff
    }
  })

  let peakDayKey = ''
  let peakCount = 0
  for (const [day, count] of Object.entries(dayCounts)) {
    if (count > peakCount) {
      peakCount = count
      peakDayKey = day
    }
  }

  const peakDayName = peakDayKey ? `${formatDay(peakDayKey)} (${peakCount} clases)` : 'Sin clases'
  const totalHours = (totalMinutes / 60).toFixed(1).replace('.0', '')

  return {
    totalSessions: all.length,
    groupsWithSchedule: groupIdsWithSched.size,
    totalGroups: data.grupos?.length || 0,
    totalHours,
    peakDay: peakDayName
  }
})

const schedulesByDay = computed(() => {
  const todayDayNumber = new Date().getDay()
  const dayMapJS = { 1: 'MONDAY', 2: 'TUESDAY', 3: 'WEDNESDAY', 4: 'THURSDAY', 5: 'FRIDAY', 6: 'SATURDAY', 0: 'SUNDAY' }
  const todayKey = dayMapJS[todayDayNumber]

  const activeDays = scheduleDayFilter.value === 'ALL'
    ? weekDays
    : weekDays.filter(wd => wd.key === scheduleDayFilter.value)

  return activeDays.map(wd => {
    const sessions = enrichedSchedules.value.filter(s => s.diaKey === wd.key)
    return {
      ...wd,
      isToday: wd.key === todayKey,
      sessions,
      totalSessions: sessions.length
    }
  })
})

const schedulesByGroup = computed(() => {
  const q = search.value.trim().toLowerCase()
  const courseFilter = scheduleCourseFilter.value

  return (data.grupos || []).map(g => {
    const oferta = (data.ofertas || []).find(o => o.id === g.ofertaId) || {}
    const curso = (data.cursos || []).find(c => c.id === oferta.cursoId) || {}
    const categoria = (data.categorias || []).find(cat => cat.id === g.categoriaId) || {}

    const groupSchedules = (data.horarios || []).filter(h => h.grupoId === g.id).map(h => ({
      ...h,
      diaTexto: formatDay(h.dia),
      diaKey: String(h.dia).toUpperCase(),
      horaInicioFormateada: formatTime(h.horaInicio),
      horaFinFormateada: formatTime(h.horaFin),
      rangoHorario: `${formatTime(h.horaInicio)} - ${formatTime(h.horaFin)}`
    })).sort((a, b) => {
      const order = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
      return order.indexOf(a.diaKey) - order.indexOf(b.diaKey)
    })

    let totalMinutes = 0
    groupSchedules.forEach(h => {
      if (h.horaInicio && h.horaFin) {
        const [sh, sm] = String(h.horaInicio).split(':').map(Number)
        const [eh, em] = String(h.horaFin).split(':').map(Number)
        const diff = (eh * 60 + (em || 0)) - (sh * 60 + (sm || 0))
        if (diff > 0) totalMinutes += diff
      }
    })
    const totalHours = (totalMinutes / 60).toFixed(1).replace('.0', '')

    let docenteName = 'Sin docente asignado'
    const asignacion = (data.asignacionesDocentes || []).find(a => a.grupoId === g.id)
    if (asignacion) {
      const d = (data.docentes || []).find(doc => doc.id === asignacion.docenteId)
      if (d) docenteName = `${d.nombre} ${d.apellidoPaterno || ''}`
      else if (asignacion.docente) docenteName = asignacion.docente
    }

    const inscritos = (data.inscripciones || []).filter(i => i.grupoId === g.id).length

    return {
      ...g,
      cursoId: curso.id,
      cursoNombre: curso.nombre || 'Taller Cultural',
      categoriaNombre: categoria.nombre || 'General',
      docenteName,
      totalInscritos: inscritos,
      horarios: groupSchedules,
      totalSesiones: groupSchedules.length,
      totalHours
    }
  }).filter(g => {
    if (courseFilter !== 'ALL' && String(g.cursoId) !== String(courseFilter)) return false
    if (q) {
      return (g.nombreGrupo && g.nombreGrupo.toLowerCase().includes(q)) ||
             (g.cursoNombre && g.cursoNombre.toLowerCase().includes(q)) ||
             (g.docenteName && g.docenteName.toLowerCase().includes(q))
    }
    return true
  })
})

function openCreateModal() {
  defaultModalGroupId.value = data.grupos[0]?.id || ''
  defaultModalDay.value = 'MONDAY'
  showModal.value = true
}

function openScheduleModalForDay(dayKey) {
  defaultModalGroupId.value = data.grupos[0]?.id || ''
  defaultModalDay.value = dayKey
  showModal.value = true
}

function openScheduleModalForGroup(groupId) {
  defaultModalGroupId.value = groupId
  defaultModalDay.value = 'MONDAY'
  showModal.value = true
}

async function handleDeleteSchedule(scheduleId, desc = '') {
  if (!confirm(`¿Estás seguro de eliminar este horario ${desc ? `"${desc}"` : ''}?`)) {
    return
  }
  isSaving.value = true
  try {
    await deleteSchedule(scheduleId)
    showToast('Horario eliminado exitosamente.')
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al eliminar el horario.', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleSaveSchedule({ grupoId, slots }) {
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
    showToast(`¡Se registraron exitosamente ${count} horario(s) para el grupo!`)
    showModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al registrar horarios', 'error')
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
          <CalendarDays :size="28" />
        </div>
        <div>
          <h1>Horarios</h1>
          <p>Programación semanal de talleres</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="openCreateModal">
          <Plus :size="18" />
          Nuevo Horario
        </button>
      </div>
    </div>

    <!-- Quick Stats Banner -->
    <HorarioStatsBanner :stats="scheduleStats" />

    <!-- Controls & Filters Toolbar -->
    <div class="schedules-toolbar">
      <div class="schedules-filters-left">
        <!-- Search box -->
        <div class="search-box">
          <Search :size="18" />
          <input
            v-model="search"
            type="text"
            placeholder="Buscar por taller, grupo, docente o día..."
          />
        </div>

        <!-- Day Filter dropdown -->
        <div class="filter-select-wrapper">
          <Calendar :size="16" class="select-icon" />
          <select v-model="scheduleDayFilter" class="filter-select">
            <option value="ALL">Todos los Días</option>
            <option v-for="wd in weekDays" :key="wd.key" :value="wd.key">
              {{ wd.name }}
            </option>
          </select>
        </div>

        <!-- Workshop Filter dropdown -->
        <div class="filter-select-wrapper">
          <BookOpen :size="16" class="select-icon" />
          <select v-model="scheduleCourseFilter" class="filter-select">
            <option value="ALL">Todos los Talleres</option>
            <option v-for="c in data.cursos" :key="c.id" :value="c.id">
              {{ c.nombre }}
            </option>
          </select>
        </div>
      </div>

      <div class="schedules-controls-right">
        <!-- View Mode Switcher -->
        <div class="view-mode-toggle">
          <button
            type="button"
            class="mode-btn"
            :class="{ active: scheduleViewMode === 'calendar' }"
            title="Vista Parrilla Semanal"
            @click="scheduleViewMode = 'calendar'"
          >
            <CalendarDays :size="15" />
            <span>Parrilla Semanal</span>
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: scheduleViewMode === 'groups' }"
            title="Vista por Grupos y Talleres"
            @click="scheduleViewMode = 'groups'"
          >
            <Layers :size="15" />
            <span>Por Grupos</span>
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: scheduleViewMode === 'list' }"
            title="Vista Tabla Detallada"
            @click="scheduleViewMode = 'list'"
          >
            <List :size="15" />
            <span>Tabla</span>
          </button>
        </div>

        <!-- Primary Action -->
        <button type="button" class="primary-button" @click="openCreateModal">
          <Plus :size="17" />
          Programar Horario
        </button>
      </div>
    </div>

    <!-- 1. VISTA PARRILLA SEMANAL -->
    <HorarioCalendarView
      v-if="scheduleViewMode === 'calendar'"
      :schedules-by-day="schedulesByDay"
      @add-day="openScheduleModalForDay"
      @delete-schedule="handleDeleteSchedule"
    />

    <!-- 2. VISTA POR GRUPOS Y TALLERES -->
    <HorarioGroupsView
      v-else-if="scheduleViewMode === 'groups'"
      :schedules-by-group="schedulesByGroup"
      @add-schedule-group="openScheduleModalForGroup"
      @delete-schedule="handleDeleteSchedule"
    />

    <!-- 3. VISTA TABLA DETALLADA -->
    <HorarioTableView
      v-else-if="scheduleViewMode === 'list'"
      :schedules="enrichedSchedules"
      :search-query="search"
      @delete-schedule="handleDeleteSchedule"
      @create-schedule="openCreateModal"
    />

    <!-- Modal Batch Creator Component -->
    <HorarioModal
      :show="showModal"
      :is-saving="isSaving"
      :grupos="data.grupos || []"
      :default-group-id="defaultModalGroupId"
      :default-day="defaultModalDay"
      @close="showModal = false"
      @save="handleSaveSchedule"
    />
  </div>
</template>
