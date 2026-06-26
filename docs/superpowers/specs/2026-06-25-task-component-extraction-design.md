# 任务组件抽取重构设计文档

## 背景

`TaskListView.vue`（740行）和 `TodayView.vue`（484行）之间存在大量重复代码：
- 编辑弹窗（el-dialog + editForm）：~60行
- 创建弹窗（el-dialog + createForm）：~40行
- 详情面板（Teleport + detail-panel）：~30行
- 状态/优先级辅助函数：~50行
- CSS 样式：~100行

任何 UI 改动都需同步修改两个文件，维护成本高且容易出错。

## 目标

将重复代码按职责抽取为独立组件 + composable，消除重复，提升可维护性，为后续功能扩展打基础。

## 架构

### 新建组件

| 组件 | 路径 | 职责 |
|------|------|------|
| `TaskEditDialog.vue` | `frontend/src/components/task/` | 新建/编辑任务弹窗，通过 `mode` prop 区分 |
| `TaskEditablePanel.vue` | `frontend/src/components/task/` | 右侧可编辑详情面板（标题 + MarkdownEditor） |

### 新建 Composable

| 文件 | 路径 | 导出 |
|------|------|------|
| `useTaskActions.js` | `frontend/src/composables/` | `statusLabel`, `statusTagType`, `priorityLabel`, `cycleStatus` 等共享函数 |

### 不变组件

- `components/task/TaskDetailPanel.vue` — 保留原样（只读面板，用于其他场景）

## 组件接口

### TaskEditDialog.vue

```vue
<Props>
  visible: boolean          // v-model，控制弹窗显隐
  mode: 'create' | 'edit'  // 弹窗模式
  task: object | null       // 编辑时传入当前任务数据
  projectOptions: array     // 项目下拉选项
</Props>

<Emits>
  update:visible            // 关闭弹窗
  save: { title, status, priority, projectId, startDate, dueDate }  // 保存
</Emits>
```

- 内部复用 el-dialog + el-form
- `mode='create'` 时默认 status='INCOMPLETE', priority='Q1'
- `mode='edit'` 时用 task prop 填充表单
- 优先级四象限 radio 样式保持现有设计

### TaskEditablePanel.vue

```vue
<Props>
  visible: boolean          // v-model，控制面板显隐
  task: object | null       // 当前任务（包含 id, title, description, status）
</Props>

<Emits>
  update:visible            // 关闭面板
  save: { title, description }  // 保存标题和描述
</Emits>
```

- 右侧 50% 宽度浮层，Teleport to body
- 顶部显示状态标签（颜色 + 文字）
- 标题使用 el-input（大号无框样式）
- 内容使用 MarkdownEditor 组件
- 底部保存按钮
- 点击遮罩区域关闭

### useTaskActions.js

```js
export function useTaskActions() {
  const statusLabel = (s) => { ... }       // INCOMPLETE → '未完成'
  const statusTagType = (s) => { ... }     // INCOMPLETE → 'info'
  const priorityLabel = (p) => { ... }     // Q1 → 'Q1 重要且紧急'
  const cycleStatus = async (task, onSuccess) => { ... }  // 循环切换状态
  return { statusLabel, statusTagType, priorityLabel, cycleStatus }
}
```

- `cycleStatus` 接受 task 对象和成功回调，内部调用 `updateTaskStatus` API
- 成功后触发 `refresh-data` 事件

## 数据流

```
TaskListView / TodayView
  ├─ <TaskEditDialog :mode="..." :task="..." :projectOptions="..." @save="handleSave" />
  ├─ <TaskEditablePanel :task="..." @save="handleSaveDesc" />
  └─ const { statusLabel, cycleStatus, ... } = useTaskActions()
```

- 组件**不直接调用 API**（除 cycleStatus 内部调用 updateTaskStatus）
- 保存/删除操作通过 emit 通知父组件
- 父组件负责 API 调用、ElMessage 提示、数据刷新

## 文件变动

| 文件 | 操作 |
|------|------|
| `components/task/TaskEditDialog.vue` | 新建 |
| `components/task/TaskEditablePanel.vue` | 新建 |
| `composables/useTaskActions.js` | 新建 |
| `views/TaskListView.vue` | 删除 ~300 行重复代码，引入新组件 |
| `views/TodayView.vue` | 删除 ~300 行重复代码，引入新组件 |

## CSS 策略

- 共享 CSS（task-card、priority-badge、quadrant-radio 等）移入 `styles/global.css` 或组件内 scoped
- 页面特有样式（filter-card、section-title 等）保留在各自页面

## 不做的事

- 不引入前端测试框架（YAGNI）
- 不改动旧 `TaskDetailPanel.vue`
- 不改变任何后端接口
- 不改变任何用户可见的 UI 行为和交互
