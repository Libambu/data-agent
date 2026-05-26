<script setup lang="ts">
import { Client, ClientFactory } from '@a2a-js/sdk/client'
import EvidenceRecallNodeCard from '@/components/evidence-recall-node-card.vue'
import FeasibilityAssessmentNodeCard from '@/components/feasibility-assessment-node-card.vue'
import HumanFeedbackNodeCard from '@/components/human-feedback-node-card.vue'
import PlanExecutionNodeCard from '@/components/plan-execution-node-card.vue'
import PlannerNodeCard from '@/components/planner-node-card.vue'
import PythonAnalysisNodeCard from '@/components/python-analysis-node-card.vue'
import PythonExecutionNodeCard from '@/components/python-execution-node-card.vue'
import PythonGenerationNodeCard from '@/components/python-generation-node-card.vue'
import ReportGenerationNodeCard from '@/components/report-generation-node-card.vue'
import SchemeRecallNodeCard from '@/components/scheme-recall-node-card.vue'
import SqlExecutionNodeCard from '@/components/sql-execution-node-card.vue'
import SqlGenerationNodeCard from '@/components/sql-generation-node-card.vue'
import TableRelationNodeCard from '@/components/table-relation-node-card.vue'
import { type AgentCard } from '@a2a-js/sdk'
import { type Component, computed, markRaw, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import {
  DATA_AGENT_ARTIFACT_OUTPUT,
  DATA_AGENT_GRAPH_NODE,
  DATA_AGENT_MESSAGE_METADATA,
  DATA_AGENT_NODE_ORDER,
} from '@/constants/data-agent-graph-spec'
import { request } from '@/utils/request'

const router = useRouter()

interface GraphStep {
  id: string
  name: string
  content: string
  data?: Record<string, unknown>
  status: 'pending' | 'success'
}

const NODE_COMPONENTS: Record<string, Component> = {
  [DATA_AGENT_GRAPH_NODE.EVIDENCE_RECALL]: markRaw(EvidenceRecallNodeCard),
  [DATA_AGENT_GRAPH_NODE.SCHEMA_RECALL]: markRaw(SchemeRecallNodeCard),
  [DATA_AGENT_GRAPH_NODE.TABLE_RELATION]: markRaw(TableRelationNodeCard),
  [DATA_AGENT_GRAPH_NODE.FEASIBILITY_ASSESSMENT]: markRaw(FeasibilityAssessmentNodeCard),
  [DATA_AGENT_GRAPH_NODE.PLANNER]: markRaw(PlannerNodeCard),
  [DATA_AGENT_GRAPH_NODE.HUMAN_FEEDBACK]: markRaw(HumanFeedbackNodeCard),
  [DATA_AGENT_GRAPH_NODE.PLAN_EXECUTION]: markRaw(PlanExecutionNodeCard),
  [DATA_AGENT_GRAPH_NODE.SQL_GENERATION]: markRaw(SqlGenerationNodeCard),
  [DATA_AGENT_GRAPH_NODE.SQL_EXECUTION]: markRaw(SqlExecutionNodeCard),
  [DATA_AGENT_GRAPH_NODE.PYTHON_GENERATION]: markRaw(PythonGenerationNodeCard),
  [DATA_AGENT_GRAPH_NODE.PYTHON_EXECUTION]: markRaw(PythonExecutionNodeCard),
  [DATA_AGENT_GRAPH_NODE.PYTHON_ANALYSIS]: markRaw(PythonAnalysisNodeCard),
  [DATA_AGENT_GRAPH_NODE.REPORT_GENERATION]: markRaw(ReportGenerationNodeCard),
}

const DEFAULT_EXAMPLES = [
  'What is the highest percentage of K–12 students eligible for free meals among schools in Alameda County?',
  'How many schools that are exclusively virtual have an average SAT Math score greater than 400?',
]
const DATABASE_OPTIONS = [
  'california_schools',
  'toxicology',
  'european_football_2',
  'student_club',
  'debit_card_specializing',
  'card_games',
  'formula_1',
  'thrombosis_prediction',
  'codebase_community',
  'financial',
] as const

const factory = new ClientFactory()
const client = shallowRef<Client | undefined>()
const steps = reactive<GraphStep[]>([])
const userInput = ref(DEFAULT_EXAMPLES[0])
const selectedDatabase = ref<(typeof DATABASE_OPTIONS)[number]>('california_schools')
const currentTaskId = ref<string>()
const currentContextId = ref<string>()
const awaitingConfirmation = ref(false)
const confirmationApproved = ref(true)
const confirmationFeedback = ref('用户确认继续执行')
const upsertStep = (
  name: string,
  chunk: string,
  status: GraphStep['status'],
  data: Record<string, unknown> = {},
) => {
  const existing = steps.find((step) => step.name === name)
  if (existing) {
    existing.content += chunk
    existing.status = status
    existing.data = { ...existing.data, ...data }
    return
  }
  steps.push({
    id: crypto.randomUUID(),
    name,
    data: data,
    content: chunk,
    status: status,
  })
}

const isRunning = ref(false)

const orderedSteps = computed(() => {
  return DATA_AGENT_NODE_ORDER
    .map((name) => steps.find((step) => step.name === name))
    .filter((step): step is GraphStep => Boolean(step))
})

const resetSteps = () => {
  steps.splice(0, steps.length)
  currentTaskId.value = undefined
  currentContextId.value = undefined
  awaitingConfirmation.value = false
  confirmationApproved.value = true
  confirmationFeedback.value = '用户确认继续执行'
}

const markAllPendingStepAsSuccess = () => {
  for (const step of steps) {
    if (step.status === 'pending') {
      step.status = 'success'
    }
  }
}

const streamMessage = async (
  input: string,
  keepSteps = false,
  metadata?: Record<string, unknown>,
) => {
  if (!client.value || !input || isRunning.value) return
  if (!keepSteps) {
    resetSteps()
  }
  isRunning.value = true
  try {
    const stream = client.value.sendMessageStream({
      message: {
        messageId: crypto.randomUUID(),
        role: 'user',
        kind: 'message',
        taskId: currentTaskId.value,
        contextId: currentContextId.value,
        metadata: {
          [DATA_AGENT_MESSAGE_METADATA.DATABASE_ID]: selectedDatabase.value,
          ...(metadata ?? {}),
        },
        parts: [{ kind: 'text', text: input }],
      },
    })
    for await (const event of stream) {
      currentTaskId.value = 'taskId' in event ? event.taskId : currentTaskId.value
      currentContextId.value = 'contextId' in event ? event.contextId : currentContextId.value

      if (event.kind === 'artifact-update') {
        const artifact = event.artifact
        const artifactName = artifact.name
        const text = artifact.parts.find((p) => p.kind === 'text')?.text || ''
        const data = artifact.parts.find((p) => p.kind === 'data')?.data

        if (artifactName) {
          const outputType = String(artifact.metadata?.outputType ?? '')
          const status: GraphStep['status'] =
            outputType === DATA_AGENT_ARTIFACT_OUTPUT.GRAPH_NODE_FINISHED ? 'success' : 'pending'
          upsertStep(artifactName, text, status, data)
        }
      }
      if (event.kind === 'status-update' && event.status.state === 'completed') {
        awaitingConfirmation.value = false
        markAllPendingStepAsSuccess()
      }
      if (event.kind === 'status-update' && event.status.state === 'input-required') {
        awaitingConfirmation.value = true
      }
    }
  } finally {
    isRunning.value = false
  }
}

const handleSend = async () => {
  const input = userInput.value?.trim() ?? ''
  await streamMessage(input, false)
}

const submitConfirmation = async () => {
  awaitingConfirmation.value = false
  const approved = confirmationApproved.value
  const feedback = (confirmationFeedback.value || '').trim()
  await streamMessage(feedback || (approved ? '确认继续' : '取消本次执行'), true, {
    [DATA_AGENT_MESSAGE_METADATA.CONFIRMATION_APPROVED]: approved,
    [DATA_AGENT_MESSAGE_METADATA.CONFIRMATION_FEEDBACK]:
      feedback || (approved ? '用户确认继续执行' : '用户取消本次执行'),
  })
}

const goHome = () => {
  router.push('/')
}

onMounted(async () => {
  const agentCard = await request.get<AgentCard, AgentCard>('/.well-known/agent-card.json')
  client.value = await factory.createFromAgentCard(agentCard)
})
</script>

<template>
  <main class="page-shell">
    <!-- 顶部导航栏 -->
    <nav class="workspace-nav">
      <div class="workspace-nav__left" @click="goHome">
        <el-icon><HomeFilled /></el-icon>
        <span>Data Agent</span>
      </div>
      <div class="workspace-nav__status">
        <span :class="['status-dot', { 'status-dot--active': isRunning }]"></span>
        {{ isRunning ? '执行中' : '就绪' }}
      </div>
    </nav>

    <!-- 输入区域 -->
    <section class="input-panel">
      <div class="input-panel__examples">
        <span class="input-panel__examples-label">示例：</span>
        <button
          v-for="example in DEFAULT_EXAMPLES"
          :key="example"
          class="example-chip"
          type="button"
          @click="userInput = example"
        >
          {{ example }}
        </button>
      </div>

      <div class="input-panel__row">
        <div class="input-panel__db">
          <el-select
            v-model="selectedDatabase"
            placeholder="选择数据库"
            size="large"
          >
            <el-option
              v-for="database in DATABASE_OPTIONS"
              :key="database"
              :label="database"
              :value="database"
            />
          </el-select>
        </div>

        <el-input
          v-model="userInput"
          class="input-panel__textarea"
          :rows="2"
          type="textarea"
          resize="none"
          placeholder="输入一个业务分析问题..."
          @keydown.enter.exact.prevent="handleSend"
        />

        <el-button
          type="primary"
          size="large"
          :loading="isRunning"
          @click="handleSend"
          class="input-panel__btn"
        >
          <el-icon v-if="!isRunning"><Promotion /></el-icon>
          {{ isRunning ? '运行中' : '执行' }}
        </el-button>
      </div>
    </section>

    <!-- 时间线区域 -->
    <section class="timeline-panel">
      <div class="timeline-panel__header">
        <div>
          <h2>节点执行轨迹</h2>
          <p>实时展示各节点的执行状态和输出结果</p>
        </div>
        <div class="timeline-panel__status">
          {{ isRunning ? '正在执行' : orderedSteps.length ? '执行完成' : '等待开始' }}
        </div>
      </div>

      <div v-if="orderedSteps.length" class="timeline-panel__list">
        <template v-for="step in orderedSteps" :key="step.id">
          <component
            v-if="NODE_COMPONENTS[step.name]"
            :is="NODE_COMPONENTS[step.name]"
            :name="step.name"
            :content="step.content"
            :data="step.data"
            :status="step.status"
          />
        </template>
      </div>

      <div v-if="awaitingConfirmation" class="human-input-panel">
        <h3>需要人工输入</h3>
        <p>请填写确认结果和反馈，提交后会继续执行后续节点。</p>
        <div class="human-input-panel__field">
          <div class="human-input-panel__label">确认结果</div>
          <el-radio-group v-model="confirmationApproved">
            <el-radio :value="true">确认继续</el-radio>
            <el-radio :value="false">取消执行</el-radio>
          </el-radio-group>
        </div>
        <div class="human-input-panel__field">
          <div class="human-input-panel__label">反馈内容</div>
          <el-input
            v-model="confirmationFeedback"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="请输入反馈内容"
          />
        </div>
        <el-button type="primary" :loading="isRunning" @click="submitConfirmation">
          提交人工输入
        </el-button>
      </div>

      <div v-else-if="!orderedSteps.length" class="empty-state">
        <el-icon class="empty-state__icon"><Cpu /></el-icon>
        <div class="empty-state__title">等待执行</div>
        <div class="empty-state__desc">
          选择示例问题或输入自定义需求，点击执行即可观察节点运行轨迹。
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.page-shell {
  min-height: 100vh;
  padding: 0 20px 48px;
  background:
    radial-gradient(circle at top left, rgba(253, 224, 71, 0.14), transparent 24%),
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.1), transparent 20%),
    linear-gradient(180deg, #fffdf8 0%, #f8fafc 100%);
}

/* 导航栏 */
.workspace-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1080px;
  margin: 0 auto;
  padding: 16px 0;
}

.workspace-nav__left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  cursor: pointer;
  transition: color 0.2s;
}

.workspace-nav__left:hover {
  color: #f97316;
}

.workspace-nav__status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}

.status-dot--active {
  background: #22c55e;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 输入区域 */
.input-panel {
  max-width: 1080px;
  margin: 0 auto 24px;
  padding: 20px 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.04);
}

.input-panel__examples {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.input-panel__examples-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.example-chip {
  border: none;
  border-radius: 999px;
  padding: 6px 12px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e2e8f0;
}

.example-chip:hover {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #ea580c;
}

.input-panel__row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.input-panel__db {
  flex-shrink: 0;
  width: 180px;
}

.input-panel__textarea {
  flex: 1;
}

.input-panel__btn {
  flex-shrink: 0;
  height: 56px;
  padding: 0 24px;
}

/* 时间线 */
.timeline-panel {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.04);
}

.timeline-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.timeline-panel__header h2 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.timeline-panel__header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

.timeline-panel__status {
  border-radius: 999px;
  padding: 8px 14px;
  background: #f1f5f9;
  color: #334155;
  font-size: 12px;
  font-weight: 500;
}

.timeline-panel__list {
  display: grid;
  gap: 16px;
}

/* 人工输入面板 */
.human-input-panel {
  margin-top: 18px;
  border: 1px solid rgba(168, 85, 247, 0.24);
  border-radius: 16px;
  padding: 16px;
  background:
    radial-gradient(circle at top left, rgba(168, 85, 247, 0.1), transparent 32%),
    linear-gradient(180deg, #ffffff, #faf5ff);
}

.human-input-panel h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.human-input-panel p {
  margin: 8px 0 0;
  color: #475569;
}

.human-input-panel__field {
  margin: 14px 0;
}

.human-input-panel__label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #334155;
}

/* 空状态 */
.empty-state {
  padding: 48px 20px;
  border: 1px dashed rgba(100, 116, 139, 0.25);
  border-radius: 16px;
  text-align: center;
  background: rgba(248, 250, 252, 0.6);
}

.empty-state__icon {
  font-size: 36px;
  color: #cbd5e1;
  margin-bottom: 12px;
}

.empty-state__title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
}

.empty-state__desc {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 768px) {
  .input-panel__row {
    flex-direction: column;
  }

  .input-panel__db {
    width: 100%;
  }

  .input-panel__btn {
    width: 100%;
    height: 44px;
  }
}
</style>
