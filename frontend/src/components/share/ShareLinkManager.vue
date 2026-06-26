<template>
  <div class="link-manager">
    <div v-if="links.length === 0" class="link-empty">
      <span>暂无分享链接</span>
      <el-dropdown trigger="click" @command="$emit('create', $event)">
        <el-button type="primary" size="small" plain>
          <el-icon><Plus /></el-icon> 创建链接
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="VIEW">创建查看链接</el-dropdown-item>
            <el-dropdown-item command="EDIT">创建编辑链接</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <div v-else class="link-list">
      <div v-for="link in links" :key="link.id" class="link-item" :class="{ disabled: !link.isActive }">
        <div class="link-info">
          <el-icon :size="14"><Link /></el-icon>
          <span class="link-code">{{ link.shareUrl }}</span>
          <el-tag size="small" :type="link.permission === 'EDIT' ? 'warning' : 'info'" effect="plain">
            {{ link.permission === 'EDIT' ? '编辑' : '查看' }}
          </el-tag>
          <el-tag v-if="!link.isActive" size="small" type="danger" effect="plain">已禁用</el-tag>
        </div>
        <div class="link-actions" v-if="link.isActive">
          <el-button text size="small" @click="copyLink(link)">
            <el-icon><DocumentCopy /></el-icon>
          </el-button>
          <el-button text type="danger" size="small" @click="$emit('disable', link.id)">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <el-dropdown trigger="click" @command="$emit('create', $event)" style="margin-top: 6px;">
        <el-button size="small" plain>
          <el-icon><Plus /></el-icon> 新建链接
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="VIEW">创建查看链接</el-dropdown-item>
            <el-dropdown-item command="EDIT">创建编辑链接</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { Plus, Link, DocumentCopy, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

defineProps({
  links: { type: Array, default: () => [] }
})

defineEmits(['create', 'disable'])

const copyLink = async (link) => {
  const url = window.location.origin + link.shareUrl
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = url
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('链接已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.link-manager { display: flex; flex-direction: column; gap: 6px; }
.link-empty {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; color: #999; padding: 8px; background: #f5f7fa; border-radius: 6px;
}
.link-list { display: flex; flex-direction: column; gap: 6px; }
.link-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 8px; border-radius: 6px; background: #f5f7fa;
}
.link-item.disabled { opacity: 0.5; }
.link-info { display: flex; align-items: center; gap: 6px; }
.link-code { font-size: 12px; color: #606266; font-family: monospace; }
.link-actions { display: flex; align-items: center; gap: 2px; }
</style>
