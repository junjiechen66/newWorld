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
            <el-option-group v-for="group in groupedProjects" :key="group.label" :label="group.label">
              <el-option v-for="item in group.options" :key="item.id" :label="item.name" :value="item.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filterArchived" placeholder="归档" clearable style="width:110px" @change="loadNotes">
            <el-option label="未归档" :value="false" />
            <el-option label="已归档" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="loadNotes">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Notes Grid grouped by project -->
    <div class="notes-scroll">
      <div v-if="loading" class="list-status"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
      <div v-else-if="notes.length === 0" class="list-status empty">暂无笔记</div>
      <div v-else>
        <div v-for="group in groupedNotes" :key="group.projectId" class="project-section">
          <div class="project-section-header">
            <span class="project-section-name">{{ group.projectName }}</span>
            <span class="project-section-count">{{ group.notes.length }}</span>
          </div>
          <div class="tasks-grid">
            <TaskCard
              v-for="note in group.notes" :key="note.id"
              :task="note"
              :showDescription="true"
              taskType="note"
              @click="openDetail"
              @right-click="openEditDialog"
              @share="handleCardShare"
              @archive="handleCardArchive"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Shared Editable Panel (note mode) -->
    <TaskEditablePanel
      v-model:visible="detailVisible"
      :task="viewNote"
      headerType="date"
      placeholder="笔记"
      :readonly="viewNote?.accessLevel === 'VIEW'"
      @save="handleSaveDesc"
    />

    <!-- Share Dialog -->
    <ShareDialog
      v-model:visible="shareDialogVisible"
      :resource="shareResource"
      resourceType="TASK"
    />

    <!-- Edit Dialog -->
    <el-dialog v-model="editing" title="编辑笔记" width="420px" :close-on-click-modal="false">
      <el-form :model="editForm" size="small" label-width="60px" @keyup.enter="saveEdit" @submit.prevent>
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="editForm.projectId" placeholder="选择项目" clearable style="width:100%">
            <el-option-group v-for="group in groupedProjects" :key="group.label" :label="group.label">
              <el-option v-for="item in group.options" :key="item.id" :label="item.name" :value="item.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="small" type="danger" plain @click="deleteEditNote">删除</el-button>
          <el-button size="small" @click="openShareFromEdit"><el-icon><Share /></el-icon> 共享</el-button>
          <el-button size="small" v-if="!editNoteRef?.archived" @click="archiveEditNote"><el-icon><FolderChecked /></el-icon> 归档</el-button>
          <el-button size="small" v-if="editNoteRef?.archived" @click="unarchiveEditNote"><el-icon><FolderOpened /></el-icon> 取消归档</el-button>
          <div class="dialog-footer-right">
            <el-button size="small" @click="editing = false">取消</el-button>
            <el-button size="small" type="primary" @click="saveEdit">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" title="新建笔记" width="520px" :close-on-click-modal="false">
      <el-form :model="createForm" size="small" label-width="60px" @keyup.enter="submitCreate" @submit.prevent>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <MarkdownEditor v-model="createForm.description" :rows="8" start-editing placeholder="笔记内容（支持Markdown）..." />
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="createForm.projectId" placeholder="选择项目" clearable style="width:100%">
            <el-option-group v-for="group in groupedProjects" :key="group.label" :label="group.label">
              <el-option v-for="item in group.options" :key="item.id" :label="item.name" :value="item.id" />
            </el-option-group>
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
import { getTaskList, createTask, updateTask, deleteTask, noteArchiveTask, noteUnarchiveTask } from '@/api/task'
import { useProjectStore } from '@/stores/project'
import { Search, Plus, Loading, Share, FolderChecked, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'
import TaskEditablePanel from '@/components/task/TaskEditablePanel.vue'
import TaskCard from '@/components/task/TaskCard.vue'
import ShareDialog from '@/components/share/ShareDialog.vue'
import { emitRefresh, onRefreshData } from '@/utils/events'

const route = useRoute()
const projectStore = useProjectStore()

const loading = ref(false)
const notes = ref([])
const searchKeyword = ref('')
const filterProjectId = ref('')
const filterArchived = ref(false)

const pageTitle = computed(() => {
  return route.query.projectId ? (projectStore.projectOptions.find(p => p.id === Number(route.query.projectId))?.name || '全部笔记') : '全部笔记'
})

// Grouped project options from tree data
const groupedProjects = computed(() => {
  const tree = projectStore.treeData
  if (!tree || !tree.length) {
    return [{ label: '', options: projectStore.projectOptions }]
  }
  return tree.map(group => ({
    label: group.name,
    options: (group.children || [])
      .filter(c => c.type === 'project')
      .map(p => ({ id: p.id, name: p.name }))
  })).filter(g => g.options.length > 0)
})

const groupedNotes = computed(() => {
  const map = {}
  const order = []
  notes.value.forEach(n => {
    const key = n.projectId || '_none'
    if (!map[key]) {
      map[key] = { projectId: n.projectId, projectName: n.projectName || '未分配项目', notes: [] }
      order.push(key)
    }
    map[key].notes.push(n)
  })
  return order.map(k => map[k])
})

// Detail panel
const detailVisible = ref(false)
const viewNote = ref(null)

// Edit
const editing = ref(false)
const editForm = reactive({ title: '', description: '', projectId: null })
const editNoteRef = ref(null)

// Create
const createVisible = ref(false)
const createForm = reactive({ title: '', description: '', projectId: null })

// Share dialog
const shareDialogVisible = ref(false)
const shareResource = ref(null)

const handleCardShare = (task) => {
  shareResource.value = task
  shareDialogVisible.value = true
}

const archiveEditNote = async () => {
  const note = editNoteRef.value
  if (!note) return
  try {
    await noteArchiveTask(note.id)
    ElMessage.success('已归档')
    editing.value = false
    editNoteRef.value = null
    await loadNotes()
  } catch (e) {}
}

const unarchiveEditNote = async () => {
  const note = editNoteRef.value
  if (!note) return
  try {
    await noteUnarchiveTask(note.id)
    ElMessage.success('已取消归档')
    editing.value = false
    editNoteRef.value = null
    await loadNotes()
  } catch (e) {}
}

const handleCardArchive = async (note) => {
  try {
    await noteArchiveTask(note.id)
    ElMessage.success('已归档')
    await loadNotes()
  } catch (e) {}
}

const openShareFromEdit = () => {
  if (editNoteRef.value) {
    shareResource.value = editNoteRef.value
    editing.value = false
    shareDialogVisible.value = true
  }
}

const resetFilters = () => {
  searchKeyword.value = ''
  filterProjectId.value = ''
  filterArchived.value = null
  loadNotes()
}

const loadNotes = async () => {
  loading.value = true
  try {
    const params = { isNote: true }
    if (filterArchived.value !== null && filterArchived.value !== '' && filterArchived.value !== undefined) {
      params.archived = filterArchived.value
    }
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
    if (filterProjectId.value) {
      params.projectId = filterProjectId.value
    } else if (route.query.projectId) {
      params.projectId = route.query.projectId
    }
    const res = await getTaskList(params)
    notes.value = (res.data || []).map(n => {
      const p = projectStore.projectOptions.find(o => o.id === n.projectId)
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
  detailVisible.value = true
}

const openEditDialog = ({ task: note }) => {
  editNoteRef.value = note
  editForm.title = note.title || ''
  editForm.description = note.description || ''
  editForm.projectId = note.projectId
  editing.value = true
}

const handleSaveDesc = async ({ title, description }) => {
  if (!viewNote.value) return
  try {
    await updateTask(viewNote.value.id, { title, description })
    ElMessage.success('已保存')
    viewNote.value.title = title
    viewNote.value.description = description
    await loadNotes()
  } catch (e) {}
}

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

const deleteEditNote = () => {
  const note = editNoteRef.value
  if (!note) return
  ElMessageBox.confirm('确定要删除此笔记吗？', '提示').then(() => {
    deleteTask(note.id).then(() => {
      ElMessage.success('已删除')
      editing.value = false
      editNoteRef.value = null
      loadNotes()
    }).catch(() => {})
  }).catch(() => {})
}

const showCreateDialog = () => {
  Object.assign(createForm, { title: '', description: '', projectId: null })
  createVisible.value = true
}

const submitCreate = async (e) => {
  if (e && e.target && e.target.tagName === 'TEXTAREA') return
  if (!createForm.title.trim()) { ElMessage.warning('请输入标题'); return }
  try {
    await createTask({ ...createForm, isNote: true })
    ElMessage.success('已创建')
    createVisible.value = false
    await loadNotes()
    await projectStore.fetchTree()
    emitRefresh()
  } catch (e) {}
}

watch(() => route.query.projectId, () => { loadNotes() })

const handleRefreshData = () => {
  projectStore.loadProjects()
  loadNotes()
}

let cleanupRefresh = null

onMounted(async () => {
  await projectStore.loadProjects()
  await loadNotes()
  cleanupRefresh = onRefreshData(handleRefreshData)
})

onBeforeUnmount(() => {
  cleanupRefresh?.()
})
</script>

<style scoped>
.notes-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.notes-scroll { flex: 1; overflow-y: auto; }
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
.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.dialog-footer-right {
  display: flex;
  gap: 8px;
  margin-left: auto;
}
</style>
