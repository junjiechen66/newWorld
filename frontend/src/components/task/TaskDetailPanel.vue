<template>
  <Teleport to="body">
    <transition name="slide-panel">
      <div v-if="visible" class="detail-panel-overlay" @click.self="close">
        <div class="detail-panel">
          <div class="panel-header">
            <h3>任务详情</h3>
            <el-button text @click="close"><el-icon><Close /></el-icon></el-button>
          </div>
          <div class="panel-body" v-if="task">
            <div class="field">
              <label>标题</label>
              <div class="value title-value">{{ task.title }}</div>
            </div>
            <div class="field">
              <label>状态</label>
              <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
            </div>
            <div class="field">
              <label>优先级</label>
              <el-tag v-if="task.priority && task.priority !== 'NONE'" :type="priorityType" size="small">{{ task.priority }}</el-tag>
              <span v-else class="value">无</span>
            </div>
            <div class="field">
              <label>标签</label>
              <span class="value">{{ task.tag || '无' }}</span>
            </div>
            <div class="field">
              <label>开始日期</label>
              <span class="value">{{ task.startDate || '无' }}</span>
            </div>
            <div class="field">
              <label>截止日期</label>
              <span class="value">{{ task.dueDate || '无' }}</span>
            </div>
            <div class="field">
              <label>创建时间</label>
              <span class="value">{{ task.createTime || '无' }}</span>
            </div>
            <div class="field" v-if="task.description">
              <label>描述</label>
              <div class="value description-value">{{ task.description }}</div>
            </div>
          </div>
          <div class="panel-footer" v-if="task">
            <el-button size="small" @click="editTask">编辑</el-button>
            <el-button size="small" type="danger" plain @click="deleteTask">删除</el-button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskById, deleteTask as apiDeleteTask } from '@/api/task'

const visible = ref(false)
const task = ref(null)

const statusType = computed(() => {
  const map = { TODO: 'info', IN_PROGRESS: 'warning', DONE: 'success', ARCHIVED: 'info' }
  return map[task.value?.status] || 'info'
})

const statusText = computed(() => {
  const map = { TODO: '待办', IN_PROGRESS: '进行中', DONE: '已完成', ARCHIVED: '已归档' }
  return map[task.value?.status] || task.value?.status
})

const priorityType = computed(() => {
  const map = { RED: 'danger', YELLOW: 'warning', BLUE: 'primary', FLAG: 'success' }
  return map[task.value?.priority] || 'info'
})

const open = async (taskId) => {
  try {
    const res = await getTaskById(taskId)
    task.value = res.data
    visible.value = true
  } catch (e) {}
}

const close = () => {
  visible.value = false
}

const editTask = () => {
  window.dispatchEvent(new CustomEvent('edit-task', { detail: { task: task.value } }))
  close()
}

const deleteTask = async () => {
  await ElMessageBox.confirm('确定要删除此任务吗？', '提示')
  await apiDeleteTask(task.value.id)
  ElMessage.success('已删除')
  close()
}

const handleOpenTaskDetail = (e) => {
  open(e.detail.taskId)
}

onMounted(() => {
  window.addEventListener('open-task-detail', handleOpenTaskDetail)
})

onBeforeUnmount(() => {
  window.removeEventListener('open-task-detail', handleOpenTaskDetail)
})
</script>

<style scoped>
.detail-panel-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2000;
  background: rgba(0,0,0,0.1);
}
.detail-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 380px;
  height: 100%;
  background: var(--card-bg);
  box-shadow: -4px 0 12px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
  z-index: 2001;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}
.panel-header h3 { margin: 0; font-size: 16px; }
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 12px; color: var(--text-secondary); margin-bottom: 4px; }
.field .value { font-size: 14px; color: var(--text-primary); }
.title-value { font-size: 16px; font-weight: 500; }
.description-value {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.panel-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 8px;
}
</style>
