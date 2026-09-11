import { apiRequest } from './apiService'

export function getStudentDashboard() {
  return apiRequest('/api/v1/portal/alumno')
}
