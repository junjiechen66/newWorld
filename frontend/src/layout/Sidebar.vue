<template>
  <div class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
    <div class="sidebar-header">
      <el-icon :size="22"><Calendar /></el-icon>
      <span v-show="!appStore.sidebarCollapsed" class="sidebar-title">NewWorld</span>
    </div>
    
    <div class="sidebar-content">
      <!-- Quick stats -->
      <div class="stats-section" v-if="!appStore.sidebarCollapsed">
        <div class="stat-item">
          <span class="stat-label">待办</span>
          <span class="stat-value">{{ stats.todoCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">进行中</span>
          <span class="stat-value">{{ stats.inProgressCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">已完成</span>
          <span class="stat-value">{{ stats.doneCount || 0 }}</span>
        </div>
      </div>

      <el-divider style="margin: 8px 0;" />

      <!-- Project Tree -->
      <div class="tree-section">
        <div class="tree-header">
          <span>项目列表</span>
          <el-button text type="primary" size="small" @click="showAddGroup = true" v-if="!appStore.sidebarCollapsed">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <el-tree
          :data="projectStore.treeData"
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
        <div class="menu-item" @click="addProject">
          <el-icon><Plus /></el-icon> 新建项目
        </div>
        <div class="menu-item" @click="renameGroup">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useAppStore } from '@/stores/app'
import { getTaskStatistics } from '@/api/task'
import { createGroup, deleteGroup } from '@/api/group'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const appStore = useAppStore()

const stats = ref({})
const showAddGroup = ref(false)
const groupForm = reactive({ name: '' })

const treeProps = { children: 'children', label: 'name' }

const contextMenu = reactive({ visible: false, x: 0, y: 0, data: null })

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
  if (data.type === 'group') {
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
}

const addProject = () => {
  contextMenu.visible = false
  // Navigate to project management or show dialog
  ElMessage.info('请在上方点击"+"号新建项目')
}

const renameGroup = () => {
  contextMenu.visible = false
  ElMessage.info('编辑功能开发中')
}

const deleteNode = async () => {
  contextMenu.visible = false
  await ElMessageBox.confirm('确定要删除这个分组吗？', '提示')
  if (contextMenu.data) {
    await deleteGroup(contextMenu.data.id)
    ElMessage.success('删除成功')
    await projectStore.fetchTree()
  }
}

// 点击页面其他区域关闭右键菜单
document.addEventListener('click', () => { contextMenu.visible = false })

onMounted(async () => {
  await projectStore.fetchTree()
  await loadStats()
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
</style>
