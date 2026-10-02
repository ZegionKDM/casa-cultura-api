<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  BarChart3,
  CreditCard,
  TrendingUp,
  DollarSign,
  AlertTriangle,
  ArrowRight,
  PieChart,
  Layers,
  Sparkles
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'

const router = useRouter()
const { data, showToast } = useAdminData()

function getPaymentAmount(p) {
  if (p.monto && Number(p.monto) > 0) return Number(p.monto)
  if (p.tipoPago === 'INSCRIPCION') return 500
  if (p.tipoPago === 'MENSUALIDAD') return 400
  if (p.tipoPago === 'RECARGO') return 150
  return 400
}

const monetizationStats = computed(() => {
  const all = data.pagos || []
  const paidList = all.filter(p => p.estado === 'PAGADO')
  const pendingList = all.filter(p => p.estado === 'PENDIENTE')
  const overdueList = all.filter(p => p.estado === 'VENCIDO')

  const totalPaidAmt = paidList.reduce((acc, p) => acc + getPaymentAmount(p), 0)
  const totalPendingAmt = pendingList.reduce((acc, p) => acc + getPaymentAmount(p), 0)
  const totalOverdueAmt = overdueList.reduce((acc, p) => acc + getPaymentAmount(p), 0)

  const inscripcionesAmt = paidList
    .filter(p => p.tipoPago === 'INSCRIPCION')
    .reduce((acc, p) => acc + getPaymentAmount(p), 0)

  const mensualidadesAmt = paidList
    .filter(p => p.tipoPago === 'MENSUALIDAD')
    .reduce((acc, p) => acc + getPaymentAmount(p), 0)

  const recargosAmt = paidList
    .filter(p => p.tipoPago === 'RECARGO')
    .reduce((acc, p) => acc + getPaymentAmount(p), 0)

  const totalProjected = totalPaidAmt + totalPendingAmt + totalOverdueAmt
  const compliancePct = totalProjected > 0 ? Math.round((totalPaidAmt / totalProjected) * 100) : 100

  // Concept percentages
  const inscripcionesPct = totalPaidAmt > 0 ? Math.round((inscripcionesAmt / totalPaidAmt) * 100) : 0
  const mensualidadesPct = totalPaidAmt > 0 ? Math.round((mensualidadesAmt / totalPaidAmt) * 100) : 0
  const recargosPct = totalPaidAmt > 0 ? Math.max(0, 100 - inscripcionesPct - mensualidadesPct) : 0

  return {
    totalCount: all.length,
    paidCount: paidList.length,
    pendingCount: pendingList.length,
    overdueCount: overdueList.length,
    totalPaidAmt,
    totalPendingAmt,
    totalOverdueAmt,
    totalProjected,
    compliancePct,
    inscripcionesAmt,
    mensualidadesAmt,
    recargosAmt,
    inscripcionesPct,
    mensualidadesPct,
    recargosPct
  }
})

// Revenue by Workshop
const revenueByWorkshop = computed(() => {
  if (!data.cursos || data.cursos.length === 0) return []

  const list = data.cursos.map(c => {
    const ofertaIds = (data.ofertas || []).filter(o => o.cursoId === c.id).map(o => o.id)
    const groupIds = (data.grupos || []).filter(g => (g.ofertaId && ofertaIds.includes(g.ofertaId)) || g.cursoId === c.id || g.nombreCurso === c.nombre).map(g => g.id)
    const enrollmentIds = (data.inscripciones || []).filter(i => groupIds.includes(i.grupoId)).map(i => i.id)

    const paidInWorkshop = (data.pagos || []).filter(p => enrollmentIds.includes(p.inscripcionId) && p.estado === 'PAGADO')
    const totalWorkshopAmt = paidInWorkshop.reduce((acc, p) => acc + getPaymentAmount(p), 0)

    return {
      id: c.id,
      nombre: c.nombre,
      enrollmentCount: enrollmentIds.length,
      revenue: totalWorkshopAmt,
      paymentsCount: paidInWorkshop.length
    }
  })

  return list.sort((a, b) => b.revenue - a.revenue)
})

function goToPayments() {
  router.push('/admin/pagos')
}
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
          <h1>Monetización y Finanzas</h1>
          <p>Métricas de recaudación e ingresos del centro cultural en tiempo real</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="goToPayments">
          <CreditCard :size="16" />
          Ver Módulo de Pagos
        </button>
      </div>
    </div>

    <!-- MAIN DASHBOARD CARDS -->
    <div class="special-panel">
      <h2>Balance Financiero General</h2>
      <p>Cifras auditadas y calculadas en tiempo real a partir de todas las transacciones registradas.</p>

      <div class="special-grid">
        <article class="special-card card-paid">
          <div class="card-icon-wrap green">
            <DollarSign :size="24" />
          </div>
          <h3>Ingresos Cobrados</h3>
          <p class="card-amount text-success">
            ${{ monetizationStats.totalPaidAmt.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
          </p>
          <span class="card-sub">{{ monetizationStats.paidCount }} pagos registrados con éxito</span>
          <button @click="goToPayments">
            Gestionar cobros <ArrowRight :size="14" />
          </button>
        </article>

        <article class="special-card card-pending">
          <div class="card-icon-wrap amber">
            <CreditCard :size="24" />
          </div>
          <h3>Por Cobrar (Pendientes)</h3>
          <p class="card-amount text-amber">
            ${{ monetizationStats.totalPendingAmt.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
          </p>
          <span class="card-sub">{{ monetizationStats.pendingCount }} adeudos vigentes</span>
          <button @click="goToPayments">
            Revisar adeudos <ArrowRight :size="14" />
          </button>
        </article>

        <article class="special-card card-overdue">
          <div class="card-icon-wrap red">
            <AlertTriangle :size="24" />
          </div>
          <h3>Cartera Vencida</h3>
          <p class="card-amount text-danger">
            ${{ monetizationStats.totalOverdueAmt.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
          </p>
          <span class="card-sub">{{ monetizationStats.overdueCount }} mensualidades vencidas</span>
          <button @click="goToPayments">
            Auditar mora <ArrowRight :size="14" />
          </button>
        </article>

        <article class="special-card card-compliance">
          <div class="card-icon-wrap indigo">
            <TrendingUp :size="24" />
          </div>
          <h3>Cumplimiento</h3>
          <p class="card-amount text-indigo">
            {{ monetizationStats.compliancePct }}%
          </p>
          <span class="card-sub">Efectividad de cobranza</span>
          <button @click="goToPayments">
            Historial de cobros <ArrowRight :size="14" />
          </button>
        </article>
      </div>

      <!-- VISUAL BARRA DE EFECTIVIDAD FINANCIERA -->
      <div class="financial-progress-section">
        <div class="progress-header">
          <div>
            <h4>Distribución de Cartera Presupuestaria</h4>
            <span class="progress-sub">Proyección total calculada: <strong>${{ monetizationStats.totalProjected.toLocaleString('es-MX', { minimumFractionDigits: 2 }) }} MXN</strong></span>
          </div>
          <span class="badge-ratio">{{ monetizationStats.compliancePct }}% Recaudado</span>
        </div>

        <div class="multi-progress-bar">
          <div
            class="progress-seg paid"
            :style="{ width: `${monetizationStats.totalProjected ? (monetizationStats.totalPaidAmt / monetizationStats.totalProjected) * 100 : 100}%` }"
            title="Recaudado"
          ></div>
          <div
            class="progress-seg pending"
            :style="{ width: `${monetizationStats.totalProjected ? (monetizationStats.totalPendingAmt / monetizationStats.totalProjected) * 100 : 0}%` }"
            title="Pendiente"
          ></div>
          <div
            class="progress-seg overdue"
            :style="{ width: `${monetizationStats.totalProjected ? (monetizationStats.totalOverdueAmt / monetizationStats.totalProjected) * 100 : 0}%` }"
            title="Vencido"
          ></div>
        </div>

        <div class="progress-legend">
          <div class="legend-item">
            <span class="leg-dot bg-success"></span>
            <span>Recaudado: <strong>${{ monetizationStats.totalPaidAmt.toLocaleString('es-MX') }}</strong></span>
          </div>
          <div class="legend-item">
            <span class="leg-dot bg-amber"></span>
            <span>Pendiente: <strong>${{ monetizationStats.totalPendingAmt.toLocaleString('es-MX') }}</strong></span>
          </div>
          <div class="legend-item">
            <span class="leg-dot bg-danger"></span>
            <span>En Mora: <strong>${{ monetizationStats.totalOverdueAmt.toLocaleString('es-MX') }}</strong></span>
          </div>
        </div>
      </div>

      <!-- DESGLOSE REAL POR CONCEPTO DE PAGO -->
      <div class="monetization-breakdown-box">
        <div class="breakdown-title-row">
          <div>
            <h3>Desglose Real por Concepto de Pago</h3>
            <p>Distribución de ingresos según el tipo de arancel registrado en la institución.</p>
          </div>
        </div>

        <div class="concepts-grid">
          <div class="concept-card concept-inscripcion">
            <div class="concept-badge-row">
              <span class="concept-label">Inscripciones</span>
              <span class="concept-pct">{{ monetizationStats.inscripcionesPct }}%</span>
            </div>
            <strong>${{ monetizationStats.inscripcionesAmt.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</strong>
            <small>Cuotas de nuevo ingreso y reinscripción</small>
            <div class="concept-mini-bar">
              <div class="fill indigo" :style="{ width: `${monetizationStats.inscripcionesPct}%` }"></div>
            </div>
          </div>

          <div class="concept-card concept-mensualidad">
            <div class="concept-badge-row">
              <span class="concept-label">Mensualidades</span>
              <span class="concept-pct">{{ monetizationStats.mensualidadesPct }}%</span>
            </div>
            <strong>${{ monetizationStats.mensualidadesAmt.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</strong>
            <small>Cuotas periódicas de talleres formativos</small>
            <div class="concept-mini-bar">
              <div class="fill green" :style="{ width: `${monetizationStats.mensualidadesPct}%` }"></div>
            </div>
          </div>

          <div class="concept-card concept-recargo">
            <div class="concept-badge-row">
              <span class="concept-label">Recargos y Trámites</span>
              <span class="concept-pct">{{ monetizationStats.recargosPct }}%</span>
            </div>
            <strong>${{ monetizationStats.recargosAmt.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</strong>
            <small>Extemporáneos, credenciales y constancias</small>
            <div class="concept-mini-bar">
              <div class="fill amber" :style="{ width: `${monetizationStats.recargosPct}%` }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- TABLA DE RECAUDACIÓN POR TALLER CULTURAL -->
      <div class="workshop-monetization-section">
        <div class="breakdown-title-row">
          <div>
            <h3>Recaudación por Taller Cultural</h3>
            <p>Ingresos generados por cada disciplina artística ofertada.</p>
          </div>
        </div>

        <div class="table-container">
          <table class="modern-table">
            <thead>
              <tr>
                <th>Taller Cultural</th>
                <th>Alumnos Inscritos</th>
                <th>Pagos Cobrados</th>
                <th style="text-align: right; padding-right: 24px;">Ingreso Recaudado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ws in revenueByWorkshop" :key="ws.id">
                <td>
                  <strong>{{ ws.nombre }}</strong>
                </td>
                <td>
                  <span class="cell-sub">{{ ws.enrollmentCount }} estudiantes</span>
                </td>
                <td>
                  <span class="cell-sub">{{ ws.paymentsCount }} transacciones</span>
                </td>
                <td style="text-align: right; padding-right: 24px;">
                  <strong class="text-success">${{ ws.revenue.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }} MXN</strong>
                </td>
              </tr>
              <tr v-if="revenueByWorkshop.length === 0">
                <td colspan="4" class="empty-table-cell">
                  No hay talleres registrados con cobros en el sistema.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.special-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.special-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.special-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
}

.card-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}

.card-icon-wrap.green { background: #ecfdf5; color: #10b981; }
.card-icon-wrap.amber { background: #fffbeb; color: #f59e0b; }
.card-icon-wrap.red { background: #fef2f2; color: #ef4444; }
.card-icon-wrap.indigo { background: #eef2ff; color: #6366f1; }

.special-card h3 {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.card-amount {
  font-size: 22px;
  font-weight: 800;
  margin: 2px 0;
  line-height: 1.2;
}

.text-success { color: #16a34a !important; }
.text-amber { color: #d97706 !important; }
.text-danger { color: #dc2626 !important; }
.text-indigo { color: #4338ca !important; }

.card-sub {
  font-size: 11.5px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.special-card button {
  margin-top: auto;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.2s ease;
}

.special-card button:hover {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

/* FINANCIAL PROGRESS SECTION */
.financial-progress-section {
  margin-top: 26px;
  padding: 20px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.progress-header h4 {
  margin: 0 0 2px;
  font-size: 15px;
  color: #0f172a;
}

.progress-sub {
  font-size: 12px;
  color: #64748b;
}

.badge-ratio {
  padding: 4px 10px;
  background: #ecfdf5;
  color: #065f46;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 700;
}

.multi-progress-bar {
  height: 14px;
  background: #f1f5f9;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  margin-bottom: 14px;
}

.progress-seg.paid { background: #10b981; }
.progress-seg.pending { background: #f59e0b; }
.progress-seg.overdue { background: #ef4444; }

.progress-legend {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}

.leg-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.bg-success { background: #10b981; }
.bg-amber { background: #f59e0b; }
.bg-danger { background: #ef4444; }

/* BREAKDOWN BOX */
.monetization-breakdown-box {
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid #edf0f4;
}

.breakdown-title-row {
  margin-bottom: 16px;
}

.breakdown-title-row h3 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #0f172a;
}

.breakdown-title-row p {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.concepts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.concept-card {
  padding: 18px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.concept-badge-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.concept-label {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
}

.concept-pct {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 12px;
  background: #f1f5f9;
  color: #334155;
}

.concept-card strong {
  font-size: 20px;
  color: #0f172a;
}

.concept-card small {
  font-size: 11.5px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.concept-mini-bar {
  height: 6px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.concept-mini-bar .fill {
  height: 100%;
  border-radius: 4px;
}

.fill.indigo { background: #6366f1; }
.fill.green { background: #10b981; }
.fill.amber { background: #f59e0b; }

/* WORKSHOP SECTION */
.workshop-monetization-section {
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid #edf0f4;
}
</style>
