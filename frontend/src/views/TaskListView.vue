<template>
  <div class="task-list-page">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <el-button type="primary" size="small" @click="showCreateDialog">
        <el-icon><Plus /></el-icon> 新建任务
      </el-button>
    </div>

    <!-- Filters -->
    <el-card class="filter-card" shadow="never">
      <el-form inline size="small" @keyup.enter="loadTaskList">
        <el-form-item>
          <el-input v-model="filters.keyword" placeholder="搜索任务..." clearable style="width:250px" @clear="loadTaskList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filters.status" placeholder="状态" clearable style="width:110px" @change="loadTaskList">
            <el-option label="未完成" value="INCOMPLETE" />
            <el-option label="已完成" value="DONE" />
            <el-option label="搁置" value="SHELVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filters.priority" placeholder="优先级" clearable style="width:130px" @change="loadTaskList">
            <el-option label="Q1 重要且紧急" value="Q1" />
            <el-option label="Q2 重要不紧急" value="Q2" />
            <el-option label="Q3 不重要但紧急" value="Q3" />
            <el-option label="Q4 不重要不紧急" value="Q4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filters.projectId" placeholder="项目" clearable style="width:130px" @change="loadTaskList">
            <el-option v-for="item in projectStore.projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="filters.dueDateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="截止起始"
            end-placeholder="截止结束"
            value-format="YYYY-MM-DD"
            style="width:240px"
            clearable
            @change="loadTaskList"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="loadTaskList">搜索</el-button>
          <el-button type="success" @click="showExportDialog">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Task Grid grouped by project -->
    <div class="tasks-scroll">
      <div v-if="loading" class="list-status"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="tasks.length === 0" class="list-status empty">暂无任务</div>
      <div v-else>
        <div v-for="group in groupedTasks" :key="group.projectId" class="project-section">
          <div class="project-section-header">
            <span class="project-section-name">{{ group.projectName }}</span>
            <span class="project-section-count">{{ group.tasks.length }}</span>
          </div>
          <div class="tasks-grid">
            <TaskCard
              v-for="task in group.tasks" :key="task.id"
              :task="task"
              :showDescription="true"
              :priorityLabelFn="priorityLabel"
              @click="openDetail"
              @right-click="handleCardRightClick"
              @toggle-status="handleToggleStatus"
              @share="openShareDialog"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Card Context Menu -->
    <Teleport to="body">
      <div v-show="cardMenu.visible" class="context-menu" :style="{ left: cardMenu.x + 'px', top: cardMenu.y + 'px' }" @click.stop>
        <div class="menu-item" v-if="cardMenu.task?.accessLevel === 'OWNER' || cardMenu.task?.accessLevel === 'EDIT'" @click="cardMenuEdit"><el-icon><Edit /></el-icon> 编辑信息</div>
        <div class="menu-item" v-if="cardMenu.task?.accessLevel === 'OWNER'" @click="cardMenuShare"><el-icon><Share /></el-icon> 共享设置</div>
      </div>
    </Teleport>

    <!-- Shared Editable Panel -->
    <TaskEditablePanel
      v-model:visible="detailVisible"
      :task="viewTask"
      :readonly="viewTask?.accessLevel === 'VIEW'"
      @save="handleSaveDesc"
    />

    <!-- Shared Edit Dialog -->
    <TaskEditDialog
      v-model:visible="editDialogVisible"
      mode="edit"
      :task="editTaskRef"
      :projectOptions="projectStore.projectOptions"
      :showDelete="editTaskRef?.accessLevel === 'OWNER'"
      @save="handleEditSave"
      @delete="handleEditDelete"
      @share="handleEditShare"
    />

    <!-- Shared Create Dialog -->
    <TaskEditDialog
      v-model:visible="createVisible"
      mode="create"
      :projectOptions="projectStore.projectOptions"
      @save="handleCreateSave"
    />

    <!-- Share Dialog -->
    <ShareDialog
      v-model:visible="shareDialogVisible"
      :resource="shareResource"
      resourceType="TASK"
    />

    <!-- Export Dialog -->
    <el-dialog v-model="exportVisible" title="按项目分组导出" width="600px" :close-on-click-modal="false">
      <div style="margin-bottom: 12px; color: #666; font-size: 13px;">
        当前筛选条件下共 {{ exportTasks.length }} 条任务，按项目分组如下：
      </div>
      <el-input
        v-model="exportText"
        type="textarea"
        :rows="14"
        readonly
        style="font-family: monospace;"
      />
      <div style="margin-top: 12px;">
        <el-input v-model="exportNoteTitle" placeholder="笔记标题（导出到笔记时使用）" size="small" style="width: 100%;" />
      </div>
      <template #footer>
        <el-button size="small" @click="exportVisible = false">关闭</el-button>
        <el-button size="small" type="primary" @click="copyToClipboard" :loading="exportCopying">
          复制到剪贴板
        </el-button>
        <el-button size="small" type="success" @click="exportToNote" :loading="exportNoteLoading">
          导出到笔记
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskList, createTask, updateTask, deleteTask } from '@/api/task'
import { useProjectStore } from '@/stores/project'
import { useTaskActions } from '@/composables/useTaskActions'
import { Search, Plus, Loading, Edit, Share } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TaskEditDialog from '@/components/task/TaskEditDialog.vue'
import TaskEditablePanel from '@/components/task/TaskEditablePanel.vue'
import TaskCard from '@/components/task/TaskCard.vue'
import ShareDialog from '@/components/share/ShareDialog.vue'
import { emitRefresh, onRefreshData } from '@/utils/events'

const route = useRoute()
const projectStore = useProjectStore()
const { priorityLabel, cycleStatus } = useTaskActions()

const handleToggleStatus = (task) => {
  if (task.accessLevel === 'VIEW') return
  cycleStatus(task)
}

const loading = ref(false)
const tasks = ref([])

const pageTitle = computed(() => {
  return route.query.projectId ? (projectStore.projectOptions.find(p => p.id === Number(route.query.projectId))?.name || '全部任务') : '全部任务'
})

const filters = reactive({
  keyword: '', status: '', priority: '', projectId: '', dueDateRange: null
})

// Detail panel
const detailVisible = ref(false)
const viewTask = ref(null)

// Edit dialog
const editDialogVisible = ref(false)
const editTaskRef = ref(null)

// Create dialog
const createVisible = ref(false)

// Share dialog
const shareDialogVisible = ref(false)
const shareResource = ref(null)

const openShareDialog = (task) => {
  shareResource.value = task
  shareDialogVisible.value = true
}

// Card context menu
const cardMenu = reactive({ visible: false, x: 0, y: 0, task: null })

const handleCardRightClick = ({ task }) => {
  openEditFromCard(task)
}

const cardMenuEdit = () => {
  cardMenu.visible = false
  if (cardMenu.task) openEditFromCard(cardMenu.task)
}

const cardMenuShare = () => {
  cardMenu.visible = false
  if (cardMenu.task) openShareDialog(cardMenu.task)
}

document.addEventListener('click', () => { cardMenu.visible = false })

const groupedTasks = computed(() => {
  const map = {}
  const order = []
  tasks.value.forEach(t => {
    const key = t.projectId || '_none'
    if (!map[key]) {
      map[key] = { projectId: t.projectId, projectName: t.projectName || '未分配项目', tasks: [] }
      order.push(key)
    }
    map[key].tasks.push(t)
  })
  return order.map(k => map[k])
})

const loadTaskList = async () => {
  loading.value = true
  try {
    const params = { isNote: false }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    if (filters.priority) params.priority = filters.priority
    if (filters.projectId) params.projectId = filters.projectId
    if (filters.dueDateRange && filters.dueDateRange.length === 2) {
      params.dueDateFrom = filters.dueDateRange[0]
      params.dueDateTo = filters.dueDateRange[1]
    }
    const res = await getTaskList(params)
    tasks.value = (res.data || []).map(t => {
      const p = projectStore.projectOptions.find(o => o.id === t.projectId)
      return { ...t, projectName: p?.name || '' }
    })
  } catch (e) { tasks.value = [] }
  finally { loading.value = false }
}

// Detail panel actions
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
    await loadTaskList()
    emitRefresh()
  } catch (e) {}
}

// Edit dialog actions
const handleEditSave = async (formData) => {
  if (!editTaskRef.value) return
  try {
    await updateTask(editTaskRef.value.id, formData)
    ElMessage.success('已保存')
    editDialogVisible.value = false
    editTaskRef.value = null
    await loadTaskList()
    emitRefresh()
  } catch (e) {}
}

// Create dialog actions
const showCreateDialog = () => {
  createVisible.value = true
}

const handleCreateSave = async (formData) => {
  try {
    await createTask({ ...formData, isNote: false })
    ElMessage.success('已创建')
    createVisible.value = false
    await loadTaskList()
    await projectStore.fetchTree()
    emitRefresh()
  } catch (e) {}
}

// Delete from edit dialog
const handleEditDelete = async () => {
  if (!editTaskRef.value) return
  try {
    await deleteTask(editTaskRef.value.id)
    ElMessage.success('已删除')
    editDialogVisible.value = false
    editTaskRef.value = null
    await loadTaskList()
    emitRefresh()
  } catch (e) {}
}

// Share from edit dialog
const handleEditShare = (task) => {
  editDialogVisible.value = false
  openShareDialog(task)
}

// Export
const exportVisible = ref(false)
const exportTasks = ref([])
const exportText = ref('')
const exportNoteTitle = ref('')
const exportCopying = ref(false)
const exportNoteLoading = ref(false)

const showExportDialog = async () => {
  const params = { isNote: false, pageSize: 9999 }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.status) params.status = filters.status
  if (filters.priority) params.priority = filters.priority
  if (filters.projectId) params.projectId = filters.projectId
  if (filters.dueDateRange && filters.dueDateRange.length === 2) {
    params.dueDateFrom = filters.dueDateRange[0]
    params.dueDateTo = filters.dueDateRange[1]
  }
  try {
    const res = await getTaskList(params)
    const list = res.data || []
    exportTasks.value = list

    const grouped = {}
    list.forEach(t => {
      const pid = t.projectId || '_none'
      if (!grouped[pid]) grouped[pid] = []
      grouped[pid].push(t)
    })

    const lines = []
    for (const pid of Object.keys(grouped)) {
      const projectName = pid === '_none'
        ? '未分配项目'
        : (projectStore.projectOptions.find(p => p.id === Number(pid))?.name || '未知项目')
      lines.push(projectName)
      grouped[pid].forEach((t, i) => {
        lines.push(`${i + 1}.${t.title || '无标题'}`)
      })
      lines.push('')
    }

    exportText.value = lines.join('\n').trim()
    exportNoteTitle.value = `任务导出 ${new Date().toLocaleDateString()}`
    exportVisible.value = true
  } catch (e) {
    ElMessage.error('获取导出数据失败')
  }
}

const copyToClipboard = async () => {
  exportCopying.value = true
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(exportText.value)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = exportText.value
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  } finally {
    exportCopying.value = false
  }
}

const exportToNote = async () => {
  if (!exportNoteTitle.value.trim()) {
    ElMessage.warning('请输入笔记标题')
    return
  }
  exportNoteLoading.value = true
  try {
    await createTask({
      title: exportNoteTitle.value,
      description: exportText.value,
      isNote: true,
      projectId: filters.projectId || null
    })
    ElMessage.success('已导出为笔记')
    exportVisible.value = false
    emitRefresh()
  } catch (e) {
    ElMessage.error('导出到笔记失败')
  } finally {
    exportNoteLoading.value = false
  }
}

watch(() => route.query.projectId, (v) => {
  filters.projectId = v || ''
  loadTaskList()
})

watch(() => route.query.status, (v) => {
  filters.status = v || 'INCOMPLETE'
  loadTaskList()
})

const handleRefreshData = () => {
  projectStore.loadProjects()
  loadTaskList()
}

let cleanupRefresh = null

onMounted(async () => {
  filters.status = route.query.status || 'INCOMPLETE'
  if (route.query.projectId) filters.projectId = route.query.projectId
  await projectStore.loadProjects()
  await loadTaskList()
  cleanupRefresh = onRefreshData(handleRefreshData)
})

onBeforeUnmount(() => {
  cleanupRefresh?.()
})
</script>

<style scoped>
.task-list-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.tasks-scroll { flex: 1; overflow-y: auto; }
.project-section { margin-bottom: 24px; }
.project-section-header {
  display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
  padding-bottom: 8px; border-bottom: 1px solid #f0f0f0;
}
.project-section-name { font-size: 14px; font-weight: 600; color: #409EFF; }
.project-section-count {
  background: #ecf5ff; color: #409EFF; padding: 1px 8px;
  border-radius: 10px; font-size: 12px; font-weight: 500;
}
.context-menu {
  position: fixed; z-index: 2000; background: #fff;
  border: 1px solid #e4e7ed; border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  padding: 4px 0; min-width: 140px;
}
.menu-item {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; font-size: 13px; cursor: pointer;
  color: #606266;
}
.menu-item:hover { background: #f5f7fa; color: #409eff; }
</style>
