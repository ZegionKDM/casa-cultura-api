<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Menu,
  Bell,
  Maximize,
  Minimize,
  UserCircle,
  LogOut
} from 'lucide-vue-next'
import { clearStoredSession } from '../../services/apiService'

const route = useRoute()
const router = useRouter()

const showNotifications = ref(false)
const showUserMenu = ref(false)
const isFullscreen = ref(false)
const storedUser = ref(null)

onMounted(() => {
  const serialized = localStorage.getItem('casa-cultura-user') || sessionStorage.getItem('casa-cultura-user')
  if (serialized) {
    try {
      storedUser.value = JSON.parse(serialized)
    } catch {
      storedUser.value = null
    }
  }
})

const isTeacher = computed(() => route.path.startsWith('/docente'))

const headerTitle = computed(() => route.meta.title || 'Dashboard')

const headerSubtitle = computed(() => route.meta.subtitle || 'Panel de Administración')

const userName = computed(() => {
  if (storedUser.value?.persona) {
    return `${storedUser.value.persona.nombre} ${storedUser.value.persona.apellidoPaterno || ''}`.trim()
  }
  return storedUser.value?.nombreUsuario || 'Super Admin'
})

const userRole = computed(() => {
  const rawRole = storedUser.value?.rol || 'SUPER_ADMIN'
  const cleanRole = String(rawRole).replace('ROLE_', '')
  if (cleanRole === 'SUPER_ADMIN') return 'Super Administrador'
  if (cleanRole === 'SUPERVISOR') return 'Supervisor'
  if (cleanRole === 'DOCENTE') return 'Docente'
  if (cleanRole === 'ALUMNO') return 'Alumno'
  return cleanRole
})

const notifications = ref([
  { title: 'Sistema actualizado', detail: 'Conexión activa con Casa de la Cultura API' },
  { title: 'Pagos y asistencias sincronizados', detail: 'Información en tiempo real' },
  { title: 'Control de accesos y roles', detail: 'Seguridad JWT activa' }
])

const toggleFullscreen = async () => {
  if (!document.fullscreenElement) {
    if (document.documentElement.requestFullscreen) {
      await document.documentElement.requestFullscreen()
      isFullscreen.value = true
    }
  } else {
    if (document.exitFullscreen) {
      await document.exitFullscreen()
      isFullscreen.value = false
    }
  }
}

const handleLogout = async () => {
  clearStoredSession()
  await router.push('/')
}
</script>

<template>
  <header class="header">
    <div class="header-left">
      <button class="icon-button" aria-label="Menú">
        <Menu :size="22" />
      </button>

      <div>
        <h2>{{ headerTitle }}</h2>
        <span>{{ headerSubtitle }}</span>
      </div>
    </div>

    <div class="header-right">
      <nav class="role-switcher" aria-label="Cambiar vista">
        <RouterLink
          to="/admin/dashboard"
          class="selected"
        >
          Administrador
        </RouterLink>
      </nav>

      <div class="notification-wrapper">
        <button
          class="icon-button notification"
          aria-label="Ver notificaciones"
          :aria-expanded="showNotifications"
          @click="showNotifications = !showNotifications; showUserMenu = false"
        >
          <Bell :size="21" />
          <span class="notification-badge">
            {{ notifications.length }}
          </span>
        </button>

        <div
          v-if="showNotifications"
          class="notifications-panel"
        >
          <h3>Notificaciones</h3>

          <div
            v-for="notification in notifications"
            :key="notification.title"
            class="notification-item"
          >
            <strong>{{ notification.title }}</strong>
            <span>{{ notification.detail }}</span>
          </div>
        </div>
      </div>

      <button
        class="icon-button"
        title="Pantalla completa"
        @click="toggleFullscreen"
      >
        <Minimize
          v-if="isFullscreen"
          :size="20"
        />
        <Maximize
          v-else
          :size="20"
        />
      </button>

      <div class="user-menu-wrapper">
        <div
          class="user-info"
          @click="showUserMenu = !showUserMenu; showNotifications = false"
        >
          <UserCircle :size="38" />
          <div>
            <strong>{{ userName }}</strong>
            <span>{{ userRole }}</span>
          </div>
        </div>

        <div
          v-if="showUserMenu"
          class="user-dropdown"
        >
          <div class="user-dropdown-header">
            <strong>{{ userName }}</strong>
            <span>{{ userRole }}</span>
          </div>
          <button
            class="logout-btn"
            @click="handleLogout"
          >
            <LogOut :size="16" />
            Cerrar Sesión
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: 85px;
  background: white;
  border-bottom: 1px solid #e6e8ee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  position: relative;
  z-index: 20;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.role-switcher {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px;
  background: #f1f4f8;
  border: 1px solid #e4e8ee;
  border-radius: 8px;
}

.role-switcher a {
  padding: 7px 12px;
  border-radius: 6px;
  color: #667085;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}

.role-switcher a:hover,
.role-switcher a.selected {
  background: white;
  color: #3156b8;
  box-shadow: 0 1px 3px rgb(31 41 55 / 12%);
}

.notification-wrapper,
.user-menu-wrapper {
  position: relative;
}

.header h2 {
  margin: 0;
  font-size: 20px;
  color: #202838;
  font-weight: 700;
}

.header-left span {
  font-size: 13px;
  color: #858c9a;
}

.icon-button {
  position: relative;
  border: none;
  background: #f5f6f9;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4d5667;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.icon-button:hover {
  background: #ebeef5;
  color: #202838;
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #dc3545;
  color: white;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notifications-panel,
.user-dropdown {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 100;
  background: white;
  border: 1px solid #e1e5ec;
  border-radius: 10px;
  box-shadow: 0 12px 30px rgb(25 35 55 / 18%);
}

.notifications-panel {
  width: min(340px, calc(100vw - 32px));
  padding: 18px;
}

.notifications-panel h3 {
  margin: 0 0 12px;
  color: #202838;
  font-size: 16px;
}

.notification-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 0;
  border-top: 1px solid #edf0f4;
}

.notification-item:first-of-type {
  border-top: 0;
  padding-top: 0;
}

.notification-item strong {
  color: #31394a;
  font-size: 13px;
}

.notification-item span {
  color: #858c9a;
  font-size: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #31394a;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.user-info:hover {
  background: #f5f6f9;
}

.user-info div {
  display: flex;
  flex-direction: column;
}

.user-info strong {
  font-size: 14px;
  font-weight: 600;
}

.user-info span {
  font-size: 11px;
  color: #8b92a0;
}

.user-dropdown {
  width: 200px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-dropdown-header {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 8px;
  border-bottom: 1px solid #edf0f4;
}

.user-dropdown-header strong {
  font-size: 13px;
  color: #202838;
}

.user-dropdown-header span {
  font-size: 11px;
  color: #8b92a0;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border: none;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.logout-btn:hover {
  background: #fecaca;
}

@media (max-width: 900px) {
  .header {
    padding: 0 18px;
  }

  .header-right {
    gap: 10px;
  }

  .role-switcher a {
    padding-inline: 8px;
  }

  .user-info div {
    display: none;
  }
}

@media (max-width: 560px) {
  .header {
    height: 68px;
    padding: 0 12px;
  }

  .header-left,
  .header-right {
    gap: 8px;
  }

  .header h2 {
    max-width: 145px;
    overflow: hidden;
    font-size: 15px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .header-left span,
  .role-switcher {
    display: none;
  }

  .icon-button {
    width: 36px;
    height: 36px;
  }

  .user-info > svg {
    width: 32px;
    height: 32px;
  }

  .notifications-panel {
    position: fixed;
    top: 76px;
    right: 12px;
  }
}
</style>
