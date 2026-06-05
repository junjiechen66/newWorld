import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTree } from '@/api/group'

export const useProjectStore = defineStore('project', () => {
  const treeData = ref([])
  const selectedProjectId = ref(null)
  const selectedGroupId = ref(null)
  const selectedTaskId = ref(null)

  const fetchTree = async () => {
    const res = await getTree()
    treeData.value = res.data
  }

  const selectProject = (projectId) => {
    selectedProjectId.value = projectId
  }

  const selectTask = (taskId) => {
    selectedTaskId.value = taskId
  }

  return { treeData, selectedProjectId, selectedGroupId, selectedTaskId, fetchTree, selectProject, selectTask }
})
