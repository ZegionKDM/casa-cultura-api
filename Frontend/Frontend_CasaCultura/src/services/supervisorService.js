import { apiRequest } from './apiService'

export function getSupervisorData() {
  return Promise.all([
    apiRequest('/api/v1/alumnos'),
    apiRequest('/api/v1/grupos'),
    apiRequest('/api/v1/inscripciones'),
  ]).then(([alumnos, grupos, inscripciones]) => ({
    alumnos,
    grupos,
    inscripciones,
  }))
}

export function createStudent(student) {
  return apiRequest('/api/v1/alumnos', {
    method: 'POST',
    body: JSON.stringify(student),
  })
}

export function updateStudent(id, student) {
  return apiRequest(`/api/v1/alumnos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(student),
  })
}

export function enrollStudent(alumnoId, grupoId) {
  return apiRequest('/api/v1/inscripciones', {
    method: 'POST',
    body: JSON.stringify({ alumnoId, grupoId }),
  })
}

export function generateStudentUser(id, password) {
  return apiRequest(`/api/v1/alumnos/${id}/usuario`, {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}
