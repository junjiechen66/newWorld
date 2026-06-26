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

    <!-- Shared Editable Panel -->
    <TaskEditablePanel
      v-model:visible="detailVisible"
      :task="viewTask"
      @save="handleSaveDesc"
    />

    <!-- Shared Edit/Create Dialog -->
    <TaskEditDialog
      v-model:visible="dialogVisible"
      :mode="editingTask ? 'edit' : 'create'"
      :task="editingTask"
      :projectOptions="projectStore.projectOptions"
      @save="handleDialogSave"
      @delete="handleDialogDelete"
    />

    <!-- Calendar-specific: extra fields overlay for date range display -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useProjectStore } from '@/stores/project'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import {
  getTaskList,
  createTask,
  updateTask,
  deleteTask
} from '@/api/task'
import TaskEditablePanel from '@/components/task/TaskEditablePanel.vue'
import TaskEditDialog from '@/components/task/TaskEditDialog.vue'
import { emitRefresh, emitCalendarDateChange, onRefreshData } from '@/utils/events'

const route = useRoute()
const appStore = useAppStore()
const projectStore = useProjectStore()
const calendarRef = ref(null)
const dialogVisible = ref(false)
const editingTask = ref(null)

// View panel state
const detailVisible = ref(false)
const viewTask = ref(null)

const allTasks = ref([])

const loadTasks = async (params = {}) => {
  try {
    const res = await getTaskList(params)
    allTasks.value = res.data
    return res.data
  } catch (e) { return [] }
}

const projectNameMap = computed(() => {
  const map = {}
  projectStore.projectOptions.forEach(p => { map[p.id] = p.name })
  return map
})

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
        priority: t.priority || 'Q1',
        status: t.status || 'INCOMPLETE',
        projectId: t.projectId,
        projectName: t.projectId ? projectNameMap.value[t.projectId] : null
      },
      classNames: [
        'task-priority-' + (t.priority || 'none').toLowerCase(),
        t.status === 'DONE' ? 'task-status-done' : ''
      ]
    }))
}

const refreshCalendar = async (projectFilter) => {
  const params = { isNote: false }
  if (projectFilter) params.projectId = projectFilter
  const tasks = await loadTasks(params)
  const newEvents = buildEvents(tasks)
  const api = calendarRef.value?.getApi()
  if (api) {
    api.removeAllEventSources()
    api.addEventSource(newEvents)
  }
}

// Pending create dates (for new task from calendar click)
const pendingDates = ref({ startDate: null, dueDate: null })

const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: { left: 'prev,next today', center: 'title', right: '' },
  buttonText: { today: '今天', month: '月' },
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
  datesSet: (info) => {
    const start = dayjs(info.start).format('YYYY-MM-DD')
    const end = dayjs(info.end).subtract(1, 'day').format('YYYY-MM-DD')
    emitCalendarDateChange(start, end)
  },
  eventDidMount: (info) => {
    info.el.addEventListener('contextmenu', (e) => {
      e.preventDefault()
      const task = allTasks.value.find(t => String(t.id) === info.event.id)
      if (task) {
        editingTask.value = task
        dialogVisible.value = true
      }
    })
  },
  eventContent: (arg) => {
    const projectName = arg.event.extendedProps.projectName
    const container = document.createElement('div')
    container.className = 'fc-event-custom-content'
    if (projectName) {
      const tag = document.createElement('span')
      tag.className = 'fc-event-project-tag'
      tag.textContent = projectName
      container.appendChild(tag)
    }
    const title = document.createElement('span')
    title.className = 'fc-event-title-text'
    title.textContent = arg.event.title
    container.appendChild(title)
    return { domNodes: [container] }
  }
}))

const handleDateClick = (info) => {
  editingTask.value = null
  pendingDates.value = { startDate: info.dateStr, dueDate: info.dateStr }
  dialogVisible.value = true
}

const handleSelect = (info) => {
  editingTask.value = null
  const endDate = dayjs(info.endStr).subtract(1, 'day')
  pendingDates.value = { startDate: info.startStr, dueDate: endDate.format('YYYY-MM-DD') }
  dialogVisible.value = true
  calendarRef.value?.getApi().unselect()
}

const handleEventClick = (info) => {
  const task = allTasks.value.find(t => String(t.id) === info.event.id)
  if (task) {
    viewTask.value = task
    detailVisible.value = true
  }
}

const handleEventDrop = async (info) => {
  const taskId = info.event.id
  const newStart = dayjs(info.event.start).format('YYYY-MM-DD')
  const newEnd = info.event.end ? dayjs(info.event.end).subtract(1, 'day').format('YYYY-MM-DD') : null
  try {
    await updateTask(parseInt(taskId), { startDate: newStart, dueDate: newEnd })
    ElMessage.success('排期已更新')
    await refreshCalendar(route.query.projectId)
    emitRefresh()
  } catch (e) { info.revert() }
}

const handleEventResize = async (info) => {
  const taskId = info.event.id
  const newEnd = info.event.end ? dayjs(info.event.end).subtract(1, 'day').format('YYYY-MM-DD') : null
  try {
    await updateTask(parseInt(taskId), { dueDate: newEnd })
    ElMessage.success('截止日期已更新')
    await refreshCalendar(route.query.projectId)
    emitRefresh()
  } catch (e) { info.revert() }
}

// Panel save
const handleSaveDesc = async ({ title, description }) => {
  if (!viewTask.value) return
  try {
    await updateTask(viewTask.value.id, { title, description })
    ElMessage.success('已保存')
    viewTask.value.title = title
    viewTask.value.description = description
    await refreshCalendar(route.query.projectId)
    emitRefresh()
  } catch (e) {}
}

// Dialog save (create or edit)
const handleDialogSave = async (formData) => {
  try {
    if (editingTask.value) {
      await updateTask(editingTask.value.id, formData)
      ElMessage.success('任务已更新')
    } else {
      const createData = {
        ...formData,
        startDate: formData.startDate || pendingDates.value.startDate,
        dueDate: formData.dueDate || pendingDates.value.dueDate
      }
      await createTask(createData)
      ElMessage.success('任务已创建')
    }
    dialogVisible.value = false
    editingTask.value = null
    await refreshCalendar(route.query.projectId)
    emitRefresh()
  } catch (e) {}
}

// Dialog delete (edit mode only)
const handleDialogDelete = async () => {
  if (!editingTask.value) return
  try {
    await deleteTask(editingTask.value.id)
    ElMessage.success('已删除')
    dialogVisible.value = false
    editingTask.value = null
    await refreshCalendar(route.query.projectId)
    emitRefresh()
  } catch (e) {}
}

watch(() => appStore.showCompleted, () => { refreshCalendar(route.query.projectId) })
watch(() => route.query.projectId, (newVal) => { refreshCalendar(newVal) })
watch(() => appStore.sidebarCollapsed, () => {
  setTimeout(() => { calendarRef.value?.getApi().render() }, 300)
})

const handleRefreshData = async () => {
  await projectStore.loadProjects()
  refreshCalendar(route.query.projectId)
}

let cleanupRefresh = null

onMounted(async () => {
  await projectStore.loadProjects()
  await refreshCalendar(route.query.projectId)
  cleanupRefresh = onRefreshData(handleRefreshData)
})

onBeforeUnmount(() => {
  cleanupRefresh?.()
})
</script>

<style scoped>
.calendar-container { padding: 16px; height: 100%; }
.calendar-wrapper { background: var(--card-bg); border-radius: 8px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
.calendar-toolbar-row { display: flex; justify-content: flex-start; align-items: center; margin-bottom: 8px; }

/* Calendar event styles */
:deep(.fc-event-custom-content) {
  display: flex; align-items: center; gap: 4px; overflow: hidden; min-width: 0;
}
:deep(.fc-event-project-tag) {
  display: inline-block; padding: 0 5px; font-size: 10px; line-height: 16px;
  border-radius: 3px; background: rgba(255,255,255,0.6); color: #555;
  font-weight: 500; white-space: nowrap; flex-shrink: 0;
}
:deep(.fc-event-title-text) {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-weight: 600; color: #333; min-width: 0;
}
:deep(.task-status-done .fc-event-project-tag) { opacity: 0.5; }
:deep(.task-status-done .fc-event-title-text) { color: #999 !important; font-weight: 400 !important; }
</style>
