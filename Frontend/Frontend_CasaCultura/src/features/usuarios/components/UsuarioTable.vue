<script setup>
import { ref } from 'vue'
import {
  MoreVertical,
  KeyRound,
  Trash2
} from 'lucide-vue-next'

const props = defineProps({
  usuarios: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['change-password', 'deactivate'])

const activeActionMenu = ref(null)

function toggleActionMenu(id) {
  activeActionMenu.value = activeActionMenu.value === id ? null : id
}
</script>

<template>
  <div class="table-container">
    <table class="modern-table">
      <thead>
        <tr>
          <th>Usuario</th>
          <th>Persona Asociada</th>
          <th>Rol Asignado</th>
          <th>Estado</th>
          <th style="text-align: right; padding-right: 20px;">Acciones</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="item in usuarios"
          :key="item.id"
        >
          <td>
            <div class="user-cell">
              <div class="avatar">
                <span>{{ item.nombreUsuario ? item.nombreUsuario.charAt(0).toUpperCase() : 'U' }}</span>
              </div>
              <div>
                <strong>{{ item.nombreUsuario }}</strong>
                <span class="cell-sub">{{ item.nombreRol || item.rol || 'SUPER_ADMIN' }}</span>
              </div>
            </div>
          </td>

          <td>
            {{ item.persona ? `${item.persona.nombre} ${item.persona.apellidoPaterno || ''}` : (item.correo || 'Usuario del Sistema') }}
          </td>

          <td>{{ item.rol || item.nombreRol || 'SUPER_ADMIN' }}</td>

          <td>
            <span
              class="status"
              :class="{
                inactive: ['INACTIVO', 'BAJA'].includes(item.estado)
              }"
            >
              {{ item.estado || 'ACTIVO' }}
            </span>
          </td>

          <td style="text-align: right; position: relative;">
            <button
              class="action-button"
              aria-label="Acciones"
              @click="toggleActionMenu(item.id)"
            >
              <MoreVertical :size="19" />
            </button>

            <!-- Kebab Popover Menu -->
            <div
              v-if="activeActionMenu === item.id"
              class="row-menu"
            >
              <button @click="emit('change-password', item); activeActionMenu = null">
                <KeyRound :size="15" /> Cambiar Contraseña
              </button>

              <button
                class="delete-opt"
                @click="emit('deactivate', item.id); activeActionMenu = null"
              >
                <Trash2 :size="15" /> Dar de baja
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="usuarios.length === 0">
          <td colspan="5" class="empty-table-cell">
            No se encontraron usuarios registrados.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
