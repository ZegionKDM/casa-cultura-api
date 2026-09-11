<script setup>
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
  Database
} from 'lucide-vue-next'

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
</script>

<template>
  <aside class="sidebar">
    <div class="logo-section">
      <div class="logo-icon">
        <BookOpen :size="32" />
      </div>

      <div>
        <h1>SIGEC Tlaxiaco</h1>
        <p>Sistema de Gestión Cultural</p>
      </div>
    </div>

    <nav class="menu">
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
        >
          <component
            :is="item.icon"
            :size="18"
          />

          <span>
            {{ item.name }}
          </span>
        </RouterLink>
      </div>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 270px;
  min-height: 100vh;
  background: #172033;
  color: white;
  display: flex;
  flex-direction: column;
  padding: 22px 15px;
  flex-shrink: 0;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 12px 28px;
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #283959;
  flex-shrink: 0;
}

.logo-section h1 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #ffffff;
}

.logo-section p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #aeb9ce;
}

.menu {
  overflow-y: auto;
  flex: 1;
}

.menu-section {
  margin-bottom: 22px;
}

.menu-title {
  font-size: 11px;
  letter-spacing: 1px;
  color: #8d99ae;
  margin: 0 0 8px 12px;
  font-weight: 700;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 11px 12px;
  border-radius: 8px;
  color: #ffffff;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: background-color 0.2s ease, color 0.2s ease;
  box-sizing: border-box;
}

.menu-item:hover {
  background: #24314b;
}

.menu-item.active {
  background: #3b82f6;
  color: white;
}

@media (max-width: 760px) {
  .sidebar {
    width: 72px;
    padding: 18px 8px;
  }

  .logo-section {
    justify-content: center;
    padding-inline: 0;
  }

  .logo-section > div:last-child,
  .menu-title,
  .menu-item span {
    display: none;
  }

  .menu-item {
    justify-content: center;
    padding-inline: 8px;
  }
}
</style>
