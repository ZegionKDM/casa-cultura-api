<script setup>
import { QrCode, Printer, X, User } from 'lucide-vue-next'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  credentialInfo: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close'])

function handlePrint() {
  window.print()
}
</script>

<template>
  <div v-if="show && credentialInfo" class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <h3>Credencial Digital Estudiantil</h3>
        <button
          class="close-btn"
          type="button"
          aria-label="Cerrar modal"
          @click="emit('close')"
        >
          <X :size="18" />
        </button>
      </div>

      <div class="modal-body">
        <div class="credential-preview">
          <div class="cred-header">
            <h4>CASA DE LA CULTURA DE TLAXIACO</h4>
            <span>Credencial Digital Estudiantil</span>
          </div>

          <div class="cred-body">
            <div class="cred-photo-box">
              <img
                v-if="credentialInfo.fotoUrl"
                :src="credentialInfo.fotoUrl"
                alt="Fotografía del alumno"
                class="cred-photo-img"
              />
              <div v-else class="cred-photo-placeholder">
                <User :size="36" />
              </div>
            </div>

            <div class="cred-qr">
              <div class="qr-box">
                <QrCode :size="110" />
              </div>
              <small>{{ credentialInfo.matricula }}</small>
            </div>

            <div class="cred-info">
              <strong>{{ credentialInfo.nombreCompleto }}</strong>
              <p><strong>Matrícula:</strong> {{ credentialInfo.matricula }}</p>
              <p><strong>Vigencia:</strong> {{ credentialInfo.vigencia || 'Ciclo 2026' }}</p>
              <span class="cred-badge">ESTUDIANTE ACTIVO</span>
            </div>
          </div>
        </div>

        <div class="cred-actions">
          <button
            type="button"
            class="secondary-button"
            @click="handlePrint"
          >
            <Printer :size="16" /> Imprimir Credencial
          </button>
        </div>
      </div>
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
  max-width: 520px;
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

.modal-header h3 {
  margin: 0;
  font-size: 17px;
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
  padding: 24px;
}

.credential-preview {
  border: 2px solid #4051a3;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;
  margin-bottom: 20px;
}

.cred-header {
  background: #4051a3;
  color: white;
  padding: 14px 18px;
  text-align: center;
}

.cred-header h4 {
  margin: 0;
  font-size: 15px;
  letter-spacing: 0.5px;
}

.cred-header span {
  font-size: 11px;
  opacity: 0.9;
}

.cred-body {
  display: flex;
  padding: 20px;
  gap: 16px;
  align-items: center;
}

.cred-photo-box {
  width: 90px;
  height: 110px;
  flex-shrink: 0;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  overflow: hidden;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cred-photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cred-photo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #94a3b8;
}

.cred-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.qr-box {
  padding: 8px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #1e293b;
}

.cred-info {
  flex: 1;
}

.cred-info strong {
  display: block;
  font-size: 15px;
  color: #0f172a;
  margin-bottom: 6px;
}

.cred-info p {
  margin: 4px 0;
  font-size: 12px;
  color: #475569;
}

.cred-badge {
  display: inline-block;
  background: #dcfce7;
  color: #15803d;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 700;
  margin-top: 6px;
}

.cred-actions {
  display: flex;
  justify-content: center;
}

.secondary-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: white;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.secondary-button:hover {
  background: #f8fafc;
  border-color: #94a3b8;
}
</style>
