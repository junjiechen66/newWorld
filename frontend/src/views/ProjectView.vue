<template>
  <div class="project-view">
    <div class="page-header">
      <h2>项目总览</h2>
      <div class="header-actions">
        <el-button size="small" @click="showAddGroup = true">
          <el-icon><Plus /></el-icon> 项目分组
        </el-button>
        <el-button type="primary" size="small" @click="showAddProject = true">
          <el-icon><Plus /></el-icon> 新建项目
        </el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col v-for="(group, groupIndex) in projectStore.treeData" :key="group.id" :span="24" style="margin-bottom: 16px;"
        draggable="true"
        @dragstart="onGroupDragStart($event, groupIndex)"
        @dragover.prevent="onGroupDragOver($event, groupIndex)"
        @dragenter="groupDragOverIndex = groupIndex"
        @dragleave="groupDragOverIndex = null"
        @drop="onGroupDrop($event, groupIndex)"
        @dragend="groupDragOverIndex = null"
        :class="{ 'drag-over': groupDragOverIndex === groupIndex }"
      >
        <el-card shadow="never" class="group-card">
          <template #header>
            <div class="group-header">
              <span><el-icon><Folder /></el-icon> {{ group.name }}</span>
            </div>
          </template>
          <el-row :gutter="12">
            <el-col v-for="(project, projIndex) in group.children" :key="project.id" :span="8" style="margin-bottom: 12px;"
              draggable="true"
              @dragstart.stop="onProjectDragStart($event, group.id, projIndex)"
              @dragover.prevent.stop="onProjectDragOver($event, projIndex)"
              @dragenter.stop="projectDragOverIndex = projIndex"
              @dragleave.stop="projectDragOverIndex = null"
              @drop.stop="onProjectDrop($event, group.id, projIndex)"
              @dragend.stop="projectDragOverIndex = null"
              :class="{ 'drag-over': projectDragOverIndex === projIndex }"
            >
              <el-card shadow="hover" class="project-card" @click="router.push('/calendar?projectId=' + project.id)" @contextmenu.prevent="openEditProject(project)">
                <div class="project-card-header">
                  <el-tag :color="project.color || '#409EFF'" class="project-color-tag" />
                  <span class="project-name">{{ project.name }}</span>
                </div>
                <div class="project-stats">
                  <span class="stat">未完成: {{ getTaskCountByStatus(project.children, 'INCOMPLETE') }}</span>
                  <span class="stat">已完成: {{ getTaskCountByStatus(project.children, 'DONE') }}</span>
                  <span class="stat">搁置: {{ getTaskCountByStatus(project.children, 'SHELVED') }}</span>
                </div>
              </el-card>
            </el-col>
            <el-col v-if="!group.children?.length" :span="24">
              <el-empty description="暂无项目" :image-size="60" />
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- Add Group Dialog -->
    <el-dialog v-model="showAddGroup" title="新建项目分组" width="400px">
      <el-form :model="groupForm" label-width="80px" size="small" @keyup.enter="submitGroup" @submit.prevent>
        <el-form-item label="分组名称">
          <el-input v-model="groupForm.name" placeholder="请输入分组名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddGroup = false">取消</el-button>
        <el-button type="primary" @click="submitGroup">确定</el-button>
      </template>
    </el-dialog>

    <!-- Add Project Dialog -->
    <el-dialog v-model="showAddProject" title="新建项目" width="450px">
      <el-form :model="projectForm" label-width="80px" size="small" @keyup.enter="submitProject" @submit.prevent>
        <el-form-item label="所属分组">
          <el-select v-model="projectForm.groupId" placeholder="选择分组" style="width: 100%">
            <el-option v-for="g in projectStore.treeData" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="projectForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目颜色">
          <div class="color-picker-group">
            <span
              v-for="c in presetColors"
              :key="c"
              class="color-dot"
              :class="{ active: projectForm.color === c }"
              :style="{ background: c }"
              @click="projectForm.color = c"
            />
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddProject = false">取消</el-button>
        <el-button type="primary" @click="submitProject">确定</el-button>
      </template>
    </el-dialog>

    <!-- Edit Project Dialog -->
    <el-dialog v-model="showEditProject" title="编辑项目" width="450px" :close-on-click-modal="false">
      <el-form :model="editProjectForm" label-width="80px" size="small" @keyup.enter="submitEditProject" @submit.prevent>
        <el-form-item label="所属分组">
          <el-select v-model="editProjectForm.groupId" placeholder="选择分组" style="width: 100%">
            <el-option v-for="g in projectStore.treeData" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="editProjectForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目颜色">
          <div class="color-picker-group">
            <span
              v-for="c in presetColors"
              :key="c"
              class="color-dot"
              :class="{ active: editProjectForm.color === c }"
              :style="{ background: c }"
              @click="editProjectForm.color = c"
            />
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editProjectForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditProject = false">取消</el-button>
        <el-button type="primary" @click="submitEditProject">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { createProject, updateProject, sortProjects } from '@/api/project'
import { createGroup, sortGroups } from '@/api/group'
import { ElMessage } from 'element-plus'
import { Plus, Folder } from '@element-plus/icons-vue'
import { PRESET_COLORS } from '@/utils/constants'

const router = useRouter()
const projectStore = useProjectStore()

const presetColors = PRESET_COLORS

// Drag sort state
const groupDragIndex = ref(null)
const groupDragOverIndex = ref(null)
const projectDragGroupId = ref(null)
const projectDragIndex = ref(null)
const projectDragOverIndex = ref(null)

const onGroupDragStart = (e, index) => {
  groupDragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', 'group')
}

const onGroupDragOver = (e, index) => {
  groupDragOverIndex.value = index
}

const onGroupDrop = async (e, dropIndex) => {
  e.preventDefault()
  const dragIndex = groupDragIndex.value
  if (dragIndex === null || dragIndex === dropIndex) return
  const treeData = projectStore.treeData
  const items = [...treeData]
  const [moved] = items.splice(dragIndex, 1)
  items.splice(dropIndex, 0, moved)
  projectStore.treeData = items
  groupDragIndex.value = null
  groupDragOverIndex.value = null
  // Save sort order
  const sortItems = items.map((g, i) => ({ id: g.id, sortOrder: i }))
  try {
    await sortGroups(sortItems)
  } catch (err) {
    ElMessage.error('排序失败')
  }
}

const onProjectDragStart = (e, groupId, index) => {
  projectDragGroupId.value = groupId
  projectDragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', 'project')
}

const onProjectDragOver = (e, index) => {
  projectDragOverIndex.value = index
}

const onProjectDrop = async (e, groupId, dropIndex) => {
  e.preventDefault()
  e.stopPropagation()
  const dragIndex = projectDragIndex.value
  const dragGroupId = projectDragGroupId.value
  if (dragGroupId !== groupId || dragIndex === null || dragIndex === dropIndex) return
  const group = projectStore.treeData.find(g => g.id === groupId)
  if (!group || !group.children) return
  const items = [...group.children]
  const [moved] = items.splice(dragIndex, 1)
  items.splice(dropIndex, 0, moved)
  group.children = items
  projectDragIndex.value = null
  projectDragOverIndex.value = null
  projectDragGroupId.value = null
  // Save sort order
  const sortItems = items.map((p, i) => ({ id: p.id, sortOrder: i }))
  try {
    await sortProjects(sortItems)
  } catch (err) {
    ElMessage.error('排序失败')
  }
}

// Add Group
const showAddGroup = ref(false)
const groupForm = reactive({ name: '' })

const submitGroup = async () => {
  if (!groupForm.name.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  try {
    await createGroup({ ...groupForm })
    ElMessage.success('分组创建成功')
    showAddGroup.value = false
    groupForm.name = ''
    await projectStore.fetchTree()
  } catch (e) {}
}

const showAddProject = ref(false)
const projectForm = reactive({
  groupId: null,
  name: '',
  color: '#409EFF',
  description: ''
})

const getTaskCountByStatus = (tasks, status) => {
  if (!tasks) return 0
  return tasks.filter(t => t.status === status).length
}

const submitProject = async () => {
  if (!projectForm.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  if (!projectForm.groupId) {
    ElMessage.warning('请选择所属分组')
    return
  }
  try {
    await createProject({ ...projectForm })
    ElMessage.success('项目创建成功')
    showAddProject.value = false
    projectForm.groupId = null
    projectForm.name = ''
    projectForm.color = '#409EFF'
    projectForm.description = ''
    await projectStore.fetchTree()
  } catch (e) {}
}

// Edit project
const showEditProject = ref(false)
const editProjectId = ref(null)
const editProjectForm = reactive({
  groupId: null,
  name: '',
  color: '#409EFF',
  description: ''
})

const openEditProject = (project) => {
  editProjectId.value = project.id
  Object.assign(editProjectForm, {
    groupId: project.groupId,
    name: project.name || '',
    color: project.color || '#409EFF',
    description: project.description || ''
  })
  showEditProject.value = true
}

const submitEditProject = async () => {
  if (!editProjectForm.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  if (!editProjectForm.groupId) {
    ElMessage.warning('请选择所属分组')
    return
  }
  try {
    await updateProject(editProjectId.value, { ...editProjectForm })
    ElMessage.success('项目已更新')
    showEditProject.value = false
    await projectStore.fetchTree()
  } catch (e) {}
}

onMounted(async () => {
  if (!projectStore.treeData.length) {
    await projectStore.fetchTree()
  }
})
</script>

<style scoped>
.project-view { padding: 16px; }
.header-actions { display: flex; gap: 8px; }
.group-card { border-radius: 8px; }
.group-header { font-size: 15px; font-weight: 500; display: flex; align-items: center; gap: 8px; }
.project-card { cursor: pointer; border-radius: 6px; transition: transform 0.15s; }
.project-card:hover { transform: translateY(-2px); }
.drag-over { opacity: 0.6; }
.project-card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.project-color-tag { width: 12px; height: 12px; border-radius: 3px; padding: 0; border: none; }
.project-name { font-size: 14px; font-weight: 500; }
.project-stats { display: flex; gap: 12px; font-size: 12px; color: var(--text-secondary); }
</style>
