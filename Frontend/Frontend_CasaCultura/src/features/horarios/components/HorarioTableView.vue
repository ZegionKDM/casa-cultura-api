<script setup>
import {
  Clock,
  Trash2,
  CalendarDays,
  Plus
} from 'lucide-vue-next'

defineProps({
  schedules: {
    type: Array,
    required: true
  },
  searchQuery: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['delete-schedule', 'create-schedule'])
</script>

<template>
  <div class="table-panel">
    <table v-if="schedules.length">
      <thead>
        <tr>
          <th>Taller Cultural</th>
          <th>Grupo</th>
          <th>Día</th>
          <th>Horario</th>
          <th>Duración</th>
          <th>Docente</th>
          <th style="text-align: right; padding-right: 20px;">Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in schedules" :key="item.id">
          <td>
            <div class="course-cell">
              <div class="course-dot" :style="{ backgroundColor: item.themeColor }"></div>
              <strong>{{ item.nombreCurso }}</strong>
            </div>
          </td>
          <td>
            <span class="group-badge-cell">{{ item.nombreGrupo }}</span>
          </td>
          <td>
            <span class="day-tag-badge" :class="item.diaKey.toLowerCase()">
              {{ item.diaTexto }}
            </span>
          </td>
          <td>
            <div class="time-cell">
              <Clock :size="13" />
              <span>{{ item.rangoHorario }}</span>
            </div>
          </td>
          <td>
            <span class="duration-cell">{{ item.durationText || '-' }}</span>
          </td>
          <td>{{ item.docenteName }}</td>
          <td style="text-align: right; padding-right: 20px;">
            <button
              type="button"
              class="action-button-danger"
              title="Eliminar horario"
              @click="emit('delete-schedule', item.id, `${item.nombreCurso} (${item.diaTexto} ${item.rangoHorario})`)"
            >
              <Trash2 :size="15" />
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-else class="empty-state-large">
      <CalendarDays :size="48" />
      <h3>No se encontraron horarios</h3>
      <p v-if="searchQuery">No hay horarios que coincidan con "{{ searchQuery }}".</p>
      <p v-else>Aún no se han programado horarios para los talleres culturales.</p>
      <button type="button" class="primary-button" @click="emit('create-schedule')">
        <Plus :size="16" />
        Programar Primer Horario
      </button>
    </div>
  </div>
</template>
