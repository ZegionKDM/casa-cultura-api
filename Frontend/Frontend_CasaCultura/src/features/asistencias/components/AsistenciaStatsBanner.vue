<script setup>
import {
  UserCheck,
  CheckCircle2,
  Clock,
  AlertTriangle
} from 'lucide-vue-next'

defineProps({
  stats: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="registrations-stats-banner attendance-banner">
    <div class="reg-stat-card">
      <div class="stat-icon-wrapper total">
        <UserCheck :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">Registros Totales</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper active">
        <CheckCircle2 :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.presentes }}</span>
        <span class="stat-label">Asistencias ({{ stats.presentesPct }}%)</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper retardos">
        <Clock :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.retardos }}</span>
        <span class="stat-label">Retardos ({{ stats.retardosPct }}%)</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper bajas">
        <AlertTriangle :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.faltas }}</span>
        <span class="stat-label">Faltas ({{ stats.faltasPct }}%)</span>
      </div>
    </div>
  </div>
</template>
