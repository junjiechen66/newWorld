<template>
  <div class="shared-with-me-page">
    <div class="page-header">
      <h2>共享给我</h2>
    </div>

    <div class="shared-scroll">
      <div v-if="loading" class="list-status">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="resources.length === 0" class="list-status empty">暂无共享内容</div>
      <div v-else>
        <!-- 任务分组 -->
        <div v-if="sharedTasks.length > 0" class="resource-section">
          <div class="section-header">
            <span class="section-name">任务</span>
            <span class="section-count">{{ sharedTasks.length }}</span>
          </div>
          <div class="tasks-grid">
            <TaskCard
              v-for="task in sharedTasks" :key="task.id"
              :task="task"
              :showDescription="true"
              @click="openDetail"
              @toggle-status="task.accessLevel === 'EDIT' ? cycleStatus(task) : null"
            />
          </div>
        </div>

        <!-- 笔记分组 -->
        <div v-if="sharedNotes.length > 0" class="resource-section">
          <div class="section-header">
            <span class="section-name">笔记</span>
            <span class="section-count">{{ sharedNotes.length }}</span>
          </div>
          <div class="tasks-grid">
            <TaskCard
              v-for="note in sharedNotes" :key="note.id"
              :task="note"
              :showDescription="true"
              taskType="note"
              @click="openDetail"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑面板 -->
    <TaskEditablePanel
      v-model:visible="detailVisible"
      :task="viewTask"
      :readonly="viewTask?.accessLevel === 'VIEW'"
      @save="handleSave"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSharedWithMe } from '@/api/share'
import { updateTaskStatus, updateTask } from '@/api/task'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TaskCard from '@/components/task/TaskCard.vue'
import TaskEditablePanel from '@/components/task/TaskEditablePanel.vue'

const loading = ref(false)
const resources = ref([])

const sharedTasks = computed(() => resources.value.filter(r => !r.isNote))
const sharedNotes = computed(() => resources.value.filter(r => r.isNote))

const detailVisible = ref(false)
const viewTask = ref(null)

const loadShared = async () => {
  loading.value = true
  try {
    const res = await getSharedWithMe()
    resources.value = res.data || []
  } catch (e) {
    resources.value = []
  } finally {
    loading.value = false
  }
}

const openDetail = (task) => {
  viewTask.value = { ...task, isNote: task.isNote }
  detailVisible.value = true
}

const cycleStatus = async (task) => {
  if (task.accessLevel !== 'EDIT') return
  const cycle = ['INCOMPLETE', 'DONE', 'SHELVED']
  const idx = cycle.indexOf(task.status)
  const next = cycle[(idx + 1) % cycle.length]
  try {
    await updateTaskStatus(task.id, next)
    task.status = next
    ElMessage.success('状态已更新')
  } catch (e) {}
}

const handleSave = async ({ title, description }) => {
  if (!viewTask.value || viewTask.value.accessLevel !== 'EDIT') return
  try {
    await updateTask(viewTask.value.id, { title, description })
    ElMessage.success('已保存')
    viewTask.value.title = title
    viewTask.value.description = description
    await loadShared()
  } catch (e) {}
}

onMounted(loadShared)
</script>

<style scoped>
.shared-with-me-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; }
.shared-scroll { flex: 1; overflow-y: auto; }
.resource-section { margin-bottom: 24px; }
.section-header {
  display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
  padding-bottom: 8px; border-bottom: 1px solid #f0f0f0;
}
.section-name { font-size: 14px; font-weight: 600; color: #409EFF; }
.section-count {
  background: #ecf5ff; color: #409EFF; padding: 1px 8px;
  border-radius: 10px; font-size: 12px; font-weight: 500;
}
.list-status { display: flex; align-items: center; gap: 6px; color: #999; font-size: 14px; padding: 20px 0; }
.list-status.empty { justify-content: center; }
.shared-by {
  font-size: 11px; color: #999; background: #f0f2f5;
  padding: 1px 6px; border-radius: 8px; margin-left: auto;
}
.tasks-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
</style>
