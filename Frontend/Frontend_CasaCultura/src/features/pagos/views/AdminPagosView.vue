<script setup>
import { ref, computed } from 'vue'
import {
  CreditCard,
  Plus,
  Search,
  Download,
  Filter,
  BookOpen
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'
import PagoStatsBanner from '../components/PagoStatsBanner.vue'
import PagoTable from '../components/PagoTable.vue'
import PagoFormModal from '../components/PagoFormModal.vue'
import {
  createPayment,
  updatePaymentStatus
} from '../../../services/superAdminService.js'

const { data, showToast, loadAllData, exportToCsv } = useAdminData()

const search = ref('')
const isSaving = ref(false)
const showModal = ref(false)

const paymentStatusFilter = ref('ALL') // 'ALL' | 'PAGADO' | 'PENDIENTE' | 'VENCIDO'
const paymentConceptFilter = ref('ALL') // 'ALL' | 'INSCRIPCION' | 'MENSUALIDAD' | 'RECARGO'
const paymentCourseFilter = ref('ALL')

const paymentsStats = computed(() => {
  const all = data.pagos || []
  const totalCount = all.length
  const paidList = all.filter(p => p.estado === 'PAGADO')
  const pendingList = all.filter(p => p.estado === 'PENDIENTE')
  const overdueList = all.filter(p => p.estado === 'VENCIDO')

  const totalPaidAmt = paidList.reduce((acc, p) => acc + (Number(p.monto) || (p.tipoPago === 'INSCRIPCION' ? 500 : 400)), 0)
  const totalPendingAmt = pendingList.reduce((acc, p) => acc + (Number(p.monto) || 400), 0)
  const totalOverdueAmt = overdueList.reduce((acc, p) => acc + (Number(p.monto) || 400), 0)

  return {
    totalCount,
    paidCount: paidList.length,
    totalPaidAmt,
    pendingCount: pendingList.length,
    totalPendingAmt,
    overdueCount: overdueList.length,
    totalOverdueAmt
  }
})

const filteredPagos = computed(() => {
  const q = search.value.trim().toLowerCase()
  const statusF = paymentStatusFilter.value
  const conceptF = paymentConceptFilter.value
  const courseF = paymentCourseFilter.value

  let list = (data.pagos || []).map(p => {
    const insc = (data.inscripciones || []).find(i => i.id === p.inscripcionId)
    const al = insc ? (data.alumnos || []).find(x => x.id === insc.alumnoId) : null
    const grp = insc ? (data.grupos || []).find(g => g.id === insc.grupoId) : null
    const ofr = grp ? (data.ofertas || []).find(o => o.id === grp.ofertaId) : null
    const crs = ofr ? (data.cursos || []).find(c => c.id === ofr.cursoId) : null

    return {
      ...p,
      alumno: p.alumno || (al ? `${al.nombre} ${al.apellidoPaterno} ${al.apellidoMaterno || ''}`.trim() : 'Alumno'),
      matricula: p.matricula || al?.matricula || '',
      grupo: p.grupo || grp?.nombreGrupo || 'Grupo Cultural',
      taller: crs?.nombre || grp?.curso || '',
      cursoId: crs?.id || ofr?.cursoId || '',
      monto: Number(p.monto) || (p.tipoPago === 'INSCRIPCION' ? 500 : 400)
    }
  })

  if (statusF !== 'ALL') {
    list = list.filter(p => p.estado === statusF)
  }
  if (conceptF !== 'ALL') {
    list = list.filter(p => p.tipoPago === conceptF)
  }
  if (courseF !== 'ALL') {
    list = list.filter(p => String(p.cursoId) === String(courseF))
  }

  if (q) {
    list = list.filter(p =>
      p.alumno.toLowerCase().includes(q) ||
      p.matricula.toLowerCase().includes(q) ||
      p.grupo.toLowerCase().includes(q) ||
      (p.taller && p.taller.toLowerCase().includes(q)) ||
      (p.periodo && p.periodo.toLowerCase().includes(q))
    )
  }

  return list
})

async function handleCreatePayment(payload) {
  isSaving.value = true
  try {
    await createPayment(payload)
    showToast('Pago registrado correctamente.')
    showModal.value = false
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al registrar el pago', 'error')
  } finally {
    isSaving.value = false
  }
}

async function handleQuickMarkAsPaid(pagoId) {
  try {
    await updatePaymentStatus(pagoId, 'PAGADO')
    showToast('Pago registrado como PAGADO exitosamente.')
    await loadAllData(true)
  } catch (err) {
    showToast(err.message || 'Error al actualizar estado del pago', 'error')
  }
}

function handleExport() {
  const rows = filteredPagos.value.map(p => ({
    ID: p.id,
    Alumno: p.alumno,
    Matricula: p.matricula,
    Concepto: p.tipoPago,
    Monto: p.monto,
    Periodo: p.periodo || '',
    Estado: p.estado,
    FechaPago: p.fechaPago || ''
  }))
  exportToCsv('pagos', rows)
}
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <CreditCard :size="28" />
        </div>
        <div>
          <h1>Pagos</h1>
          <p>Control de cuotas y mensualidades</p>
        </div>
      </div>

      <div class="header-actions-group">
        <button class="primary-button" @click="showModal = true">
          <Plus :size="18" />
          Registrar Pago
        </button>
      </div>
    </div>

    <!-- Payments Stats Banner -->
    <PagoStatsBanner :stats="paymentsStats" />

    <!-- Controls Bar -->
    <div class="module-content-wrapper">
      <div class="filter-actions-bar">
        <div class="search-input-wrapper">
          <Search class="search-icon" :size="18" />
          <input
            v-model="search"
            type="text"
            placeholder="Buscar por alumno, matrícula o concepto..."
            class="search-input"
          />
        </div>

        <div class="filters-group-right">
          <div class="filter-pill-select">
            <Filter :size="15" />
            <select v-model="paymentConceptFilter">
              <option value="ALL">Todos los Conceptos</option>
              <option value="INSCRIPCION">Inscripciones</option>
              <option value="MENSUALIDAD">Mensualidades</option>
              <option value="RECARGO">Recargos</option>
            </select>
          </div>

          <div class="filter-pill-select">
            <Filter :size="15" />
            <select v-model="paymentStatusFilter">
              <option value="ALL">Todos los Estados</option>
              <option value="PAGADO">Pagado</option>
              <option value="PENDIENTE">Pendiente</option>
              <option value="VENCIDO">Vencido</option>
            </select>
          </div>

          <div class="filter-pill-select">
            <BookOpen :size="15" />
            <select v-model="paymentCourseFilter">
              <option value="ALL">Todos los Talleres</option>
              <option v-for="c in data.cursos" :key="c.id" :value="c.id">
                {{ c.nombre }}
              </option>
            </select>
          </div>

          <button class="export-button" @click="handleExport">
            <Download :size="16" />
            Exportar Lista
          </button>
        </div>
      </div>

      <!-- Pago Table Component -->
      <PagoTable
        :pagos="filteredPagos"
        @mark-as-paid="handleQuickMarkAsPaid"
      />
    </div>

    <!-- Pago Form Modal Component -->
    <PagoFormModal
      :show="showModal"
      :is-saving="isSaving"
      :inscripciones="data.inscripciones || []"
      :alumnos="data.alumnos || []"
      @close="showModal = false"
      @save="handleCreatePayment"
    />
  </div>
</template>
