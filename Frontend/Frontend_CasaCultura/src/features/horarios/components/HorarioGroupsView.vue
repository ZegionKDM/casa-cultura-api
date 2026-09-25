<script setup>
import {
  BookOpen,
  Tag,
  UserCheck,
  GraduationCap,
  Plus,
  Clock,
  Trash2,
  AlertCircle,
  Layers
} from 'lucide-vue-next'

defineProps({
  schedulesByGroup: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['add-schedule-group', 'delete-schedule'])
</script>

<template>
  <div class="groups-schedules-grid">
    <div
      v-for="grp in schedulesByGroup"
      :key="grp.id"
      class="group-schedule-card"
    >
      <div class="group-schedule-header">
        <div class="group-header-left">
          <div class="group-course-badge">
            <BookOpen :size="13" />
            <span>{{ grp.cursoNombre }}</span>
          </div>
          <h4>{{ grp.nombreGrupo }}</h4>
          <div class="group-meta-chips">
            <span class="meta-chip category">
              <Tag :size="11" />
              {{ grp.categoriaNombre }}
            </span>
            <span class="meta-chip teacher">
              <UserCheck :size="11" />
              {{ grp.docenteName }}
            </span>
            <span class="meta-chip students">
              <GraduationCap :size="11" />
              {{ grp.totalInscritos }} alumnos
            </span>
          </div>
        </div>

        <div class="group-header-right">
          <button
            type="button"
            class="add-schedule-to-group-btn"
            @click="emit('add-schedule-group', grp.id)"
          >
            <Plus :size="14" />
            Agregar Horario
          </button>
        </div>
      </div>

      <!-- List of scheduled days for this group -->
      <div class="group-schedules-body">
        <div class="schedule-slots-title">
          <span>Días y horarios asignados:</span>
          <strong v-if="grp.horarios.length">
            {{ grp.totalSesiones }} {{ grp.totalSesiones === 1 ? 'sesión semanal' : 'sesiones semanales' }} ({{ grp.totalHours }} hrs/sem)
          </strong>
        </div>

        <div v-if="grp.horarios.length" class="schedule-slots-flex">
          <div
            v-for="sched in grp.horarios"
            :key="sched.id"
            class="schedule-slot-pill"
          >
            <div class="slot-day-badge">
              {{ sched.diaTexto }}
            </div>
            <div class="slot-time-range">
              <Clock :size="13" />
              <span>{{ sched.rangoHorario }}</span>
            </div>
            <button
              type="button"
              class="slot-remove-btn"
              title="Eliminar este horario"
              @click="emit('delete-schedule', sched.id, `${grp.nombreGrupo} - ${sched.diaTexto} ${sched.rangoHorario}`)"
            >
              <Trash2 :size="12" />
            </button>
          </div>
        </div>

        <div v-else class="group-no-schedules-alert">
          <AlertCircle :size="16" />
          <span>Este grupo aún no tiene horarios asignados en la semana.</span>
          <button
            type="button"
            class="assign-now-link"
            @click="emit('add-schedule-group', grp.id)"
          >
            Programar ahora
          </button>
        </div>
      </div>
    </div>

    <!-- Empty groups state -->
    <div v-if="schedulesByGroup.length === 0" class="empty-state-large">
      <Layers :size="48" />
      <h3>No se encontraron grupos</h3>
      <p>Crea grupos en el módulo de Talleres para poder programarles horarios semanales.</p>
    </div>
  </div>
</template>
