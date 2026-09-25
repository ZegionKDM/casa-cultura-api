<script setup>
import { onMounted } from 'vue'
import { CheckCircle2, AlertCircle, X } from 'lucide-vue-next'
import AdminSidebar from '../components/admin/AdminSidebar.vue'
import AdminHeader from '../components/admin/AdminHeader.vue'
import { useAdminData } from '../features/admin/composables/useAdminData.js'

const { toast, loadAllData } = useAdminData()

onMounted(() => {
  loadAllData()
})
</script>

<template>
  <div class="admin-layout">
    <AdminSidebar />

    <main class="main-content">
      <AdminHeader />

      <section class="module-content">
        <!-- Global Toast feedback for admin modules -->
        <transition name="fade">
          <div
            v-if="toast.show"
            class="toast-alert"
            :class="toast.type"
          >
            <CheckCircle2
              v-if="toast.type === 'success'"
              :size="18"
            />
            <AlertCircle
              v-else
              :size="18"
            />
            <span>{{ toast.message }}</span>
            <button
              class="toast-close"
              @click="toast.show = false"
            >
              <X :size="14" />
            </button>
          </div>
        </transition>

        <router-view />
      </section>
    </main>
  </div>
</template>

<style src="../assets/styles/admin-module.css"></style>
