<script setup>
import { computed, onMounted, ref } from 'vue'
import DashboardLayout from '../components/DashboardLayout.vue'
import { clearStoredSession } from '../services/apiService'
import { getStudentDashboard } from '../services/studentService'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeView = ref('resumen')
const dashboard = ref(null)
const errorMessage = ref('')
const isLoading = ref(true)

const navItems = [
  { id: 'resumen', label: 'Mi Dashboard', icon: '▦' },
  { id: 'cursos', label: 'Mis Cursos', icon: '▣' },
  { id: 'horarios', label: 'Mis Horarios', icon: '▦' },
  { id: 'asistencias', label: 'Asistencias', icon: '✓' },
  { id: 'pagos', label: 'Mis Pagos', icon: '$' },
  { id: 'credencial', label: 'Mi Credencial', icon: '▤' },
]

const username = computed(() => dashboard.value?.alumno?.matricula || 'Alumno')
const activeTitle = computed(() => navItems.find((item) => item.id === activeView.value)?.label)

onMounted(async () => {
  try {
    dashboard.value = await getStudentDashboard()
  } catch (error) {
    errorMessage.value = error.message
    if (error.message.includes('401') || error.message.includes('403')) {
      clearStoredSession()
      router.push('/')
    }
  } finally {
    isLoading.value = false
  }
})

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium' }).format(new Date(`${value}T00:00:00`))
}

function statusClass(value) {
  return String(value || '').toLowerCase()
}
</script>

<template>
  <DashboardLayout
    role="ALUMNO"
    :username="username"
    :items="navItems"
    :active="activeView"
    @navigate="activeView = $event"
  >
    <section class="page-heading">
      <div>
        <span class="eyebrow">PORTAL DEL ALUMNO</span>
        <h1>{{ activeTitle }}</h1>
        <p>Consulta tu información académica y administrativa.</p>
      </div>
      <div class="heading-icon">♙</div>
    </section>

    <div v-if="isLoading" class="state-card">Cargando tu información...</div>
    <div v-else-if="errorMessage" class="state-card error-state">{{ errorMessage }}</div>

    <template v-else-if="dashboard">
      <template v-if="activeView === 'resumen'">
        <div class="welcome-card">
          <div>
            <span class="eyebrow">BIENVENIDO DE NUEVO</span>
            <h2>{{ dashboard.alumno.nombre }} {{ dashboard.alumno.apellidoPaterno }}</h2>
            <p>Desde aquí puedes consultar tus cursos, horarios, asistencias y pagos.</p>
          </div>
          <div class="welcome-avatar">♙</div>
        </div>

        <div class="stat-grid">
          <article><span class="stat-icon purple">▣</span><strong>{{ dashboard.inscripciones.length }}</strong><small>Cursos inscritos</small></article>
          <article><span class="stat-icon blue">▦</span><strong>{{ dashboard.horarios.length }}</strong><small>Horarios activos</small></article>
          <article><span class="stat-icon green">✓</span><strong>{{ dashboard.asistencias.length }}</strong><small>Registros de asistencia</small></article>
          <article><span class="stat-icon orange">$</span><strong>{{ dashboard.pagos.length }}</strong><small>Pagos registrados</small></article>
        </div>

        <div class="content-grid">
          <section class="panel">
            <div class="panel-heading"><div><h2>Mis cursos</h2><p>Cursos en los que estás inscrito actualmente.</p></div><button type="button" @click="activeView = 'cursos'">Ver todos</button></div>
            <div v-if="dashboard.inscripciones.length" class="course-list">
              <div v-for="course in dashboard.inscripciones.slice(0, 3)" :key="course.id" class="course-row">
                <span class="course-icon">▣</span>
                <div><strong>{{ course.grupo }}</strong><small>Matrícula {{ course.matricula }} · {{ course.estado }}</small></div>
                <span class="status active">Activo</span>
              </div>
            </div>
            <p v-else class="empty-text">No tienes cursos inscritos.</p>
          </section>
          <section class="panel">
            <div class="panel-heading"><div><h2>Últimas asistencias</h2><p>Historial reciente.</p></div><button type="button" @click="activeView = 'asistencias'">Ver historial</button></div>
            <div v-if="dashboard.asistencias.length" class="attendance-list">
              <div v-for="attendance in dashboard.asistencias.slice(0, 4)" :key="attendance.id" class="attendance-row">
                <span class="date-box">{{ new Date(`${attendance.fecha}T00:00:00`).getDate() }}</span>
                <div><strong>{{ attendance.grupo }}</strong><small>{{ formatDate(attendance.fecha) }}</small></div>
                <span class="status" :class="statusClass(attendance.estado)">{{ attendance.estado }}</span>
              </div>
            </div>
            <p v-else class="empty-text">Aún no hay registros de asistencia.</p>
          </section>
        </div>
      </template>

      <section v-else-if="activeView === 'cursos'" class="panel full-panel">
        <div class="panel-heading"><div><h2>Cursos inscritos</h2><p>Consulta el estado de tus inscripciones.</p></div></div>
        <div class="table-wrap"><table><thead><tr><th>Curso / grupo</th><th>Matrícula</th><th>Fecha de inscripción</th><th>Estado</th></tr></thead><tbody><tr v-for="course in dashboard.inscripciones" :key="course.id"><td>{{ course.grupo }}</td><td>{{ course.matricula }}</td><td>{{ formatDate(course.fechaInscripcion) }}</td><td><span class="status active">{{ course.estado }}</span></td></tr></tbody></table></div>
      </section>

      <section v-else-if="activeView === 'horarios'" class="panel full-panel">
        <div class="panel-heading"><div><h2>Mis horarios</h2><p>Consulta los días y horarios de tus cursos.</p></div></div>
        <div class="schedule-grid"><article v-for="schedule in dashboard.horarios" :key="schedule.id"><span class="schedule-day">{{ schedule.dia }}</span><strong>{{ schedule.nombreGrupo }}</strong><small>{{ schedule.horaInicio }} - {{ schedule.horaFin }}</small></article></div>
      </section>

      <section v-else-if="activeView === 'asistencias'" class="panel full-panel">
        <div class="panel-heading"><div><h2>Historial de asistencia</h2><p>Todos tus registros de asistencia.</p></div></div>
        <div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Grupo</th><th>Hora de registro</th><th>Estado</th></tr></thead><tbody><tr v-for="attendance in dashboard.asistencias" :key="attendance.id"><td>{{ formatDate(attendance.fecha) }}</td><td>{{ attendance.grupo }}</td><td>{{ attendance.horaRegistro?.slice(11, 16) || '-' }}</td><td><span class="status" :class="statusClass(attendance.estado)">{{ attendance.estado }}</span></td></tr></tbody></table></div>
      </section>

      <section v-else-if="activeView === 'pagos'" class="panel full-panel">
        <div class="panel-heading"><div><h2>Historial de pagos</h2><p>Consulta el estado de tus pagos e inscripciones.</p></div></div>
        <div class="table-wrap"><table><thead><tr><th>Concepto</th><th>Periodo</th><th>Vencimiento</th><th>Fecha de pago</th><th>Estado</th></tr></thead><tbody><tr v-for="payment in dashboard.pagos" :key="payment.id"><td>{{ payment.tipoPago }}</td><td>{{ payment.periodo || '-' }}</td><td>{{ formatDate(payment.fechaVencimiento) }}</td><td>{{ formatDate(payment.fechaPago) }}</td><td><span class="status" :class="statusClass(payment.estado)">{{ payment.estado }}</span></td></tr></tbody></table></div>
      </section>

      <section v-else class="credential-card">
        <div class="credential-header"><span class="eyebrow">IDENTIFICACIÓN DIGITAL</span><h2>Mi credencial</h2><p>Presenta esta información para identificarte en la Casa de la Cultura.</p></div>
        <div class="credential-body"><div class="credential-photo">♙</div><div><strong>{{ dashboard.alumno.nombre }} {{ dashboard.alumno.apellidoPaterno }} {{ dashboard.alumno.apellidoMaterno }}</strong><span>Matrícula</span><b>{{ dashboard.alumno.matricula }}</b><span>Correo</span><b>{{ dashboard.alumno.correo || 'No registrado' }}</b></div><div class="credential-code">{{ dashboard.alumno.matricula }}</div></div>
      </section>
    </template>
  </DashboardLayout>
</template>

<style scoped>
.page-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.eyebrow { color: #5363e5; font-size: 9px; font-weight: 800; letter-spacing: .03em; }
h1, h2, p { margin: 0; }
h1 { margin-top: 3px; color: #252b44; font-size: 23px; }
.page-heading p, .panel-heading p { margin-top: 4px; color: #9299aa; font-size: 10px; }
.heading-icon, .welcome-avatar { display: grid; place-items: center; border-radius: 10px; color: #5664e1; background: #eef0ff; }
.heading-icon { width: 43px; height: 43px; font-size: 22px; }
.welcome-card { display: flex; justify-content: space-between; align-items: center; min-height: 130px; margin-bottom: 16px; padding: 22px 27px; border: 1px solid #e3e6ef; border-radius: 8px; background: #fff; }
.welcome-card h2 { margin-top: 7px; font-size: 19px; }
.welcome-card p { margin-top: 7px; color: #858da1; font-size: 11px; }
.welcome-avatar { width: 76px; height: 76px; border-radius: 50%; font-size: 35px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 13px; margin-bottom: 16px; }
.stat-grid article { display: grid; grid-template-columns: 37px 1fr; column-gap: 10px; align-items: center; padding: 15px; border: 1px solid #e4e7ef; border-radius: 8px; background: #fff; }
.stat-icon { grid-row: span 2; width: 37px; height: 37px; display: grid; place-items: center; border-radius: 8px; font-size: 19px; }
.stat-icon.purple { color: #636fe4; background: #eeefff; } .stat-icon.blue { color: #3a91d4; background: #e9f5ff; } .stat-icon.green { color: #36a378; background: #e8f8f1; } .stat-icon.orange { color: #d58b46; background: #fff3e7; }
.stat-grid strong { font-size: 20px; } .stat-grid small { color: #9299aa; font-size: 9px; }
.content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel, .credential-card, .state-card { border: 1px solid #e2e5ed; border-radius: 8px; background: #fff; }
.panel { padding: 18px 16px; } .full-panel { min-height: 280px; }
.panel-heading { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 15px; }
.panel-heading h2 { font-size: 13px; } .panel-heading button { border: 0; color: #5664df; background: transparent; font-size: 10px; cursor: pointer; }
.course-list, .attendance-list { display: grid; gap: 10px; }
.course-row, .attendance-row { display: flex; align-items: center; gap: 10px; padding: 10px; border: 1px solid #edf0f5; border-radius: 6px; }
.course-icon { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 7px; color: #5967df; background: #eff0ff; }
.course-row div, .attendance-row div { flex: 1; } .course-row strong, .attendance-row strong { display: block; font-size: 11px; } .course-row small, .attendance-row small { color: #969dad; font-size: 9px; }
.status { padding: 4px 7px; border-radius: 10px; font-size: 8px; font-weight: 800; text-transform: capitalize; }
.status.active, .status.activa, .status.pagado, .status.presente { color: #19865d; background: #e5f7ef; }
.status.baja, .status.vencido, .status.falta { color: #bc5361; background: #ffebee; }
.status.retardo, .status.pendiente { color: #b2782d; background: #fff3dd; }
.date-box { width: 31px; height: 31px; display: grid; place-items: center; border-radius: 6px; color: #5967df; background: #eff0ff; font-size: 13px; font-weight: 800; }
.empty-text, .state-card { color: #9299aa; font-size: 11px; } .state-card { padding: 25px; } .error-state { color: #a82d43; }
.table-wrap { overflow-x: auto; } table { width: 100%; border-collapse: collapse; font-size: 10px; } th { color: #8d95a7; font-size: 9px; text-align: left; } th, td { padding: 12px 8px; border-bottom: 1px solid #eef0f5; white-space: nowrap; } td { color: #4c556c; }
.schedule-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; } .schedule-grid article { display: grid; gap: 8px; padding: 17px; border: 1px solid #edf0f5; border-radius: 7px; } .schedule-day { color: #5664df; font-size: 10px; font-weight: 800; } .schedule-grid strong { font-size: 12px; } .schedule-grid small { color: #9299aa; font-size: 10px; }
.credential-card { max-width: 650px; overflow: hidden; } .credential-header { padding: 22px 25px; border-bottom: 1px solid #edf0f5; } .credential-header h2 { margin-top: 5px; font-size: 21px; } .credential-header p { margin-top: 6px; color: #9299aa; font-size: 11px; } .credential-body { display: flex; align-items: center; gap: 18px; padding: 25px; } .credential-photo { width: 70px; height: 70px; display: grid; place-items: center; border-radius: 50%; color: #5664df; background: #eef0ff; font-size: 32px; } .credential-body strong { display: block; margin-bottom: 8px; font-size: 14px; } .credential-body span, .credential-body b { display: block; font-size: 9px; } .credential-body span { margin-top: 5px; color: #9299aa; } .credential-body b { color: #4d5872; } .credential-code { margin-left: auto; padding: 18px 10px; border: 1px dashed #b7bce5; color: #5664df; font-size: 12px; font-weight: 800; writing-mode: vertical-rl; }
@media (max-width: 900px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } .content-grid { grid-template-columns: 1fr; } }
@media (max-width: 580px) { .stat-grid { grid-template-columns: 1fr 1fr; } .welcome-card { padding: 16px; } .welcome-avatar { width: 52px; height: 52px; font-size: 25px; } .schedule-grid { grid-template-columns: 1fr; } .credential-body { align-items: flex-start; flex-wrap: wrap; } .credential-code { margin-left: 0; } }
</style>
