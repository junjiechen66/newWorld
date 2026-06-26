<template>
  <div class="quadrant-page">
    <div class="page-header">
      <h2>四象限</h2>
      <el-button type="primary" size="small" @click="showCreateDialog">
        <el-icon><Plus /></el-icon> 新建任务
      </el-button>
    </div>

    <div class="quadrant-grid">
      <QuadrantCard
        v-for="q in quadrants" :key="q.key"
        :priority="q.key"
        :title="q.title"
        :icon="q.icon"
        :tasks="q.tasks"
        :isDragOver="dragOver === q.key"
        @dragover="dragOver = q.key"
        @dragleave="dragOver = ''"
        @dragend="dragOver = ''"
        @drop="handleDrop($event, q.key)"
        @dragstart="handleDragStart"
        @task-click="openDetail"
        @task-right-click="openEditDialog"
        @toggle-status="toggleStatus"
      />
    </div>

    <!-- Shared Editable Panel (left-click: edit title and content) -->
    <TaskEditablePanel
      v-model:visible="detailVisible"
      :task="viewTask"
      @save="handleSaveDesc"
    />

    <!-- Shared Edit Dialog (right-click: edit other info) -->
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
import { ref, computed, onMounted, onBeforeUnmount, markRaw } from 'vue'
import { getTaskList, createTask, updateTask, updateTaskStatus, updateTaskPriority, deleteTask } from '@/api/task'
import { useProjectStore } from '@/stores/project'
import { Plus, WarningFilled, Clock, Timer, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TaskEditDialog from '@/components/task/TaskEditDialog.vue'
import TaskEditablePanel from '@/components/task/TaskEditablePanel.vue'
import QuadrantCard from '@/components/task/QuadrantCard.vue'
import { emitRefresh, onRefreshData } from '@/utils/events'

const projectStore = useProjectStore()
const tasks = ref([])

// Drag and drop
const dragOver = ref('')
const draggingTask = ref(null)

const handleDragStart = (e, task) => {
  draggingTask.value = task
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(task.id))
  e.target.style.opacity = '0.4'
  setTimeout(() => { e.target.style.opacity = '' }, 0)
}

const handleDrop = async (e, targetPriority) => {
  dragOver.value = ''
  const task = draggingTask.value
  if (!task || task.priority === targetPriority) return
  try {
    await updateTaskPriority(task.id, targetPriority)
    task.priority = targetPriority
    ElMessage.success(`已移动到「${priorityName(targetPriority)}」`)
    emitRefresh()
  } catch (err) {
    ElMessage.error('移动失败')
  }
  draggingTask.value = null
}

const priorityName = (p) => {
  const map = { Q1: '重要且紧急', Q2: '重要不紧急', Q3: '不重要但紧急', Q4: '不重要不紧急' }
  return map[p] || p
}

const todayDate = computed(() => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
})

const parseDate = (str) => {
  if (!str) return null
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

const diffDays = (a, b) => Math.round((a - b) / (1000 * 60 * 60 * 24))

const getDateInfo = (task) => {
  if (task.status === 'DONE') return { text: '', cls: '' }
  const now = todayDate.value
  const start = parseDate(task.startDate)
  const due = parseDate(task.dueDate)

  if (due && due < now) return { text: `已过期 ${diffDays(now, due)} 天`, cls: 'date-overdue' }
  if (due && due.getTime() === now.getTime()) {
    if (start && start < now) return { text: `已开始 ${diffDays(now, start)} 天，今天截止`, cls: 'date-today' }
    return { text: '今天截止', cls: 'date-today' }
  }
  if (start && start <= now && due && due > now) {
    return { text: `已开始 ${diffDays(now, start)} 天，剩余 ${diffDays(due, now)} 天`, cls: 'date-progress' }
  }
  if (start && start > now) return { text: `${diffDays(start, now)} 天后开始`, cls: 'date-future' }
  if (start && start.getTime() === now.getTime() && !due) return { text: '今天开始', cls: 'date-today' }
  if (due && due > now && !start) return { text: `剩余 ${diffDays(due, now)} 天`, cls: 'date-remaining' }
  return { text: '', cls: '' }
}

const activeTasks = computed(() => tasks.value.filter(t => t.status !== 'DONE' && t.status !== 'SHELVED'))

const quadrants = computed(() => [
  { key: 'Q1', title: '重要且紧急', icon: markRaw(WarningFilled), tasks: activeTasks.value.filter(t => t.priority === 'Q1').map(t => ({ ...t, dateInfo: getDateInfo(t) })) },
  { key: 'Q2', title: '重要不紧急', icon: markRaw(Clock), tasks: activeTasks.value.filter(t => t.priority === 'Q2').map(t => ({ ...t, dateInfo: getDateInfo(t) })) },
  { key: 'Q3', title: '不重要但紧急', icon: markRaw(Timer), tasks: activeTasks.value.filter(t => t.priority === 'Q3').map(t => ({ ...t, dateInfo: getDateInfo(t) })) },
  { key: 'Q4', title: '不重要不紧急', icon: markRaw(CircleCheck), tasks: activeTasks.value.filter(t => t.priority === 'Q4').map(t => ({ ...t, dateInfo: getDateInfo(t) })) }
])

const loadTasks = async () => {
  try {
    const res = await getTaskList({ isNote: false })
    const list = res.data || []
    const projectMap = {}
    projectStore.projectOptions.forEach(p => { projectMap[p.id] = p.name })
    tasks.value = list.map(t => ({ ...t, projectName: projectMap[t.projectId] || '' }))
  } catch (e) {
    tasks.value = []
  }
}

const toggleStatus = async (task, val) => {
  const newStatus = val ? 'DONE' : 'INCOMPLETE'
  try {
    await updateTaskStatus(task.id, newStatus)
    task.status = newStatus
    emitRefresh()
  } catch (e) {}
}

// Detail panel (left-click: edit title and content)
const detailVisible = ref(false)
const viewTask = ref(null)

const openDetail = (task) => {
  viewTask.value = task
  detailVisible.value = true
}

const handleSaveDesc = async ({ title, description }) => {
  if (!viewTask.value) return
  try {
    await updateTask(viewTask.value.id, { title, description })
    ElMessage.success('已保存')
    viewTask.value.title = title
    viewTask.value.description = description
    await loadTasks()
    emitRefresh()
  } catch (e) {}
}

// Edit dialog (right-click: edit other info)
const editDialogVisible = ref(false)
const editTaskRef = ref(null)

const openEditDialog = (task) => {
  editTaskRef.value = task
  editDialogVisible.value = true
}

const handleEditSave = async (formData) => {
  if (!editTaskRef.value) return
  try {
    await updateTask(editTaskRef.value.id, formData)
    ElMessage.success('已保存')
    editDialogVisible.value = false
    editTaskRef.value = null
    await loadTasks()
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
    await loadTasks()
    emitRefresh()
  } catch (e) {}
}

// Create dialog
const createVisible = ref(false)

const showCreateDialog = () => {
  createVisible.value = true
}

const handleCreateSave = async (formData) => {
  if (!formData.title.trim()) return
  try {
    await createTask({ ...formData, isNote: false })
    ElMessage.success('已创建')
    createVisible.value = false
    await loadTasks()
    await projectStore.fetchTree()
    emitRefresh()
  } catch (e) {}
}

const handleRefreshData = () => { loadTasks() }

let cleanupRefresh = null

onMounted(async () => {
  await projectStore.loadProjects()
  await loadTasks()
  cleanupRefresh = onRefreshData(handleRefreshData)
})

onBeforeUnmount(() => {
  cleanupRefresh?.()
})
</script>

<style scoped>
.quadrant-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.quadrant-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}
</style>
