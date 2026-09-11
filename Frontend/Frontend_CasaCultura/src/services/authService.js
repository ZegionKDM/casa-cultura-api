const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || 'No fue posible completar la solicitud.')
  }
  return payload?.data ?? payload
}

async function login(credentials) {
  return request('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
}

async function getCurrentUser(token) {
  return request('/api/v1/auth/me', {
    headers: { Authorization: `Bearer ${token}` },
  })
}

export async function changePassword(token, passwordActual, passwordNueva) {
  return request('/api/v1/auth/me/password', {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({ passwordActual, passwordNueva }),
  })
}

export async function loginUser(credentials) {
  const session = await login(credentials)
  return { ...session, user: await getCurrentUser(session.token) }
}
