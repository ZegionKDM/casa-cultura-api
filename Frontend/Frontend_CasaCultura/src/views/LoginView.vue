<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { changePassword, loginUser } from '../services/authService'
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
            src="../assets/casacul.png"
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
            src="../assets/casa_lodgo.png"
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

<style scoped>
.login-page {
  min-height: 100vh;
  width: 100%;
  position: relative;
  background: linear-gradient(135deg, #fdf8f9 0%, #f7eff1 50%, #fdf5f7 100%);
  color: #282528;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
}

.login-shell {
  width: 100%;
  max-width: 1120px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
}

/* Left Column */
.auth-column {
  width: 100%;
  max-width: 380px;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  z-index: 2;
}

.brand-header {
  margin-bottom: 22px;
  width: 100%;
  display: flex;
  justify-content: center;
}

.brand-logo {
  max-width: 250px;
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0 2px 8px rgba(167, 13, 45, 0.08));
}

/* Main Card */
.auth-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 14px;
  padding: 24px 22px 20px;
  box-shadow: 0 12px 36px rgba(158, 18, 50, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(240, 203, 212, 0.6);
  box-sizing: border-box;
  backdrop-filter: blur(8px);
}

.welcome-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 10px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #9e1232;
  background: #fde9ee;
}

.welcome-icon svg {
  width: 26px;
  height: 26px;
}

h1 {
  margin: 0;
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #201e20;
}

.subtitle {
  margin: 4px 0 20px;
  color: #595255;
  text-align: center;
  font-size: 12px;
  line-height: 1.45;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

label {
  color: #373235;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  width: 16px !important;
  height: 16px !important;
  min-width: 16px !important;
  min-height: 16px !important;
  max-width: 16px !important;
  max-height: 16px !important;
  color: #9e1232;
  pointer-events: none;
  flex-shrink: 0;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  height: 38px;
  padding: 0 36px 0 36px;
  border: 1px solid #f0cbd4;
  border-radius: 8px;
  outline: none;
  color: #2b2528;
  background: #ffffff;
  font-size: 12px;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus {
  border-color: #a80f32;
  box-shadow: 0 0 0 3px rgba(168, 15, 50, 0.1);
}

input::placeholder {
  color: #b0a8aa;
}

.password-toggle {
  position: absolute;
  right: 10px;
  display: grid;
  place-items: center;
  padding: 4px;
  border: 0;
  color: #a3828c;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.15s ease;
}

.password-toggle:hover {
  color: #9e1232;
}

.remember-option {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 2px 0 4px;
  font-size: 11px;
  color: #554e51;
  font-weight: 500;
  cursor: pointer;
}

.remember-option input {
  width: 14px;
  height: 14px;
  accent-color: #9e1232;
  cursor: pointer;
}

.feedback-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 11px;
  border-radius: 7px;
  font-size: 11.5px;
  line-height: 1.35;
  margin: 2px 0;
}

.feedback-alert.error {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.alert-svg {
  flex-shrink: 0;
}

.submit-button {
  width: 100%;
  min-height: 38px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  border: 0;
  border-radius: 8px;
  color: white;
  background: #a70d2d;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.4px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 4px 12px rgba(167, 13, 45, 0.2);
  margin-top: 4px;
}

.submit-button:hover:not(:disabled) {
  background: #880a25;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(167, 13, 45, 0.28);
}

.submit-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.btn-arrow {
  transition: transform 0.2s ease;
}

.submit-button:hover:not(:disabled) .btn-arrow {
  transform: translateX(2px);
}

.help-block {
  display: flex;
  gap: 10px;
  margin: 18px -22px -20px;
  padding: 12px 22px;
  border-top: 1px solid #f7e9ed;
  background: #fffafa;
  border-radius: 0 0 14px 14px;
}

.help-icon {
  flex: 0 0 22px;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #c77c91;
  background: #fff1f4;
}

.help-block strong {
  display: block;
  margin-bottom: 2px;
  font-size: 11px;
  color: #3b3437;
}

.help-block p {
  margin: 0;
  color: #777074;
  font-size: 9.5px;
  line-height: 1.4;
}

.security-note {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  margin-top: 18px;
  color: #7d7579;
  font-size: 9.5px;
  line-height: 1.4;
}

.shield-icon {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #d694a6;
  background: #fff0f3;
  flex-shrink: 0;
}

/* ===================================================
   CHANGE PASSWORD CARD STYLING
   =================================================== */
.change-password-card {
  max-width: 400px;
  padding: 24px 22px;
}

.security-header-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.security-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fde9ee 0%, #fee2e2 100%);
  color: #a70d2d;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid rgba(167, 13, 45, 0.15);
}

.security-header-badge h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e1b1d;
  line-height: 1.2;
}

.security-tagline {
  margin: 2px 0 0;
  font-size: 11px;
  color: #887e82;
  font-weight: 500;
}

.account-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #fdf2f4;
  border: 1px solid #fbd5de;
  border-radius: 8px;
  margin-bottom: 12px;
}

.account-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #a70d2d;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.account-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.account-user {
  font-size: 12px;
  font-weight: 700;
  color: #2b2326;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.account-badge {
  font-size: 10px;
  color: #a70d2d;
  font-weight: 600;
}

.change-intro {
  margin: 0 0 16px;
  font-size: 11px;
  color: #5a5356;
  line-height: 1.45;
}

/* Criteria checklist */
.criteria-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 9px 12px;
  background: #fbfbfc;
  border: 1px solid #f0edf0;
  border-radius: 7px;
  margin: 2px 0 6px;
}

.criteria-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #7b7276;
  transition: color 0.2s ease;
}

.criteria-item.valid {
  color: #15803d;
  font-weight: 600;
}

.criteria-item.invalid {
  color: #dc2626;
}

.criteria-bullet {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d4ccd0;
  margin: 0 4px;
}

.criteria-icon.success {
  color: #16a34a;
}

.criteria-icon.danger {
  color: #dc2626;
}

.primary-change-btn {
  margin-top: 6px;
}

.cancel-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  padding: 8px;
  border: 1px solid #e7d8dc;
  border-radius: 8px;
  background: white;
  color: #7a6e73;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background: #fef4f6;
  color: #a70d2d;
  border-color: #f0cbd4;
}

/* Success Card */
.success-card {
  text-align: center;
  padding: 28px 22px;
}

.success-mark {
  width: 50px;
  height: 50px;
  margin: 0 auto 12px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #a70d2d;
  font-size: 26px;
}

.user-summary {
  margin: -6px 0 20px;
  color: #a70d2d;
  font-size: 13px;
  font-weight: 700;
}

/* Right Column (Historic Building Showcase) */
.showcase-column {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.building-frame {
  position: relative;
  max-width: 680px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.building-photo {
  width: 100%;
  max-width: 580px;
  height: auto;
  object-fit: contain;
  mix-blend-mode: multiply;
  opacity: 0.85;
  filter: drop-shadow(0 8px 20px rgba(158, 18, 50, 0.08));
  transition: transform 0.4s ease;
}

.building-photo:hover {
  transform: scale(1.01);
}

.building-caption {
  margin-top: 12px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.caption-title {
  font-size: 13px;
  font-weight: 700;
  color: #4a4245;
  letter-spacing: 0.4px;
}

.caption-city {
  font-size: 11px;
  color: #8a7e83;
}

/* ===================================================
   RESPONSIVE MEDIA QUERIES (MOBILE / TABLET)
   =================================================== */
@media (max-width: 960px) {
  .login-shell {
    justify-content: center;
  }

  .showcase-column {
    display: none;
  }

  .auth-column {
    max-width: 420px;
  }

  .brand-logo {
    max-width: 230px;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 20px 14px;
  }

  .auth-card {
    padding: 20px 16px 18px;
    border-radius: 12px;
  }

  .brand-logo {
    max-width: 200px;
  }

  .brand-header {
    margin-bottom: 16px;
  }

  h1 {
    font-size: 20px;
  }

  .security-header-badge h2 {
    font-size: 16px;
  }

  .security-icon-box {
    width: 38px;
    height: 38px;
  }

  .submit-button {
    font-size: 11px;
  }
}
</style>
