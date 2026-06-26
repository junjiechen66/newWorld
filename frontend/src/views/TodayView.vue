<template>
  <div class="today-page">
    <div class="page-header">
      <div class="header-left">
        <h2>今日任务</h2>
        <span class="today-date">{{ todayStr }}</span>
      </div>
      <el-button type="primary" size="small" @click="showCreateDialog">
        <el-icon><Plus /></el-icon> 新建任务
      </el-button>
    </div>

    <!-- Task Sections -->
    <div class="today-scroll">
      <div v-if="loading" class="list-status"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="incompleteTasks.length === 0 && completedTasks.length === 0" class="list-status empty">
        今日暂无任务，继续保持专注 🎯
      </div>
      <div v-else>
        <!-- Incomplete -->
        <div v-if="incompleteTasks.length > 0" class="task-section">
          <div class="section-title section-incomplete">
            <el-icon><Clock /></el-icon> 未完成（{{ incompleteTasks.length }}）
          </div>
          <div class="tasks-grid">
            <TaskCard
              v-for="task in incompleteTasks" :key="task.id"
              :task="task"
              :priorityLabelFn="priorityShortLabel"
              @click="openDetail"
              @right-click="openEditFromCard"
              @toggle-status="cycleStatus"
            />
          </div>
        </div>

        <!-- Completed -->
        <div v-if="completedTasks.length > 0" class="task-section">
          <div class="section-title section-completed">
            <el-icon><CircleCheck /></el-icon> 已完成（{{ completedTasks.length }}）
          </div>
          <div class="tasks-grid">
            <TaskCard
              v-for="task in completedTasks" :key="task.id"
              :task="task"
              :priorityLabelFn="priorityShortLabel"
              @click="openDetail"
              @right-click="openEditFromCard"
              @toggle-status="cycleStatus"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Shared Editable Panel -->
    <TaskEditablePanel
      v-model:visible="detailVisible"
      :task="viewTask"
      @save="handleSaveDesc"
    />

    <!-- Shared Edit Dialog -->
    <TaskEditDialog
      v-model:visible="editDialogVisible"
      mode="edit"
      :task="editTaskRef"
      :projectOptions="projectStore.projectOptions"
      @save="handleEditSave"
      @delete="handleEditDelete"
    />

    <!-- Shared Create Dialog -->
    <TaskEditDialog
      v-model:visible="createVisible"
      mode="create"
      :projectOptions="projectStore.projectOptions"
      @save="handleCreateSave"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getTodayTasks, createTask, updateTask, deleteTask } from '@/api/task'
import { useProjectStore } from '@/stores/project'
import { useTaskActions } from '@/composables/useTaskActions'
import { Plus, Loading, Clock, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TaskEditDialog from '@/components/task/TaskEditDialog.vue'
import TaskEditablePanel from '@/components/task/TaskEditablePanel.vue'
import TaskCard from '@/components/task/TaskCard.vue'
import { emitRefresh, onRefreshData } from '@/utils/events'
import dayjs from 'dayjs'

const projectStore = useProjectStore()
const { priorityShortLabel, cycleStatus } = useTaskActions()

const loading = ref(false)
const allTasks = ref([])
const todayStr = computed(() => dayjs().format('YYYY-MM-DD dddd'))

// Split into incomplete and completed
const incompleteTasks = computed(() => allTasks.value.filter(t => t.status !== 'DONE'))
const completedTasks = computed(() => allTasks.value.filter(t => t.status === 'DONE'))

const loadTodayTasks = async () => {
  loading.value = true
  try {
    const res = await getTodayTasks()
    allTasks.value = (res.data || []).map(t => {
      const p = projectStore.projectOptions.find(o => o.id === t.projectId)
      return { ...t, projectName: p?.name || '' }
    })
  } catch (e) {
    allTasks.value = []
  } finally {
    loading.value = false
  }
}

// Detail panel
const detailVisible = ref(false)
const viewTask = ref(null)

// Edit dialog
const editDialogVisible = ref(false)
const editTaskRef = ref(null)

// Create dialog
const createVisible = ref(false)

const openDetail = (task) => {
  viewTask.value = task
  detailVisible.value = true
}

// Right-click opens edit dialog (other info: priority, status, dates, project)
const openEditFromCard = (task) => {
  editTaskRef.value = task
  editDialogVisible.value = true
}

const handleSaveDesc = async ({ title, description }) => {
  if (!viewTask.value) return
  try {
    await updateTask(viewTask.value.id, { title, description })
    ElMessage.success('已保存')
    viewTask.value.title = title
    viewTask.value.description = description
    await loadTodayTasks()
    emitRefresh()
  } catch (e) {}
}

const handleEditSave = async (formData) => {
  if (!editTaskRef.value) return
  try {
    await updateTask(editTaskRef.value.id, formData)
    ElMessage.success('已保存')
    editDialogVisible.value = false
    editTaskRef.value = null
    await loadTodayTasks()
    emitRefresh()
  } catch (e) {}
}

const showCreateDialog = () => {
  createVisible.value = true
}

const handleCreateSave = async (formData) => {
  try {
    const todayDate = dayjs().format('YYYY-MM-DD')
    await createTask({ ...formData, isNote: false, startDate: formData.startDate || todayDate, dueDate: formData.dueDate || todayDate })
    ElMessage.success('已创建')
    createVisible.value = false
    await loadTodayTasks()
    await projectStore.fetchTree()
    emitRefresh()
  } catch (e) {}
}

const handleEditDelete = async () => {
  if (!editTaskRef.value) return
  try {
    await deleteTask(editTaskRef.value.id)
    ElMessage.success('已删除')
    editDialogVisible.value = false
    editTaskRef.value = null
    await loadTodayTasks()
    emitRefresh()
  } catch (e) {}
}

const handleRefreshData = () => {
  projectStore.loadProjects()
  loadTodayTasks()
}

let cleanupRefresh = null

onMounted(async () => {
  await projectStore.loadProjects()
  await loadTodayTasks()
  cleanupRefresh = onRefreshData(handleRefreshData)
})

onBeforeUnmount(() => {
  cleanupRefresh?.()
})
</script>

<style scoped>
.today-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.header-left { display: flex; align-items: baseline; gap: 12px; }
.header-left h2 { font-size: 18px; font-weight: 600; margin: 0; }
.today-date { font-size: 13px; color: var(--text-secondary); }
.today-scroll { flex: 1; overflow-y: auto; }
.task-section { margin-bottom: 24px; }
.section-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.section-incomplete { color: #E6A23C; }
.section-completed { color: #67C23A; }
</style>
