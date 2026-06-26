<template>
  <div
    ref="editorRef"
    class="md-editor"
    :class="{
      'md-preview-only': !editable,
      'md-fullscreen': isFullscreen,
      'md-editing': isEditing
    }"
    @pointerdown="handleEditorPointerDown"
  >
    <!-- Toolbar -->
    <div v-if="editable && (isEditing || isFullscreen)" class="md-toolbar" @mousedown.prevent>
      <el-tooltip content="粗体" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('bold')"><b>B</b></el-button>
      </el-tooltip>
      <el-tooltip content="斜体" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('italic')"><i>I</i></el-button>
      </el-tooltip>
      <el-tooltip content="标题" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('heading')">H2</el-button>
      </el-tooltip>
      <el-tooltip content="无序列表" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('ul')">☰</el-button>
      </el-tooltip>
      <el-tooltip content="有序列表" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('ol')">#.</el-button>
      </el-tooltip>
      <el-tooltip content="引用" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('quote')">"</el-button>
      </el-tooltip>
      <el-tooltip content="代码块" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('code')">&lt;/&gt;</el-button>
      </el-tooltip>
      <el-tooltip content="链接" placement="top">
        <el-button text size="small" class="tb-btn" @click="insertMarkdown('link')">🔗</el-button>
      </el-tooltip>
      <el-tooltip content="图片" placement="top">
        <el-button text size="small" class="tb-btn" @click="triggerImageUpload">🖼</el-button>
      </el-tooltip>
      <input
        ref="fileInputRef"
        type="file"
        accept="image/jpeg,image/png,image/gif,image/webp"
        style="display: none"
        @change="handleImageUpload"
      />
      <div style="flex:1"></div>
      <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'" placement="top">
        <el-button text size="small" class="tb-btn" @click="toggleFullscreen">
          {{ isFullscreen ? '⊡' : '⊞' }}
        </el-button>
      </el-tooltip>
    </div>

    <!-- Fullscreen split-pane mode -->
    <template v-if="isFullscreen">
      <div class="md-split" @click.stop>
        <textarea
          ref="textareaRef"
          :value="modelValue"
          @input="onInput"
          @focusout="handleTextareaFocusOut"
          @keydown.esc.prevent="exitFullscreen"
          @paste="handlePaste"
          @drop.prevent="handleDrop"
          @dragover.prevent
          class="md-textarea md-split-left"
          :placeholder="placeholder"
        ></textarea>
        <div class="md-split-divider"></div>
        <div class="md-preview md-split-right" v-html="renderedHtml"></div>
      </div>
      <div class="md-fullscreen-bg" @click="exitFullscreen"></div>
    </template>

    <!-- Normal mode: click-to-edit / click-outside-to-preview -->
    <template v-else>
      <!-- Preview area (shown when NOT editing, or readonly) -->
      <div
        v-if="!isEditing || !editable"
        class="md-preview"
        :class="{ 'md-preview-clickable': editable }"
        @click="enterEdit"
        v-html="renderedHtml"
      ></div>

      <!-- Edit area (shown when editing) -->
      <textarea
        v-if="editable && isEditing"
        ref="textareaRef"
        :value="modelValue"
        @input="onInput"
        @focusout="handleTextareaFocusOut"
        @keydown.esc.prevent="exitEdit"
        @paste="handlePaste"
        @drop.prevent="handleDrop"
        @dragover.prevent
        class="md-textarea"
        :rows="rows"
        :placeholder="placeholder"
      ></textarea>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/upload'

const props = defineProps({
  modelValue: { type: String, default: '' },
  editable: { type: Boolean, default: true },
  placeholder: { type: String, default: '请输入内容...' },
  rows: { type: Number, default: 6 },
  startEditing: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref(null)
const textareaRef = ref(null)
const fileInputRef = ref(null)
const uploading = ref(false)
const isEditing = ref(props.startEditing)
const isFullscreen = ref(false)

const onInput = (e) => {
  emit('update:modelValue', e.target.value)
}

const enterEdit = () => {
  if (!props.editable) return
  isEditing.value = true
  nextTick(() => {
    textareaRef.value?.focus()
  })
}

const exitEdit = () => {
  isEditing.value = false
}

const exitFullscreen = () => {
  isFullscreen.value = false
}

// Pointer capture: when user presses inside editor, capture the pointer
// so that mouseup/click events stay on the editor instead of leaking to
// parent overlay (which may close the panel/dialog)
const handleEditorPointerDown = (e) => {
  if (isEditing.value && editorRef.value) {
    try {
      editorRef.value.setPointerCapture(e.pointerId)
    } catch (_) { /* ignore if element can't capture */ }
  }
}

// Focus restoration: if textarea loses focus while editing, restore it
const handleTextareaFocusOut = (e) => {
  if (!isEditing.value || isFullscreen.value) return
  // If focus moves to another element inside the editor (e.g. toolbar button), don't interfere
  const el = editorRef.value
  if (el && e.relatedTarget && el.contains(e.relatedTarget)) return
  // Restore focus to textarea so user can keep typing
  nextTick(() => {
    if (isEditing.value) {
      textareaRef.value?.focus()
    }
  })
}

// Click-outside handler: exit edit only on explicit click outside the editor
const handleDocumentMousedown = (e) => {
  if (!isEditing.value || isFullscreen.value) return
  const el = editorRef.value
  if (el && !el.contains(e.target)) {
    exitEdit()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentMousedown)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentMousedown)
})

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  if (!isEditing.value) {
    isEditing.value = true
  }
  nextTick(() => {
    textareaRef.value?.focus()
  })
}

const renderedHtml = computed(() => {
  if (!props.modelValue) return '<p style="color:#999">暂无内容</p>'
  try {
    return marked(props.modelValue, { breaks: true })
  } catch (e) {
    return props.modelValue
  }
})

const insertMarkdown = (type) => {
  const ta = textareaRef.value
  if (!ta) return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const selected = props.modelValue.substring(start, end) || ''
  let before, after, insert

  switch (type) {
    case 'bold':
      insert = `**${selected || '粗体文字'}**`
      break
    case 'italic':
      insert = `*${selected || '斜体文字'}*`
      break
    case 'heading':
      insert = `\n## ${selected || '标题'}\n`
      break
    case 'ul':
      insert = `\n- ${selected || '列表项'}\n`
      break
    case 'ol':
      insert = `\n1. ${selected || '列表项'}\n`
      break
    case 'quote':
      insert = `\n> ${selected || '引用文字'}\n`
      break
    case 'code':
      insert = `\n\`\`\`\n${selected || '代码'}\n\`\`\`\n`
      break
    case 'link':
      insert = `[${selected || '链接文字'}](url)`
      break
    default:
      return
  }

  insertText(insert)
}

const insertText = (text) => {
  const ta = textareaRef.value
  if (!ta) return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const before = props.modelValue.substring(0, start)
  const after = props.modelValue.substring(end)
  const newVal = before + text + after
  emit('update:modelValue', newVal)

  const cursorPos = start + text.length
  requestAnimationFrame(() => {
    ta.focus()
    ta.setSelectionRange(cursorPos, cursorPos)
  })
}

const triggerImageUpload = () => {
  fileInputRef.value?.click()
}

const handleImageUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  await processImageFile(file)
  event.target.value = ''
}

// Shared: upload and insert image markdown
const processImageFile = async (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只支持图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(file)
    const url = res.data.url
    insertText(`\n![image](${url})\n`)
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
  }
}

// Ctrl+V paste image
const handlePaste = (e) => {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) processImageFile(file)
      return
    }
  }
}

// Drag and drop image
const handleDrop = (e) => {
  const files = e.dataTransfer?.files
  if (!files || !files.length) return
  for (const file of files) {
    if (file.type.startsWith('image/')) {
      processImageFile(file)
      return
    }
  }
}
</script>

<style scoped>
.md-editor {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: visible;
  position: relative;
  width: 100%;
}
.md-editor.md-preview-only {
  border: none;
}
.md-editor.md-fullscreen {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 9999;
  border-radius: 0;
  border: none;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.md-fullscreen-bg {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: -1;
  background: rgba(0,0,0,0.4);
}
.md-split {
  flex: 1;
  display: flex;
  min-height: 0;
}
.md-split-left {
  flex: 1;
  resize: none !important;
  min-height: 0;
  border-right: none;
}
.md-split-divider {
  width: 1px;
  background: #e4e7ed;
  flex-shrink: 0;
}
.md-split-right {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.md-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-wrap: wrap;
  flex-shrink: 0;
}
.tb-btn {
  font-size: 13px;
  min-width: 30px;
  height: 28px;
  padding: 0 6px;
}
.tb-btn b { font-weight: 700; }
.tb-btn i { font-style: italic; }
.md-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: vertical;
  padding: 10px 12px;
  font-size: 14px;
  font-family: 'Courier New', 'Consolas', monospace;
  line-height: 1.6;
  background: #fff;
  box-sizing: border-box;
}
.md-editor.md-fullscreen .md-textarea {
  flex: 1;
  resize: none;
  min-height: 0;
}
.md-textarea:focus {
  background: #fafafa;
}
.md-preview {
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.8;
  background: #fff;
  min-height: 60px;
  overflow-y: auto;
  width: 100%;
  box-sizing: border-box;
}
.md-preview.md-preview-clickable {
  cursor: text;
}
.md-preview.md-preview-clickable:hover {
  background: #fafcff;
}
.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3) {
  margin: 12px 0 8px;
  font-weight: 600;
}
.md-preview :deep(h1) { font-size: 20px; }
.md-preview :deep(h2) { font-size: 18px; }
.md-preview :deep(h3) { font-size: 16px; }
.md-preview :deep(p) { margin: 6px 0; }
.md-preview :deep(ul),
.md-preview :deep(ol) { padding-left: 20px; margin: 6px 0; }
.md-preview :deep(li) { margin: 2px 0; }
.md-preview :deep(blockquote) {
  border-left: 3px solid #409EFF;
  padding: 6px 12px;
  margin: 8px 0;
  background: #f5f7fa;
  color: #666;
}
.md-preview :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'Courier New', 'Consolas', monospace;
}
.md-preview :deep(pre) {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}
.md-preview :deep(pre code) {
  background: none;
  padding: 0;
}
.md-preview :deep(a) { color: #409EFF; text-decoration: none; }
.md-preview :deep(a:hover) { text-decoration: underline; }
.md-preview :deep(hr) { border: none; border-top: 1px solid #e4e7ed; margin: 12px 0; }
.md-preview :deep(img) { max-width: 100%; border-radius: 4px; }
</style>
