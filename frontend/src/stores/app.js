import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const searchKeyword = ref('')

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setSearchKeyword = (keyword) => {
    searchKeyword.value = keyword
  }

  return { sidebarCollapsed, searchKeyword, toggleSidebar, setSearchKeyword }
})
