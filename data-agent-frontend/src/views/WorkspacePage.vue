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
const userInput = ref(DEFAULT_EXAMPLES[0] ?? '')
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

const completedStepCount = computed(
  () => orderedSteps.value.filter((step) => step.status === 'success').length,
)
const progressPercentage = computed(() => {
  if (isRunning.value && !orderedSteps.value.length) return 8
  return Math.round((completedStepCount.value / DATA_AGENT_NODE_ORDER.length) * 100)
})
const isClientReady = computed(() => Boolean(client.value))
const canSubmit = computed(() => Boolean((userInput.value ?? '').trim()) && isClientReady.value && !isRunning.value)
const workspaceStatusText = computed(() => {
  if (!isClientReady.value) return '连接中'
  if (isRunning.value) return '执行中'
  if (orderedSteps.value.length) return '执行完成'
  return '就绪'
})
const timelineStatusText = computed(() => {
  if (!isClientReady.value) return '正在连接 Agent'
  if (isRunning.value) return '正在执行'
  if (orderedSteps.value.length) return '执行完成'
  return '等待开始'
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
        <span class="workspace-nav__logo">
          <el-icon><HomeFilled /></el-icon>
        </span>
        <div>
          <span>Data Agent</span>
          <small>智能数据分析工作台</small>
        </div>
      </div>
      <div class="workspace-nav__status">
        <span
          :class="[
            'status-dot',
            { 'status-dot--active': isRunning, 'status-dot--ready': isClientReady && !isRunning },
          ]"
        ></span>
        {{ workspaceStatusText }}
      </div>
    </nav>

    <section class="workspace-hero">
      <div>
        <div class="workspace-hero__eyebrow">Agent Command Center</div>
        <h1>把业务问题转化为可追踪的数据分析流程</h1>
        <p>选择数据库、输入自然语言问题，系统会自动完成召回、规划、SQL/Python 执行与报告生成。</p>
      </div>
      <div class="workspace-hero__stats">
        <div class="workspace-stat">
          <strong>{{ orderedSteps.length }}</strong>
          <span>已触发节点</span>
        </div>
        <div class="workspace-stat">
          <strong>{{ completedStepCount }}</strong>
          <span>完成节点</span>
        </div>
        <div class="workspace-stat">
          <strong>{{ progressPercentage }}%</strong>
          <span>执行进度</span>
        </div>
      </div>
    </section>

    <!-- 输入区域 -->
    <section class="input-panel">
      <div class="input-panel__topline">
        <div>
          <h2>分析请求</h2>
          <p>可直接使用示例，也可以输入自己的业务分析问题。</p>
        </div>
        <div class="input-panel__db">
          <span>数据集</span>
          <el-select v-model="selectedDatabase" placeholder="选择数据库" size="large">
            <el-option
              v-for="database in DATABASE_OPTIONS"
              :key="database"
              :label="database"
              :value="database"
            />
          </el-select>
        </div>
      </div>

      <div class="input-panel__examples">
        <span class="input-panel__examples-label">推荐问题</span>
        <button
          v-for="example in DEFAULT_EXAMPLES"
          :key="example"
          class="example-chip"
          type="button"
          @click="userInput = example"
        >
          <el-icon><ChatLineRound /></el-icon>
          {{ example }}
        </button>
      </div>

      <div class="input-panel__composer">
        <el-input
          v-model="userInput"
          class="input-panel__textarea"
          :rows="3"
          type="textarea"
          resize="none"
          placeholder="例如：统计 Alameda County 中 K-12 学生免费餐资格比例最高的学校..."
          @keydown.enter.exact.prevent="handleSend"
        />

        <div class="input-panel__actions">
          <span>{{ isClientReady ? 'Agent 已连接' : '正在连接 Agent...' }}</span>
          <el-button
            class="input-panel__btn"
            type="primary"
            size="large"
            :disabled="!canSubmit"
            :loading="isRunning"
            @click="handleSend"
          >
            <el-icon v-if="!isRunning"><Promotion /></el-icon>
            {{ isRunning ? '运行中' : '开始执行' }}
          </el-button>
        </div>
      </div>
    </section>

    <!-- 时间线区域 -->
    <section class="timeline-panel">
      <div class="timeline-panel__header">
        <div>
          <span class="timeline-panel__eyebrow">Execution Timeline</span>
          <h2>节点执行轨迹</h2>
          <p>实时展示各节点状态、结构化输出、代码执行结果和最终报告。</p>
        </div>
        <div class="timeline-panel__status">
          {{ timelineStatusText }}
        </div>
      </div>

      <div class="progress-card">
        <div class="progress-card__info">
          <span>整体进度</span>
          <strong>{{ completedStepCount }} / {{ DATA_AGENT_NODE_ORDER.length }}</strong>
        </div>
        <div class="progress-card__bar" aria-hidden="true">
          <span :style="{ width: `${progressPercentage}%` }"></span>
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
        <div class="human-input-panel__header">
          <div class="human-input-panel__icon">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div>
            <h3>需要人工输入</h3>
            <p>请填写确认结果和反馈，提交后会继续执行后续节点。</p>
          </div>
        </div>
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
        <div class="empty-state__orb">
          <el-icon class="empty-state__icon"><Cpu /></el-icon>
        </div>
        <div class="empty-state__title">等待执行</div>
        <div class="empty-state__desc">
          选择示例问题或输入自定义需求，点击执行即可观察节点运行轨迹。
        </div>
        <div class="empty-state__steps">
          <span>召回</span>
          <span>规划</span>
          <span>执行</span>
          <span>报告</span>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.page-shell {
  position: relative;
  min-height: 100vh;
  padding: 18px clamp(16px, 2.5vw, 34px) 58px;
  overflow: hidden;
  background:
    radial-gradient(circle at 8% 0%, rgba(249, 115, 22, 0.16), transparent 30%),
    radial-gradient(circle at 92% 6%, rgba(37, 99, 235, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(255, 251, 235, 0.92) 0%, rgba(248, 250, 252, 0.96) 48%, rgba(239, 246, 255, 0.96) 100%);
}

.page-shell::before,
.page-shell::after {
  position: absolute;
  z-index: 0;
  pointer-events: none;
  content: '';
  border-radius: 999px;
}

.page-shell::before {
  top: 160px;
  left: -120px;
  width: 280px;
  height: 280px;
  background: rgba(249, 115, 22, 0.1);
  filter: blur(4px);
}

.page-shell::after {
  right: -160px;
  bottom: 8%;
  width: 360px;
  height: 360px;
  background: rgba(37, 99, 235, 0.1);
  filter: blur(4px);
}

/* 导航栏 */
.workspace-nav,
.workspace-hero,
.input-panel,
.timeline-panel {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin-right: auto;
  margin-left: auto;
}

.workspace-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.07);
  backdrop-filter: blur(20px);
}

.workspace-nav__left {
  display: flex;
  align-items: center;
  gap: 11px;
  color: #0f172a;
  cursor: pointer;
  transition: color 0.2s, transform 0.2s;
}

.workspace-nav__left:hover {
  color: #f97316;
  transform: translateY(-1px);
}

.workspace-nav__left span:not(.workspace-nav__logo) {
  display: block;
  font-size: 16px;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.workspace-nav__left small {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.workspace-nav__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 15px;
  color: #ffffff;
  background: linear-gradient(135deg, #f97316, #2563eb);
  box-shadow: 0 12px 28px rgba(249, 115, 22, 0.22);
}

.workspace-nav__status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 13px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 800;
}

.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #94a3b8;
  box-shadow: 0 0 0 4px rgba(148, 163, 184, 0.12);
}

.status-dot--ready {
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.14);
}

.status-dot--active {
  background: #22c55e;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.14);
  }

  50% {
    opacity: 0.55;
    box-shadow: 0 0 0 8px rgba(34, 197, 94, 0.08);
  }
}

.workspace-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 26px;
  align-items: end;
  margin-bottom: 22px;
  padding: 26px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 32px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(248, 250, 252, 0.74)),
    radial-gradient(circle at top right, rgba(249, 115, 22, 0.1), transparent 30%);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(22px);
}

.workspace-hero__eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 7px 12px;
  border: 1px solid rgba(249, 115, 22, 0.2);
  border-radius: 999px;
  color: #ea580c;
  background: rgba(255, 247, 237, 0.72);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.workspace-hero h1 {
  max-width: 760px;
  margin: 0;
  color: #0f172a;
  font-size: clamp(30px, 4vw, 52px);
  font-weight: 950;
  line-height: 1.08;
  letter-spacing: -0.055em;
}

.workspace-hero p {
  max-width: 720px;
  margin: 14px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.8;
}

.workspace-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, 112px);
  gap: 10px;
}

.workspace-stat {
  padding: 15px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  text-align: center;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.workspace-stat strong,
.workspace-stat span {
  display: block;
}

.workspace-stat strong {
  color: #0f172a;
  font-size: 26px;
  line-height: 1;
}

.workspace-stat span {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

/* 输入区域 */
.input-panel {
  margin-bottom: 24px;
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 20px 58px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.input-panel__topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.input-panel__topline h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.03em;
}

.input-panel__topline p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.input-panel__db {
  display: grid;
  flex-shrink: 0;
  gap: 7px;
  width: 236px;
}

.input-panel__db span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.input-panel__examples {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-bottom: 16px;
}

.input-panel__examples-label {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
}

.example-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: min(100%, 520px);
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  padding: 8px 13px;
  color: #475569;
  background: rgba(248, 250, 252, 0.82);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease,
    color 0.2s ease;
}

.example-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(249, 115, 22, 0.28);
  color: #ea580c;
  background: rgba(255, 247, 237, 0.9);
}

.input-panel__composer {
  display: grid;
  gap: 14px;
}

.input-panel__textarea {
  width: 100%;
}

.input-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.input-panel__actions span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.input-panel__btn {
  min-width: 148px;
  height: 48px;
  padding: 0 24px;
}

/* 时间线 */
.timeline-panel {
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 20px 58px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.timeline-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.timeline-panel__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: #ea580c;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.timeline-panel__header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 950;
  letter-spacing: -0.04em;
}

.timeline-panel__header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.timeline-panel__status {
  flex-shrink: 0;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  padding: 9px 14px;
  color: #334155;
  background: rgba(248, 250, 252, 0.86);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  font-size: 12px;
  font-weight: 900;
}

.progress-card {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(255, 247, 237, 0.72), rgba(239, 246, 255, 0.64)),
    rgba(255, 255, 255, 0.72);
}

.progress-card__info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.progress-card__info strong {
  color: #0f172a;
}

.progress-card__bar {
  overflow: hidden;
  height: 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.88);
}

.progress-card__bar span {
  display: block;
  min-width: 0;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #f97316, #2563eb);
  box-shadow: 0 10px 24px rgba(249, 115, 22, 0.24);
  transition: width 0.35s ease;
}

.timeline-panel__list {
  display: grid;
  gap: 18px;
}

/* 人工输入面板 */
.human-input-panel {
  margin-top: 18px;
  border: 1px solid rgba(168, 85, 247, 0.22);
  border-radius: 24px;
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(168, 85, 247, 0.12), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 245, 255, 0.9));
  box-shadow: 0 18px 44px rgba(88, 28, 135, 0.08);
}

.human-input-panel__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.human-input-panel__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  color: #9333ea;
  background: #faf5ff;
  box-shadow: 0 10px 24px rgba(147, 51, 234, 0.12);
}

.human-input-panel h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 900;
}

.human-input-panel p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.human-input-panel__field {
  margin: 16px 0;
}

.human-input-panel__label {
  margin-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

/* 空状态 */
.empty-state {
  padding: 56px 20px;
  border: 1px dashed rgba(100, 116, 139, 0.28);
  border-radius: 24px;
  text-align: center;
  background:
    radial-gradient(circle at center top, rgba(249, 115, 22, 0.08), transparent 34%),
    rgba(248, 250, 252, 0.62);
}

.empty-state__orb {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 78px;
  height: 78px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 28px;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 20px 46px rgba(15, 23, 42, 0.08);
}

.empty-state__icon {
  color: #f97316;
  font-size: 36px;
}

.empty-state__title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 900;
}

.empty-state__desc {
  max-width: 520px;
  margin: 8px auto 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.empty-state__steps {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.empty-state__steps span {
  padding: 6px 11px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 960px) {
  .workspace-hero {
    grid-template-columns: 1fr;
  }

  .workspace-hero__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .input-panel__topline {
    flex-direction: column;
  }

  .input-panel__db {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .page-shell {
    padding: 14px 14px 44px;
  }

  .workspace-nav {
    align-items: flex-start;
    border-radius: 24px;
  }

  .workspace-nav__left small {
    display: none;
  }

  .workspace-hero,
  .input-panel,
  .timeline-panel {
    border-radius: 24px;
    padding: 18px;
  }

  .workspace-hero__stats {
    grid-template-columns: 1fr;
  }

  .input-panel__actions,
  .timeline-panel__header {
    flex-direction: column;
    align-items: stretch;
  }

  .input-panel__btn {
    width: 100%;
  }

  .example-chip {
    width: 100%;
    justify-content: flex-start;
    border-radius: 18px;
  }
}
</style>
