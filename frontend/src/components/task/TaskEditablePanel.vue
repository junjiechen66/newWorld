<template>
  <Teleport to="body">
    <transition name="panel-slide">
      <div v-if="visible" class="detail-overlay" @mousedown.self="overlayMousedown = true" @click.self="handleOverlayClick">
        <div class="detail-panel">
          <div class="panel-header">
            <span v-if="headerType === 'date'" class="panel-date">{{ headerDate }}</span>
            <span v-else class="panel-status" :style="{ background: panelStatusColor }">{{ panelStatusText }}</span>
            <span class="panel-close" @click="close">✕</span>
          </div>
          <div class="panel-body">
            <div class="title-editor-area">
              <label class="section-label">标题</label>
              <el-input v-model="editTitle" size="large" :placeholder="placeholder + '标题...'" class="title-input" :readonly="readonly" />
            </div>
            <div class="desc-editor-area">
              <label class="section-label">内容（支持Markdown）</label>
              <MarkdownEditor v-model="editDesc" :placeholder="placeholder + '内容（支持Markdown）...'" :editable="!readonly" />
            </div>
          </div>
          <div class="panel-footer">
            <el-button v-if="!readonly" size="small" type="primary" @click="handleSave">保存</el-button>
            <el-tag v-else size="small" type="info" effect="plain">仅查看</el-tag>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  task: { type: Object, default: null },
  headerType: { type: String, default: 'status' }, // 'status' | 'date'
  placeholder: { type: String, default: '任务' }, // '任务' | '笔记'
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'save'])

// Overlay click protection: only close if mousedown was on the overlay itself,
// not dragged from inside the panel
const overlayMousedown = ref(false)

const handleOverlayClick = () => {
  if (overlayMousedown.value) {
    overlayMousedown.value = false
    close()
  }
}

const editTitle = ref('')
const editDesc = ref('')
const originalTitle = ref('')
const originalDesc = ref('')

watch(() => props.visible, (val) => {
  if (val && props.task) {
    editTitle.value = props.task.title || ''
    editDesc.value = props.task.description || ''
    originalTitle.value = props.task.title || ''
    originalDesc.value = props.task.description || ''
  } else if (!val) {
    editDesc.value = ''
  }
})

// Dirty detection: true when title or description changed
const isDirty = computed(() =>
  editTitle.value !== originalTitle.value || editDesc.value !== originalDesc.value
)

const headerDate = computed(() => {
  if (!props.task?.createTime) return ''
  return props.task.createTime.substring(0, 10)
})

const panelStatusColor = computed(() => {
  if (!props.task) return '#909399'
  const map = { INCOMPLETE: '#909399', DONE: '#67C23A', SHELVED: '#c0c4cc' }
  return map[props.task.status] || '#909399'
})

const panelStatusText = computed(() => {
  if (!props.task) return ''
  const map = { INCOMPLETE: '未完成', DONE: '已完成', SHELVED: '搁置' }
  return map[props.task.status] || '未完成'
})

// Unified close: if dirty, prompt to save before closing
const close = async () => {
  if (!props.readonly && isDirty.value) {
    try {
      await ElMessageBox.confirm(
        `${props.placeholder}内容已修改，是否保存后关闭？`,
        '确认关闭',
        {
          confirmButtonText: '保存',
          cancelButtonText: '不保存',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      )
      // "Save" clicked
      handleSave()
      emit('update:visible', false)
    } catch (action) {
      // ESC on dialog or X button — abort close
      if (action === 'close') return
      // "不保存" button — close without saving
      emit('update:visible', false)
    }
  } else {
    emit('update:visible', false)
  }
}

const handleSave = () => {
  emit('save', { title: editTitle.value, description: editDesc.value })
  // Update originals after save so isDirty resets
  originalTitle.value = editTitle.value
  originalDesc.value = editDesc.value
}

// ESC key closes the panel
const handleKeydown = (e) => {
  if (e.key === 'Escape' && props.visible) {
    e.stopPropagation()
    close()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.detail-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 1000;
  pointer-events: auto;
}
.detail-panel {
  position: absolute;
  right: 0; top: 0; bottom: 0;
  width: 50%;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 0;
}
.panel-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
}
.panel-date {
  font-size: 12px;
  color: #999;
}
.panel-close {
  cursor: pointer;
  color: #bbb;
  font-size: 18px;
  line-height: 1;
  padding: 4px;
}
.panel-close:hover { color: #333; }
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 16px;
  display: flex;
  flex-direction: column;
}
.title-editor-area { margin-bottom: 20px; }
.section-label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
  font-weight: 500;
}
.title-input :deep(.el-input__wrapper) {
  font-size: 22px;
  font-weight: 700;
  padding-left: 0;
  box-shadow: none !important;
  border-bottom: 1px solid #e0e0e0;
  border-radius: 0;
}
.title-input :deep(.el-input__inner) {
  height: 44px;
  padding: 0;
}
.desc-editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.desc-editor-area :deep(.md-editor) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 300px;
}
.desc-editor-area :deep(.md-textarea) {
  flex: 1;
  resize: none;
  min-height: 240px;
}
.desc-editor-area :deep(.md-preview) { flex: 1; }
.panel-footer {
  display: flex;
  gap: 8px;
  padding: 12px 24px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.panel-slide-enter-active { transition: transform 0.2s ease; }
.panel-slide-leave-active { transition: transform 0.15s ease; }
.panel-slide-enter-from { transform: translateX(100%); }
.panel-slide-leave-to { transform: translateX(100%); }
</style>
