import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTree } from '@/api/group'
import { getProjectList } from '@/api/project'

export const useProjectStore = defineStore('project', () => {
  const treeData = ref([])
  const selectedProjectId = ref(null)
  const projectOptions = ref([])

  const fetchTree = async (startDate, endDate) => {
    const res = await getTree(null, startDate, endDate)
    treeData.value = res.data
  }

  const loadProjects = async () => {
    try {
      const res = await getProjectList()
      projectOptions.value = res.data || []
    } catch (e) {}
  }

  const selectProject = (projectId) => {
    selectedProjectId.value = projectId
  }

  return { treeData, selectedProjectId, projectOptions, fetchTree, loadProjects, selectProject }
})
