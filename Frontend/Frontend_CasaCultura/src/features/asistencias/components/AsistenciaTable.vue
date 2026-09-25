<script setup>
defineProps({
  asistencias: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <div class="table-container">
    <table class="modern-table">
      <thead>
        <tr>
          <th>Alumno</th>
          <th>Taller / Grupo</th>
          <th>Fecha y Hora</th>
          <th>Estado</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="item in asistencias"
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

          <td>
            {{ item.fecha || '-' }}
            <span v-if="item.hora" class="cell-sub-block">{{ item.hora }}</span>
          </td>

          <td>
            <span
              class="status"
              :class="{
                paid: item.estado === 'PRESENTE',
                inactive: ['FALTA', 'INACTIVO'].includes(item.estado),
                pending: item.estado === 'RETARDO',
                justified: item.estado === 'JUSTIFICADA'
              }"
            >
              {{ item.estado }}
            </span>
          </td>
        </tr>

        <tr v-if="asistencias.length === 0">
          <td colspan="4" class="empty-table-cell">
            No se encontraron asistencias registradas.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
