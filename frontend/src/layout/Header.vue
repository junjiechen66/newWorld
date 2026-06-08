<template>
  <header class="header">
    <div class="header-right">
      <el-button text @click="router.push('/calendar')">
        <el-icon><Calendar /></el-icon>
        <span style="margin-left: 4px;">日历</span>
      </el-button>
      <el-button text @click="router.push('/tasks')">
        <el-icon><List /></el-icon>
        <span style="margin-left: 4px;">任务</span>
      </el-button>
      <el-button text @click="router.push('/notes')">
        <el-icon><EditPen /></el-icon>
        <span style="margin-left: 4px;">笔记</span>
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
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- User Info Dialog -->
    <el-dialog v-model="userInfoVisible" title="个人信息" width="400px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userInfoVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUserInfo">保存</el-button>
      </template>
    </el-dialog>

    <!-- Change Password Dialog -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="旧密码" required>
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword">确认修改</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { updateUserInfo, changePassword } from '@/api/auth'
import { List, EditPen, Calendar, Collection } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const userInfoVisible = ref(false)
const userForm = reactive({ username: '', nickname: '' })

const pwdVisible = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const handleCommand = (cmd) => {
  if (cmd === 'info') {
    userForm.username = authStore.user?.username || ''
    userForm.nickname = authStore.user?.nickname || ''
    userInfoVisible.value = true
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdVisible.value = true
  } else if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

const submitUserInfo = async () => {
  try {
    await updateUserInfo({ nickname: userForm.nickname })
    ElMessage.success('修改成功')
    userInfoVisible.value = false
    await authStore.fetchUserInfo()
  } catch (e) {}
}

const submitPassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码不少于6位')
    return
  }
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    authStore.logout()
    router.push('/login')
  } catch (e) {}
}
</script>

<style scoped>
.header {
  height: var(--header-height);
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 16px;
  gap: 16px;
}
.header-right { display: flex; align-items: center; gap: 8px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 13px; color: var(--text-regular); }
</style>
