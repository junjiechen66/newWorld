<template>
  <div class="task-card" :class="{ 'completed-card': isCompleted }" @click="$emit('click', task)" @contextmenu.prevent="$emit('right-click', { task, x: $event.clientX, y: $event.clientY })">
    <div class="task-header">
      <h3 class="task-title" :class="{ 'is-done': isCompleted }">{{ task.title || '无标题' }}</h3>
      <div class="header-actions">
        <el-icon v-if="taskType !== 'note'" class="action-icon-btn" :class="{ 'is-completed': isCompleted }" :title="isCompleted ? '已完成' : '标记完成'" @click.stop="$emit('toggle-status', task)"><CircleCheck /></el-icon>
        <el-icon v-if="taskType === 'note'" class="action-icon-btn" title="归档" @click.stop="$emit('archive', task)"><FolderChecked /></el-icon>
        <el-icon v-if="task.accessLevel === 'OWNER'" class="action-icon-btn" title="共享" @click.stop="$emit('share', task)"><Share /></el-icon>
      </div>
    </div>
    <div v-if="showDescription && task.description" class="task-preview">{{ task.description }}</div>
    <div class="task-footer">
      <!-- Task mode: status + priority + dueDate -->
      <template v-if="taskType !== 'note'">
        <el-tag
          :type="isCompleted ? 'success' : statusTagType"
          size="small" effect="plain" class="status-tag"
          @click.stop="$emit('toggle-status', task)"
        >
          {{ statusText }}
        </el-tag>
        <span v-if="task.priority && task.priority !== 'NONE'" class="priority-badge" :class="'priority-' + task.priority.toLowerCase()">
          {{ priorityText }}
        </span>
        <span v-if="task.dueDate" class="task-date" :class="{ 'overdue-date': isOverdue }">
          <el-icon><Calendar /></el-icon> {{ task.dueDate }}
        </span>
      </template>
      <!-- Note mode: createTime -->
      <span v-else-if="task.createTime" class="task-date">
        <el-icon><Calendar /></el-icon> {{ task.createTime.substring(0, 10) }}
      </span>
      <span v-if="task.projectName" class="task-group-badge">{{ task.projectName }}</span>
      <!-- 共享标识 -->
      <el-tooltip v-if="task.shareMemberCount > 0" :content="'协作者: ' + task.shareMemberCount + '人'" placement="top">
        <span class="share-badge">
          <el-icon :size="12"><User /></el-icon>{{ task.shareMemberCount }}
        </span>
      </el-tooltip>
      <el-tooltip v-if="task.hasShareLink" content="有公开分享链接" placement="top">
        <span class="share-badge">
          <el-icon :size="12"><Link /></el-icon>
        </span>
      </el-tooltip>
      <!-- 共享来源标识 -->
      <span v-if="task.sharedByNickname" class="shared-by-badge">
        来自 {{ task.sharedByNickname }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Calendar, User, Link, Share, CircleCheck, FolderChecked } from '@element-plus/icons-vue'
import { useTaskActions } from '@/composables/useTaskActions'

const props = defineProps({
  task: { type: Object, required: true },
  showDescription: { type: Boolean, default: false },
  priorityLabelFn: { type: Function, default: null },
  taskType: { type: String, default: 'task' } // 'task' | 'note'
})

defineEmits(['click', 'right-click', 'toggle-status', 'share', 'archive'])

const { statusLabel, statusTagType: getStatusTagType, priorityLabel, priorityShortLabel } = useTaskActions()

const isCompleted = computed(() => props.task.status === 'DONE')

const statusTagType = computed(() => getStatusTagType(props.task.status))
const statusText = computed(() => statusLabel(props.task.status))

const priorityText = computed(() => {
  if (props.priorityLabelFn) return props.priorityLabelFn(props.task.priority)
  return priorityLabel(props.task.priority)
})

const isOverdue = computed(() => {
  if (!props.task.dueDate || isCompleted.value) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return props.task.dueDate < today.toISOString().substring(0, 10)
})
</script>

<style scoped>
.task-card {
  background: #fff; border-radius: 8px; padding: 14px 16px; cursor: pointer;
  border: 1px solid #eee; transition: box-shadow 0.15s, border-color 0.15s;
  display: flex; flex-direction: column;
}
.task-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); border-color: #dcdfe6; }
.action-icon-btn {
  display: none; cursor: pointer; color: #909399; font-size: 20px; flex-shrink: 0;
  padding: 4px; border-radius: 4px; transition: color 0.15s, background 0.15s;
}
.action-icon-btn:hover { color: #409eff; background: #ecf5ff; }
.action-icon-btn.is-completed { color: #67c23a; }
.action-icon-btn.is-completed:hover { color: #85ce61; background: #f0f9eb; }
.task-card:hover .action-icon-btn { display: inline-flex; }
.header-actions { display: flex; gap: 2px; flex-shrink: 0; }
.completed-card { opacity: 0.7; }
.task-header { display: flex; align-items: flex-start; margin-bottom: 8px; gap: 8px; }
.task-title { font-size: 14px; font-weight: 600; margin: 0; flex: 1; line-height: 1.5; word-break: break-word; }
.task-title.is-done { text-decoration: line-through; color: #999; }
.task-preview { font-size: 13px; color: #666; line-height: 1.7; white-space: pre-wrap; overflow-wrap: break-word; word-break: break-word; max-height: 110px; overflow: hidden; margin-bottom: 8px; }
.task-footer { display: flex; align-items: center; gap: 6px; font-size: 12px; flex-wrap: wrap; margin-top: auto; }
.status-tag { flex-shrink: 0; cursor: pointer; }
.task-date { display: flex; align-items: center; gap: 3px; color: #999; }
.overdue-date { color: #F56C6C; }
.share-badge {
  display: inline-flex; align-items: center; gap: 2px;
  font-size: 11px; color: #999; background: #f5f7fa;
  padding: 1px 6px; border-radius: 8px;
}
.shared-by-badge {
  font-size: 11px; color: #999; background: #f0f2f5;
  padding: 1px 6px; border-radius: 8px; margin-left: auto;
}
</style>
