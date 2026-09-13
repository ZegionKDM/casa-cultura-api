import { apiRequest } from './apiService'

export function getTeacherDashboard() {
  return apiRequest('/api/v1/portal/docente/dashboard')
}

export function getTeacherProfile() {
  return apiRequest('/api/v1/portal/docente/perfil')
}

export function getTeacherGroups() {
  return apiRequest('/api/v1/portal/docente/grupos')
}

export function getTeacherGroupStudents(grupoId) {
  return apiRequest(`/api/v1/portal/docente/grupos/${grupoId}/alumnos`)
}

export function getAllTeacherStudents() {
  return apiRequest('/api/v1/portal/docente/alumnos')
}

export function getTeacherGroupAttendance(grupoId, fecha = '') {
  const query = fecha ? `?fecha=${fecha}` : ''
  return apiRequest(`/api/v1/portal/docente/grupos/${grupoId}/asistencias${query}`)
}

export function saveBatchAttendance(grupoId, payload) {
  return apiRequest(`/api/v1/portal/docente/grupos/${grupoId}/asistencias/batch`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function saveManualAttendance(payload) {
  return apiRequest('/api/v1/asistencias/manual', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function scanQrAttendance(payload) {
  return apiRequest('/api/v1/asistencias/qr', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function getTeacherSchedules() {
  return apiRequest('/api/v1/portal/docente/horarios')
}
