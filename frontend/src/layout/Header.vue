<template>
  <header class="header">
    <div class="header-left">
      <el-button text @click="appStore.toggleSidebar">
        <el-icon :size="20"><Fold /></el-icon>
      </el-button>
    </div>
    <div class="header-center">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索任务..."
        :prefix-icon="Search"
        clearable
        class="search-input"
        @keyup.enter="doSearch"
      />
    </div>
    <div class="header-right">
      <el-button text @click="router.push('/calendar')">
        <el-icon><Calendar /></el-icon>
        <span style="margin-left: 4px;">日历</span>
      </el-button>
      <el-button text @click="router.push('/projects')">
        <el-icon><Collection /></el-icon>
        <span style="margin-left: 4px;">项目</span>
      </el-button>
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="28">{{ authStore.user?.nickname?.[0] || 'U' }}</el-avatar>
          <span class="username">{{ authStore.user?.nickname || '用户' }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="info">个人信息</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const searchKeyword = ref('')

const doSearch = () => {
  if (searchKeyword.value.trim()) {
    window.dispatchEvent(new CustomEvent('search-tasks', { detail: { keyword: searchKeyword.value } }))
  }
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.header {
  height: var(--header-height);
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  gap: 16px;
}
.header-left { display: flex; align-items: center; }
.header-center { flex: 1; display: flex; justify-content: center; }
.search-input { max-width: 400px; }
.header-right { display: flex; align-items: center; gap: 8px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 13px; color: var(--text-regular); }
</style>
