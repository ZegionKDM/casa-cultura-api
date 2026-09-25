<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { changePassword, loginUser } from '../../../services/authService'
import {
  Lock,
  KeyRound,
  Eye,
  EyeOff,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
  User,
  LogOut,
  HelpCircle,
  Heart
} from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const credentials = reactive({
  nombreUsuario: '',
  password: '',
})

const rememberSession = ref(false)
const showPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const authenticatedUser = ref(null)
const mustChangePassword = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')
const activeToken = ref('')

const savedUsername = localStorage.getItem('casa-cultura-username')

onMounted(() => {
  if (route.query.expired) {
    errorMessage.value = 'Tu sesión anterior había expirado. Por favor, inicia sesión de nuevo.'
  }
  if (savedUsername) {
    credentials.nombreUsuario = savedUsername
    rememberSession.value = true
  }
})

const buttonLabel = computed(() => (isLoading.value ? 'VALIDANDO...' : 'INICIAR SESIÓN'))

// Real-time password criteria
const hasMinLength = computed(() => newPassword.value.length >= 8)
const passwordsMatch = computed(() => Boolean(newPassword.value && newPassword.value === confirmPassword.value))
const passwordsMismatch = computed(() => Boolean(confirmPassword.value.length > 0 && newPassword.value !== confirmPassword.value))
const canSubmitPasswordChange = computed(() => hasMinLength.value && passwordsMatch.value)

const roleDisplay = computed(() => {
  const r = String(authenticatedUser.value?.rol || '').replace('ROLE_', '')
  if (r === 'ALUMNO') return 'Estudiante'
  if (r === 'SUPERVISOR') return 'Supervisor'
  if (r === 'SUPER_ADMIN') return 'Super Administrador'
  if (r === 'DOCENTE') return 'Docente'
  return r || 'Usuario'
})

async function submitLogin() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!credentials.nombreUsuario.trim() || !credentials.password) {
    errorMessage.value = 'Ingresa tu usuario y contraseña.'
    return
  }

  isLoading.value = true

  try {
    const session = await loginUser({
      nombreUsuario: credentials.nombreUsuario.trim(),
      password: credentials.password,
    })

    const storage = rememberSession.value ? localStorage : sessionStorage
    const otherStorage = rememberSession.value ? sessionStorage : localStorage
    storage.setItem('casa-cultura-token', session.token)
    storage.setItem('casa-cultura-user', JSON.stringify(session.user))
    otherStorage.removeItem('casa-cultura-token')
    otherStorage.removeItem('casa-cultura-user')

    if (rememberSession.value) {
      localStorage.setItem('casa-cultura-username', credentials.nombreUsuario.trim())
    } else {
      localStorage.removeItem('casa-cultura-username')
    }

    authenticatedUser.value = session.user
    activeToken.value = session.token
    mustChangePassword.value = Boolean(session.user?.debeCambiarPassword)
    successMessage.value = mustChangePassword.value
      ? ''
      : 'Sesión iniciada correctamente.'
    if (mustChangePassword.value) return
    await navigateToRole(session.user)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

async function submitPasswordChange() {
  errorMessage.value = ''
  successMessage.value = ''
  if (newPassword.value.length < 8) {
    errorMessage.value = 'La nueva contraseña debe tener al menos 8 caracteres.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Las contraseñas no coinciden.'
    return
  }
  isLoading.value = true
  try {
    const storage = rememberSession.value ? localStorage : sessionStorage
    await changePassword(activeToken.value, credentials.password, newPassword.value)
    authenticatedUser.value = { ...authenticatedUser.value, debeCambiarPassword: false }
    storage.setItem('casa-cultura-user', JSON.stringify(authenticatedUser.value))
    await navigateToRole(authenticatedUser.value)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

async function navigateToRole(user) {
  const role = String(user?.rol || '').replace('ROLE_', '')
  if (role === 'ALUMNO') {
    await router.push('/alumno')
  } else if (role === 'SUPERVISOR') {
    await router.push('/supervisor')
  } else if (role === 'SUPER_ADMIN') {
    await router.push('/admin/dashboard')
  } else if (role === 'DOCENTE') {
    await router.push('/docente')
  }
}

function clearSession() {
  authenticatedUser.value = null
  successMessage.value = ''
  mustChangePassword.value = false
  newPassword.value = ''
  confirmPassword.value = ''
  credentials.password = ''
  activeToken.value = ''
  localStorage.removeItem('casa-cultura-token')
  localStorage.removeItem('casa-cultura-user')
  sessionStorage.removeItem('casa-cultura-token')
  sessionStorage.removeItem('casa-cultura-user')
}
</script>

<template>
  <main class="login-page">
    <div class="login-shell">
      <!-- Left Column: Branding + Cards -->
      <section class="auth-column" aria-label="Acceso al Sistema">
        <header class="brand-header">
          <img
            class="brand-logo"
            src="../../../assets/casacul.png"
            alt="Casa de la Cultura de Tlaxiaco"
          />
        </header>

        <!-- CARD 1: Standard Login Form -->
        <div v-if="!authenticatedUser" class="auth-card">
          <div class="welcome-icon" aria-hidden="true">
            <User :size="24" />
          </div>

          <h1>Bienvenido</h1>
          <p class="subtitle">Inicia sesión para acceder a tu espacio en la Casa de la Cultura.</p>

          <form @submit.prevent="submitLogin" novalidate class="auth-form">
            <div class="field-group">
              <label for="username">Usuario</label>
              <div class="input-wrapper">
                <User class="input-icon" :size="16" />
                <input
                  id="username"
                  v-model="credentials.nombreUsuario"
                  type="text"
                  autocomplete="username"
                  placeholder="Ingresa tu usuario"
                />
              </div>
            </div>

            <div class="field-group">
              <label for="password">Contraseña</label>
              <div class="input-wrapper">
                <Lock class="input-icon" :size="16" />
                <input
                  id="password"
                  v-model="credentials.password"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  placeholder="Ingresa tu contraseña"
                />
                <button
                  class="password-toggle"
                  type="button"
                  :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                  @click="showPassword = !showPassword"
                >
                  <EyeOff v-if="showPassword" :size="16" />
                  <Eye v-else :size="16" />
                </button>
              </div>
            </div>

            <label class="remember-option">
              <input v-model="rememberSession" type="checkbox" />
              <span>Recordar sesión</span>
            </label>

            <p v-if="errorMessage" class="feedback-alert error" role="alert">
              <AlertCircle :size="16" class="alert-svg" />
              <span>{{ errorMessage }}</span>
            </p>

            <button class="submit-button" type="submit" :disabled="isLoading">
              <span>{{ buttonLabel }}</span>
              <ArrowRight :size="16" class="btn-arrow" />
            </button>
          </form>

          <div class="help-block">
            <span class="help-icon">
              <HelpCircle :size="14" />
            </span>
            <div>
              <strong>¿Problemas para iniciar sesión?</strong>
              <p>Acude a la Casa de la Cultura para solicitar asistencia o restablecimiento.</p>
            </div>
          </div>
        </div>

        <!-- CARD 2: Redesigned Change Password Screen -->
        <div v-else-if="mustChangePassword" class="auth-card change-password-card">
          <div class="security-header-badge">
            <div class="security-icon-box">
              <ShieldCheck :size="24" />
            </div>
            <div>
              <h2>Actualiza tu Contraseña</h2>
              <p class="security-tagline">Primer inicio de sesión seguro</p>
            </div>
          </div>

          <!-- User Account Identifier Pill -->
          <div class="account-pill">
            <div class="account-avatar">
              <User :size="15" />
            </div>
            <div class="account-details">
              <span class="account-user">{{ authenticatedUser?.nombreUsuario }}</span>
              <span class="account-badge">{{ roleDisplay }}</span>
            </div>
          </div>

          <p class="change-intro">
            Por seguridad institucional, debes definir una contraseña personal que solo tú conozcas antes de acceder al portal.
          </p>

          <form @submit.prevent="submitPasswordChange" novalidate class="auth-form">
            <!-- Field 1: Nueva Contraseña -->
            <div class="field-group">
              <label for="new-password">Nueva Contraseña</label>
              <div class="input-wrapper">
                <KeyRound class="input-icon" :size="17" />
                <input
                  id="new-password"
                  v-model="newPassword"
                  :type="showNewPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  placeholder="Mínimo 8 caracteres"
                />
                <button
                  class="password-toggle"
                  type="button"
                  :aria-label="showNewPassword ? 'Ocultar contraseña' : 'Ver contraseña'"
                  @click="showNewPassword = !showNewPassword"
                >
                  <EyeOff v-if="showNewPassword" :size="16" />
                  <Eye v-else :size="16" />
                </button>
              </div>
            </div>

            <!-- Field 2: Confirmar Contraseña -->
            <div class="field-group">
              <label for="confirm-password">Confirmar Contraseña</label>
              <div class="input-wrapper">
                <Lock class="input-icon" :size="17" />
                <input
                  id="confirm-password"
                  v-model="confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  placeholder="Repite tu nueva contraseña"
                />
                <button
                  class="password-toggle"
                  type="button"
                  :aria-label="showConfirmPassword ? 'Ocultar contraseña' : 'Ver contraseña'"
                  @click="showConfirmPassword = !showConfirmPassword"
                >
                  <EyeOff v-if="showConfirmPassword" :size="16" />
                  <Eye v-else :size="16" />
                </button>
              </div>
            </div>

            <!-- Validation Badges Checklist -->
            <div class="criteria-list">
              <div class="criteria-item" :class="{ valid: hasMinLength }">
                <CheckCircle2 v-if="hasMinLength" :size="14" class="criteria-icon success" />
                <span v-else class="criteria-bullet"></span>
                <span>Al menos 8 caracteres</span>
              </div>
              <div
                class="criteria-item"
                :class="{ valid: passwordsMatch, invalid: passwordsMismatch }"
              >
                <CheckCircle2 v-if="passwordsMatch" :size="14" class="criteria-icon success" />
                <AlertCircle v-else-if="passwordsMismatch" :size="14" class="criteria-icon danger" />
                <span v-else class="criteria-bullet"></span>
                <span>Las contraseñas coinciden</span>
              </div>
            </div>

            <p v-if="errorMessage" class="feedback-alert error" role="alert">
              <AlertCircle :size="16" class="alert-svg" />
              <span>{{ errorMessage }}</span>
            </p>

            <button
              class="submit-button primary-change-btn"
              type="submit"
              :disabled="isLoading || !canSubmitPasswordChange"
            >
              <span>{{ isLoading ? 'GUARDANDO CONTRASEÑA...' : 'GUARDAR Y ACCEDER AL PORTAL' }}</span>
              <ArrowRight :size="16" class="btn-arrow" />
            </button>

            <button class="cancel-btn" type="button" @click="clearSession">
              <LogOut :size="14" />
              <span>Cerrar sesión / Usar otra cuenta</span>
            </button>
          </form>
        </div>

        <!-- CARD 3: Success state fallback -->
        <div v-else class="auth-card success-card">
          <div class="success-mark">✓</div>
          <h1>Sesión iniciada</h1>
          <p class="subtitle">{{ successMessage }}</p>
          <p class="user-summary">{{ authenticatedUser.nombreUsuario }}</p>
          <button class="submit-button" type="button" @click="clearSession">
            <LogOut :size="15" />
            <span>CERRAR SESIÓN</span>
          </button>
        </div>

        <footer class="security-note">
          <span class="shield-icon" aria-hidden="true">
            <Heart :size="12" />
          </span>
          <span>Tus datos de acceso son proporcionados y gestionados por la administración de la Casa de la Cultura de Tlaxiaco.</span>
        </footer>
      </section>

      <!-- Right Column: Institutional Historic Building Showcase (Desktop) -->
      <section class="showcase-column" aria-hidden="true">
        <div class="building-frame">
          <img
            class="building-photo"
            src="../../../assets/casa_lodgo.png"
            alt="Casa de la Cultura Heroica Ciudad de Tlaxiaco"
          />
          <div class="building-caption">
            <span class="caption-title">Casa de la Cultura</span>
            <span class="caption-city">Heroica Ciudad de Tlaxiaco, Oaxaca</span>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped src="../../../assets/styles/login.css"></style>
