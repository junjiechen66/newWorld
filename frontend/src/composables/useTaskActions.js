import { updateTaskStatus } from '@/api/task'
import { emitRefresh } from '@/utils/events'

export function useTaskActions() {
  const statusLabel = (s) => {
    const map = { INCOMPLETE: '未完成', DONE: '已完成', SHELVED: '搁置' }
    return map[s] || '未完成'
  }

  const statusTagType = (s) => {
    const map = { INCOMPLETE: 'info', DONE: 'success', SHELVED: 'warning' }
    return map[s] || 'info'
  }

  const priorityLabel = (p) => {
    const map = {
      Q1: 'Q1 重要且紧急', Q2: 'Q2 重要不紧急',
      Q3: 'Q3 不重要但紧急', Q4: 'Q4 不重要不紧急',
      RED: '高', YELLOW: '中', BLUE: '低'
    }
    return map[p] || ''
  }

  // 短标签（用于卡片）
  const priorityShortLabel = (p) => {
    const map = { Q1: 'Q1', Q2: 'Q2', Q3: 'Q3', Q4: 'Q4', RED: '高', YELLOW: '中', BLUE: '低' }
    return map[p] || ''
  }

  const cycleStatus = async (task) => {
    const cycle = ['INCOMPLETE', 'DONE', 'SHELVED']
    const idx = cycle.indexOf(task.status)
    const next = cycle[(idx + 1) % cycle.length]
    try {
      await updateTaskStatus(task.id, next)
      task.status = next
      emitRefresh()
      return next
    } catch (e) {
      return null
    }
  }

  return {
    statusLabel,
    statusTagType,
    priorityLabel,
    priorityShortLabel,
    cycleStatus
  }
}
