<script setup>
import {
  CalendarDays,
  Layers,
  Clock,
  Sparkles
} from 'lucide-vue-next'

defineProps({
  stats: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="schedules-stats-banner">
    <div class="sc-stat-item">
      <div class="sc-stat-icon calendar-icon">
        <CalendarDays :size="20" />
      </div>
      <div class="sc-stat-info">
        <strong>{{ stats.totalSessions }}</strong>
        <span>Sesiones Semanales</span>
      </div>
    </div>

    <div class="sc-stat-item">
      <div class="sc-stat-icon group-icon">
        <Layers :size="20" />
      </div>
      <div class="sc-stat-info">
        <strong>{{ stats.groupsWithSchedule }} / {{ stats.totalGroups }}</strong>
        <span>Grupos Programados</span>
      </div>
    </div>

    <div class="sc-stat-item">
      <div class="sc-stat-icon hours-icon">
        <Clock :size="20" />
      </div>
      <div class="sc-stat-info">
        <strong>{{ stats.totalHours }} hrs</strong>
        <span>Carga Horaria Semanal</span>
      </div>
    </div>

    <div class="sc-stat-item">
      <div class="sc-stat-icon peak-icon">
        <Sparkles :size="20" />
      </div>
      <div class="sc-stat-info">
        <strong>{{ stats.peakDay }}</strong>
        <span>Día con Mayor Actividad</span>
      </div>
    </div>
  </div>
</template>
