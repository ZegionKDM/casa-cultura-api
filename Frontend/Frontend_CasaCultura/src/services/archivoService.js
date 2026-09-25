import { apiUpload } from './apiService'

/**
 * Sube una fotografía de alumno antes de guardarlo o durante su registro.
 * @param {File|Blob} file Archivo o Blob de imagen
 * @returns {Promise<{ url: string }>}
 */
export async function uploadStudentPhoto(file) {
  const formData = new FormData()
  // Si es un blob sin nombre, asignarle uno con extensión jpg
  const fileName = file.name || `captura_${Date.now()}.jpg`
  formData.append('archivo', file, fileName)

  return apiUpload('/api/v1/archivos/fotos/alumno', formData)
}

/**
 * Actualiza la fotografía de un alumno ya existente.
 * @param {number|string} studentId ID del alumno
 * @param {File|Blob} file Archivo o Blob de imagen
 * @returns {Promise<{ url: string }>}
 */
export async function updateStudentPhoto(studentId, file) {
  const formData = new FormData()
  const fileName = file.name || `captura_${Date.now()}.jpg`
  formData.append('archivo', file, fileName)

  return apiUpload(`/api/v1/alumnos/${studentId}/foto`, formData)
}

/**
 * Sube una fotografía de docente antes de guardarlo o durante su registro.
 * @param {File|Blob} file Archivo o Blob de imagen
 * @returns {Promise<{ url: string }>}
 */
export async function uploadTeacherPhoto(file) {
  const formData = new FormData()
  const fileName = file.name || `captura_docente_${Date.now()}.jpg`
  formData.append('archivo', file, fileName)

  return apiUpload('/api/v1/archivos/fotos/docente', formData)
}
