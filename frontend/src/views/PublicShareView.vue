<template>
  <div class="public-share-page">
    <div v-if="loading" class="share-loading">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="error" class="share-error">
      <el-icon :size="48" color="#F56C6C"><CircleCloseFilled /></el-icon>
      <h3>{{ error }}</h3>
      <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
    </div>

    <div v-else-if="resource" class="share-content">
      <div class="share-header">
        <h2>{{ resource.title || '无标题' }}</h2>
        <div class="share-meta">
          <el-tag :type="permission === 'EDIT' ? 'warning' : 'info'" size="small" effect="plain">
            {{ permission === 'EDIT' ? '可编辑' : '仅查看' }}
          </el-tag>
          <span v-if="ownerNickname" class="share-owner">
            由 <b>{{ ownerNickname }}</b> 分享
          </span>
          <span v-if="projectName" class="share-project">{{ projectName }}</span>
        </div>
      </div>

      <!-- 查看模式：Markdown 渲染 -->
      <div v-if="!editing" class="share-body">
        <div v-if="resource.description" class="share-description" v-html="renderedDescription" />
        <div v-else class="share-empty-desc">暂无内容</div>

        <div v-if="permission === 'EDIT'" class="share-edit-bar">
          <el-button type="primary" size="small" @click="startEdit">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
        </div>
      </div>

      <!-- 编辑模式 -->
      <div v-else class="share-body">
        <MarkdownEditor v-model="editDescription" :rows="16" start-editing />
        <div class="share-edit-actions">
          <el-button size="small" @click="cancelEdit">取消</el-button>
          <el-button size="small" type="primary" :loading="saving" @click="saveEdit">保存</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicSharedResource, updatePublicSharedResource } from '@/api/share'
import { Loading, CircleCloseFilled, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownEditor from '@/components/common/MarkdownEditor.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const resource = ref(null)
const permission = ref('VIEW')
const ownerNickname = ref('')
const projectName = ref('')

const editing = ref(false)
const editDescription = ref('')
const saving = ref(false)

const renderedDescription = computed(() => {
  if (!resource.value?.description) return ''
  // 简单的换行渲染，保留格式
  return resource.value.description
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})

const shareCode = route.params.shareCode

const loadResource = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getPublicSharedResource(shareCode)
    const data = res.data
    resource.value = data.resource
    permission.value = data.permission
    ownerNickname.value = data.ownerNickname || ''
    projectName.value = data.projectName || ''
  } catch (e) {
    if (e.response && e.response.status === 401) {
      error.value = '编辑需要登录'
    } else {
      error.value = e.response?.data?.msg || '加载失败'
    }
  } finally {
    loading.value = false
  }
}

const startEdit = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再编辑')
    router.push('/login')
    return
  }
  editDescription.value = resource.value?.description || ''
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
}

const saveEdit = async () => {
  saving.value = true
  try {
    await updatePublicSharedResource(shareCode, {
      title: resource.value.title,
      description: editDescription.value
    })
    resource.value.description = editDescription.value
    editing.value = false
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadResource)
</script>

<style scoped>
.public-share-page {
  min-height: 100vh; background: #f5f7fa;
  display: flex; justify-content: center; padding: 40px 20px;
}
.share-loading {
  display: flex; align-items: center; gap: 8px; color: #999; font-size: 14px;
}
.share-error {
  display: flex; flex-direction: column; align-items: center; gap: 16px;
  padding: 60px 20px; text-align: center;
}
.share-error h3 { margin: 0; color: #606266; }
.share-content {
  background: #fff; border-radius: 12px; padding: 32px;
  max-width: 800px; width: 100%; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  align-self: flex-start;
}
.share-header { margin-bottom: 24px; }
.share-header h2 { margin: 0 0 8px; font-size: 20px; }
.share-meta { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #999; }
.share-owner { }
.share-project { background: #ecf5ff; color: #409eff; padding: 1px 8px; border-radius: 10px; font-size: 12px; }
.share-body { }
.share-description { font-size: 14px; line-height: 1.8; color: #333; white-space: pre-wrap; word-break: break-word; }
.share-empty-desc { color: #999; font-size: 14px; padding: 20px 0; }
.share-edit-bar { margin-top: 20px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.share-edit-actions { margin-top: 12px; display: flex; justify-content: flex-end; gap: 8px; }
</style>
