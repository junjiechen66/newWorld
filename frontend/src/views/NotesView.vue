<template>
  <div class="notes-page">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <el-button type="primary" size="small" @click="showCreateDialog">
        <el-icon><Plus /></el-icon> 新建笔记
      </el-button>
    </div>

    <!-- Search -->
    <el-card class="filter-card" shadow="never">
      <el-form inline size="small" @keyup.enter="loadNotes">
        <el-form-item>
          <el-input v-model="searchKeyword" placeholder="搜索笔记..." clearable style="width:250px" @clear="loadNotes">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filterProjectId" placeholder="项目" clearable style="width:130px" @change="loadNotes">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadNotes">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Notes Grid -->
    <div class="notes-scroll">
      <div v-if="loading" class="list-status"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="notes.length === 0" class="list-status empty">暂无笔记</div>
      <div v-else class="notes-grid">
        <div v-for="note in notes" :key="note.id" class="note-card" @click="openDetail(note)">
          <div class="note-header">
            <h3 class="note-title">{{ note.title || '无标题' }}</h3>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, note)" @click.stop>
              <el-button text size="small" class="note-more-btn" @click.stop>
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
          <div class="note-preview">{{ note.description || '暂无内容' }}</div>
          <div class="note-footer">
            <span class="note-date">{{ note.createTime ? note.createTime.substring(0, 10) : '' }}</span>
            <span v-if="note.projectName" class="note-group-badge">{{ note.projectName }}</span>
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
              <span class="panel-date">{{ viewNote?.createTime ? viewNote.createTime.substring(0, 10) : '' }}</span>
              <span class="panel-close" @click="closeDetail">✕</span>
            </div>
            <div class="panel-body">
              <div class="title-editor-area">
                <label class="section-label">标题</label>
                <el-input v-model="editTitle" size="large" placeholder="笔记标题..." class="title-input" />
              </div>
              <div class="desc-editor-area">
                <label class="section-label">内容（支持Markdown）</label>
                <MarkdownEditor v-model="editDesc" placeholder="笔记内容（支持Markdown）..." />
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
    <el-dialog v-model="editing" title="编辑笔记" width="420px" :close-on-click-modal="false">
      <el-form :model="editForm" size="small" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="editForm.projectId" placeholder="选择项目分组" clearable style="width:100%">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="editing = false">取消</el-button>
        <el-button size="small" type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" title="新建笔记" width="520px" :close-on-click-modal="false">
      <el-form :model="createForm" size="small" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <MarkdownEditor v-model="createForm.description" :rows="8" placeholder="笔记内容（支持Markdown）..." />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="createForm.projectId" placeholder="选择项目分组" clearable style="width:100%">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
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
import { getTaskList, createTask, updateTask, deleteTask } from '@/api/task'
import { getProjectList } from '@/api/project'
import { useProjectStore } from '@/stores/project'
import { Search, Plus, Loading, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'

const route = useRoute()
const projectStore = useProjectStore()

const loading = ref(false)
const notes = ref([])
const searchKeyword = ref('')
const filterProjectId = ref('')
const projectOptions = ref([])

const loadProjects = async () => {
  try {
    const res = await getProjectList()
    projectOptions.value = res.data
  } catch (e) {}
}

const pageTitle = computed(() => {
  return route.query.projectId ? (projectOptions.value.find(p => p.id === Number(route.query.projectId))?.name || '全部笔记') : '全部笔记'
})

// Detail panel
const detailVisible = ref(false)
const viewNote = ref(null)
const editing = ref(false)
const editForm = reactive({
  title: '', description: '', projectId: null
})

// Edit title & desc (always editable)
const editTitle = ref('')
const editDesc = ref('')

// Create
const createVisible = ref(false)
const createForm = reactive({
  title: '', description: '', projectId: null
})

const loadNotes = async () => {
  loading.value = true
  try {
    const params = { isNote: true }
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    if (filterProjectId.value) {
      params.projectId = filterProjectId.value
    } else if (route.query.projectId) {
      params.projectId = route.query.projectId
    }
    const res = await getTaskList(params)
    notes.value = (res.data || []).map(n => {
      const p = projectOptions.value.find(o => o.id === n.projectId)
      return { ...n, projectName: p?.name || '' }
    })
  } catch (e) {
    notes.value = []
  } finally {
    loading.value = false
  }
}

const openDetail = (note) => {
  viewNote.value = note
  editing.value = false
  detailVisible.value = true
  editTitle.value = note.title || ''
  editDesc.value = note.description || ''
}

const closeDetail = () => {
  detailVisible.value = false
  viewNote.value = null
  editing.value = false
  editDesc.value = ''
}

const editNoteRef = ref(null)

const saveEdit = async () => {
  const note = editNoteRef.value
  if (!note) return
  if (!editForm.title.trim()) { ElMessage.warning('请输入标题'); return }
  try {
    await updateTask(note.id, { ...editForm })
    ElMessage.success('已保存')
    editing.value = false
    editNoteRef.value = null
    await loadNotes()
  } catch (e) {}
}

const saveDesc = async () => {
  if (!viewNote.value) return
  try {
    await updateTask(viewNote.value.id, {
      title: editTitle.value,
      description: editDesc.value
    })
    ElMessage.success('已保存')
    viewNote.value.description = editDesc.value
    await loadNotes()
  } catch (e) {}
}

const handleCommand = (cmd, note) => {
  if (cmd === 'edit') {
    editNoteRef.value = note
    Object.assign(editForm, {
      title: note.title || '',
      description: note.description || '',
      projectId: note.projectId
    })
    editing.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定要删除此笔记吗？', '提示').then(() => {
      deleteTask(note.id).then(() => {
        ElMessage.success('已删除')
        loadNotes()
      }).catch(() => {})
    }).catch(() => {})
  }
}

const showCreateDialog = () => {
  Object.assign(createForm, { title: '', description: '', projectId: null })
  createVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.title.trim()) { ElMessage.warning('请输入标题'); return }
  try {
    await createTask({ ...createForm, isNote: true })
    ElMessage.success('已创建')
    createVisible.value = false
    await loadNotes()
    await projectStore.fetchTree()
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

watch(() => route.query.projectId, () => {
  loadNotes()
})

// Listen for data refresh events from Sidebar
const handleRefreshData = () => {
  loadProjects()
  loadNotes()
}

onMounted(async () => {
  await loadProjects()
  await loadNotes()
  window.addEventListener('refresh-data', handleRefreshData)
})

onBeforeUnmount(() => {
  window.removeEventListener('refresh-data', handleRefreshData)
})
</script>

<style scoped>
.notes-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-shrink: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; margin: 0; }
.filter-card { margin-bottom: 12px; border-radius: 8px; flex-shrink: 0; }
.notes-scroll { flex: 1; overflow-y: auto; }
.list-status { text-align: center; padding: 60px 0; color: var(--text-secondary); font-size: 14px; }
.notes-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.note-card { background: #fff; border-radius: 8px; padding: 16px; cursor: pointer; border: 1px solid #eee; transition: box-shadow 0.15s; }
.note-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.note-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; gap: 8px; }
.note-title { font-size: 14px; font-weight: 600; margin: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.note-more-btn { flex-shrink: 0; }
.note-preview { font-size: 13px; color: #666; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
.note-footer { display: flex; align-items: center; gap: 8px; margin-top: 10px; font-size: 12px; }
.note-date { color: #999; }
.note-group-badge { background: #ecf5ff; color: #409EFF; padding: 1px 8px; border-radius: 4px; font-size: 11px; }

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
.panel-date {
  font-size: 12px;
  color: #999;
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
.view-desc {
  font-size: 15px;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
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
