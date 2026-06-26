<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="'共享设置 - ' + (resource?.title || resource?.name || '')"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="resource" class="share-dialog-content">
      <!-- 搜索添加协作者 -->
      <div class="share-section">
        <div class="share-section-title">添加协作者</div>
        <el-autocomplete
          v-model="searchKeyword"
          :fetch-suggestions="handleSearch"
          placeholder="搜索用户名或昵称"
          :trigger-on-focus="false"
          style="width: 100%"
          size="small"
          @select="handleSelectUser"
          popper-class="share-user-suggest"
        >
          <template #default="{ item }">
            <div class="user-suggest-item">
              <span class="user-nickname">{{ item.nickname || item.username }}</span>
              <span class="user-username">({{ item.username }})</span>
            </div>
          </template>
        </el-autocomplete>
      </div>

      <!-- 协作者列表 -->
      <div class="share-section">
        <div class="share-section-title">协作者 ({{ members.length }})</div>
        <MemberList
          :members="members"
          @update-permission="handleUpdatePermission"
          @remove="handleRemoveMember"
        />
      </div>

      <!-- 分享链接 -->
      <div class="share-section">
        <div class="share-section-title">分享链接</div>
        <ShareLinkManager
          :links="links"
          @create="handleCreateLink"
          @disable="handleDisableLink"
        />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getShareMembers, addShareMember, updateShareMember, removeShareMember,
  getShareLinks, createShareLink, disableShareLink, searchShareUsers
} from '@/api/share'
import MemberList from './MemberList.vue'
import ShareLinkManager from './ShareLinkManager.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  resource: { type: Object, default: null },
  resourceType: { type: String, default: 'TASK' } // TASK / PROJECT
})

const emit = defineEmits(['update:visible'])

const searchKeyword = ref('')
const members = ref([])
const links = ref([])

const loadData = async () => {
  if (!props.resource) return
  const id = props.resource.id
  try {
    const [membersRes, linksRes] = await Promise.all([
      getShareMembers(props.resourceType, id),
      getShareLinks(props.resourceType, id)
    ])
    members.value = membersRes.data || []
    links.value = linksRes.data || []
  } catch (e) {}
}

watch(() => props.visible, (val) => {
  if (val && props.resource) {
    loadData()
    searchKeyword.value = ''
  }
})

const handleSearch = async (keyword, cb) => {
  if (!keyword || keyword.length < 1) { cb([]); return }
  try {
    const res = await searchShareUsers(keyword)
    cb(res.data || [])
  } catch (e) { cb([]) }
}

const handleSelectUser = async (user) => {
  searchKeyword.value = ''
  try {
    await addShareMember({
      resourceType: props.resourceType,
      resourceId: props.resource.id,
      username: user.username,
      permission: 'VIEW'
    })
    ElMessage.success('已添加 ' + (user.nickname || user.username))
    await loadData()
  } catch (e) {}
}

const handleUpdatePermission = async (memberId, permission) => {
  try {
    await updateShareMember(memberId, permission)
    ElMessage.success('权限已更新')
    await loadData()
  } catch (e) {}
}

const handleRemoveMember = async (memberId) => {
  try {
    await removeShareMember(memberId)
    ElMessage.success('已移除')
    await loadData()
  } catch (e) {}
}

const handleCreateLink = async (permission) => {
  try {
    await createShareLink({
      resourceType: props.resourceType,
      resourceId: props.resource.id,
      permission
    })
    ElMessage.success('分享链接已创建')
    await loadData()
  } catch (e) {}
}

const handleDisableLink = async (linkId) => {
  try {
    await disableShareLink(linkId)
    ElMessage.success('链接已禁用')
    await loadData()
  } catch (e) {}
}
</script>

<style scoped>
.share-dialog-content { display: flex; flex-direction: column; gap: 16px; }
.share-section-title { font-size: 13px; font-weight: 600; color: #606266; margin-bottom: 8px; }
.user-suggest-item { display: flex; gap: 4px; align-items: center; }
.user-nickname { font-weight: 500; }
.user-username { color: #999; font-size: 12px; }
</style>
