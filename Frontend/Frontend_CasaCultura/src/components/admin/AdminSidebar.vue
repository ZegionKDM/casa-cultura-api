<script setup>
import { useRouter } from 'vue-router'
import {
  LayoutDashboard,
  Users,
  ShieldCheck,
  GraduationCap,
  BookOpen,
  CalendarDays,
  ClipboardList,
  UserCheck,
  CreditCard,
  BarChart3,
  Settings,
  Database,
  X,
  LogOut
} from 'lucide-vue-next'
import { isMobileSidebarOpen, closeAdminSidebar } from './adminNavState.js'
import { clearStoredSession } from '../../services/apiService.js'

const router = useRouter()

const menu = [
  {
    title: 'PRINCIPAL',
    items: [
      {
        name: 'Dashboard',
        icon: LayoutDashboard,
        route: '/admin/dashboard'
      }
    ]
  },
  {
    title: 'ADMINISTRACIÓN',
    items: [
      {
        name: 'Usuarios',
        icon: Users,
        route: '/admin/usuarios'
      },
      {
        name: 'Roles y Permisos',
        icon: ShieldCheck,
        route: '/admin/roles'
      },
      {
        name: 'Alumnos',
        icon: GraduationCap,
        route: '/admin/alumnos'
      },
      {
        name: 'Docentes',
        icon: UserCheck,
        route: '/admin/docentes'
      },
      {
        name: 'Talleres',
        icon: BookOpen,
        route: '/admin/talleres'
      },
      {
        name: 'Horarios',
        icon: CalendarDays,
        route: '/admin/horarios'
      },
      {
        name: 'Inscripciones',
        icon: ClipboardList,
        route: '/admin/inscripciones'
      }
    ]
  },
  {
    title: 'CONTROL',
    items: [
      {
        name: 'Asistencias',
        icon: UserCheck,
        route: '/admin/asistencias'
      },
      {
        name: 'Pagos',
        icon: CreditCard,
        route: '/admin/pagos'
      }
    ]
  },
  {
    title: 'MONETIZACIÓN Y ESTADÍSTICAS',
    items: [
      {
        name: 'Monetización',
        icon: BarChart3,
        route: '/admin/monetizacion'
      },
      {
        name: 'Reportes y Gráficas',
        icon: BarChart3,
        route: '/admin/reportes'
      }
    ]
  },
  {
    title: 'CONFIGURACIÓN',
    items: [
      {
        name: 'Configuración',
        icon: Settings,
        route: '/admin/configuracion'
      },
      {
        name: 'Bitácora de Actividad',
        icon: Database,
        route: '/admin/bitacora'
      }
    ]
  }
]

function handleItemClick() {
  closeAdminSidebar()
}

function handleLogout() {
  clearStoredSession()
  closeAdminSidebar()
  router.push('/')
}
</script>

<template>
  <div>
    <!-- Backdrop for mobile drawer -->
    <Transition name="fade">
      <div
        v-if="isMobileSidebarOpen"
        class="sidebar-backdrop"
        @click="closeAdminSidebar"
      ></div>
    </Transition>

    <aside class="sidebar" :class="{ 'is-mobile-open': isMobileSidebarOpen }">
      <!-- Institutional Branding Header with Logo -->
      <div class="sidebar-brand">
        <button
          class="mobile-close-btn"
          type="button"
          aria-label="Cerrar menú lateral"
          @click="closeAdminSidebar"
        >
          <X :size="18" />
        </button>

        <div class="logo-card">
          <img
            src="../../assets/casacul.png"
            alt="Casa de la Cultura de Tlaxiaco"
            class="brand-logo-img"
          />
        </div>

        <div class="brand-text">
          <h1 class="brand-title">Casa de la Cultura</h1>
          <span class="brand-subtitle">Heroica Ciudad de Tlaxiaco</span>
          <div class="role-pill">
            <span class="pulse-dot"></span>
            <span>Super Administrador</span>
          </div>
        </div>
      </div>

      <!-- Navigation Menu -->
      <nav class="menu" aria-label="Navegación de Administración">
        <div
          v-for="section in menu"
          :key="section.title"
          class="menu-section"
        >
          <p class="menu-title">
            {{ section.title }}
          </p>

          <RouterLink
            v-for="item in section.items"
            :key="item.name"
            :to="item.route"
            class="menu-item"
            exact-active-class="active"
            @click="handleItemClick"
          >
            <component
              :is="item.icon"
              :size="18"
              class="menu-item-icon"
            />
            <span>
              {{ item.name }}
            </span>
          </RouterLink>
        </div>
      </nav>

      <!-- Sidebar Footer -->
      <div class="sidebar-footer">
        <button
          class="sidebar-logout-btn"
          type="button"
          @click="handleLogout"
          title="Cerrar sesión del sistema"
        >
          <LogOut :size="16" />
          <span>Cerrar Sesión</span>
        </button>
      </div>
    </aside>
  </div>
</template>

<style scoped>
/* Backdrop */
.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(10, 15, 28, 0.6);
  backdrop-filter: blur(4px);
  z-index: 998;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Sidebar Container */
.sidebar {
  width: 270px;
  min-height: 100vh;
  height: 100%;
  background: #172033;
  color: white;
  display: flex;
  flex-direction: column;
  padding: 20px 14px 16px;
  flex-shrink: 0;
  box-sizing: border-box;
  z-index: 40;
}

/* Institutional Logo Header */
.sidebar-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 8px 8px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 16px;
  text-align: center;
}

.mobile-close-btn {
  display: none;
  position: absolute;
  top: 4px;
  right: 4px;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #e2e8f0;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.mobile-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
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
  font-size: 14.5px;
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
  background: rgba(99, 102, 241, 0.18);
  border: 1px solid rgba(99, 102, 241, 0.4);
  color: #a5b4fc;
  border-radius: 20px;
  font-size: 10.5px;
  font-weight: 600;
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
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.85); }
}

/* Menu */
.menu {
  overflow-y: auto;
  flex: 1;
  padding-right: 2px;
}

.menu::-webkit-scrollbar {
  width: 4px;
}

.menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}

.menu-section {
  margin-bottom: 20px;
}

.menu-title {
  font-size: 10.5px;
  letter-spacing: 1px;
  color: #7b88a0;
  margin: 0 0 6px 10px;
  font-weight: 700;
  text-transform: uppercase;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  color: #cbd5e1;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.15s ease;
  box-sizing: border-box;
}

.menu-item:hover {
  background: #222f47;
  color: #ffffff;
  transform: translateX(2px);
}

.menu-item.active {
  background: #3b82f6;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.35);
}

.menu-item-icon {
  flex-shrink: 0;
}

/* Footer Logout */
.sidebar-footer {
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin-top: 8px;
}

.sidebar-logout-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.08);
  color: #f87171;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-logout-btn:hover {
  background: #dc2626;
  color: #ffffff;
  border-color: #dc2626;
}

/* RESPONSIVE MOBILE DRAWER */
@media (max-width: 840px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: -290px;
    bottom: 0;
    width: 280px;
    height: 100vh;
    z-index: 999;
    box-shadow: 6px 0 24px rgba(0, 0, 0, 0.4);
    transition: left 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .sidebar.is-mobile-open {
    left: 0;
  }

  .mobile-close-btn {
    display: flex;
  }
}
</style>
