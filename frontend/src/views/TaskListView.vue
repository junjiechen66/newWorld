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
          <el-select v-model="filters.priority" placeholder="优先级" clearable style="width:100px" @change="loadTaskList">
            <el-option label="高" value="RED" />
            <el-option label="中" value="YELLOW" />
            <el-option label="低" value="BLUE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filters.projectId" placeholder="项目" clearable style="width:130px" @change="loadTaskList">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadTaskList">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Task Grid -->
    <div class="tasks-scroll">
      <div v-if="loading" class="list-status"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="tasks.length === 0" class="list-status empty">暂无任务</div>
      <div v-else class="tasks-grid">
        <div v-for="task in tasks" :key="task.id" class="task-card" @click="openDetail(task)">
          <div class="task-header">
            <h3 class="task-title" :class="{ 'is-done': task.status === 'DONE' }">{{ task.title || '无标题' }}</h3>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, task)" @click.stop>
              <el-button text size="small" class="task-more-btn" @click.stop>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="delete" divided style="color:var(--danger-color)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="task-preview">{{ task.description || '暂无描述' }}</div>
          <div class="task-footer">
            <el-tag
              :type="statusTagType(task.status)"
              size="small" effect="plain" class="status-tag"
              @click.stop="cycleStatus(task)"
            >
              {{ statusLabel(task.status) }}
            </el-tag>
            <span v-if="task.priority && task.priority !== 'NONE'" class="priority-badge" :class="'priority-' + task.priority.toLowerCase()">
              {{ priorityLabel(task.priority) }}
            </span>
            <span v-if="task.dueDate" class="task-date">
              <el-icon><Calendar /></el-icon> {{ task.dueDate }}
            </span>
            <span v-if="task.projectName" class="task-group-badge">{{ task.projectName }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Detail Panel -->
    <Teleport to="body">
      <transition name="panel-slide">
        <div v-if="detailVisible" class="detail-overlay" @click.self="closeDetail">
          <div class="detail-panel">
            <div class="panel-header">
              <span class="panel-status" :style="{ background: statusColor }">{{ statusText }}</span>
              <span class="panel-close" @click="closeDetail">✕</span>
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

    <!-- Edit Dialog -->
    <el-dialog v-model="editing" title="编辑任务" width="480px" :close-on-click-modal="false">
      <el-form :model="editForm" size="small" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width:100%">
            <el-option label="未完成" value="INCOMPLETE" />
            <el-option label="已完成" value="DONE" />
            <el-option label="搁置" value="SHELVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="editForm.priority">
            <el-radio value="NONE">无</el-radio>
            <el-radio value="RED">高</el-radio>
            <el-radio value="YELLOW">中</el-radio>
            <el-radio value="BLUE">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="editForm.projectId" placeholder="选择项目" clearable style="width:100%">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="editForm.startDate" type="date" placeholder="开始" style="width:48%" value-format="YYYY-MM-DD" />
          <el-date-picker v-model="editForm.dueDate" type="date" placeholder="截止" style="width:48%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="editing = false">取消</el-button>
        <el-button size="small" type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" title="新建任务" width="480px" :close-on-click-modal="false">
      <el-form :model="createForm" size="small" label-width="60px">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="createForm.projectId" placeholder="选择项目" clearable style="width:100%">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="createForm.startDate" type="date" placeholder="开始" style="width:48%" value-format="YYYY-MM-DD" />
          <el-date-picker v-model="createForm.dueDate" type="date" placeholder="截止" style="width:48%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="createForm.status" style="width:100%">
            <el-option label="未完成" value="INCOMPLETE" />
            <el-option label="已完成" value="DONE" />
            <el-option label="搁置" value="SHELVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="createForm.priority">
            <el-radio value="NONE">无</el-radio>
            <el-radio value="RED">高</el-radio>
            <el-radio value="YELLOW">中</el-radio>
            <el-radio value="BLUE">低</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="createVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskList, createTask, updateTask, deleteTask, updateTaskStatus } from '@/api/task'
import { getProjectList } from '@/api/project'
import { useProjectStore } from '@/stores/project'
import { Search, Plus, Loading, MoreFilled, Calendar } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'

const route = useRoute()
const projectStore = useProjectStore()

const loading = ref(false)
const tasks = ref([])
const projectOptions = ref([])

const loadProjects = async () => {
  try {
    const res = await getProjectList()
    projectOptions.value = res.data
  } catch (e) {}
}

const pageTitle = computed(() => {
  return route.query.projectId ? (projectOptions.value.find(p => p.id === Number(route.query.projectId))?.name || '全部任务') : '全部任务'
})

const filters = reactive({
  keyword: '', status: '', priority: '', projectId: ''
})

// Right panel
const detailVisible = ref(false)
const viewTask = ref(null)

// Edit overlay
const editing = ref(false)
const editForm = reactive({
  title: '', status: 'INCOMPLETE', priority: 'NONE', projectId: null, startDate: null, dueDate: null
})

// Edit title & desc (always editable in panel)
const editTitle = ref('')
const editDesc = ref('')
const editTaskRef = ref(null)

// Create dialog
const createVisible = ref(false)
const createForm = reactive({
  title: '', priority: 'NONE', status: 'INCOMPLETE', projectId: null, startDate: null, dueDate: null
})

// Status helpers
const statusLabel = (s) => {
  const map = { INCOMPLETE: '未完成', DONE: '已完成', SHELVED: '搁置' }
  return map[s] || '未完成'
}
const statusTagType = (s) => {
  const map = { INCOMPLETE: 'info', DONE: 'success', SHELVED: 'warning' }
  return map[s] || 'info'
}
const statusColor = computed(() => {
  if (!viewTask.value) return '#909399'
  const map = { INCOMPLETE: '#909399', DONE: '#67C23A', SHELVED: '#c0c4cc' }
  return map[viewTask.value.status] || '#909399'
})
const statusText = computed(() => {
  if (!viewTask.value) return ''
  return statusLabel(viewTask.value.status)
})
const priorityLabel = (p) => {
  const map = { RED: '高', YELLOW: '中', BLUE: '低' }
  return map[p] || ''
}

// Cycle status: INCOMPLETE → DONE → SHELVED → INCOMPLETE
const cycleStatus = async (task) => {
  const cycle = ['INCOMPLETE', 'DONE', 'SHELVED']
  const idx = cycle.indexOf(task.status)
  const next = cycle[(idx + 1) % cycle.length]
  try {
    await updateTaskStatus(task.id, next)
    task.status = next
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

const loadTaskList = async () => {
  loading.value = true
  try {
    const params = { isNote: false }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    if (filters.priority) params.priority = filters.priority
    if (filters.projectId) params.projectId = filters.projectId
    const res = await getTaskList(params)
    tasks.value = (res.data || []).map(t => {
      const p = projectOptions.value.find(o => o.id === t.projectId)
      return { ...t, projectName: p?.name || '' }
    })
  } catch (e) { tasks.value = [] }
  finally { loading.value = false }
}

// Detail panel
const openDetail = (task) => {
  viewTask.value = task
  editing.value = false
  detailVisible.value = true
  editTitle.value = task.title || ''
  editDesc.value = task.description || ''
}

const closeDetail = () => {
  detailVisible.value = false
  viewTask.value = null
  editing.value = false
  editDesc.value = ''
}

const saveEdit = async () => {
  const task = editTaskRef.value
  if (!task) return
  if (!editForm.title.trim()) { ElMessage.warning('请输入标题'); return }
  try {
    await updateTask(task.id, { ...editForm })
    ElMessage.success('已保存')
    editing.value = false
    editTaskRef.value = null
    await loadTaskList()
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
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
    await loadTaskList()
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

const handleCommand = (cmd, task) => {
  if (cmd === 'edit') {
    editTaskRef.value = task
    Object.assign(editForm, {
      title: task.title || '',
      status: task.status || 'INCOMPLETE',
      priority: task.priority || 'NONE',
      projectId: task.projectId,
      startDate: task.startDate,
      dueDate: task.dueDate
    })
    editing.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定要删除此任务吗？', '提示').then(() => {
      deleteTask(task.id).then(() => {
        ElMessage.success('已删除')
        loadTaskList()
        window.dispatchEvent(new CustomEvent('refresh-data'))
      }).catch(() => {})
    }).catch(() => {})
  }
}

const showCreateDialog = () => {
  Object.assign(createForm, { title: '', priority: 'NONE', status: 'INCOMPLETE', projectId: null, startDate: null, dueDate: null })
  createVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.title.trim()) { ElMessage.warning('请输入标题'); return }
  try {
    await createTask({ ...createForm, isNote: false })
    ElMessage.success('已创建')
    createVisible.value = false
    await loadTaskList()
    await projectStore.fetchTree()
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

watch(() => route.query.projectId, (v) => {
  filters.projectId = v || ''
  loadTaskList()
})

// Listen for data refresh events from Sidebar
const handleRefreshData = () => {
  loadProjects()
  loadTaskList()
}

onMounted(async () => {
  if (route.query.status) filters.status = route.query.status
  if (route.query.projectId) filters.projectId = route.query.projectId
  await loadProjects()
  await loadTaskList()
  window.addEventListener('refresh-data', handleRefreshData)
})

onBeforeUnmount(() => {
  window.removeEventListener('refresh-data', handleRefreshData)
})
</script>

<style scoped>
.task-list-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-shrink: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; margin: 0; }
.filter-card { margin-bottom: 12px; border-radius: 8px; flex-shrink: 0; }
.tasks-scroll { flex: 1; overflow-y: auto; }
.list-status { text-align: center; padding: 60px 0; color: var(--text-secondary); font-size: 14px; }
.tasks-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.task-card { background: #fff; border-radius: 8px; padding: 16px; cursor: pointer; border: 1px solid #eee; transition: box-shadow 0.15s; }
.task-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.task-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; gap: 8px; }
.task-title { font-size: 14px; font-weight: 600; margin: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-title.is-done { text-decoration: line-through; color: #999; }
.task-more-btn { flex-shrink: 0; }
.task-preview { font-size: 13px; color: #666; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
.task-footer { display: flex; align-items: center; gap: 8px; margin-top: 10px; font-size: 12px; flex-wrap: wrap; }
.status-tag { flex-shrink: 0; cursor: pointer; }
.priority-badge { padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.priority-badge.priority-red { background: #fef0f0; color: #F56C6C; }
.priority-badge.priority-yellow { background: #fdf6ec; color: #E6A23C; }
.priority-badge.priority-blue { background: #ecf5ff; color: #409EFF; }
.task-date { display: flex; align-items: center; gap: 3px; color: #999; }
.task-group-badge { background: #ecf5ff; color: #409EFF; padding: 1px 8px; border-radius: 4px; font-size: 11px; }

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
  position: relative;
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
