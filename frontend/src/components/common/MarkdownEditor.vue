<template>
  <div class="md-editor" :class="{ 'md-preview-only': !editable }">
    <!-- Toolbar (only in edit mode) -->
    <div v-if="editable" class="md-toolbar">
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
      <el-divider direction="vertical" />
      <el-tooltip content="预览" placement="top">
        <el-button text size="small" :type="showPreview ? 'primary' : ''" class="tb-btn" @click="togglePreview">{{ showPreview ? '编辑' : '预览' }}</el-button>
      </el-tooltip>
    </div>

    <!-- Edit area -->
    <textarea
      v-if="editable && !showPreview"
      ref="textareaRef"
      :value="modelValue"
      @input="onInput"
      class="md-textarea"
      :rows="rows"
      :placeholder="placeholder"
    ></textarea>

    <!-- Preview area -->
    <div v-if="showPreview || !editable" class="md-preview" v-html="renderedHtml"></div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  modelValue: { type: String, default: '' },
  editable: { type: Boolean, default: true },
  placeholder: { type: String, default: '请输入内容...' },
  rows: { type: Number, default: 6 }
})

const emit = defineEmits(['update:modelValue'])

const textareaRef = ref(null)
const showPreview = ref(false)

const onInput = (e) => {
  emit('update:modelValue', e.target.value)
}

const togglePreview = () => {
  showPreview.value = !showPreview.value
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

  before = props.modelValue.substring(0, start)
  after = props.modelValue.substring(end)
  const newVal = before + insert + after
  emit('update:modelValue', newVal)

  // Restore cursor position after Vue update
  const cursorPos = start + insert.length
  requestAnimationFrame(() => {
    ta.focus()
    ta.setSelectionRange(cursorPos, cursorPos)
  })
}
</script>

<style scoped>
.md-editor {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}
.md-editor.md-preview-only {
  border: none;
}
.md-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-wrap: wrap;
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
.md-textarea:focus {
  background: #fafafa;
}
.md-preview {
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.8;
  background: #fff;
  min-height: 60px;
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
