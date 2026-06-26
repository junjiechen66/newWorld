<template>
  <div class="quadrant-card" :class="[priority.toLowerCase() + '-card', { 'drag-over': isDragOver }]">
    <div class="quadrant-header" :class="priority.toLowerCase() + '-header'">
      <span class="header-icon">
        <el-icon :size="16"><component :is="icon" /></el-icon>
      </span>
      <span class="header-title">{{ title }}</span>
      <span class="task-count">{{ tasks.length }}</span>
    </div>
    <div class="quadrant-body"
      @dragover.prevent="$emit('dragover')"
      @dragleave="$emit('dragleave')"
      @drop="handleDrop"
    >
      <div v-if="tasks.length === 0" class="empty-text">没有任务</div>
      <div v-else class="task-list">
        <div v-for="task in tasks" :key="task.id" class="quadrant-task-item"
          draggable="true"
          @dragstart="handleDragStart($event, task)"
          @dragend="$emit('dragend')"
          @click="$emit('task-click', task)"
          @contextmenu.prevent="$emit('task-right-click', task)"
        >
          <el-icon class="drag-handle" :size="12"><Rank /></el-icon>
          <el-checkbox
            :model-value="task.status === 'DONE'"
            @change="(val) => $emit('toggle-status', task, val)"
            @click.stop
          />
          <span class="task-name" :class="{ 'is-done': task.status === 'DONE' }">{{ task.title }}</span>
          <span v-if="task.projectName" class="project-name">{{ task.projectName }}</span>
          <span v-if="task.dateInfo?.text" class="date-info" :class="task.dateInfo.cls">{{ task.dateInfo.text }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Rank } from '@element-plus/icons-vue'

defineProps({
  priority: { type: String, required: true },      // Q1/Q2/Q3/Q4
  title: { type: String, required: true },
  icon: { type: [Object, String], required: true },
  tasks: { type: Array, default: () => [] },
  isDragOver: { type: Boolean, default: false }
})

const emit = defineEmits(['drop', 'dragstart', 'dragover', 'dragleave', 'dragend', 'task-click', 'task-right-click', 'toggle-status'])

const handleDrop = (e) => {
  e.preventDefault()
  emit('drop')
}

const handleDragStart = (e, task) => {
  emit('dragstart', e, task)
}
</script>

<style scoped>
.quadrant-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: box-shadow 0.2s, border-color 0.2s;
}
.quadrant-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.quadrant-card.drag-over {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.3), 0 4px 16px rgba(0,0,0,0.1);
}
.quadrant-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #fff;
  flex-shrink: 0;
}
.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  flex-shrink: 0;
}
.header-title { flex: 1; }
.task-count {
  background: rgba(255,255,255,0.3);
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}
.q1-header { background: #F56C6C; }
.q2-header { background: #E6A23C; }
.q3-header { background: #409EFF; }
.q4-header { background: #67C23A; }
.q1-card.drag-over { border-color: #F56C6C; }
.q2-card.drag-over { border-color: #E6A23C; }
.q3-card.drag-over { border-color: #409EFF; }
.q4-card.drag-over { border-color: #67C23A; }
.quadrant-body {
  flex: 1;
  padding: 12px 16px;
  overflow-y: auto;
  background: #fafafa;
  transition: background 0.15s;
}
.drag-over .quadrant-body { background: #f0f7ff; }
.empty-text { text-align: center; color: #bbb; font-size: 13px; padding: 40px 0; }
.task-list { display: flex; flex-direction: column; gap: 8px; }
.quadrant-task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s;
  border: 1px solid #f0f0f0;
  font-size: 13px;
}
.quadrant-task-item:hover { background: #f5f7fa; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.quadrant-task-item[draggable="true"] { cursor: grab; }
.quadrant-task-item[draggable="true"]:active { cursor: grabbing; }
.drag-handle { color: #ccc; flex-shrink: 0; cursor: grab; }
.drag-handle:active { cursor: grabbing; }
.task-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #333; }
.task-name.is-done { text-decoration: line-through; color: #bbb; }
.project-name { background: #ecf5ff; color: #409EFF; padding: 1px 8px; border-radius: 4px; font-size: 11px; flex-shrink: 0; }
.date-info { font-size: 11px; font-weight: 500; flex-shrink: 0; padding: 1px 6px; border-radius: 4px; }
.date-overdue { color: #F56C6C; background: #fef0f0; }
.date-today { color: #E6A23C; background: #fdf6ec; }
.date-progress { color: #409EFF; background: #ecf5ff; }
.date-future { color: #67C23A; background: #f0f9eb; }
.date-remaining { color: #909399; background: #f4f4f5; }
</style>
