<template>
  <div class="project-view">
    <div class="page-header">
      <h2>项目总览</h2>
      <el-button type="primary" size="small" @click="showAddProject = true">
        <el-icon><Plus /></el-icon> 新建项目
      </el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="group in projectStore.treeData" :key="group.id" :span="24" style="margin-bottom: 16px;">
        <el-card shadow="never" class="group-card">
          <template #header>
            <div class="group-header">
              <span><el-icon><Folder /></el-icon> {{ group.name }}</span>
            </div>
          </template>
          <el-row :gutter="12">
            <el-col v-for="project in group.children" :key="project.id" :span="8" style="margin-bottom: 12px;">
              <el-card shadow="hover" class="project-card" @click="router.push('/calendar?projectId=' + project.id)">
                <div class="project-card-header">
                  <el-tag :color="project.color || '#409EFF'" class="project-color-tag" />
                  <span class="project-name">{{ project.name }}</span>
                </div>
                <div class="project-stats">
                  <span class="stat">未完成: {{ getTaskCountByStatus(project.children, 'INCOMPLETE') }}</span>
                  <span class="stat">已完成: {{ getTaskCountByStatus(project.children, 'DONE') }}</span>
                  <span class="stat">搁置: {{ getTaskCountByStatus(project.children, 'SHELVED') }}</span>
                </div>
              </el-card>
            </el-col>
            <el-col v-if="!group.children?.length" :span="24">
              <el-empty description="暂无项目" :image-size="60" />
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- Add Project Dialog -->
    <el-dialog v-model="showAddProject" title="新建项目" width="450px">
      <el-form :model="projectForm" label-width="80px" size="small">
        <el-form-item label="所属分组">
          <el-select v-model="projectForm.groupId" placeholder="选择分组" style="width: 100%">
            <el-option v-for="g in projectStore.treeData" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="projectForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目颜色">
          <el-color-picker v-model="projectForm.color" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddProject = false">取消</el-button>
        <el-button type="primary" @click="submitProject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { createProject } from '@/api/project'
import { ElMessage } from 'element-plus'

const router = useRouter()
const projectStore = useProjectStore()

const showAddProject = ref(false)
const projectForm = reactive({
  groupId: null,
  name: '',
  color: '#409EFF',
  description: ''
})

const getTaskCountByStatus = (tasks, status) => {
  if (!tasks) return 0
  return tasks.filter(t => t.status === status).length
}

const submitProject = async () => {
  if (!projectForm.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  if (!projectForm.groupId) {
    ElMessage.warning('请选择所属分组')
    return
  }
  try {
    await createProject({ ...projectForm })
    ElMessage.success('项目创建成功')
    showAddProject.value = false
    projectForm.groupId = null
    projectForm.name = ''
    projectForm.color = '#409EFF'
    projectForm.description = ''
    await projectStore.fetchTree()
  } catch (e) {}
}

onMounted(async () => {
  if (!projectStore.treeData.length) {
    await projectStore.fetchTree()
  }
})
</script>

<style scoped>
.project-view { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 18px; font-weight: 600; margin: 0; }
.group-card { border-radius: 8px; }
.group-header { font-size: 15px; font-weight: 500; display: flex; align-items: center; gap: 8px; }
.project-card { cursor: pointer; border-radius: 6px; transition: transform 0.15s; }
.project-card:hover { transform: translateY(-2px); }
.project-card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.project-color-tag { width: 12px; height: 12px; border-radius: 3px; padding: 0; border: none; }
.project-name { font-size: 14px; font-weight: 500; }
.project-stats { display: flex; gap: 12px; font-size: 12px; color: var(--text-secondary); }
</style>
