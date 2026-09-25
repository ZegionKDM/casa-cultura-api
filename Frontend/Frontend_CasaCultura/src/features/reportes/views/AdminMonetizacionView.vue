<script setup>
import { computed } from 'vue'
import {
  BarChart3,
  CreditCard,
  TrendingUp,
  DollarSign
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'

const { data, showToast } = useAdminData()

const monetizationStats = computed(() => {
  const all = data.pagos || []
  const paidList = all.filter(p => p.estado === 'PAGADO')
  const pendingList = all.filter(p => p.estado === 'PENDIENTE')

  const totalPaidAmt = paidList.reduce((acc, p) => acc + (Number(p.monto) || (p.tipoPago === 'INSCRIPCION' ? 500 : 400)), 0)
  const totalPendingAmt = pendingList.reduce((acc, p) => acc + (Number(p.monto) || 400), 0)

  const inscripcionesAmt = paidList
    .filter(p => p.tipoPago === 'INSCRIPCION')
    .reduce((acc, p) => acc + (Number(p.monto) || 500), 0)

  const mensualidadesAmt = paidList
    .filter(p => p.tipoPago === 'MENSUALIDAD')
    .reduce((acc, p) => acc + (Number(p.monto) || 400), 0)

  const recargosAmt = paidList
    .filter(p => p.tipoPago === 'RECARGO')
    .reduce((acc, p) => acc + (Number(p.monto) || 150), 0)

  const totalExpected = totalPaidAmt + totalPendingAmt
  const compliancePct = totalExpected > 0 ? Math.round((totalPaidAmt / totalExpected) * 100) : 100

  return {
    totalPaidAmt,
    totalPendingAmt,
    pendingCount: pendingList.length,
    compliancePct,
    inscripcionesAmt,
    mensualidadesAmt,
    recargosAmt
  }
})
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <BarChart3 :size="28" />
        </div>
        <div>
          <h1>Monetización</h1>
          <p>Ingresos y finanzas del centro cultural</p>
        </div>
      </div>
    </div>

    <div class="special-panel">
      <h2>Resumen Financiero y Recaudación</h2>
      <p>Estadísticas económicas calculadas en tiempo real a partir del módulo de pagos.</p>

      <div class="special-grid">
        <article class="special-card">
          <DollarSign :size="28" />
          <h3>Ingresos Recaudados</h3>
          <p>${{ monetizationStats.totalPaidAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }} MXN</p>
          <button @click="showToast('Verificando pagos en base de datos...')">
            Ver pagos
          </button>
        </article>

        <article class="special-card">
          <CreditCard :size="28" />
          <h3>Pagos Pendientes</h3>
          <p>${{ monetizationStats.totalPendingAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }} MXN ({{ monetizationStats.pendingCount }} adeudos)</p>
          <button @click="showToast('Revisando adeudos...')">
            Revisar adeudos
          </button>
        </article>

        <article class="special-card">
          <TrendingUp :size="28" />
          <h3>Tasa de Cumplimiento</h3>
          <p>{{ monetizationStats.compliancePct }}% de pagos completados</p>
          <button @click="showToast('Auditando cuotas escolares...')">
            Auditar cuotas
          </button>
        </article>
      </div>

      <!-- DESGLOSE FINANCIERO EN MONETIZACIÓN -->
      <div class="monetization-breakdown-box">
        <h3>Desglose Real por Concepto de Pago</h3>
        <div class="concepts-grid">
          <div class="concept-card">
            <span>Inscripciones</span>
            <strong>${{ monetizationStats.inscripcionesAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }}</strong>
            <small>Cuotas de nuevo ingreso</small>
          </div>
          <div class="concept-card">
            <span>Mensualidades</span>
            <strong>${{ monetizationStats.mensualidadesAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }}</strong>
            <small>Cuotas periódicas de taller</small>
          </div>
          <div class="concept-card">
            <span>Recargos / Trámites</span>
            <strong>${{ monetizationStats.recargosAmt.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }}</strong>
            <small>Extemporáneos y constancias</small>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
