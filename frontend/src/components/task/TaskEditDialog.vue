<template>
  <el-dialog
    :model-value="visible"
    :title="mode === 'create' ? '新建任务' : '编辑任务'"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form :model="form" size="small" label-width="60px" @keyup.enter="handleSave" @submit.prevent>
      <el-form-item label="标题" required>
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="截止"
          value-format="YYYY-MM-DD"
          style="width:100%"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio value="INCOMPLETE">未完成</el-radio>
          <el-radio value="DONE">已完成</el-radio>
          <el-radio value="SHELVED">搁置</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="项目">
        <el-select v-model="form.projectId" placeholder="选择项目" clearable style="width:100%">
          <el-option-group v-for="group in groupedProjects" :key="group.label" :label="group.label">
            <el-option v-for="item in group.options" :key="item.id" :label="item.name" :value="item.id" />
          </el-option-group>
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-radio-group v-model="form.priority" class="priority-grid">
          <el-radio value="Q1">
            <span class="quadrant-radio q1-radio">Q1</span> 重要且紧急
          </el-radio>
          <el-radio value="Q2">
            <span class="quadrant-radio q2-radio">Q2</span> 重要不紧急
          </el-radio>
          <el-radio value="Q3">
            <span class="quadrant-radio q3-radio">Q3</span> 不重要但紧急
          </el-radio>
          <el-radio value="Q4">
            <span class="quadrant-radio q4-radio">Q4</span> 不重要不紧急
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="mode === 'edit' && showDelete" size="small" type="danger" plain @click="handleDelete">删除</el-button>
        <el-button v-if="mode === 'edit' && showDelete" size="small" @click="emit('share', props.task)"><el-icon><Share /></el-icon> 共享</el-button>
        <div class="dialog-footer-right">
          <el-button size="small" @click="$emit('update:visible', false)">取消</el-button>
          <el-button size="small" type="primary" @click="handleSave">{{ mode === 'create' ? '确定' : '保存' }}</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Share } from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: { type: String, default: 'create' }, // 'create' | 'edit'
  task: { type: Object, default: null },
  projectOptions: { type: Array, default: () => [] },
  showDelete: { type: Boolean, default: true }
})

const emit = defineEmits(['update:visible', 'save', 'delete', 'share'])

const projectStore = useProjectStore()

// Grouped project options from tree data
const groupedProjects = computed(() => {
  const tree = projectStore.treeData
  if (!tree || !tree.length) {
    // Fallback to flat list
    return [{ label: '', options: props.projectOptions }]
  }
  return tree.map(group => ({
    label: group.name,
    options: (group.children || [])
      .filter(c => c.type === 'project')
      .map(p => ({ id: p.id, name: p.name, color: p.color }))
  })).filter(g => g.options.length > 0)
})

const defaultForm = () => ({
  title: '', status: 'INCOMPLETE', priority: 'Q1', projectId: null, startDate: null, dueDate: null
})

const form = reactive(defaultForm())

// Date range computed (syncs with form.startDate / form.dueDate)
const dateRange = computed({
  get: () => {
    if (form.startDate && form.dueDate) return [form.startDate, form.dueDate]
    if (form.startDate) return [form.startDate, form.startDate]
    return null
  },
  set: (val) => {
    if (val && val.length === 2) {
      form.startDate = val[0]
      form.dueDate = val[1]
    } else {
      form.startDate = null
      form.dueDate = null
    }
  }
})

watch(() => props.visible, (val) => {
  if (val) {
    if (props.mode === 'edit' && props.task) {
      Object.assign(form, {
        title: props.task.title || '',
        status: props.task.status || 'INCOMPLETE',
        priority: props.task.priority || 'Q1',
        projectId: props.task.projectId,
        startDate: props.task.startDate,
        dueDate: props.task.dueDate
      })
    } else {
      Object.assign(form, defaultForm())
    }
  }
})

const handleSave = () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  emit('save', { ...form })
}

const handleDelete = () => {
  ElMessageBox.confirm('确定要删除此任务吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('delete')
  }).catch(() => {})
}
</script>

<style scoped>
.priority-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  width: 100%;
}
.priority-grid :deep(.el-radio) {
  margin-right: 0;
  align-items: center;
}
.quadrant-radio {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  margin-right: 6px;
  vertical-align: middle;
  flex-shrink: 0;
}
.q1-radio { background: #F56C6C; }
.q2-radio { background: #E6A23C; }
.q3-radio { background: #409EFF; }
.q4-radio { background: #67C23A; }
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
