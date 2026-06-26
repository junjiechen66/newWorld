<template>
  <div class="member-list">
    <div v-if="members.length === 0" class="member-empty">暂无协作者</div>
    <div v-for="m in members" :key="m.id" class="member-item">
      <div class="member-info">
        <el-icon :size="16"><User /></el-icon>
        <span class="member-name">{{ m.nickname || m.username }}</span>
        <span class="member-username">({{ m.username }})</span>
      </div>
      <div class="member-actions">
        <el-select
          :model-value="m.permission"
          size="small"
          style="width: 90px"
          @change="$emit('update-permission', m.id, $event)"
        >
          <el-option label="查看" value="VIEW" />
          <el-option label="编辑" value="EDIT" />
        </el-select>
        <el-button type="danger" text size="small" @click="$emit('remove', m.id)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { User, Delete } from '@element-plus/icons-vue'

defineProps({
  members: { type: Array, default: () => [] }
})

defineEmits(['update-permission', 'remove'])
</script>

<style scoped>
.member-list { display: flex; flex-direction: column; gap: 6px; }
.member-empty { font-size: 13px; color: #999; padding: 8px 0; text-align: center; }
.member-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 8px; border-radius: 6px; background: #f5f7fa;
}
.member-info { display: flex; align-items: center; gap: 6px; }
.member-name { font-size: 13px; font-weight: 500; }
.member-username { font-size: 12px; color: #999; }
.member-actions { display: flex; align-items: center; gap: 4px; }
</style>
