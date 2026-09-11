import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import StudentView from '../views/StudentView.vue'
import SupervisorView from '../views/SupervisorView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import AdminModuleView from '../views/admin/AdminModuleView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/alumno',
      name: 'student',
      component: StudentView,
    },
    {
      path: '/supervisor',
      name: 'supervisor',
      component: SupervisorView,
    },
    {
      path: '/super-admin',
      redirect: '/admin/dashboard',
    },
    {
      path: '/admin',
      redirect: '/admin/dashboard',
    },
    {
      path: '/admin/dashboard',
      name: 'admin-dashboard',
      component: AdminDashboardView,
      meta: {
        title: 'Dashboard',
        subtitle: 'Panel de Control Principal',
        type: 'dashboard',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/usuarios',
      name: 'admin-users',
      component: AdminModuleView,
      meta: {
        title: 'Usuarios',
        subtitle: 'Gestión de cuentas y accesos del sistema',
        type: 'users',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/roles',
      name: 'admin-roles',
      component: AdminModuleView,
      meta: {
        title: 'Roles y Permisos',
        subtitle: 'Control de accesos y permisos',
        type: 'roles',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/alumnos',
      name: 'admin-students',
      component: AdminModuleView,
      meta: {
        title: 'Alumnos',
        subtitle: 'Padrón de estudiantes inscritos',
        type: 'students',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/docentes',
      name: 'admin-teachers',
      component: AdminModuleView,
      meta: {
        title: 'Docentes',
        subtitle: 'Plantilla de instructores y talleristas',
        type: 'teachers',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/talleres',
      name: 'admin-workshops',
      component: AdminModuleView,
      meta: {
        title: 'Talleres',
        subtitle: 'Cursos y talleres culturales ofertados',
        type: 'workshops',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/horarios',
      name: 'admin-schedules',
      component: AdminModuleView,
      meta: {
        title: 'Horarios',
        subtitle: 'Programación semanal de talleres',
        type: 'schedules',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/inscripciones',
      name: 'admin-registrations',
      component: AdminModuleView,
      meta: {
        title: 'Inscripciones',
        subtitle: 'Control de matrículas en grupos',
        type: 'registrations',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/asistencias',
      name: 'admin-attendance',
      component: AdminModuleView,
      meta: {
        title: 'Asistencias',
        subtitle: 'Registro diario y control de faltas',
        type: 'attendance',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/pagos',
      name: 'admin-payments',
      component: AdminModuleView,
      meta: {
        title: 'Pagos',
        subtitle: 'Control de cuotas y mensualidades',
        type: 'payments',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/monetizacion',
      name: 'admin-monetization',
      component: AdminModuleView,
      meta: {
        title: 'Monetización',
        subtitle: 'Ingresos y finanzas del centro cultural',
        type: 'monetization',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/reportes',
      name: 'admin-reports',
      component: AdminModuleView,
      meta: {
        title: 'Reportes y Gráficas',
        subtitle: 'Estadísticas e informes analíticos',
        type: 'reports',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/configuracion',
      name: 'admin-settings',
      component: AdminModuleView,
      meta: {
        title: 'Configuración',
        subtitle: 'Parámetros generales de la institución',
        type: 'settings',
        requiresAdmin: true
      }
    },
    {
      path: '/admin/bitacora',
      name: 'admin-logs',
      component: AdminModuleView,
      meta: {
        title: 'Bitácora de Actividad',
        subtitle: 'Auditoría y registro de eventos',
        type: 'logs',
        requiresAdmin: true
      }
    }
  ],
})

function getStoredUser() {
  const serializedUser = localStorage.getItem('casa-cultura-user')
    || sessionStorage.getItem('casa-cultura-user')
  return serializedUser ? JSON.parse(serializedUser) : null
}

router.beforeEach((to) => {
  const token = localStorage.getItem('casa-cultura-token')
    || sessionStorage.getItem('casa-cultura-token')
  const user = getStoredUser()
  const role = String(user?.rol || '').replace('ROLE_', '')

  if (to.path === '/') {
    return true
  }

  if (!token || !user) {
    return '/'
  }

  if (to.path === '/alumno' && role !== 'ALUMNO') {
    return role === 'SUPERVISOR' ? '/supervisor' : role === 'SUPER_ADMIN' ? '/admin/dashboard' : '/'
  }

  if (to.path === '/supervisor' && role !== 'SUPERVISOR') {
    return role === 'ALUMNO' ? '/alumno' : role === 'SUPER_ADMIN' ? '/admin/dashboard' : '/'
  }

  if ((to.path.startsWith('/admin') || to.path === '/super-admin') && role !== 'SUPER_ADMIN') {
    return role === 'SUPERVISOR' ? '/supervisor' : role === 'ALUMNO' ? '/alumno' : '/'
  }

  return true
})

export default router
