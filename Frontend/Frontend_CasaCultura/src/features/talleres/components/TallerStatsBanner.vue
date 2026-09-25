<script setup>
import {
  BookOpen,
  Layers,
  UserCheck,
  GraduationCap
} from 'lucide-vue-next'

defineProps({
  stats: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="workshop-stats-banner">
    <div class="ws-stat-item">
      <div class="ws-stat-icon course-icon">
        <BookOpen :size="20" />
      </div>
      <div class="ws-stat-info">
        <strong>{{ stats.totalCourses }}</strong>
        <span>Talleres / Cursos</span>
      </div>
    </div>

    <div class="ws-stat-item">
      <div class="ws-stat-icon group-icon">
        <Layers :size="20" />
      </div>
      <div class="ws-stat-info">
        <strong>{{ stats.totalGroups }}</strong>
        <span>Grupos Abiertos</span>
      </div>
    </div>

    <div class="ws-stat-item">
      <div class="ws-stat-icon teacher-icon">
        <UserCheck :size="20" />
      </div>
      <div class="ws-stat-info">
        <strong>{{ stats.assignedTeachersCount }}</strong>
        <span>Docentes con Grupo</span>
      </div>
    </div>

    <div class="ws-stat-item">
      <div class="ws-stat-icon student-icon">
        <GraduationCap :size="20" />
      </div>
      <div class="ws-stat-info">
        <strong>{{ stats.totalStudents }}</strong>
        <span>Inscripciones Activas</span>
      </div>
    </div>
  </div>
</template>
