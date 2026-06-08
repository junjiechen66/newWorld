<template>
  <div class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
    <div class="sidebar-header">
      <el-icon :size="22"><Calendar /></el-icon>
      <span v-show="!appStore.sidebarCollapsed" class="sidebar-title">NewWorld</span>
    </div>
    
    <div class="sidebar-content">
      <!-- Collapsed nav icons -->
      <div class="collapsed-nav" v-if="appStore.sidebarCollapsed">
        <el-tooltip content="日历" placement="right">
          <div class="nav-icon" :class="{ active: route.path === '/calendar' }" @click="router.push('/calendar')">
            <el-icon :size="20"><Calendar /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="任务" placement="right">
          <div class="nav-icon" :class="{ active: route.path === '/tasks' }" @click="router.push('/tasks')">
            <el-icon :size="20"><List /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="笔记" placement="right">
          <div class="nav-icon" :class="{ active: route.path === '/notes' }" @click="router.push('/notes')">
            <el-icon :size="20"><EditPen /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="项目" placement="right">
          <div class="nav-icon" :class="{ active: route.path === '/projects' }" @click="router.push('/projects')">
            <el-icon :size="20"><Collection /></el-icon>
          </div>
        </el-tooltip>
        <div class="nav-divider"></div>
        <el-tooltip content="收起" placement="right">
          <div class="nav-icon" @click="appStore.toggleSidebar">
            <el-icon :size="20"><Fold /></el-icon>
          </div>
        </el-tooltip>
      </div>

      <!-- Quick stats -->
      <div class="stats-section" v-if="!appStore.sidebarCollapsed">
        <div class="stat-item" @click="navigateToTasks('INCOMPLETE')">
          <span class="stat-label">未完成</span>
          <span class="stat-value">{{ stats.incompleteCount || 0 }}</span>
        </div>
        <div class="stat-item" @click="navigateToTasks('DONE')">
          <span class="stat-label">已完成</span>
          <span class="stat-value">{{ stats.doneCount || 0 }}</span>
        </div>
        <div class="stat-item" @click="navigateToTasks('SHELVED')">
          <span class="stat-label">搁置</span>
          <span class="stat-value">{{ stats.shelvedCount || 0 }}</span>
        </div>
      </div>

      <el-divider style="margin: 8px 0;" v-if="!appStore.sidebarCollapsed" />

      <!-- Project Tree -->
      <div class="tree-section" v-if="!appStore.sidebarCollapsed">
        <div class="tree-header">
          <span>项目列表</span>
          <el-button text type="primary" size="small" @click="showAddGroup = true" v-if="!appStore.sidebarCollapsed">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <div
          class="all-projects-item"
          :class="{ active: isAllSelected }"
          @click="selectAll"
        >
          <el-icon :size="16"><FolderOpened /></el-icon>
          <span class="node-label">全部任务</span>
        </div>
        <el-tree
          ref="treeRef"
          :data="filteredTreeData"
          :props="treeProps"
          node-key="id"
          default-expand-all
          highlight-current
          @node-click="handleNodeClick"
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <div class="tree-node" @contextmenu.prevent="handleContextMenu($event, data)">
              <el-icon v-if="data.type === 'group'" :size="16"><Folder /></el-icon>
              <el-icon v-else-if="data.type === 'project'" :size="16" :color="data.color || '#409EFF'"><Document /></el-icon>
              <el-tag v-else :type="priorityTagType(data.priority)" size="small" class="priority-dot" />
              <span class="node-label">{{ data.name }}</span>
              <span class="task-count" v-if="data.type === 'project' && data.children?.length">
                {{ data.children.length }}
              </span>
            </div>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- Context Menu -->
    <Teleport to="body">
      <div v-show="contextMenu.visible" class="context-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }" @click.stop>
        <div v-if="contextMenu.data?.type === 'group'" class="menu-item" @click="addProject">
          <el-icon><Plus /></el-icon> 新建项目
        </div>
        <div class="menu-item" @click="openRenameDialog">
          <el-icon><Edit /></el-icon> 重命名
        </div>
        <div class="menu-divider" />
        <div class="menu-item" style="color: var(--danger-color);" @click="deleteNode">
          <el-icon><Delete /></el-icon> 删除
        </div>
      </div>
    </Teleport>

    <!-- Add Group Dialog -->
    <el-dialog v-model="showAddGroup" title="新建分组" width="400px">
      <el-form :model="groupForm" label-width="80px">
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
    <el-dialog v-model="showAddProjectDialog" title="新建项目" width="400px">
      <el-form :model="projectForm" label-width="80px">
        <el-form-item label="项目名称" required>
          <el-input v-model="projectForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="projectForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddProjectDialog = false">取消</el-button>
        <el-button type="primary" @click="submitProject">确定</el-button>
      </template>
    </el-dialog>

    <!-- Rename Dialog (for both group and project) -->
    <el-dialog v-model="showRenameDialog" :title="renameTitle" width="400px">
      <el-form :model="renameForm" label-width="80px">
        <el-form-item :label="renameType === 'group' ? '分组名称' : '项目名称'">
          <el-input v-model="renameForm.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="颜色" v-if="renameType === 'project'">
          <el-color-picker v-model="renameForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useAppStore } from '@/stores/app'
import { getTaskStatistics } from '@/api/task'
import { createGroup, updateGroup, deleteGroup } from '@/api/group'
import { createProject, updateProject, deleteProject } from '@/api/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, List, EditPen, Collection, FolderOpened } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const appStore = useAppStore()

const stats = ref({})
const showAddGroup = ref(false)
const groupForm = reactive({ name: '' })

const isAllSelected = computed(() => !route.query.projectId)
const treeRef = ref(null)

const selectAll = () => {
  contextMenu.visible = false
  if (treeRef.value) {
    treeRef.value.setCurrentKey(null)
  }
  router.push({ path: '/calendar', query: {} })
}

const treeProps = { children: 'children', label: 'name' }

const contextMenu = reactive({ visible: false, x: 0, y: 0, data: null })

// Filter tree data based on showCompleted
const filteredTreeData = computed(() => {
  const filterTree = (nodes) => {
    if (!nodes) return nodes
    return nodes.map(node => {
      if (node.children) {
        const filteredChildren = filterTree(node.children)
        return { ...node, children: filteredChildren }
      }
      // For task-type children, filter out DONE when showCompleted is false
      if (node.type === 'task' && !appStore.showCompleted && (node.status === 'DONE' || node.status === 'SHELVED')) {
        return null
      }
      return node
    }).filter(Boolean)
  }
  return filterTree(projectStore.treeData)
})

const priorityTagType = (priority) => {
  const map = { RED: 'danger', YELLOW: 'warning', BLUE: 'primary', FLAG: 'success', NONE: 'info' }
  return map[priority] || 'info'
}

const handleNodeClick = (data) => {
  contextMenu.visible = false
  if (data.type === 'project') {
    projectStore.selectProject(data.id)
    router.push({ path: '/calendar', query: { projectId: data.id } })
  } else if (data.type === 'task') {
    projectStore.selectTask(data.id)
    // Emit event to open task detail panel
    window.dispatchEvent(new CustomEvent('open-task-detail', { detail: { taskId: data.id } }))
  }
}

const handleContextMenu = (event, data) => {
  if (data.type === 'group' || data.type === 'project') {
    contextMenu.visible = true
    contextMenu.x = event.clientX
    contextMenu.y = event.clientY
    contextMenu.data = data
  }
}

const loadStats = async () => {
  try {
    const res = await getTaskStatistics()
    stats.value = res.data
  } catch (e) {}
}

const submitGroup = async () => {
  if (!groupForm.name) return
  await createGroup({ name: groupForm.name, sortOrder: 0 })
  ElMessage.success('分组创建成功')
  showAddGroup.value = false
  groupForm.name = ''
  await projectStore.fetchTree()
  window.dispatchEvent(new CustomEvent('refresh-data'))
}

const addProject = () => {
  contextMenu.visible = false
  const groupId = contextMenu.data?.id
  if (groupId) {
    projectForm.groupId = groupId
    projectForm.name = ''
    projectForm.color = '#409EFF'
    showAddProjectDialog.value = true
  }
}

// Add Project dialog state
const showAddProjectDialog = ref(false)
const projectForm = reactive({ name: '', color: '#409EFF', groupId: null })

const submitProject = async () => {
  if (!projectForm.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  try {
    await createProject({ name: projectForm.name, color: projectForm.color, groupId: projectForm.groupId })
    ElMessage.success('项目创建成功')
    showAddProjectDialog.value = false
    await projectStore.fetchTree()
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

const navigateToTasks = (status) => {
  router.push({ path: '/tasks', query: { status } })
}

// Rename dialog state
const showRenameDialog = ref(false)
const renameType = ref('group')
const renameForm = reactive({ name: '', color: '#409EFF' })

const renameTitle = computed(() => {
  return renameType.value === 'group' ? '重命名分组' : '重命名项目'
})

const openRenameDialog = () => {
  contextMenu.visible = false
  const data = contextMenu.data
  if (!data) return
  renameType.value = data.type
  renameForm.name = data.name || ''
  renameForm.color = data.color || '#409EFF'
  showRenameDialog.value = true
}

const submitRename = async () => {
  if (!renameForm.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  try {
    const data = contextMenu.data
    if (!data) return
    if (renameType.value === 'group') {
      await updateGroup(data.id, { name: renameForm.name })
    } else {
      await updateProject(data.id, { name: renameForm.name, color: renameForm.color })
    }
    ElMessage.success('重命名成功')
    showRenameDialog.value = false
    await projectStore.fetchTree()
    window.dispatchEvent(new CustomEvent('refresh-data'))
  } catch (e) {}
}

const deleteNode = async () => {
  contextMenu.visible = false
  const nodeType = contextMenu.data?.type
  const nodeId = contextMenu.data?.id
  const label = nodeType === 'project' ? '项目' : '分组'
  await ElMessageBox.confirm(`确定要删除这个${label}吗？`, '提示')
  if (contextMenu.data) {
    if (nodeType === 'project') {
      await deleteProject(contextMenu.data.id)
    } else {
      await deleteGroup(contextMenu.data.id)
    }
    ElMessage.success('删除成功')
    await projectStore.fetchTree()
    // 如果删除的是当前筛选的项目，跳转到全部
    if (nodeType === 'project' && String(route.query.projectId) === String(nodeId)) {
      router.push({ path: '/calendar', query: {} })
    } else {
      window.dispatchEvent(new CustomEvent('refresh-data'))
    }
  }
}

// 点击页面其他区域关闭右键菜单
document.addEventListener('click', () => { contextMenu.visible = false })

// 监听数据刷新事件
const handleRefreshData = () => {
  projectStore.fetchTree()
  loadStats()
}

onMounted(async () => {
  await projectStore.fetchTree()
  await loadStats()
  window.addEventListener('refresh-data', handleRefreshData)
})

onBeforeUnmount(() => {
  window.removeEventListener('refresh-data', handleRefreshData)
})
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.25s;
  overflow: hidden;
}
.sidebar.collapsed { width: 60px; }
.sidebar-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
  font-size: 16px;
}
.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.stats-section {
  display: flex;
  justify-content: space-around;
  padding: 8px 4px;
}
.stat-item { text-align: center; }
.stat-label { font-size: 12px; color: var(--text-secondary); display: block; }
.stat-value { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  font-size: 13px;
  color: var(--text-secondary);
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  font-size: 13px;
}
.node-label { 
  flex: 1; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  white-space: nowrap; 
}
.priority-dot { width: 8px; height: 8px; padding: 0; border-radius: 50%; flex-shrink: 0; }
.task-count {
  font-size: 11px;
  color: var(--text-secondary);
  background: #e4e7ed;
  padding: 0 6px;
  border-radius: 8px;
}
.all-projects-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  margin: 2px 0;
  font-size: 13px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.15s;
}
.all-projects-item:hover {
  background: #e8ecf1;
  color: #409eff;
}
.all-projects-item.active {
  background: #d9e8fb;
  color: #409eff;
  font-weight: 600;
}
.show-completed-row {
  padding: 0 12px;
  margin-top: 4px;
  font-size: 13px;
}

/* Collapsed nav */
.collapsed-nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 0;
}
.nav-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  color: #666;
  transition: all 0.15s;
}
.nav-icon:hover {
  background: #e8ecf1;
  color: #409eff;
}
.nav-icon.active {
  background: #d9e8fb;
  color: #409eff;
}
.nav-divider {
  width: 24px;
  height: 1px;
  background: #e0e0e0;
  margin: 4px 0;
}
</style>
