import { reactive, ref } from 'vue'
import { getSuperAdminData } from '../../../services/superAdminService.js'

// Centralized reactive state shared across admin features
const data = reactive({
  alumnos: [],
  docentes: [],
  cursos: [],
  ofertas: [],
  grupos: [],
  categorias: [],
  horarios: [],
  asignacionesDocentes: [],
  inscripciones: [],
  pagos: [],
  asistencias: [],
  usuarios: [],
  roles: []
})

const isLoading = ref(false)
const hasLoaded = ref(false)
const toast = reactive({
  show: false,
  message: '',
  type: 'success'
})

let toastTimeout = null

export function useAdminData() {
  function showToast(message, type = 'success') {
    if (toastTimeout) clearTimeout(toastTimeout)
    toast.message = message
    toast.type = type
    toast.show = true
    toastTimeout = setTimeout(() => {
      toast.show = false
    }, 3500)
  }

  async function loadAllData(force = false) {
    if (isLoading.value) return
    if (hasLoaded.value && !force) return

    isLoading.value = true
    try {
      const res = await getSuperAdminData()
      Object.assign(data, res)
      hasLoaded.value = true
    } catch (err) {
      showToast(err.message || 'Error cargando información', 'error')
    } finally {
      isLoading.value = false
    }
  }

  function exportToCsv(filename, rows) {
    if (!rows || rows.length === 0) {
      showToast('No hay datos disponibles para exportar', 'error')
      return
    }

    const keys = Object.keys(rows[0]).filter(k => k !== 'raw')
    const csvContent = [
      keys.join(','),
      ...rows.map(row => keys.map(k => `"${String(row[k] || '').replace(/"/g, '""')}"`).join(','))
    ].join('\n')

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.setAttribute('href', url)
    link.setAttribute('download', `${filename}_${new Date().toISOString().slice(0, 10)}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    showToast('Archivo CSV exportado exitosamente.')
  }

  return {
    data,
    isLoading,
    hasLoaded,
    toast,
    showToast,
    loadAllData,
    exportToCsv
  }
}
