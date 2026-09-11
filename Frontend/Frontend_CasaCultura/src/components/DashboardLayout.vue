<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearStoredSession } from '../services/apiService'
import { Bell, LogOut, Menu, UserCircle2 } from 'lucide-vue-next'

const props = defineProps({
  role: {
    type: String,
    required: true,
  },
  username: {
    type: String,
    default: 'Usuario',
  },
  items: {
    type: Array,
    required: true,
  },
  active: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['navigate'])
const router = useRouter()
const menuOpen = ref(false)
const sidebarCollapsed = ref(false)

const roleLabel = computed(() => ({
  ALUMNO: 'Alumno',
  SUPERVISOR: 'Supervisor',
  SUPER_ADMIN: 'Super administrador',
}[props.role] || props.role))

function logout() {
  clearStoredSession()
  router.push('/')
}

function navigate(item) {
  emit('navigate', item.id)
  menuOpen.value = false
}

function toggleMenu() {
  if (window.innerWidth <= 760) {
    menuOpen.value = !menuOpen.value
    return
  }
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div class="dashboard-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <aside class="sidebar" :class="{ 'is-open': menuOpen }">
      <div class="sidebar-brand">
        <img src="../assets/casacul.png" alt="Casa de la Cultura de Tlaxiaco" />
        <div>
          <strong>Casa de la Cultura</strong>
          <small>{{ roleLabel }}</small>
        </div>
      </div>

      <nav class="sidebar-nav" aria-label="Navegación principal">
        <button
          v-for="item in items"
          :key="item.id"
          type="button"
          :class="{ active: active === item.id }"
          @click="navigate(item)"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <button class="sidebar-logout" type="button" @click="logout">
        <LogOut class="nav-icon" />
        Cerrar sesión
      </button>
    </aside>

    <section class="dashboard-main">
      <header class="topbar">
        <button class="menu-button" type="button" aria-label="Mostrar u ocultar menú" @click="toggleMenu">
          <Menu class="menu-icon" />
        </button>
        <div class="topbar-title">
          <strong>{{ items.find((item) => item.id === active)?.label }}</strong>
          <small>Panel de {{ roleLabel.toLowerCase() }}</small>
        </div>
        <div class="topbar-user">
          <span class="notification">
            <Bell class="notification-icon" />
            <b>3</b>
          </span>
          <span class="avatar"><UserCircle2 class="avatar-icon" /></span>
          <span>
            <strong>{{ username }}</strong>
            <small>{{ roleLabel }}</small>
          </span>
        </div>
      </header>

      <main class="dashboard-content">
        <slot />
      </main>
    </section>
  </div>
</template>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
  display: flex;
  color: #242942;
  background: #f5f6fa;
}

.sidebar {
  width: 226px;
  min-width: 226px;
  display: flex;
  flex-direction: column;
  padding: 21px 10px 18px;
  color: #fff;
  background: #202754;
  overflow: hidden;
  transition: width .2s ease, min-width .2s ease, padding .2s ease, left .2s ease;
}

.sidebar-brand {
  display: block;
  min-height: 48px;
  padding: 0 8px 20px;
}

.sidebar-brand img {
  display: block;
  width: 150px;
  height: 48px;
  object-fit: contain;
  border-radius: 8px;
  padding: 5px 7px;
  background: #fff;
}

.sidebar-brand strong,
.sidebar-brand small,
.topbar-user strong,
.topbar-user small {
  display: block;
}

.sidebar-brand strong {
  margin-top: 8px;
  font-size: 12px;
}

.sidebar-brand small,
.topbar-user small {
  color: #aeb5d3;
  font-size: 9px;
}

.sidebar-nav {
  display: grid;
  gap: 5px;
  padding-top: 18px;
}

.sidebar-nav button,
.sidebar-logout {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 10px 11px;
  border: 0;
  border-radius: 6px;
  color: #f2f4ff;
  background: transparent;
  font: inherit;
  font-size: 10px;
  text-align: left;
  cursor: pointer;
}

.sidebar-nav button:hover,
.sidebar-nav button.active {
  background: #4c5be0;
}

.nav-icon {
  width: 16px;
  color: #dce1ff;
  flex: 0 0 auto;
}

.sidebar-logout {
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, .14);
  border-radius: 0;
  padding-top: 23px;
}

.dashboard-main {
  min-width: 0;
  flex: 1;
}

.dashboard-shell.sidebar-collapsed .sidebar {
  width: 0;
  min-width: 0;
  padding-right: 0;
  padding-left: 0;
}

.topbar {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 24px;
  border-bottom: 1px solid #e6e8ef;
  background: #fff;
}

.menu-button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 7px;
  color: #5a647e;
  background: #f3f5f9;
  cursor: pointer;
}

.menu-icon,
.notification-icon,
.avatar-icon {
  width: 16px;
  height: 16px;
}

.notification-icon,
.avatar-icon {
  width: 18px;
  height: 18px;
}

.topbar-title {
  flex: 1;
}

.topbar-title strong {
  display: block;
  font-size: 14px;
}

.topbar-title small {
  color: #9098aa;
  font-size: 9px;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 10px;
}

.topbar-user small {
  color: #9ba2b2;
}

.notification {
  position: relative;
  color: #5c6785;
  display: grid;
  place-items: center;
}

.notification b {
  position: absolute;
  top: -3px;
  right: -7px;
  width: 13px;
  height: 13px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #e23c52;
  font-size: 8px;
}

.avatar {
  width: 33px;
  height: 33px;
  display: grid;
  place-items: center;
  border: 2px solid #dce1ef;
  border-radius: 50%;
  color: #687394;
}

.dashboard-content {
  padding: 26px 24px;
}

@media (max-width: 760px) {
  .sidebar {
    position: fixed;
    z-index: 10;
    top: 0;
    bottom: 0;
    left: -226px;
    transition: left .2s ease;
  }

  .dashboard-shell.sidebar-collapsed .sidebar {
    width: 226px;
    min-width: 226px;
    padding: 21px 10px 18px;
  }

  .sidebar.is-open {
    left: 0;
  }

  .topbar {
    padding: 0 14px;
  }

  .dashboard-content {
    padding: 18px 14px;
  }
}
</style>
