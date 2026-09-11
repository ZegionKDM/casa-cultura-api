const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export function getStoredToken() {
  return localStorage.getItem('casa-cultura-token')
    || sessionStorage.getItem('casa-cultura-token')
}

export function clearStoredSession() {
  localStorage.removeItem('casa-cultura-token')
  localStorage.removeItem('casa-cultura-user')
  localStorage.removeItem('casa-cultura-username')
  sessionStorage.removeItem('casa-cultura-token')
  sessionStorage.removeItem('casa-cultura-user')
}

export async function apiRequest(path, options = {}) {
  const token = getStoredToken()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
    ...options,
  })

  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || 'No fue posible completar la solicitud.')
  }

  return payload?.data ?? payload
}
