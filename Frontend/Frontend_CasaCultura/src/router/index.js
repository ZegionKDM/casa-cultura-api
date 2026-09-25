import { createRouter, createWebHistory } from 'vue-router'

// Layouts
import AdminLayout from '../layouts/AdminLayout.vue'

// Auth Feature
import LoginView from '../features/auth/views/LoginView.vue'

// Role Portals
import StudentPortalView from '../features/alumnos/views/StudentPortalView.vue'
import SupervisorPortalView from '../features/supervisor/views/SupervisorPortalView.vue'
import DocentePortalView from '../features/docentes/views/DocentePortalView.vue'

// Admin Features
import AdminDashboardView from '../features/dashboard/views/AdminDashboardView.vue'
import AdminUsuariosView from '../features/usuarios/views/AdminUsuariosView.vue'
import AdminRolesView from '../features/usuarios/views/AdminRolesView.vue'
import AdminAlumnosView from '../features/alumnos/views/AdminAlumnosView.vue'
import AdminDocentesView from '../features/docentes/views/AdminDocentesView.vue'
import AdminTalleresView from '../features/talleres/views/AdminTalleresView.vue'
import AdminHorariosView from '../features/horarios/views/AdminHorariosView.vue'
import AdminInscripcionesView from '../features/inscripciones/views/AdminInscripcionesView.vue'
import AdminAsistenciasView from '../features/asistencias/views/AdminAsistenciasView.vue'
import AdminPagosView from '../features/pagos/views/AdminPagosView.vue'
import AdminMonetizacionView from '../features/reportes/views/AdminMonetizacionView.vue'
import AdminReportesView from '../features/reportes/views/AdminReportesView.vue'
import AdminConfiguracionView from '../features/reportes/views/AdminConfiguracionView.vue'
import AdminBitacoraView from '../features/reportes/views/AdminBitacoraView.vue'

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
      component: StudentPortalView,
    },
    {
      path: '/supervisor',
      name: 'supervisor',
      component: SupervisorPortalView,
    },
    {
      path: '/docente',
      name: 'docente',
      component: DocentePortalView,
      meta: {
        title: 'Portal del Docente',
        subtitle: 'Panel de Control e Instrucción',
        requiresTeacher: true
      }
    },
    {
      path: '/super-admin',
      redirect: '/admin/dashboard',
    },
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      meta: { requiresAdmin: true },
      children: [
        {
          path: 'dashboard',
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
          path: 'usuarios',
          name: 'admin-users',
          component: AdminUsuariosView,
          meta: {
            title: 'Usuarios',
            subtitle: 'Gestión de cuentas y accesos del sistema',
            type: 'users',
            requiresAdmin: true
          }
        },
        {
          path: 'roles',
          name: 'admin-roles',
          component: AdminRolesView,
          meta: {
            title: 'Roles y Permisos',
            subtitle: 'Control de accesos y permisos',
            type: 'roles',
            requiresAdmin: true
          }
        },
        {
          path: 'alumnos',
          name: 'admin-students',
          component: AdminAlumnosView,
          meta: {
            title: 'Alumnos',
            subtitle: 'Padrón de estudiantes inscritos',
            type: 'students',
            requiresAdmin: true
          }
        },
        {
          path: 'docentes',
          name: 'admin-teachers',
          component: AdminDocentesView,
          meta: {
            title: 'Docentes',
            subtitle: 'Plantilla de instructores y talleristas',
            type: 'teachers',
            requiresAdmin: true
          }
        },
        {
          path: 'talleres',
          name: 'admin-workshops',
          component: AdminTalleresView,
          meta: {
            title: 'Talleres',
            subtitle: 'Cursos y talleres culturales ofertados',
            type: 'workshops',
            requiresAdmin: true
          }
        },
        {
          path: 'horarios',
          name: 'admin-schedules',
          component: AdminHorariosView,
          meta: {
            title: 'Horarios',
            subtitle: 'Programación semanal de talleres',
            type: 'schedules',
            requiresAdmin: true
          }
        },
        {
          path: 'inscripciones',
          name: 'admin-registrations',
          component: AdminInscripcionesView,
          meta: {
            title: 'Inscripciones',
            subtitle: 'Control de matrículas en grupos',
            type: 'registrations',
            requiresAdmin: true
          }
        },
        {
          path: 'asistencias',
          name: 'admin-attendance',
          component: AdminAsistenciasView,
          meta: {
            title: 'Asistencias',
            subtitle: 'Registro diario y control de faltas',
            type: 'attendance',
            requiresAdmin: true
          }
        },
        {
          path: 'pagos',
          name: 'admin-payments',
          component: AdminPagosView,
          meta: {
            title: 'Pagos',
            subtitle: 'Control de cuotas y mensualidades',
            type: 'payments',
            requiresAdmin: true
          }
        },
        {
          path: 'monetizacion',
          name: 'admin-monetization',
          component: AdminMonetizacionView,
          meta: {
            title: 'Monetización',
            subtitle: 'Ingresos y finanzas del centro cultural',
            type: 'monetization',
            requiresAdmin: true
          }
        },
        {
          path: 'reportes',
          name: 'admin-reports',
          component: AdminReportesView,
          meta: {
            title: 'Reportes y Gráficas',
            subtitle: 'Estadísticas e informes analíticos',
            type: 'reports',
            requiresAdmin: true
          }
        },
        {
          path: 'configuracion',
          name: 'admin-settings',
          component: AdminConfiguracionView,
          meta: {
            title: 'Configuración',
            subtitle: 'Parámetros generales de la institución',
            type: 'settings',
            requiresAdmin: true
          }
        },
        {
          path: 'bitacora',
          name: 'admin-logs',
          component: AdminBitacoraView,
          meta: {
            title: 'Bitácora de Actividad',
            subtitle: 'Auditoría y registro de eventos',
            type: 'logs',
            requiresAdmin: true
          }
        }
      ]
    }
  ]
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

  if (user?.debeCambiarPassword && to.path !== '/') {
    return '/'
  }

  if (to.path === '/docente' && role !== 'DOCENTE') {
    return role === 'SUPERVISOR' ? '/supervisor' : role === 'ALUMNO' ? '/alumno' : role === 'SUPER_ADMIN' ? '/admin/dashboard' : '/'
  }

  if (to.path === '/alumno' && role !== 'ALUMNO') {
    return role === 'DOCENTE' ? '/docente' : role === 'SUPERVISOR' ? '/supervisor' : role === 'SUPER_ADMIN' ? '/admin/dashboard' : '/'
  }

  if (to.path === '/supervisor' && role !== 'SUPERVISOR') {
    return role === 'DOCENTE' ? '/docente' : role === 'ALUMNO' ? '/alumno' : role === 'SUPER_ADMIN' ? '/admin/dashboard' : '/'
  }

  if ((to.path.startsWith('/admin') || to.path === '/super-admin') && role !== 'SUPER_ADMIN') {
    return role === 'DOCENTE' ? '/docente' : role === 'SUPERVISOR' ? '/supervisor' : role === 'ALUMNO' ? '/alumno' : '/'
  }

  return true
})

export default router
