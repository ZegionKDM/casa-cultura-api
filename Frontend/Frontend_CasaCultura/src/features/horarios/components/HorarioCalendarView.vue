<script setup>
import {
  CalendarDays,
  Plus,
  Clock,
  UserCheck,
  Trash2
} from 'lucide-vue-next'

defineProps({
  schedulesByDay: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['add-day', 'delete-schedule'])
</script>

<template>
  <div class="calendar-timetable-grid">
    <div
      v-for="col in schedulesByDay"
      :key="col.key"
      class="day-column"
      :class="{ 'is-today': col.isToday }"
    >
      <!-- Day Column Header -->
      <div class="day-column-header">
        <div class="day-title-row">
          <span class="day-short">{{ col.short }}</span>
          <span class="day-name">{{ col.name }}</span>
          <span v-if="col.isToday" class="today-badge">HOY</span>
        </div>
        <div class="day-meta-row">
          <span class="sessions-counter">
            {{ col.totalSessions === 1 ? '1 clase' : `${col.totalSessions} clases` }}
          </span>
          <button
            type="button"
            class="quick-add-day-btn"
            title="Programar clase para este día"
            @click="emit('add-day', col.key)"
          >
            <Plus :size="13" />
          </button>
        </div>
      </div>

      <!-- Sessions in this day -->
      <div class="day-sessions-container">
        <div
          v-for="session in col.sessions"
          :key="session.id"
          class="session-card"
          :style="{ '--accent-color': session.themeColor }"
        >
          <div class="session-card-top">
            <div class="time-range-badge">
              <Clock :size="12" />
              <span>{{ session.horaInicioFormateada }} - {{ session.horaFinFormateada }}</span>
            </div>
            <span v-if="session.durationText" class="duration-pill">
              {{ session.durationText }}
            </span>
          </div>

          <div class="session-course-name">
            {{ session.nombreCurso }}
          </div>

          <div class="session-group-name">
            {{ session.nombreGrupo }}
          </div>

          <div class="session-card-bottom">
            <div class="session-teacher" :title="session.docenteName">
              <UserCheck :size="12" />
              <span>{{ session.docenteName }}</span>
            </div>

            <button
              type="button"
              class="session-delete-btn"
              title="Eliminar este horario"
              @click="emit('delete-schedule', session.id, `${session.nombreCurso} - ${session.diaTexto} ${session.rangoHorario}`)"
            >
              <Trash2 :size="13" />
            </button>
          </div>
        </div>

        <!-- Empty Day State -->
        <div v-if="col.sessions.length === 0" class="empty-day-state">
          <div class="empty-day-icon">
            <CalendarDays :size="20" />
          </div>
          <span>Sin clases programadas</span>
          <button
            type="button"
            class="empty-day-add-link"
            @click="emit('add-day', col.key)"
          >
            + Asignar clase
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
