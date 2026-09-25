<script setup>
import { ref } from 'vue'
import {
  MoreVertical,
  Trash2
} from 'lucide-vue-next'

const props = defineProps({
  inscripciones: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['deactivate'])

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
          <th>Alumno</th>
          <th>Grupo Asignado</th>
          <th>Fecha Inscripción</th>
          <th>Estado</th>
          <th style="text-align: right; padding-right: 20px;">Acciones</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="item in inscripciones"
          :key="item.id"
        >
          <td>
            <div class="user-cell">
              <div class="avatar">
                <span>{{ item.alumno ? item.alumno.charAt(0).toUpperCase() : 'A' }}</span>
              </div>
              <div>
                <strong>{{ item.alumno || 'Alumno del Centro' }}</strong>
                <span v-if="item.matricula" class="cell-sub">Mat: {{ item.matricula }}</span>
              </div>
            </div>
          </td>

          <td>
            <strong>{{ item.grupo || 'Grupo Cultural' }}</strong>
            <span v-if="item.taller" class="cell-sub-block">{{ item.taller }}</span>
          </td>

          <td>{{ item.fechaInscripcion || '-' }}</td>

          <td>
            <span
              class="status"
              :class="{
                inactive: item.estado === 'BAJA'
              }"
            >
              {{ item.estado || 'ACTIVA' }}
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
              <button
                v-if="item.estado === 'ACTIVA' || !item.estado"
                class="delete-opt"
                @click="emit('deactivate', item.id); activeActionMenu = null"
              >
                <Trash2 :size="15" /> Dar de baja inscripción
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="inscripciones.length === 0">
          <td colspan="5" class="empty-table-cell">
            No se encontraron inscripciones registradas.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
