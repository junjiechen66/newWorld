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
            <el-option v-for="item in projectStore.projectOptions" :key="item.id" :label="item.name" :value="item.id" />
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
      <div v-else class="tasks-grid">
        <TaskCard
          v-for="note in notes" :key="note.id"
          :task="note"
          :showDescription="true"
          taskType="note"
          @click="openDetail"
          @right-click="handleCardRightClick"
          @share="handleCardShare"
        />
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

    <!-- Note Context Menu -->
    <Teleport to="body">
      <div v-show="cardMenu.visible" class="context-menu" :style="{ left: cardMenu.x + 'px', top: cardMenu.y + 'px' }" @click.stop>
        <div class="menu-item" v-if="cardMenu.note?.accessLevel === 'OWNER' || cardMenu.note?.accessLevel === 'EDIT'" @click="cardMenuEdit"><el-icon><Edit /></el-icon> 编辑信息</div>
        <div class="menu-item" v-if="cardMenu.note?.accessLevel === 'OWNER'" @click="cardMenuShare"><el-icon><Share /></el-icon> 共享设置</div>
      </div>
    </Teleport>

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
import { getTaskList, createTask, updateTask, deleteTask } from '@/api/task'
import { useProjectStore } from '@/stores/project'
import { Search, Plus, Loading, Edit, Share } from '@element-plus/icons-vue'
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

// Detail panel
const detailVisible = ref(false)
const viewNote = ref(null)

// Edit
const editing = ref(false)
const editForm = reactive({ title: '', projectId: null })
const editNoteRef = ref(null)

// Create
const createVisible = ref(false)
const createForm = reactive({ title: '', description: '', projectId: null })

// Share dialog
const shareDialogVisible = ref(false)
const shareResource = ref(null)

// Card context menu
const cardMenu = reactive({ visible: false, x: 0, y: 0, note: null })

const handleCardRightClick = ({ task }) => {
  openEditDialog(task)
}

const cardMenuEdit = () => {
  cardMenu.visible = false
  if (cardMenu.note) openEditDialog(cardMenu.note)
}

const cardMenuShare = () => {
  cardMenu.visible = false
  if (cardMenu.note) {
    shareResource.value = cardMenu.note
    shareDialogVisible.value = true
  }
}

const handleCardShare = (task) => {
  shareResource.value = task
  shareDialogVisible.value = true
}

const openShareFromEdit = () => {
  if (editNoteRef.value) {
    shareResource.value = editNoteRef.value
    editing.value = false
    shareDialogVisible.value = true
  }
}

document.addEventListener('click', () => { cardMenu.visible = false })

const loadNotes = async () => {
  loading.value = true
  try {
    const params = { isNote: true }
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

// Right-click opens edit dialog (other info: project/group)
const openEditDialog = (note) => {
  editNoteRef.value = note
  Object.assign(editForm, { title: note.title || '', projectId: note.projectId })
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
