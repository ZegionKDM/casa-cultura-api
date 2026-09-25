<script setup>
import {
  ClipboardList,
  CheckCircle2,
  AlertCircle,
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
  <div class="registrations-stats-banner">
    <div class="reg-stat-card">
      <div class="stat-icon-wrapper total">
        <ClipboardList :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.total }}</span>
        <span class="stat-label">Inscripciones Totales</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper active">
        <CheckCircle2 :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.active }}</span>
        <span class="stat-label">Inscripciones Activas</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper bajas">
        <AlertCircle :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.bajas }}</span>
        <span class="stat-label">Alumnos Dados de Baja</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper students">
        <GraduationCap :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.uniqueStudents }}</span>
        <span class="stat-label">Alumnos con Matrícula</span>
      </div>
    </div>
  </div>
</template>
