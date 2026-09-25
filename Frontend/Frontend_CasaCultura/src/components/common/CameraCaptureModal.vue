<template>
  <div v-if="show" class="camera-modal-backdrop" @click.self="handleClose">
    <div class="camera-modal-card">
      <!-- Modal Header -->
      <div class="modal-header">
        <div class="header-info">
          <div class="icon-circle">
            <Camera :size="20" />
          </div>
          <div>
            <h3>{{ title }}</h3>
            <p class="subtitle">Alinea el rostro del alumno dentro del marco guía</p>
          </div>
        </div>

        <div class="header-actions">
          <!-- Flip Camera button (if multiple video devices exist) -->
          <button
            v-if="!capturedImage && videoDevices.length > 1"
            type="button"
            class="btn-icon"
            title="Alternar cámara frontal / trasera"
            @click="toggleCamera"
          >
            <RefreshCw :size="18" />
          </button>

          <button
            type="button"
            class="btn-icon btn-close"
            title="Cerrar"
            @click="handleClose"
          >
            <X :size="20" />
          </button>
        </div>
      </div>

      <!-- Main Stage -->
      <div class="modal-body">
        <!-- Error Banner if camera failed -->
        <div v-if="cameraError" class="camera-error-banner">
          <AlertCircle :size="20" class="error-icon flex-shrink-0" />
          <div class="error-text">
            <strong>No se pudo acceder a la cámara</strong>
            <span>{{ cameraError }}</span>
          </div>
        </div>

        <!-- CAMERA LIVE VIEW -->
        <div v-show="!capturedImage" class="viewport-wrapper">
          <video
            ref="videoElement"
            autoplay
            playsinline
            muted
            class="camera-video"
            :class="{ 'mirror-mode': isFrontCamera }"
          ></video>

          <!-- Framing Guide Overlay (Oval for Credential Photo) -->
          <div class="framing-overlay">
            <div class="oval-guide">
              <span class="guide-text">Centra aquí el rostro</span>
            </div>
          </div>

          <!-- Flash Effect -->
          <div v-if="isFlashing" class="camera-flash"></div>

          <!-- Loading Spinner while camera connects -->
          <div v-if="isLoadingCamera && !cameraError" class="camera-loading">
            <div class="spinner"></div>
            <span>Conectando cámara...</span>
          </div>
        </div>

        <!-- PREVIEW MODE (Photo snapped) -->
        <div v-if="capturedImage" class="viewport-wrapper preview-mode">
          <img :src="capturedImage" alt="Vista previa de foto" class="preview-image" />
          <div class="preview-badge">
            <Sparkles :size="14" />
            <span>Fotografía capturada</span>
          </div>
        </div>

        <!-- Hidden canvas for rendering image -->
        <canvas ref="canvasElement" class="hidden-canvas"></canvas>

        <!-- Hidden file input for gallery/disk selection or native mobile capture -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          class="hidden-input"
          @change="onFileSelected"
        />
        <input
          ref="nativeCameraInputRef"
          type="file"
          accept="image/*"
          capture="user"
          class="hidden-input"
          @change="onFileSelected"
        />
      </div>

      <!-- Modal Footer Controls -->
      <div class="modal-footer">
        <!-- Controls during Camera Mode -->
        <template v-if="!capturedImage">
          <div class="footer-left">
            <button
              type="button"
              class="btn-secondary"
              @click="triggerFileInput"
            >
              <Upload :size="16" />
              <span>Subir de mis archivos</span>
            </button>
          </div>

          <div class="footer-center">
            <!-- Shutter Button -->
            <button
              type="button"
              class="btn-shutter"
              :disabled="isLoadingCamera || !!cameraError"
              title="Tomar foto ahora"
              @click="captureSnapshot"
            >
              <div class="shutter-inner">
                <Camera :size="24" />
              </div>
            </button>
          </div>

          <div class="footer-right">
            <button
              type="button"
              class="btn-text"
              @click="handleClose"
            >
              Cancelar
            </button>
          </div>
        </template>

        <!-- Controls during Preview Mode -->
        <template v-else>
          <div class="footer-left">
            <button
              type="button"
              class="btn-secondary"
              :disabled="isUploading"
              @click="retakePhoto"
            >
              <RotateCcw :size="16" />
              <span>Volver a tomar</span>
            </button>
          </div>

          <div class="footer-right actions-right">
            <button
              type="button"
              class="btn-text"
              :disabled="isUploading"
              @click="handleClose"
            >
              Cancelar
            </button>
            <button
              type="button"
              class="btn-confirm"
              :disabled="isUploading"
              @click="confirmPhoto"
            >
              <div v-if="isUploading" class="spinner-small"></div>
              <Check v-else :size="18" />
              <span>{{ isUploading ? 'Guardando foto...' : 'Aceptar y usar foto' }}</span>
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  Camera,
  X,
  RefreshCw,
  Upload,
  AlertCircle,
  Sparkles,
  RotateCcw,
  Check
} from 'lucide-vue-next'
import { uploadStudentPhoto, uploadTeacherPhoto } from '@/services/archivoService'

const props = defineProps({
  show: {
    type: Boolean,
    default: true
  },
  title: {
    type: String,
    default: 'Tomar Fotografía'
  },
  entityType: {
    type: String,
    default: 'alumno' // 'alumno' | 'docente'
  },
  studentId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['close', 'photo-saved', 'saved', 'error'])

// Element refs
const videoElement = ref(null)
const canvasElement = ref(null)
const fileInputRef = ref(null)
const nativeCameraInputRef = ref(null)

// State
const mediaStream = ref(null)
const isLoadingCamera = ref(false)
const cameraError = ref('')
const isFlashing = ref(false)
const isUploading = ref(false)
const capturedImage = ref(null)
const capturedBlob = ref(null)

// Camera devices & facing mode
const videoDevices = ref([])
const currentFacingMode = ref('user') // 'user' (front) or 'environment' (rear)
const isFrontCamera = ref(true)

// Lifecycle: Start camera on mount if visible
onMounted(() => {
  if (props.show) {
    resetState()
    nextTick(() => {
      startCamera()
    })
  }
})

// Watch open/close state
watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      resetState()
      nextTick(() => {
        startCamera()
      })
    } else {
      stopCamera()
    }
  }
)

onBeforeUnmount(() => {
  stopCamera()
})

function resetState() {
  capturedImage.value = null
  capturedBlob.value = null
  cameraError.value = ''
  isUploading.value = false
}

async function startCamera() {
  isLoadingCamera.value = true
  cameraError.value = ''

  // Stop any previous active track
  stopCameraTracks()

  try {
    // Check if navigator.mediaDevices is supported
    if (!navigator?.mediaDevices?.getUserMedia) {
      throw new Error('Tu navegador o dispositivo no soporta captura de video en vivo en este contexto. Puedes seleccionar una foto desde tus archivos.')
    }

    // List available video devices
    try {
      const devices = await navigator.mediaDevices.enumerateDevices()
      videoDevices.value = devices.filter((d) => d.kind === 'videoinput')
    } catch {
      videoDevices.value = []
    }

    const constraints = {
      video: {
        facingMode: currentFacingMode.value,
        width: { ideal: 1280, max: 1920 },
        height: { ideal: 720, max: 1080 }
      },
      audio: false
    }

    const stream = await navigator.mediaDevices.getUserMedia(constraints)
    mediaStream.value = stream
    isFrontCamera.value = currentFacingMode.value === 'user'

    if (videoElement.value) {
      videoElement.value.srcObject = stream
      await videoElement.value.play()
    }
  } catch (err) {
    console.warn('Error al acceder a la cámara:', err)
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      cameraError.value = 'Permiso denegado para usar la cámara. Permite el acceso a la cámara en tu navegador o selecciona una imagen desde tus archivos.'
    } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
      cameraError.value = 'No se encontró ninguna cámara conectada a este equipo. Puedes seleccionar una foto desde tus archivos.'
    } else if (err.name === 'NotReadableError') {
      cameraError.value = 'La cámara está siendo utilizada por otra aplicación. Ciérrala e intenta de nuevo.'
    } else {
      cameraError.value = err.message || 'No fue posible inicializar la cámara.'
    }
  } finally {
    isLoadingCamera.value = false
  }
}

function stopCameraTracks() {
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach((track) => track.stop())
    mediaStream.value = null
  }
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
}

function stopCamera() {
  stopCameraTracks()
  resetState()
}

function toggleCamera() {
  currentFacingMode.value = currentFacingMode.value === 'user' ? 'environment' : 'user'
  startCamera()
}

function captureSnapshot() {
  if (!videoElement.value || !canvasElement.value) return

  const video = videoElement.value
  const canvas = canvasElement.value

  // Visual flash effect
  isFlashing.value = true
  setTimeout(() => {
    isFlashing.value = false
  }, 200)

  // Target output size: 600x600 px (square credential format)
  const targetSize = 600
  canvas.width = targetSize
  canvas.height = targetSize

  const ctx = canvas.getContext('2d')

  // Calculate center square crop from video feed
  const videoWidth = video.videoWidth || 640
  const videoHeight = video.videoHeight || 480
  const minDim = Math.min(videoWidth, videoHeight)

  const startX = (videoWidth - minDim) / 2
  const startY = (videoHeight - minDim) / 2

  // If front camera, flip horizontally on canvas so it matches what user saw (mirror)
  ctx.save()
  if (isFrontCamera.value) {
    ctx.translate(targetSize, 0)
    ctx.scale(-1, 1)
  }

  ctx.drawImage(
    video,
    startX,
    startY,
    minDim,
    minDim,
    0,
    0,
    targetSize,
    targetSize
  )
  ctx.restore()

  // Convert canvas to Blob (JPEG high quality)
  canvas.toBlob(
    (blob) => {
      if (blob) {
        capturedBlob.value = blob
        capturedImage.value = URL.createObjectURL(blob)
        // Stop camera while previewing to save battery/resources
        stopCameraTracks()
      }
    },
    'image/jpeg',
    0.88
  )
}

function retakePhoto() {
  if (capturedImage.value) {
    URL.revokeObjectURL(capturedImage.value)
  }
  capturedImage.value = null
  capturedBlob.value = null
  startCamera()
}

function triggerFileInput() {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (!file) return

  // Validate type
  if (!file.type.startsWith('image/')) {
    cameraError.value = 'Por favor selecciona un archivo de imagen válido (JPG, PNG o WEBP).'
    return
  }

  // Read and load onto canvas to square-crop and optimize
  const img = new Image()
  const reader = new FileReader()

  reader.onload = (e) => {
    img.onload = () => {
      const canvas = canvasElement.value
      const targetSize = 600
      canvas.width = targetSize
      canvas.height = targetSize
      const ctx = canvas.getContext('2d')

      const minDim = Math.min(img.width, img.height)
      const startX = (img.width - minDim) / 2
      const startY = (img.height - minDim) / 2

      ctx.drawImage(img, startX, startY, minDim, minDim, 0, 0, targetSize, targetSize)

      canvas.toBlob(
        (blob) => {
          if (blob) {
            capturedBlob.value = blob
            capturedImage.value = URL.createObjectURL(blob)
            stopCameraTracks()
          }
        },
        'image/jpeg',
        0.88
      )
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(file)

  // Reset input value so change fires again if same file selected
  event.target.value = ''
}

async function confirmPhoto() {
  if (!capturedBlob.value) return

  isUploading.value = true
  try {
    const uploadFn = props.entityType === 'docente' ? uploadTeacherPhoto : uploadStudentPhoto
    const result = await uploadFn(capturedBlob.value)
    const eventPayload = {
      url: result?.url || result,
      preview: capturedImage.value
    }
    emit('photo-saved', eventPayload)
    emit('saved', eventPayload)
    handleClose()
  } catch (err) {
    cameraError.value = err.message || 'No fue posible guardar la fotografía en el servidor.'
    emit('error', cameraError.value)
  } finally {
    isUploading.value = false
  }
}

function handleClose() {
  stopCamera()
  emit('close')
}
</script>

<style scoped>
.camera-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1250;
  padding: 1rem;
  animation: fadeIn 0.2s ease-out;
}

.camera-modal-card {
  background: #ffffff;
  border-radius: 1.25rem;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid #e2e8f0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Header */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.icon-circle {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #e0e7ff;
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-info h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #0f172a;
}

.subtitle {
  margin: 0.15rem 0 0;
  font-size: 0.82rem;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-icon:hover {
  background: #f1f5f9;
  color: #0f172a;
  border-color: #cbd5e1;
}

.btn-close:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fca5a5;
}

/* Body / Viewport */
.modal-body {
  padding: 1.25rem 1.5rem;
  background: #0f172a;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.camera-error-banner {
  width: 100%;
  margin-bottom: 1rem;
  padding: 0.875rem 1rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 0.75rem;
  color: #991b1b;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  font-size: 0.85rem;
}

.error-text {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.viewport-wrapper {
  position: relative;
  width: 100%;
  max-width: 380px;
  aspect-ratio: 1 / 1;
  border-radius: 1rem;
  overflow: hidden;
  background: #000000;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-video.mirror-mode {
  transform: scaleX(-1);
}

/* Credential Oval Guide Overlay */
.framing-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 40px rgba(0, 0, 0, 0.6);
}

.oval-guide {
  width: 70%;
  height: 80%;
  border: 2px dashed rgba(255, 255, 255, 0.85);
  border-radius: 50%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 1rem;
  box-shadow: 0 0 0 9999px rgba(15, 23, 42, 0.4);
}

.guide-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: #ffffff;
  background: rgba(15, 23, 42, 0.65);
  padding: 0.25rem 0.6rem;
  border-radius: 9999px;
  letter-spacing: 0.02em;
}

/* Flash Effect */
.camera-flash {
  position: absolute;
  inset: 0;
  background: #ffffff;
  animation: flashAnim 0.2s ease-out;
  pointer-events: none;
  z-index: 20;
}

@keyframes flashAnim {
  from { opacity: 0.95; }
  to { opacity: 0; }
}

.camera-loading {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  color: #cbd5e1;
  font-size: 0.9rem;
  z-index: 10;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* Preview Mode */
.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-badge {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(4px);
  color: #38bdf8;
  padding: 0.35rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.8rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border: 1px solid rgba(56, 189, 248, 0.3);
}

/* Footer Controls */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  gap: 1rem;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.footer-right {
  justify-content: flex-end;
}

.footer-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Shutter Button */
.btn-shutter {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  border: 4px solid #e2e8f0;
  background: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.15s ease;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.btn-shutter:hover:not(:disabled) {
  border-color: #c7d2fe;
  transform: scale(1.05);
}

.btn-shutter:active:not(:disabled) {
  transform: scale(0.95);
}

.btn-shutter:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.shutter-inner {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #4338ca);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Standard Buttons */
.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  background: #f8fafc;
  color: #334155;
  border: 1px solid #cbd5e1;
  border-radius: 0.625rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #94a3b8;
  color: #0f172a;
}

.btn-confirm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  background: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 0.625rem;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25);
  transition: all 0.15s ease;
}

.btn-confirm:hover:not(:disabled) {
  background: #059669;
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.35);
}

.btn-confirm:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-text {
  background: none;
  border: none;
  color: #64748b;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  transition: all 0.15s ease;
}

.btn-text:hover:not(:disabled) {
  color: #0f172a;
  background: #f1f5f9;
}

.hidden-canvas,
.hidden-input {
  display: none;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 640px) {
  .modal-footer {
    flex-direction: column;
    align-items: stretch;
  }
  .footer-left, .footer-right, .footer-center {
    justify-content: center;
    width: 100%;
  }
  .actions-right {
    flex-direction: column-reverse;
  }
  .btn-confirm, .btn-secondary {
    width: 100%;
    justify-content: center;
  }
}
</style>
