import { ref } from 'vue'

export const isMobileSidebarOpen = ref(false)

export function toggleAdminSidebar() {
  isMobileSidebarOpen.value = !isMobileSidebarOpen.value
}

export function closeAdminSidebar() {
  isMobileSidebarOpen.value = false
}
