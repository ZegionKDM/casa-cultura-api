<script setup>
import { ref } from 'vue'
import {
  MoreVertical,
  UserPlus,
  Pencil,
  Trash2
} from 'lucide-vue-next'

const props = defineProps({
  docentes: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['edit', 'deactivate', 'assign'])

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
          <th>Docente</th>
          <th>Especialidad</th>
          <th>Contacto</th>
          <th>Estado</th>
          <th style="text-align: right; padding-right: 20px;">Acciones</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="item in docentes"
          :key="item.id"
        >
          <td>
            <div class="user-cell">
              <div class="avatar">
                <img
                  v-if="item.fotoUrl"
                  :src="item.fotoUrl"
                  alt="Foto del docente"
                  class="avatar-img-table"
                />
                <span v-else>{{ item.nombre ? item.nombre.charAt(0).toUpperCase() : 'D' }}</span>
              </div>
              <div>
                <strong>{{ item.nombre }} {{ item.apellidoPaterno }} {{ item.apellidoMaterno || '' }}</strong>
                <span class="cell-sub">{{ item.especialidad || 'General' }}</span>
              </div>
            </div>
          </td>

          <td>{{ item.especialidad || 'Instructor' }}</td>

          <td>{{ item.correo || item.telefono || 'Sin contacto directo' }}</td>

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
              <button @click="emit('assign', item); activeActionMenu = null">
                <UserPlus :size="15" /> Asignar a Grupo
              </button>

              <button @click="emit('edit', item); activeActionMenu = null">
                <Pencil :size="15" /> Editar
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

        <tr v-if="docentes.length === 0">
          <td colspan="5" class="empty-table-cell">
            No se encontraron docentes registrados que coincidan con la búsqueda.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
