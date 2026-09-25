<script setup>
import {
  CreditCard,
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
  <div class="registrations-stats-banner payments-banner">
    <div class="reg-stat-card">
      <div class="stat-icon-wrapper total">
        <CreditCard :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">{{ stats.totalCount }}</span>
        <span class="stat-label">Cobros Generados</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper active">
        <CheckCircle2 :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">${{ stats.totalPaidAmt.toLocaleString() }}</span>
        <span class="stat-label">{{ stats.paidCount }} Pagados (Cobrados)</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper retardos">
        <Clock :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">${{ stats.totalPendingAmt.toLocaleString() }}</span>
        <span class="stat-label">{{ stats.pendingCount }} Pendientes</span>
      </div>
    </div>

    <div class="reg-stat-card">
      <div class="stat-icon-wrapper bajas">
        <AlertTriangle :size="20" />
      </div>
      <div class="stat-meta">
        <span class="stat-num">${{ stats.totalOverdueAmt.toLocaleString() }}</span>
        <span class="stat-label">{{ stats.overdueCount }} En Mora / Vencidos</span>
      </div>
    </div>
  </div>
</template>
