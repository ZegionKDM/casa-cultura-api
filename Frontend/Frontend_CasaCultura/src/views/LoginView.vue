<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, loginUser } from '../services/authService'

const router = useRouter()
const credentials = reactive({
  nombreUsuario: '',
  password: '',
})

const rememberSession = ref(false)
const showPassword = ref(false)
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
  if (savedUsername) {
    credentials.nombreUsuario = savedUsername
    rememberSession.value = true
  }
})

const buttonLabel = computed(() => (isLoading.value ? 'VALIDANDO...' : 'INICIAR SESIÓN'))

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
    <section class="brand-panel" aria-label="Casa de la Cultura de Tlaxiaco">
      <img class="brand-logo" src="../assets/casacul.png" alt="Casa de la Cultura de Tlaxiaco" />
      <img class="building-image" src="../assets/casa_lodgo.png" alt="" />
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="welcome-icon" aria-hidden="true">
          <svg viewBox="0 0 32 32" fill="none">
            <circle cx="16" cy="10" r="5.5" stroke="currentColor" stroke-width="2" />
            <path d="M6.5 27c.8-5 4-7.5 9.5-7.5s8.7 2.5 9.5 7.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            <path d="M24 18v7M20.5 21.5H27.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </div>

        <template v-if="!authenticatedUser">
          <h1>Bienvenido</h1>
          <p class="subtitle">Inicia sesión para acceder a<br />tu espacio en la Casa de la Cultura.</p>

          <form @submit.prevent="submitLogin" novalidate>
            <label for="username">Usuario</label>
            <div class="input-wrapper">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="12" cy="8" r="3.5" stroke="currentColor" stroke-width="1.8" />
                <path d="M5 20c.5-3.5 2.8-5.3 7-5.3s6.5 1.8 7 5.3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              </svg>
              <input id="username" v-model="credentials.nombreUsuario" type="text" autocomplete="username" placeholder="Ingresa tu usuario" />
            </div>

            <label for="password">Contraseña</label>
            <div class="input-wrapper">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <rect x="5" y="10" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.8" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              </svg>
              <input id="password" v-model="credentials.password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="Ingresa tu contraseña" />
              <button class="password-toggle" type="button" :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'" @click="showPassword = !showPassword">
                <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none">
                  <path d="M3 12s3.2-5 9-5 9 5 9 5-3.2 5-9 5-9-5-9-5Z" stroke="currentColor" stroke-width="1.7" />
                  <circle cx="12" cy="12" r="2.5" stroke="currentColor" stroke-width="1.7" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none">
                  <path d="m4 4 16 16M10.6 6.9A9.6 9.6 0 0 1 12 6.8c5.8 0 9 5.2 9 5.2a17 17 0 0 1-2.3 2.7M6.2 8.2C4.1 9.6 3 12 3 12s3.2 5.2 9 5.2c1.4 0 2.6-.3 3.7-.7" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                </svg>
              </button>
            </div>

            <label class="remember-option">
              <input v-model="rememberSession" type="checkbox" />
              <span>Recordar sesion</span>
            </label>

            <p v-if="errorMessage" class="feedback error" role="alert">{{ errorMessage }}</p>
            <button class="submit-button" type="submit" :disabled="isLoading">
              {{ buttonLabel }}
              <span aria-hidden="true">-&gt;</span>
            </button>
          </form>

          <div class="help-block">
            <span class="help-icon">?</span>
            <div>
              <strong>¿Problemas para iniciar sesión?</strong>
              <p>Acude a la Casa de la Cultura para solicitar ayuda.</p>
            </div>
          </div>
        </template>

        <div v-else-if="mustChangePassword" class="success-state password-change-state">
          <div class="success-mark">!</div>
          <h1>Cambia tu contraseña</h1>
          <p class="subtitle">Por seguridad, debes reemplazar la contraseña proporcionada por el supervisor.</p>
          <form @submit.prevent="submitPasswordChange" novalidate>
            <label for="new-password">Nueva contraseña</label>
            <input id="new-password" v-model="newPassword" type="password" autocomplete="new-password" placeholder="Mínimo 8 caracteres" />
            <label for="confirm-password">Confirmar contraseña</label>
            <input id="confirm-password" v-model="confirmPassword" type="password" autocomplete="new-password" placeholder="Repite tu contraseña" />
            <p v-if="errorMessage" class="feedback error" role="alert">{{ errorMessage }}</p>
            <button class="submit-button" type="submit" :disabled="isLoading">{{ isLoading ? 'GUARDANDO...' : 'GUARDAR CONTRASEÑA' }}</button>
          </form>
          <p class="password-help">Si olvidas esta contraseña, deberás solicitar un restablecimiento al superadministrador.</p>
        </div>

        <div v-else class="success-state">
          <div class="success-mark">✓</div>
          <h1>Sesión iniciada</h1>
          <p class="subtitle">{{ successMessage }}</p>
          <p class="user-summary">{{ authenticatedUser.nombreUsuario }}</p>
          <button class="submit-button" type="button" @click="clearSession">CERRAR SESION</button>
        </div>
      </div>

      <footer class="security-note">
        <span class="shield-icon">♡</span>
        <span>Tus datos de acceso son proporcionados<br />por la administración de la Casa de la Cultura.</span>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: #fcf7f8;
  color: #282528;
}

.brand-panel {
  position: relative;
  min-height: 635px;
  height: 100vh;
}

.brand-logo {
  position: absolute;
  top: 45px;
  left: 130px;
  width: 265px;
  height: auto;
  object-fit: contain;
}

.building-image {
  position: absolute;
  z-index: 0;
  top: 198px;
  right: 12px;
  width: min(760px, 63vw);
  max-width: none;
  opacity: .7;
}

.login-panel {
  position: absolute;
  z-index: 1;
  top: 167px;
  left: 117px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-card {
  width: min(100%, 302px);
  padding: 20px 20px 18px;
  background: rgba(255, 255, 255, .95);
  box-shadow: 0 10px 32px rgba(145, 70, 91, .05);
}

.welcome-icon {
  width: 49px;
  height: 49px;
  margin: 0 auto 7px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #9e1232;
  background: #fde9ee;
}

.welcome-icon svg {
  width: 29px;
  height: 29px;
}

h1 {
  margin: 0;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
}

.subtitle {
  margin: 3px 0 22px;
  color: #4f4a4c;
  text-align: center;
  font-size: 12px;
  line-height: 1.45;
}

form {
  display: flex;
  flex-direction: column;
}

label {
  margin: 0 0 5px;
  color: #373235;
  font-size: 10px;
  font-weight: 700;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 13px;
}

.input-wrapper > svg {
  position: absolute;
  left: 10px;
  width: 18px;
  height: 18px;
  color: #9e1232;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  height: 34px;
  padding: 0 34px 0 33px;
  border: 1px solid #f0cbd4;
  border-radius: 6px;
  outline: none;
  color: #302b2e;
  background: #fff;
  font-size: 11px;
}

input:focus {
  border-color: #a80f32;
  box-shadow: 0 0 0 3px rgba(168, 15, 50, .08);
}

input::placeholder {
  color: #aaa4a6;
}

.password-toggle {
  position: absolute;
  right: 9px;
  display: grid;
  padding: 0;
  border: 0;
  color: #c696a3;
  background: transparent;
  cursor: pointer;
}

.password-toggle svg {
  width: 16px;
  height: 16px;
}

.remember-option {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 14px;
  font-size: 10px;
  font-weight: 500;
  cursor: pointer;
}

.remember-option input {
  width: 13px;
  height: 13px;
  accent-color: #9e1232;
}

.feedback {
  margin: -3px 0 10px;
  font-size: 11px;
  line-height: 1.35;
}

.feedback.error {
  color: #aa1735;
}

.submit-button {
  width: 100%;
  min-height: 33px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  border: 0;
  border-radius: 5px;
  color: white;
  background: #a70d2d;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: background .2s ease, transform .2s ease;
}

.submit-button:hover:not(:disabled) {
  background: #880a25;
  transform: translateY(-1px);
}

.submit-button:disabled {
  opacity: .7;
  cursor: wait;
}

.help-block {
  display: flex;
  gap: 10px;
  margin: 20px -20px -18px;
  padding: 13px 20px 12px;
  border-top: 1px solid #f7e9ed;
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
  font-size: 13px;
  font-weight: 700;
}

.help-block strong {
  display: block;
  margin: 1px 0 2px;
  font-size: 10px;
}

.help-block p,
.security-note {
  color: #777074;
  font-size: 9px;
  line-height: 1.45;
}

.security-note {
  display: flex;
  align-items: center;
  gap: 10px;
  width: min(100%, 302px);
  margin-top: 18px;
}

.shield-icon {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #d694a6;
  background: #fff0f3;
}

.success-state {
  padding: 18px 0 8px;
  text-align: center;
}

.success-mark {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #a70d2d;
  font-size: 26px;
}

.user-summary {
  margin: -7px 0 23px;
  color: #a70d2d;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 800px) {
  .login-page {
    display: block;
  }

  .brand-panel {
    min-height: 100vh;
  }

  .brand-logo {
    top: 25px;
    left: 50%;
    width: 260px;
    transform: translateX(-50%);
  }

  .building-image {
    top: 155px;
    right: 50%;
    width: 620px;
    transform: translateX(50%);
  }

  .login-panel {
    top: 210px;
    left: 50%;
    width: min(302px, calc(100% - 32px));
    transform: translateX(-50%);
  }

  .login-card,
  .security-note {
    width: 100%;
  }
}
</style>
