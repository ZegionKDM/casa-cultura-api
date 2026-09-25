<script setup>
import { ref, watch } from 'vue'
import { KeyRound, X } from 'lucide-vue-next'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  isSaving: {
    type: Boolean,
    default: false
  },
  errorMessage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'confirm'])

const password = ref('')
const localError = ref('')

watch(() => props.show, (newVal) => {
  if (newVal) {
    password.value = ''
    localError.value = ''
  }
})

function handleSubmit() {
  const val = password.value.trim()
  if (!val) {
    localError.value = 'La nueva contraseña es obligatoria'
    return
  }
  if (val.length < 8) {
    localError.value = 'La contraseña debe contener al menos 8 caracteres'
    return
  }
  localError.value = ''
  emit('confirm', val)
}
</script>

<template>
  <div v-if="show" class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <div class="header-title">
          <KeyRound :size="20" class="header-icon" />
          <h3>Restablecer Contraseña</h3>
        </div>
        <button
          class="close-btn"
          type="button"
          aria-label="Cerrar modal"
          @click="emit('close')"
        >
          <X :size="18" />
        </button>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <p class="modal-help-text">
            Ingresa la nueva contraseña para el usuario seleccionado.
          </p>

          <div class="field">
            <label for="new-pass-input">Nueva Contraseña * (Mínimo 8 caracteres)</label>
            <input
              id="new-pass-input"
              v-model="password"
              type="password"
              placeholder="••••••••"
              autocomplete="new-password"
              :class="{ 'has-error': localError || errorMessage }"
            />
            <span v-if="localError || errorMessage" class="error-text">
              {{ localError || errorMessage }}
            </span>
          </div>
        </div>

        <div class="modal-footer">
          <button
            type="button"
            class="cancel-button"
            :disabled="isSaving"
            @click="emit('close')"
          >
            Cancelar
          </button>
          <button
            type="submit"
            class="submit-button"
            :disabled="isSaving"
          >
            {{ isSaving ? 'Guardando...' : 'Restablecer Contraseña' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  padding: 16px;
}

.modal-card {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  color: #4f46e5;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.close-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  border-radius: 8px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.modal-body {
  padding: 20px 24px;
}

.modal-help-text {
  margin: 0 0 16px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.field input {
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.field input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.field input.has-error {
  border-color: #ef4444;
}

.error-text {
  font-size: 12px;
  color: #ef4444;
  margin-top: 2px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.cancel-button {
  padding: 9px 16px;
  background: white;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-button:hover:not(:disabled) {
  background: #f1f5f9;
}

.submit-button {
  padding: 9px 18px;
  background: #4f46e5;
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-button:hover:not(:disabled) {
  background: #4338ca;
}

.submit-button:disabled, .cancel-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
