<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearStoredSession } from '../services/apiService'
import { Bell, LogOut, Menu, User, Sparkles } from 'lucide-vue-next'

const props = defineProps({
  role: {
    type: String,
    required: true,
  },
  username: {
    type: String,
    default: 'Usuario',
  },
  fullName: {
    type: String,
    default: '',
  },
  badge: {
    type: String,
    default: '',
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
  ALUMNO: 'Portal del Alumno',
  SUPERVISOR: 'Panel del Supervisor',
  SUPER_ADMIN: 'Super Administrador',
  DOCENTE: 'Portal del Docente',
}[props.role] || props.role))

const displayName = computed(() => props.fullName || props.username || 'Estudiante')

const userInitials = computed(() => {
  if (props.fullName) {
    const parts = props.fullName.trim().split(' ')
    if (parts.length >= 2) return `${parts[0][0]}${parts[1][0]}`.toUpperCase()
    return parts[0].slice(0, 2).toUpperCase()
  }
  return props.username ? props.username.slice(0, 2).toUpperCase() : 'AL'
})

const activeItem = computed(() => props.items.find((item) => item.id === props.active))

function logout() {
  clearStoredSession()
  router.push('/')
}

function navigate(item) {
  emit('navigate', item.id)
  menuOpen.value = false
}

function toggleMenu() {
  if (window.innerWidth <= 840) {
    menuOpen.value = !menuOpen.value
    return
  }
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div class="dashboard-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <!-- Backdrop for mobile drawer -->
    <div
      v-if="menuOpen"
      class="sidebar-backdrop"
      @click="menuOpen = false"
    ></div>

    <!-- SIDEBAR -->
    <aside class="sidebar" :class="{ 'is-open': menuOpen }">
      <div class="sidebar-inner">
        <!-- Institutional Logo & Branding Header -->
        <div class="sidebar-brand">
          <div class="logo-card">
            <img
              src="../assets/casacul.png"
              alt="Casa de la Cultura de Tlaxiaco"
              class="brand-logo-img"
            />
          </div>
          <div class="brand-text">
            <h1 class="brand-title">Casa de la Cultura</h1>
            <span class="brand-subtitle">Heroica Ciudad de Tlaxiaco</span>
            <div class="role-pill">
              <span class="pulse-dot"></span>
              <span>{{ roleLabel }}</span>
            </div>
          </div>
        </div>

        <!-- Navigation Section -->
        <div class="sidebar-menu-wrapper">
          <p class="sidebar-section-title">MENÚ PRINCIPAL</p>
          <nav class="sidebar-nav" aria-label="Navegación principal">
            <button
              v-for="item in items"
              :key="item.id"
              type="button"
              class="nav-btn"
              :class="{ active: active === item.id }"
              @click="navigate(item)"
            >
              <component
                :is="item.icon"
                v-if="typeof item.icon !== 'string'"
                class="nav-icon"
                :size="18"
              />
              <span
                v-else
                class="nav-icon nav-icon-text"
              >{{ item.icon }}</span>
              <span class="nav-label">{{ item.label }}</span>
            </button>
          </nav>
        </div>

        <!-- Footer / Logout -->
        <div class="sidebar-footer">
          <button class="sidebar-logout" type="button" @click="logout" title="Cerrar sesión segura">
            <LogOut class="nav-icon" :size="17" />
            <span>Cerrar sesión</span>
          </button>
        </div>
      </div>
    </aside>

    <!-- MAIN CONTENT SHELL -->
    <section class="dashboard-main">
      <!-- TOPBAR -->
      <header class="topbar">
        <div class="topbar-left">
          <button
            class="menu-button"
            type="button"
            aria-label="Mostrar u ocultar menú"
            @click="toggleMenu"
          >
            <Menu class="menu-icon" :size="19" />
          </button>
          <div class="topbar-title">
            <strong>{{ activeItem?.label || 'Dashboard' }}</strong>
            <small>Sistema Escolar y Cultural &bull; Heroica Ciudad de Tlaxiaco</small>
          </div>
        </div>

        <div class="topbar-right">
          <!-- Institutional Cycle Chip -->
          <div class="cycle-badge">
            <Sparkles :size="13" class="sparkle-icon" />
            <span>Ciclo Escolar 2026</span>
          </div>

          <!-- User Profile Capsule -->
          <div class="topbar-user">
            <div class="avatar-capsule">
              <span class="avatar-initials">{{ userInitials }}</span>
            </div>
            <div class="user-meta">
              <strong class="user-name">{{ displayName }}</strong>
              <div class="user-sub-row">
                <span class="matricula-tag" v-if="badge || username">{{ badge || username }}</span>
                <span class="user-status-dot" title="Sesión activa"></span>
              </div>
            </div>
            <button
              class="quick-logout-btn"
              type="button"
              title="Cerrar sesión"
              @click="logout"
            >
              <LogOut :size="16" />
            </button>
          </div>
        </div>
      </header>

      <!-- VIEW SLOT -->
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
  color: #1e293b;
  background: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  position: relative;
}

/* ==========================================================
   SIDEBAR (Navy slate matching Super Admin)
   ========================================================== */
.sidebar {
  width: 260px;
  min-width: 260px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 20px 14px;
  color: #ffffff;
  background: #172033;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              min-width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              padding 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.25s ease,
              visibility 0.28s ease,
              left 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 40;
  overflow-x: hidden;
  overflow-y: auto;
}

.sidebar-inner {
  width: 232px;
  min-width: 232px;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 40px);
  height: 100%;
  flex: 1;
  transition: opacity 0.2s ease, transform 0.25s ease;
}

/* Institutional Logo Header */
.sidebar-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 10px 8px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 16px;
  text-align: center;
}

.logo-card {
  width: 100%;
  max-width: 200px;
  height: 64px;
  background: #ffffff;
  border-radius: 12px;
  padding: 8px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.logo-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.32);
}

.brand-logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.brand-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.brand-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.3px;
}

.brand-subtitle {
  font-size: 10.5px;
  color: #94a3b8;
  letter-spacing: 0.2px;
}

.role-pill {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: rgba(64, 81, 163, 0.25);
  border: 1px solid rgba(99, 102, 241, 0.4);
  color: #a5b4fc;
  border-radius: 20px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 8px #22c55e;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.85); }
  100% { opacity: 1; transform: scale(1); }
}

/* Sidebar Menu Items */
.sidebar-menu-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.sidebar-section-title {
  font-size: 10px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.8px;
  padding: 0 10px;
  margin: 0 0 10px;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 11px 14px;
  border: 1px solid transparent;
  border-radius: 9px;
  color: #cbd5e1;
  background: transparent;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: left;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #ffffff;
  transform: translateX(3px);
}

.nav-btn.active {
  background: #4051a3;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(64, 81, 163, 0.4);
}

.nav-icon {
  flex-shrink: 0;
  color: #a5b4fc;
  transition: color 0.2s ease;
}

.nav-btn.active .nav-icon {
  color: #ffffff;
}

.nav-icon-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
}

.nav-label {
  flex: 1;
}

/* Sidebar Footer / Logout */
.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-logout {
  width: 100%;
  padding: 11px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 9px;
  border: 1px solid rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.08);
  color: #fca5a5;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-logout:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.5);
  color: #ffffff;
}

/* ==========================================================
   SIDEBAR COLLAPSED STATE (Desktop)
   ========================================================== */
.dashboard-shell.sidebar-collapsed .sidebar {
  width: 0 !important;
  min-width: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
  border-right: none !important;
  opacity: 0 !important;
  visibility: hidden !important;
  pointer-events: none !important;
  overflow: hidden !important;
}

.dashboard-shell.sidebar-collapsed .sidebar-inner {
  opacity: 0 !important;
  transform: translateX(-20px) !important;
  pointer-events: none !important;
}

/* ==========================================================
   MAIN VIEW SHELL & TOPBAR
   ========================================================== */
.dashboard-main {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
  position: sticky;
  top: 0;
  z-index: 30;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-button {
  width: 38px;
  height: 38px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #475569;
  background: #f8fafc;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.menu-button:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.topbar-title strong {
  display: block;
  font-size: 17px;
  color: #0f172a;
  font-weight: 700;
}

.topbar-title small {
  color: #64748b;
  font-size: 11.5px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cycle-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 600;
  color: #475569;
}

.sparkle-icon {
  color: #f59e0b;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 8px 5px 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 30px;
}

.avatar-capsule {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4051a3 0%, #6366f1 100%);
  color: #ffffff;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 12px;
  box-shadow: 0 2px 6px rgba(64, 81, 163, 0.3);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.user-name {
  font-size: 12.5px;
  color: #0f172a;
  font-weight: 600;
  line-height: 1.2;
}

.user-sub-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

.matricula-tag {
  font-size: 10px;
  font-weight: 700;
  color: #4051a3;
  background: #e0e7ff;
  padding: 1px 6px;
  border-radius: 4px;
}

.user-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #16a34a;
}

.quick-logout-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #94a3b8;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-left: 2px;
}

.quick-logout-btn:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* ==========================================================
   CONTENT AREA
   ========================================================== */
.dashboard-content {
  padding: 28px 32px 40px;
  flex: 1;
}

/* ==========================================================
   PRINT MEDIA RULES FOR LAYOUT
   ========================================================== */
@media print {
  .sidebar,
  .topbar,
  .sidebar-backdrop {
    display: none !important;
  }

  .dashboard-shell {
    background: transparent !important;
    min-height: auto !important;
    display: block !important;
    padding: 0 !important;
    margin: 0 !important;
  }

  .dashboard-main {
    display: block !important;
    padding: 0 !important;
    margin: 0 !important;
    min-width: auto !important;
  }

  .dashboard-content {
    display: block !important;
    padding: 0 !important;
    margin: 0 !important;
  }
}

/* ==========================================================
   RESPONSIVE DESIGN (Mobile / Tablet)
   ========================================================== */
@media (max-width: 840px) {
  .sidebar {
    position: fixed;
    z-index: 50;
    top: 0;
    bottom: 0;
    left: -270px;
    box-shadow: 6px 0 24px rgba(0, 0, 0, 0.4);
  }

  .sidebar.is-open {
    left: 0;
  }

  .sidebar-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.5);
    backdrop-filter: blur(2px);
    z-index: 45;
  }

  .topbar {
    padding: 0 16px;
  }

  .dashboard-content {
    padding: 20px 16px;
  }

  .cycle-badge {
    display: none;
  }
}

@media (max-width: 580px) {
  .user-meta {
    display: none;
  }

  .topbar-title strong {
    font-size: 15px;
  }
}
</style>
