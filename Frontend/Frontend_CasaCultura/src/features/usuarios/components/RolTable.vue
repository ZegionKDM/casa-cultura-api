<script setup>
import { ref } from 'vue'
import {
  MoreVertical,
  Pencil,
  Trash2
} from 'lucide-vue-next'

const props = defineProps({
  roles: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['edit', 'delete'])

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
          <th>Rol</th>
          <th>Descripción</th>
          <th>Permisos</th>
          <th>Estado</th>
          <th style="text-align: right; padding-right: 20px;">Acciones</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="item in roles"
          :key="item.id"
        >
          <td>
            <div class="user-cell">
              <div class="avatar">
                <span>{{ item.nombre ? item.nombre.charAt(0).toUpperCase() : 'R' }}</span>
              </div>
              <div>
                <strong>{{ item.nombre }}</strong>
                <span class="cell-sub">ID: #{{ item.id }}</span>
              </div>
            </div>
          </td>

          <td>{{ item.descripcion || 'Sin descripción detallada' }}</td>

          <td>Permisos del Sistema</td>

          <td>
            <span class="status">
              ACTIVO
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
              <button @click="emit('edit', item); activeActionMenu = null">
                <Pencil :size="15" /> Editar
              </button>

              <button
                class="delete-opt"
                @click="emit('delete', item.id); activeActionMenu = null"
              >
                <Trash2 :size="15" /> Eliminar
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="roles.length === 0">
          <td colspan="5" class="empty-table-cell">
            No se encontraron roles registrados.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
