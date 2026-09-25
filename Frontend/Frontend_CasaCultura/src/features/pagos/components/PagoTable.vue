<script setup>
import { ref } from 'vue'
import {
  MoreVertical,
  Check
} from 'lucide-vue-next'

const props = defineProps({
  pagos: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['mark-as-paid'])

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
          <th>Concepto y Monto</th>
          <th>Taller / Periodo</th>
          <th>Estado</th>
          <th style="text-align: right; padding-right: 20px;">Acciones</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="item in pagos"
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
            <strong class="text-success">${{ item.monto?.toLocaleString() || '0' }} MXN</strong>
            <span class="cell-sub-block">{{ item.tipoPago || 'PAGO' }}</span>
          </td>

          <td>
            <strong>{{ item.taller || item.grupo || 'Taller Cultural' }}</strong>
            <span class="cell-sub-block">Periodo: {{ item.periodo || 'General' }}</span>
          </td>

          <td>
            <span
              class="status"
              :class="{
                paid: item.estado === 'PAGADO',
                inactive: item.estado === 'VENCIDO',
                pending: item.estado === 'PENDIENTE'
              }"
            >
              {{ item.estado }}
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
                v-if="item.estado !== 'PAGADO'"
                @click="emit('mark-as-paid', item.id); activeActionMenu = null"
              >
                <Check :size="15" /> Marcar como Pagado
              </button>
            </div>
          </td>
        </tr>

        <tr v-if="pagos.length === 0">
          <td colspan="5" class="empty-table-cell">
            No se encontraron cobros registrados.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
