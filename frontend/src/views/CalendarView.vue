<template>
  <div class="calendar-container">
    <div class="calendar-wrapper">
      <div class="calendar-toolbar-row">
        <el-checkbox v-model="appStore.showCompleted" size="small">显示已完成</el-checkbox>
      </div>
      <FullCalendar
        ref="calendarRef"
        :options="calendarOptions"
      />
    </div>

    <!-- Right Content Panel (view mode) -->
    <Teleport to="body">
      <transition name="panel-slide">
        <div v-if="detailVisible" class="detail-overlay" @click.self="closeDetailView">
          <div class="detail-panel">
            <div class="panel-header">
              <span class="panel-status" :style="{ background: statusColor }">{{ statusText }}</span>
              <span class="panel-close" @click="closeDetailView">✕</span>
            </div>
            <div class="panel-body">
              <div class="title-editor-area">
                <label class="section-label">标题</label>
                <el-input v-model="editTitle" size="large" placeholder="任务标题..." class="title-input" />
              </div>
              <div class="desc-editor-area">
                <label class="section-label">内容（支持Markdown）</label>
                <MarkdownEditor v-model="editDesc" placeholder="任务内容（支持Markdown）..." />
              </div>
            </div>
            <div class="panel-footer">
              <el-button size="small" type="primary" @click="saveDesc">保存</el-button>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- Task Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingTask ? '编辑任务' : dialogTitle"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="taskForm" label-width="80px" size="small">
        <el-form-item label="标题" required>
          <el-input v-model="taskForm.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="taskForm.projectId" placeholder="选择项目" clearable style="width: 100%">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="taskForm.startDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.dueDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="taskForm.status" style="width:100%">
            <el-option label="未完成" value="INCOMPLETE" />
            <el-option label="已完成" value="DONE" />
            <el-option label="搁置" value="SHELVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="taskForm.priority">
            <el-radio value="NONE">无</el-radio>
            <el-radio value="RED">
              <span class="priority-circle" style="background:#F56C6C"></span> 高
            </el-radio>
            <el-radio value="YELLOW">
              <span class="priority-circle" style="background:#E6A23C"></span> 中
            </el-radio>
            <el-radio value="BLUE">
              <span class="priority-circle" style="background:#409EFF"></span> 低
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="editingTask" type="danger" @click="deleteFromDialog">删除</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTaskList,
  createTask,
  updateTask,
  deleteTask,
  updateTaskStatus,
  updateTaskPriority,
  duplicateTask
} from '@/api/task'
import { getProjectList } from '@/api/project'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'

const route = useRoute()
const appStore = useAppStore()
const calendarRef = ref(null)
const dialogVisible = ref(false)
const editingTask = ref(null)

// View panel state
const detailVisible = ref(false)
const viewTask = ref(null)
const editTitle = ref('')
const editDesc = ref('')

const statusColor = computed(() => {
  if (!viewTask.value) return '#909399'
  const map = { INCOMPLETE: '#909399', DONE: '#67C23A', SHELVED: '#c0c4cc' }
  return map[viewTask.value.status] || '#909399'
})
const statusText = computed(() => {
  if (!viewTask.value) return ''
  const map = { INCOMPLETE: '未完成', DONE: '已完成', SHELVED: '搁置' }
  return map[viewTask.value.status] || ''
})

// Computed dialog title
const dialogTitle = computed(() => {
  if (taskForm.startDate && taskForm.dueDate && taskForm.startDate !== taskForm.dueDate) {
    return `新建任务 (${taskForm.startDate} ~ ${taskForm.dueDate})`
  }
  return '新建任务'
})

// Task form
const taskForm = reactive({
  title: '',
  projectId: null,
  startDate: null,
  dueDate: null,
  status: 'INCOMPLETE',
  priority: 'NONE',
  description: ''
})

// Project options for the form
const projectOptions = ref([])

// Load all tasks for calendar
const allTasks = ref([])

// Build project options from the tree
const loadProjects = async () => {
  try {
    const res = await getProjectList()
    projectOptions.value = res.data
  } catch (e) {}
}

// Get tasks from backend
const loadTasks = async (params = {}) => {
  try {
    const res = await getTaskList(params)
    allTasks.value = res.data
    return res.data
  } catch (e) {
    return []
  }
}

// Convert tasks to FullCalendar events
const buildEvents = (tasks) => {
  return tasks
    .filter(t => !t.isNote && t.status !== 'SHELVED')
    .filter(t => appStore.showCompleted || t.status !== 'DONE')
    .map(t => ({
      id: String(t.id),
      title: t.title,
      start: t.startDate,
      end: t.dueDate ? dayjs(t.dueDate).add(1, 'day').format('YYYY-MM-DD') : t.startDate,
      allDay: true,
      extendedProps: {
        priority: t.priority || 'NONE',
        status: t.status || 'INCOMPLETE',
        projectId: t.projectId
      },
      classNames: [
        'task-priority-' + (t.priority || 'none').toLowerCase(),
        t.status === 'DONE' ? 'task-status-done' : ''
      ]
    }))
}

const calendarEvents = ref([])

// Refresh calendar data
const refreshCalendar = async (projectFilter) => {
  const params = { isNote: false }
  if (projectFilter) params.projectId = projectFilter
  const tasks = await loadTasks(params)
  const newEvents = buildEvents(tasks)
  calendarEvents.value = newEvents
  // Update events purely via FullCalendar API (bypasses Vue watcher conflicts)
  const api = calendarRef.value?.getApi()
  if (api) {
    api.removeAllEventSources()
    api.addEventSource(newEvents)
  }
}

// FullCalendar options
const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: ''
  },
  buttonText: {
    today: '今天',
    month: '月'
  },
  locale: 'zh-cn',
  firstDay: 1,
  height: 'auto',
  events: [],
  editable: true,
  droppable: true,
  dateClick: handleDateClick,
  selectable: true,
  selectMirror: true,
  select: handleSelect,
  unselectAuto: false,
  eventClick: handleEventClick,
  eventDrop: handleEventDrop,
  eventResize: handleEventResize,
  eventDidMount: (info) => {
    // Right click → open edit dialog directly
    info.el.addEventListener('contextmenu', (e) => {
      e.preventDefault()
      const task = allTasks.value.find(t => String(t.id) === info.event.id)
      if (task) {
        editingTask.value = task
        taskForm.title = task.title
        taskForm.projectId = task.projectId
        taskForm.startDate = task.startDate
        taskForm.dueDate = task.dueDate
        taskForm.status = task.status || 'INCOMPLETE'
        taskForm.priority = task.priority || 'NONE'
        taskForm.description = task.description || ''
        dialogVisible.value = true
      }
    })
  }
}))

// Date click - create new task (single day)
const handleDateClick = (info) => {
  editingTask.value = null
  resetForm()
  taskForm.startDate = info.dateStr
  taskForm.dueDate = info.dateStr
  dialogVisible.value = true
}

// Drag select - create task spanning multiple dates
const handleSelect = (info) => {
  editingTask.value = null
  resetForm()
  taskForm.startDate = info.startStr
  // endStr is exclusive (day after last selected), so subtract 1 day
  const endDate = dayjs(info.endStr).subtract(1, 'day')
  taskForm.dueDate = endDate.format('YYYY-MM-DD')
  dialogVisible.value = true
  // Clear selection highlight
  calendarRef.value?.getApi().unselect()
}

// Event click (left) → open content view panel
const handleEventClick = (info) => {
  const task = allTasks.value.find(t => String(t.id) === info.event.id)
  if (task) {
    viewTask.value = task
    editTitle.value = task.title || ''
    editDesc.value = task.description || ''
    detailVisible.value = true
  }
}

// Event drop - reschedule
const handleEventDrop = async (info) => {
  const taskId = info.event.id
  const newStart = dayjs(info.event.start).format('YYYY-MM-DD')
  const newEnd = info.event.end ? dayjs(info.event.end).subtract(1, 'day').format('YYYY-MM-DD') : null
  try {
    await updateTask(parseInt(taskId), { startDate: newStart, dueDate: newEnd })
    ElMessage.success('排期已更新')
    await refreshCalendar(route.query.projectId)
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {
    info.revert()
  }
}

// Event resize
const handleEventResize = async (info) => {
  const taskId = info.event.id
  const newEnd = info.event.end ? dayjs(info.event.end).subtract(1, 'day').format('YYYY-MM-DD') : null
  try {
    await updateTask(parseInt(taskId), { dueDate: newEnd })
    ElMessage.success('截止日期已更新')
    await refreshCalendar(route.query.projectId)
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {
    info.revert()
  }
}

// View panel actions
const closeDetailView = () => {
  detailVisible.value = false
  viewTask.value = null
}

const saveDesc = async () => {
  if (!viewTask.value) return
  try {
    await updateTask(viewTask.value.id, {
      title: editTitle.value,
      description: editDesc.value
    })
    ElMessage.success('已保存')
    viewTask.value.title = editTitle.value
    viewTask.value.description = editDesc.value
    await refreshCalendar(route.query.projectId)
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

const editFromPanel = () => {
  if (!viewTask.value) return
  const t = viewTask.value
  editingTask.value = t
  taskForm.title = t.title
  taskForm.projectId = t.projectId
  taskForm.startDate = t.startDate
  taskForm.dueDate = t.dueDate
  taskForm.status = t.status || 'INCOMPLETE'
  taskForm.priority = t.priority || 'NONE'
  taskForm.description = t.description || ''
  detailVisible.value = false
  dialogVisible.value = true
}

const deleteFromPanel = async () => {
  if (!viewTask.value) return
  await ElMessageBox.confirm('确定要删除此任务吗？', '提示')
  try {
    await deleteTask(viewTask.value.id)
    ElMessage.success('已删除')
    closeDetailView()
    await refreshCalendar(route.query.projectId)
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

// Edit dialog actions
const setPriority = async (priority) => {
  // kept from context menu - used elsewhere
}

const deleteFromDialog = async () => {
  if (!editingTask.value) return
  dialogVisible.value = false
  await ElMessageBox.confirm('确定要删除此任务吗？', '提示')
  try {
    await deleteTask(editingTask.value.id)
    ElMessage.success('已删除')
    editingTask.value = null
    await refreshCalendar(route.query.projectId)
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

// Form actions
const resetForm = () => {
  taskForm.title = ''
  taskForm.projectId = null
  taskForm.startDate = null
  taskForm.dueDate = null
  taskForm.status = 'INCOMPLETE'
  taskForm.priority = 'NONE'
  taskForm.description = ''
}

const submitTask = async () => {
  if (!taskForm.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  try {
    if (editingTask.value) {
      await updateTask(editingTask.value.id, { ...taskForm })
      ElMessage.success('任务已更新')
    } else {
      await createTask({ ...taskForm })
      ElMessage.success('任务已创建')
    }
    dialogVisible.value = false
    await refreshCalendar(route.query.projectId)
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

// Listen for custom events from Sidebar
const handleOpenTaskDetail = (e) => {}

// Close context menu on click outside

// Watch showCompleted to refresh calendar
watch(() => appStore.showCompleted, () => {
  refreshCalendar(route.query.projectId)
})

// Watch route query changes (project filter from Sidebar)
watch(() => route.query.projectId, (newVal) => {
  refreshCalendar(newVal)
})

// Watch sidebar collapse to re-render calendar with delay for CSS transition
watch(() => appStore.sidebarCollapsed, () => {
  setTimeout(() => {
    calendarRef.value?.getApi().render()
  }, 300)
})

// Wrapper for refresh-data event listener (ignores CustomEvent argument)
const handleRefreshData = () => refreshCalendar(route.query.projectId)

onMounted(async () => {
  await loadProjects()
  await refreshCalendar(route.query.projectId)

  window.addEventListener('open-task-detail', handleOpenTaskDetail)
  window.addEventListener('refresh-data', handleRefreshData)
})

onBeforeUnmount(() => {
  window.removeEventListener('open-task-detail', handleOpenTaskDetail)
  window.removeEventListener('refresh-data', handleRefreshData)
})
</script>

<style scoped>
.calendar-container {
  padding: 16px;
  height: 100%;
}
.calendar-wrapper {
  background: var(--card-bg);
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.calendar-toolbar-row {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 8px;
}
.priority-circle {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
}

/* Right content panel */
.detail-overlay {
  position: fixed;
  top: 0; right: 0; bottom: 0;
  width: 50%;
  z-index: 1000;
  pointer-events: auto;
}
.detail-panel {
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 0;
}
.panel-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
}
.panel-close {
  cursor: pointer;
  color: #bbb;
  font-size: 18px;
  line-height: 1;
  padding: 4px;
}
.panel-close:hover { color: #333; }
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 16px;
  display: flex;
  flex-direction: column;
}
.title-editor-area {
  margin-bottom: 20px;
}
.section-label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
  font-weight: 500;
}
.title-input :deep(.el-input__wrapper) {
  font-size: 22px;
  font-weight: 700;
  padding-left: 0;
  box-shadow: none !important;
  border-bottom: 1px solid #e0e0e0;
  border-radius: 0;
}
.title-input :deep(.el-input__inner) {
  height: 44px;
  padding: 0;
}
.desc-editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.desc-editor-area :deep(.md-editor) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 300px;
}
.desc-editor-area :deep(.md-textarea) {
  flex: 1;
  resize: none;
  min-height: 240px;
}
.desc-editor-area :deep(.md-preview) {
  flex: 1;
}
.panel-footer {
  display: flex;
  gap: 8px;
  padding: 12px 24px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

/* Slide animation */
.panel-slide-enter-active { transition: transform 0.2s ease; }
.panel-slide-leave-active { transition: transform 0.15s ease; }
.panel-slide-enter-from { transform: translateX(100%); }
.panel-slide-leave-to { transform: translateX(100%); }
</style>
