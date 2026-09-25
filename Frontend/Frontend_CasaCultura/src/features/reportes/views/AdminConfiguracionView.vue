<script setup>
import {
  Settings,
  ShieldCheck,
  QrCode,
  Building
} from 'lucide-vue-next'
import { useAdminData } from '../../admin/composables/useAdminData.js'

const { showToast } = useAdminData()
</script>

<template>
  <div class="feature-view">
    <!-- Header Row -->
    <div class="page-title">
      <div class="title-with-icon">
        <div class="title-icon">
          <Settings :size="28" />
        </div>
        <div>
          <h1>Configuración</h1>
          <p>Parámetros generales de la institución</p>
        </div>
      </div>
    </div>

    <div class="special-panel">
      <h2>Configuración General del Sistema</h2>
      <p>Parámetros institucionales de SIGEC Casa de la Cultura de Tlaxiaco.</p>

      <div class="special-grid">
        <article class="special-card">
          <Building :size="28" />
          <h3>Datos de la Institución</h3>
          <p>Casa de la Cultura de Tlaxiaco · Heroica Ciudad de Tlaxiaco, Oaxaca.</p>
          <button @click="showToast('Información institucional verificada.')">
            Editar información
          </button>
        </article>

        <article class="special-card">
          <ShieldCheck :size="28" />
          <h3>Seguridad y Accesos JWT</h3>
          <p>Políticas de contraseñas seguras y caducidad de tokens activa.</p>
          <button @click="showToast('Seguridad JWT configurada.')">
            Configurar
          </button>
        </article>

        <article class="special-card">
          <QrCode :size="28" />
          <h3>Notificaciones y Credenciales</h3>
          <p>Generación digital de credenciales con código QR activada.</p>
          <button @click="showToast('Generador de credenciales QR activo.')">
            Ver estado
          </button>
        </article>
      </div>
    </div>
  </div>
</template>
