<template>
  <div class="calendar-container">
    <div class="calendar-wrapper">
      <FullCalendar
        ref="calendarRef"
        :options="calendarOptions"
      />
    </div>

    <!-- Task Context Menu -->
    <Teleport to="body">
      <div
        v-show="contextMenu.visible"
        class="context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <div class="menu-item" @click="setPriority('RED')">
          <span class="priority-circle" style="background:#F56C6C"></span> 优先级-红
        </div>
        <div class="menu-item" @click="setPriority('YELLOW')">
          <span class="priority-circle" style="background:#E6A23C"></span> 优先级-黄
        </div>
        <div class="menu-item" @click="setPriority('BLUE')">
          <span class="priority-circle" style="background:#409EFF"></span> 优先级-蓝
        </div>
        <div class="menu-item" @click="setPriority('FLAG')">
          <span class="priority-circle" style="background:#67C23A"></span> 旗标
        </div>
        <div class="menu-divider" />
        <div class="menu-item" @click="setStatus('IN_PROGRESS')">开始进行</div>
        <div class="menu-item" @click="setStatus('DONE')">标记完成</div>
        <div class="menu-divider" />
        <div class="menu-item" @click="duplicateCurrent">创建副本</div>
        <div class="menu-item" @click="archiveCurrent">归档</div>
        <div class="menu-item" @click="convertCurrentToNote">转换为笔记</div>
        <div class="menu-divider" />
        <div class="menu-item" @click="editCurrentTask">编辑</div>
        <div class="menu-item" style="color: var(--danger-color);" @click="deleteCurrentTask">删除</div>
      </div>
    </Teleport>

    <!-- Task Form Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingTask ? '编辑任务' : '新建任务'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="taskForm" label-width="80px" size="small">
        <el-form-item label="标题" required>
          <el-input v-model="taskForm.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="taskForm.projectId" placeholder="选择项目" clearable style="width: 100%">
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            >
              <span>{{ item.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="taskForm.startDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.dueDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="taskForm.priority">
            <el-radio value="NONE">无</el-radio>
            <el-radio value="RED">
              <span class="priority-circle" style="background:#F56C6C"></span> 红
            </el-radio>
            <el-radio value="YELLOW">
              <span class="priority-circle" style="background:#E6A23C"></span> 黄
            </el-radio>
            <el-radio value="BLUE">
              <span class="priority-circle" style="background:#409EFF"></span> 蓝
            </el-radio>
            <el-radio value="FLAG">
              <span class="priority-circle" style="background:#67C23A"></span> 旗标
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="taskForm.tag" placeholder="标签名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="4" placeholder="任务描述..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
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
  duplicateTask,
  archiveTask,
  convertToNote
} from '@/api/task'
import { getProjectList } from '@/api/project'

const route = useRoute()
const calendarRef = ref(null)
const dialogVisible = ref(false)
const editingTask = ref(null)

// Task form
const taskForm = reactive({
  title: '',
  projectId: null,
  startDate: null,
  dueDate: null,
  priority: 'NONE',
  tag: '',
  description: ''
})

// Context menu
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  taskId: null,
  task: null
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
    .filter(t => !t.isNote && t.status !== 'ARCHIVED')
    .map(t => ({
      id: String(t.id),
      title: t.title,
      start: t.startDate,
      end: t.dueDate ? dayjs(t.dueDate).add(1, 'day').format('YYYY-MM-DD') : t.startDate,
      allDay: true,
      extendedProps: {
        priority: t.priority || 'NONE',
        status: t.status || 'TODO',
        projectId: t.projectId,
        tag: t.tag
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
  const params = {}
  if (projectFilter) params.projectId = projectFilter
  const tasks = await loadTasks(params)
  calendarEvents.value = buildEvents(tasks)
}

// FullCalendar options
const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  buttonText: {
    today: '今天',
    month: '月',
    week: '周',
    day: '日'
  },
  locale: 'zh-cn',
  firstDay: 1,
  height: 'auto',
  events: calendarEvents.value,
  editable: true,
  droppable: true,
  dateClick: handleDateClick,
  eventClick: handleEventClick,
  eventDrop: handleEventDrop,
  eventResize: handleEventResize,
  eventDidMount: (info) => {
    // Right click handler
    info.el.addEventListener('contextmenu', (e) => {
      e.preventDefault()
      const task = allTasks.value.find(t => String(t.id) === info.event.id)
      if (task) {
        contextMenu.visible = true
        contextMenu.x = e.clientX
        contextMenu.y = e.clientY
        contextMenu.taskId = task.id
        contextMenu.task = task
      }
    })
  }
}))

// Date click - create new task
const handleDateClick = (info) => {
  editingTask.value = null
  resetForm()
  taskForm.startDate = info.dateStr
  taskForm.dueDate = info.dateStr
  dialogVisible.value = true
}

// Event click - open detail
const handleEventClick = (info) => {
  const task = allTasks.value.find(t => String(t.id) === info.event.id)
  if (task) {
    window.dispatchEvent(new CustomEvent('open-task-detail', { detail: { taskId: task.id, task } }))
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
  } catch (e) {
    info.revert()
  }
}

// Context menu actions
const setPriority = async (priority) => {
  if (contextMenu.taskId) {
    await updateTaskPriority(contextMenu.taskId, priority)
    ElMessage.success('优先级已更新')
    contextMenu.visible = false
    await refreshCalendar(route.query.projectId)
  }
}

const setStatus = async (status) => {
  if (contextMenu.taskId) {
    await updateTaskStatus(contextMenu.taskId, status)
    ElMessage.success('状态已更新')
    contextMenu.visible = false
    await refreshCalendar(route.query.projectId)
  }
}

const duplicateCurrent = async () => {
  if (contextMenu.taskId) {
    await duplicateTask(contextMenu.taskId)
    ElMessage.success('已创建副本')
    contextMenu.visible = false
    await refreshCalendar(route.query.projectId)
  }
}

const archiveCurrent = async () => {
  if (contextMenu.taskId) {
    await archiveTask(contextMenu.taskId)
    ElMessage.success('已归档')
    contextMenu.visible = false
    await refreshCalendar(route.query.projectId)
  }
}

const convertCurrentToNote = async () => {
  if (contextMenu.taskId) {
    await convertToNote(contextMenu.taskId)
    ElMessage.success('已转换为笔记')
    contextMenu.visible = false
    await refreshCalendar(route.query.projectId)
  }
}

const editCurrentTask = () => {
  if (contextMenu.task) {
    const t = contextMenu.task
    editingTask.value = t
    taskForm.title = t.title
    taskForm.projectId = t.projectId
    taskForm.startDate = t.startDate
    taskForm.dueDate = t.dueDate
    taskForm.priority = t.priority || 'NONE'
    taskForm.tag = t.tag || ''
    taskForm.description = t.description || ''
    dialogVisible.value = true
  }
  contextMenu.visible = false
}

const deleteCurrentTask = async () => {
  if (contextMenu.taskId) {
    contextMenu.visible = false
    await ElMessageBox.confirm('确定要删除此任务吗？', '提示')
    await deleteTask(contextMenu.taskId)
    ElMessage.success('已删除')
    await refreshCalendar(route.query.projectId)
  }
}

// Form actions
const resetForm = () => {
  taskForm.title = ''
  taskForm.projectId = null
  taskForm.startDate = null
  taskForm.dueDate = null
  taskForm.priority = 'NONE'
  taskForm.tag = ''
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
  } catch (e) {}
}

// Listen for custom events from Sidebar
const handleOpenTaskDetail = (e) => {
  // Could be extended to open detail panel
}

const handleSearch = async (e) => {
  const keyword = e.detail.keyword
  if (keyword) {
    const { searchTasks } = await import('@/api/task')
    const res = await searchTasks(keyword)
    calendarEvents.value = buildEvents(res.data)
  } else {
    await refreshCalendar(route.query.projectId)
  }
}

// Close context menu on click outside
document.addEventListener('click', () => { contextMenu.visible = false })

// Watch for route changes (project filter)
const unwatch = route.query.$watch || null

onMounted(async () => {
  await loadProjects()
  await refreshCalendar(route.query.projectId)

  window.addEventListener('open-task-detail', handleOpenTaskDetail)
  window.addEventListener('search-tasks', handleSearch)
})

onBeforeUnmount(() => {
  window.removeEventListener('open-task-detail', handleOpenTaskDetail)
  window.removeEventListener('search-tasks', handleSearch)
  document.removeEventListener('click', () => { contextMenu.visible = false })
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
.priority-circle {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
}
</style>
