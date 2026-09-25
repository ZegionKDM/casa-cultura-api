<script setup>
import {
  Database,
  Users,
  BookOpen,
  KeyRound
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'

const { data, showToast } = useAdminData()
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <Database :size="28" />
        </div>
        <div>
          <h1>Bitácora de Actividad</h1>
          <p>Auditoría y registro de eventos</p>
        </div>
      </div>
    </div>

    <div class="special-panel">
      <h2>Bitácora de Operaciones</h2>
      <p>Eventos del sistema y sincronización de datos con el backend Spring Boot.</p>

      <div class="special-grid">
        <article class="special-card">
          <Users :size="28" />
          <h3>Auditoría de Usuarios</h3>
          <p>{{ data.usuarios.length }} cuentas administrativas y operativas activas.</p>
          <button @click="showToast('Auditoría de usuarios sincronizada.')">
            Ver auditoría
          </button>
        </article>

        <article class="special-card">
          <BookOpen :size="28" />
          <h3>Sincronización de Catálogo</h3>
          <p>{{ data.cursos.length }} cursos y {{ data.grupos.length }} grupos verificados en base de datos.</p>
          <button @click="showToast('Catálogo sincronizado.')">
            Ver catálogo
          </button>
        </article>

        <article class="special-card">
          <KeyRound :size="28" />
          <h3>Seguridad de Contraseñas</h3>
          <p>Cifrado BCrypt y autorización basada en roles (RBAC) activa.</p>
          <button @click="showToast('Políticas de seguridad vigentes.')">
            Revisar políticas
          </button>
        </article>
      </div>
    </div>
  </div>
</template>
