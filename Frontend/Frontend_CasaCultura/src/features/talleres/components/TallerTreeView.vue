<script setup>
import { ref, watch } from 'vue'
import {
  BookOpen,
  Calendar,
  Plus,
  Pencil,
  ChevronDown,
  ChevronUp,
  Tag,
  Clock,
  GraduationCap,
  AlertCircle,
  FolderPlus
} from 'lucide-vue-next'

const props = defineProps({
  courses: {
    type: Array,
    required: true
  }
})

const emit = defineEmits([
  'add-group',
  'edit-course',
  'add-schedule',
  'assign-teacher'
])

const expandedWorkshops = ref(new Set())

watch(
  () => props.courses,
  (newCourses) => {
    if (newCourses && newCourses.length > 0 && expandedWorkshops.value.size === 0) {
      newCourses.forEach(c => expandedWorkshops.value.add(c.id))
    }
  },
  { immediate: true }
)

function toggleWorkshopExpand(courseId) {
  if (expandedWorkshops.value.has(courseId)) {
    expandedWorkshops.value.delete(courseId)
  } else {
    expandedWorkshops.value.add(courseId)
  }
}

function expandAll() {
  props.courses.forEach(c => expandedWorkshops.value.add(c.id))
}

function collapseAll() {
  expandedWorkshops.value.clear()
}

defineExpose({
  expandAll,
  collapseAll
})

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
</script>

<template>
  <div v-if="courses.length > 0" class="workshops-tree-container">
    <div
      v-for="course in courses"
      :key="course.id"
      class="course-card-hierarchy"
      :class="{ 'is-expanded': expandedWorkshops.has(course.id) }"
    >
      <!-- Course Card Header (Level 1: Course & Current Cycle) -->
      <div
        class="course-header-bar"
        @click="toggleWorkshopExpand(course.id)"
      >
        <div class="course-title-section">
          <div class="course-avatar-box">
            <BookOpen :size="22" />
          </div>
          <div>
            <div class="course-title-row">
              <h2>{{ course.nombre }}</h2>
              <span
                v-if="course.ofertaActual"
                class="cycle-pill"
              >
                <Calendar :size="12" />
                {{ course.ofertaActual.tipo }}
                <small v-if="course.ofertaActual.fechaInicio">({{ course.ofertaActual.fechaInicio }} a {{ course.ofertaActual.fechaFin }})</small>
              </span>
              <span
                v-else
                class="cycle-pill no-cycle"
              >
                Sin ciclo activo
              </span>
            </div>
            <div class="course-meta-tags">
              <span class="meta-tag">
                <strong>{{ course.totalGrupos }}</strong> {{ course.totalGrupos === 1 ? 'Grupo' : 'Grupos' }}
              </span>
              <span class="meta-bullet">•</span>
              <span class="meta-tag">
                <strong>{{ course.totalAlumnos }}</strong> {{ course.totalAlumnos === 1 ? 'Alumno inscrito' : 'Alumnos inscritos' }}
              </span>
            </div>
          </div>
        </div>

        <div
          class="course-actions-section"
          @click.stop
        >
          <button
            class="primary-action-btn"
            title="Agregar un nuevo grupo a este taller"
            @click="emit('add-group', course)"
          >
            <Plus :size="15" />
            + Agregar Grupo
          </button>

          <button
            class="icon-action-btn"
            title="Editar nombre del taller"
            @click="emit('edit-course', course)"
          >
            <Pencil :size="15" />
          </button>

          <button
            class="expand-toggle-btn"
            @click="toggleWorkshopExpand(course.id)"
          >
            <ChevronUp
              v-if="expandedWorkshops.has(course.id)"
              :size="19"
            />
            <ChevronDown
              v-else
              :size="19"
            />
          </button>
        </div>
      </div>

      <!-- Course Expanded Body (Level 2: Groups Grid) -->
      <div
        v-if="expandedWorkshops.has(course.id)"
        class="course-groups-body"
      >
        <div
          v-if="course.grupos.length > 0"
          class="groups-grid"
        >
          <div
            v-for="group in course.grupos"
            :key="group.id"
            class="group-card-item"
          >
            <!-- Group Header: Title & Category -->
            <div class="group-card-header">
              <div>
                <div class="group-title-row">
                  <h4>{{ group.nombreGrupo }}</h4>
                  <span class="category-badge">
                    <Tag :size="11" />
                    {{ group.categoriaNombre }}
                  </span>
                </div>
                <small class="group-id-sub">Grupo ID: #{{ group.id }}</small>
              </div>
            </div>

            <!-- Teacher Section -->
            <div class="group-section-block">
              <div class="section-label-row">
                <span class="section-label">Instructor / Docente</span>
                <button
                  class="sub-action-link"
                  @click="emit('assign-teacher', group, course.nombre)"
                >
                  {{ group.docenteAsignado ? 'Cambiar' : '+ Asignar' }}
                </button>
              </div>

              <div
                v-if="group.docenteAsignado"
                class="teacher-assigned-badge"
              >
                <div class="teacher-avatar">
                  {{ group.docenteAsignado.nombre?.charAt(0).toUpperCase() }}
                </div>
                <div class="teacher-info">
                  <strong>{{ group.docenteAsignado.nombre }} {{ group.docenteAsignado.apellidoPaterno || '' }}</strong>
                  <small>{{ group.docenteAsignado.especialidad || 'Docente de Taller' }}</small>
                </div>
              </div>
              <div
                v-else
                class="no-teacher-badge"
              >
                <AlertCircle :size="14" />
                <span>Sin docente asignado todavía</span>
              </div>
            </div>

            <!-- Schedules Section -->
            <div class="group-section-block">
              <div class="section-label-row">
                <span class="section-label">Horarios Semanales</span>
                <button
                  class="sub-action-link"
                  @click="emit('add-schedule', group, course.nombre)"
                >
                  + Horario
                </button>
              </div>

              <div
                v-if="group.horariosList && group.horariosList.length > 0"
                class="schedules-pill-list"
              >
                <div
                  v-for="sched in group.horariosList"
                  :key="sched.id"
                  class="schedule-tag"
                >
                  <Clock :size="12" />
                  <span><strong>{{ formatDay(sched.dia) }}:</strong> {{ formatTime(sched.horaInicio) }} - {{ formatTime(sched.horaFin) }}</span>
                </div>
              </div>
              <div
                v-else
                class="no-schedules-box"
              >
                <span>Sin horarios programados</span>
              </div>
            </div>

            <!-- Students & Enrollment Stats -->
            <div class="group-card-footer">
              <div class="students-count-tag">
                <GraduationCap :size="14" />
                <span><strong>{{ group.totalInscritos }}</strong> alumnos inscritos</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty state for this specific course -->
        <div
          v-else
          class="empty-course-groups"
        >
          <FolderPlus :size="36" />
          <h4>No hay grupos creados para {{ course.nombre }}</h4>
          <p>Crea el primer grupo (por ejemplo: "Grupo A - Niños" o "Grupo B - Adultos") para comenzar a inscribir alumnos y programar clases.</p>
          <button
            class="primary-button small"
            @click="emit('add-group', course)"
          >
            <Plus :size="15" />
            Crear Primer Grupo
          </button>
        </div>
      </div>
    </div>
  </div>

  <div
    v-else
    class="no-workshops-found"
  >
    <BookOpen :size="48" />
    <h3>No se encontraron talleres</h3>
    <p>Intenta con otro término de búsqueda o crea tu primer taller cultural.</p>
  </div>
</template>
