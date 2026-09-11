import { apiRequest } from './apiService'

export function getSuperAdminData() {
  return Promise.all([
    apiRequest('/api/v1/alumnos'),
    apiRequest('/api/v1/docentes'),
    apiRequest('/api/v1/cursos'),
    apiRequest('/api/v1/ofertas'),
    apiRequest('/api/v1/grupos'),
    apiRequest('/api/v1/categorias-edad'),
    apiRequest('/api/v1/inscripciones'),
    apiRequest('/api/v1/pagos'),
    apiRequest('/api/v1/asistencias'),
    apiRequest('/api/v1/auth/usuarios'),
    apiRequest('/api/v1/roles'),
    apiRequest('/api/v1/horarios').catch(() => []),
    apiRequest('/api/v1/asignaciones-docentes').catch(() => [])
  ]).then(([alumnos, docentes, cursos, ofertas, grupos, categorias, inscripciones, pagos, asistencias, usuarios, roles, horarios, asignacionesDocentes]) => ({
    alumnos: alumnos || [],
    docentes: docentes || [],
    cursos: cursos || [],
    ofertas: ofertas || [],
    grupos: grupos || [],
    categorias: categorias || [],
    inscripciones: inscripciones || [],
    pagos: pagos || [],
    asistencias: asistencias || [],
    usuarios: usuarios || [],
    roles: roles || [],
    horarios: horarios || [],
    asignacionesDocentes: asignacionesDocentes || []
  }))
}

// Alumnos
export function createStudent(student) {
  return apiRequest('/api/v1/alumnos', { method: 'POST', body: JSON.stringify(student) })
}

export function updateStudent(id, student) {
  return apiRequest(`/api/v1/alumnos/${id}`, { method: 'PUT', body: JSON.stringify(student) })
}

export function deactivateStudent(id) {
  return apiRequest(`/api/v1/alumnos/${id}`, { method: 'DELETE' })
}

export function regenerateStudentQr(id) {
  return apiRequest(`/api/v1/alumnos/${id}/qr`, { method: 'POST' })
}

export function generateStudentCredential(id) {
  return apiRequest(`/api/v1/alumnos/${id}/credencial`, { method: 'POST' })
}

export function generateStudentUser(id, request) {
  return apiRequest(`/api/v1/alumnos/${id}/usuario`, { method: 'POST', body: JSON.stringify(request) })
}

// Docentes
export function createTeacher(teacher) {
  return apiRequest('/api/v1/docentes', { method: 'POST', body: JSON.stringify(teacher) })
}

export function updateTeacher(id, teacher) {
  return apiRequest(`/api/v1/docentes/${id}`, { method: 'PUT', body: JSON.stringify(teacher) })
}

export function deactivateTeacher(id) {
  return apiRequest(`/api/v1/docentes/${id}`, { method: 'DELETE' })
}

export function assignTeacherToGroup(assignment) {
  return apiRequest('/api/v1/asignaciones-docentes', { method: 'POST', body: JSON.stringify(assignment) })
}

export function getTeacherAssignmentsByGroup(grupoId) {
  return apiRequest(`/api/v1/asignaciones-docentes/grupo/${grupoId}`)
}

export function getAllTeacherAssignments() {
  return apiRequest('/api/v1/asignaciones-docentes')
}

// Cursos, Categorías, Ofertas y Grupos
export function createCourse(course) {
  return apiRequest('/api/v1/cursos', { method: 'POST', body: JSON.stringify(course) })
}

export function updateCourse(id, course) {
  return apiRequest(`/api/v1/cursos/${id}`, { method: 'PUT', body: JSON.stringify(course) })
}

export function createAgeCategory(category) {
  return apiRequest('/api/v1/categorias-edad', { method: 'POST', body: JSON.stringify(category) })
}

export function updateAgeCategory(id, category) {
  return apiRequest(`/api/v1/categorias-edad/${id}`, { method: 'PUT', body: JSON.stringify(category) })
}

export function createOffer(offer) {
  return apiRequest('/api/v1/ofertas', { method: 'POST', body: JSON.stringify(offer) })
}

export function updateOffer(id, offer) {
  return apiRequest(`/api/v1/ofertas/${id}`, { method: 'PUT', body: JSON.stringify(offer) })
}

export function createGroup(group) {
  return apiRequest('/api/v1/grupos', { method: 'POST', body: JSON.stringify(group) })
}

export function updateGroup(id, group) {
  return apiRequest(`/api/v1/grupos/${id}`, { method: 'PUT', body: JSON.stringify(group) })
}

// Horarios
export function createSchedule(schedule) {
  return apiRequest('/api/v1/horarios', { method: 'POST', body: JSON.stringify(schedule) })
}

export function updateSchedule(id, schedule) {
  return apiRequest(`/api/v1/horarios/${id}`, { method: 'PUT', body: JSON.stringify(schedule) })
}

export function getAllSchedules() {
  return apiRequest('/api/v1/horarios')
}

export function getGroupSchedules(grupoId) {
  return apiRequest(`/api/v1/grupos/${grupoId}/horarios`)
}

// Inscripciones
export function createEnrollment(enrollment) {
  return apiRequest('/api/v1/inscripciones', { method: 'POST', body: JSON.stringify(enrollment) })
}

// Pagos
export function createPayment(payment) {
  return apiRequest('/api/v1/pagos', { method: 'POST', body: JSON.stringify(payment) })
}

// Usuarios
export function createUser(user) {
  return apiRequest('/api/v1/auth/usuarios', { method: 'POST', body: JSON.stringify(user) })
}

export function deactivateUser(id) {
  return apiRequest(`/api/v1/auth/usuarios/${id}`, { method: 'DELETE' })
}

export function resetUserPassword(id, password) {
  return apiRequest(`/api/v1/auth/usuarios/${id}/password`, {
    method: 'PUT',
    body: JSON.stringify({ passwordNueva: password }),
  })
}

// Roles
export function createRole(role) {
  return apiRequest('/api/v1/roles', { method: 'POST', body: JSON.stringify(role) })
}

export function updateRole(id, role) {
  return apiRequest(`/api/v1/roles/${id}`, { method: 'PUT', body: JSON.stringify(role) })
}

export function deleteRole(id) {
  return apiRequest(`/api/v1/roles/${id}`, { method: 'DELETE' })
}

// Asistencias
export function registerAttendance(attendance) {
  return apiRequest('/api/v1/asistencias/manual', { method: 'POST', body: JSON.stringify(attendance) })
}

export function registerQrAttendance(payload) {
  return apiRequest('/api/v1/asistencias/qr', { method: 'POST', body: JSON.stringify(payload) })
}

export function generateAbsences(date) {
  const query = date ? `?fecha=${date}` : ''
  return apiRequest(`/api/v1/asistencias/faltas${query}`, { method: 'POST' })
}

// Reportes
export function getAttendanceReport(from, to) {
  const params = new URLSearchParams()
  if (from) params.set('desde', from)
  if (to) params.set('hasta', to)
  return apiRequest(`/api/v1/reportes/asistencias/resumen?${params.toString()}`)
}
