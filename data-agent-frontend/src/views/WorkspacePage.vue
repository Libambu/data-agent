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

// 节点元信息：用于 Rail 与 MiniMap 显示
const NODE_META: Record<string, { label: string; phase: 'recall' | 'plan' | 'exec' | 'output'; idx: number }> = {
  [DATA_AGENT_GRAPH_NODE.EVIDENCE_RECALL]:        { label: 'Evidence Recall', phase: 'recall', idx: 1 },
  [DATA_AGENT_GRAPH_NODE.SCHEMA_RECALL]:          { label: 'Schema Recall',   phase: 'recall', idx: 2 },
  [DATA_AGENT_GRAPH_NODE.TABLE_RELATION]:         { label: 'Table Relation',  phase: 'recall', idx: 3 },
  [DATA_AGENT_GRAPH_NODE.FEASIBILITY_ASSESSMENT]: { label: 'Feasibility',     phase: 'plan',   idx: 4 },
  [DATA_AGENT_GRAPH_NODE.PLANNER]:                { label: 'Planner',         phase: 'plan',   idx: 5 },
  [DATA_AGENT_GRAPH_NODE.HUMAN_FEEDBACK]:         { label: 'Human Feedback',  phase: 'plan',   idx: 6 },
  [DATA_AGENT_GRAPH_NODE.PLAN_EXECUTION]:         { label: 'Plan Dispatch',   phase: 'exec',   idx: 7 },
  [DATA_AGENT_GRAPH_NODE.SQL_GENERATION]:         { label: 'SQL Generation',  phase: 'exec',   idx: 8 },
  [DATA_AGENT_GRAPH_NODE.SQL_EXECUTION]:          { label: 'SQL Execution',   phase: 'exec',   idx: 9 },
  [DATA_AGENT_GRAPH_NODE.PYTHON_GENERATION]:      { label: 'Python Gen',      phase: 'exec',   idx: 10 },
  [DATA_AGENT_GRAPH_NODE.PYTHON_EXECUTION]:       { label: 'Python Exec',     phase: 'exec',   idx: 11 },
  [DATA_AGENT_GRAPH_NODE.PYTHON_ANALYSIS]:        { label: 'Python Analysis', phase: 'exec',   idx: 12 },
  [DATA_AGENT_GRAPH_NODE.REPORT_GENERATION]:      { label: 'Report',          phase: 'output', idx: 13 },
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
  if (!isClientReady.value) return 'Connecting'
  if (isRunning.value) return 'Running'
  if (orderedSteps.value.length) return 'Completed'
  return 'Ready'
})
const timelineStatusText = computed(() => {
  if (!isClientReady.value) return '正在连接 Agent'
  if (isRunning.value) return '正在执行'
  if (orderedSteps.value.length) return '执行完成'
  return '等待开始'
})

// 步骤状态映射，给 Rail / MiniMap 使用
const stepStatusMap = computed(() => {
  const map: Record<string, GraphStep['status'] | 'idle'> = {}
  for (const node of DATA_AGENT_NODE_ORDER) {
    const step = steps.find((s) => s.name === node)
    map[node] = step ? step.status : 'idle'
  }
  return map
})

// 当前活跃节点
const activeNode = computed(() => {
  const pending = orderedSteps.value.find((s) => s.status === 'pending')
  if (pending) return pending.name
  if (orderedSteps.value.length) return orderedSteps.value[orderedSteps.value.length - 1]?.name
  return null
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

        // 后端在图执行结束时会发送一个 name === "__END__" 的 artifact，
        // 它代表整个 Graph 已经跑完，但不是真实节点；
        // 同时后端在正常完成时不会再发 status-update completed，
        // 因此这里需要把它当作完成信号：把所有 pending 节点标记为 success，
        // 并且不要把它 push 到 steps 里。
        if (artifactName === '__END__') {
          awaitingConfirmation.value = false
          markAllPendingStepAsSuccess()
        } else if (artifactName) {
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

// 滚动到节点
const scrollToNode = (nodeName: string) => {
  const el = document.getElementById(`node-${nodeName}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(async () => {
  const agentCard = await request.get<AgentCard, AgentCard>('/.well-known/agent-card.json')
  client.value = await factory.createFromAgentCard(agentCard)
})
</script>

<template>
  <main class="ws">
    <!-- ============ 顶部 AppBar ============ -->
    <header class="appbar">
      <div class="appbar__l" @click="goHome">
        <span class="appbar__logo">
          <el-icon><DataAnalysis /></el-icon>
        </span>
        <div class="appbar__brand">
          <span class="appbar__name">Data Agent</span>
          <span class="appbar__sub">智能数据分析工作台</span>
        </div>
      </div>

      <div class="appbar__c">
        <span class="appbar__crumb appbar__crumb--muted">Workspace</span>
        <span class="appbar__crumb-sep">/</span>
        <span class="appbar__crumb">{{ selectedDatabase }}</span>
        <span class="appbar__crumb-sep">/</span>
        <span class="appbar__crumb appbar__crumb--accent">
          {{ orderedSteps.length ? `Run-${(currentTaskId || '').slice(0, 6) || 'new'}` : 'New Session' }}
        </span>
      </div>

      <div class="appbar__r">
        <div class="appbar__status">
          <span :class="['dot', { 'dot--ok': isClientReady && !isRunning, 'dot--run': isRunning }]"></span>
          <span class="appbar__status-text">{{ workspaceStatusText }}</span>
        </div>
        <button class="appbar__home" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          <span>Home</span>
        </button>
      </div>
    </header>

    <!-- ============ 三栏主体 ============ -->
    <div class="grid">
      <!-- ====== 左 Rail：Pipeline ====== -->
      <aside class="rail">
        <div class="rail__head">
          <span class="rail__label">PIPELINE</span>
          <span class="rail__count">{{ completedStepCount }}/{{ DATA_AGENT_NODE_ORDER.length }}</span>
        </div>

        <div class="rail__list">
          <button
            v-for="node in DATA_AGENT_NODE_ORDER"
            :key="node"
            class="rail-item"
            :class="[
              `rail-item--${NODE_META[node]?.phase}`,
              `rail-item--${stepStatusMap[node]}`,
              { 'rail-item--active': activeNode === node },
            ]"
            type="button"
            :disabled="stepStatusMap[node] === 'idle'"
            @click="scrollToNode(node)"
          >
            <span class="rail-item__idx">
              {{ String(NODE_META[node]?.idx ?? 0).padStart(2, '0') }}
            </span>
            <span class="rail-item__body">
              <span class="rail-item__name">{{ NODE_META[node]?.label }}</span>
              <span class="rail-item__phase">PHASE · {{ NODE_META[node]?.phase }}</span>
            </span>
            <span class="rail-item__dot" aria-hidden="true"></span>
          </button>
        </div>

        <div class="rail__foot">
          <div class="rail-progress">
            <div class="rail-progress__bar">
              <span :style="{ width: `${progressPercentage}%` }"></span>
            </div>
            <div class="rail-progress__num">{{ progressPercentage }}%</div>
          </div>
        </div>
      </aside>

      <!-- ====== 中央 Stage ====== -->
      <section class="stage">
        <!-- 输入面板（Composer） -->
        <div class="composer">
          <div class="composer__top">
            <div class="composer__title">
              <span class="composer__eyebrow">QUERY</span>
              <h2>把业务问题转化为可追踪的数据分析</h2>
            </div>
            <div class="composer__db">
              <span class="composer__db-label">Database</span>
              <el-select v-model="selectedDatabase" size="default">
                <el-option
                  v-for="database in DATABASE_OPTIONS"
                  :key="database"
                  :label="database"
                  :value="database"
                />
              </el-select>
            </div>
          </div>

          <div class="composer__chips">
            <span class="composer__chips-label">PRESETS</span>
            <button
              v-for="example in DEFAULT_EXAMPLES"
              :key="example"
              class="chip"
              type="button"
              @click="userInput = example"
            >
              <el-icon><ChatLineRound /></el-icon>
              <span>{{ example }}</span>
            </button>
          </div>

          <div class="composer__box">
            <el-input
              v-model="userInput"
              :rows="3"
              type="textarea"
              resize="none"
              placeholder="例如：统计 Alameda County 中 K-12 学生免费餐资格比例最高的学校..."
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="composer__actions">
              <span class="composer__hint">
                <kbd>Enter</kbd> 提交
                <span class="composer__sep"></span>
                {{ isClientReady ? 'Agent connected' : 'Connecting...' }}
              </span>
              <el-button
                type="primary"
                size="large"
                :disabled="!canSubmit"
                :loading="isRunning"
                @click="handleSend"
              >
                <el-icon v-if="!isRunning"><Promotion /></el-icon>
                <span>{{ isRunning ? 'Running' : 'Run Pipeline' }}</span>
              </el-button>
            </div>
          </div>
        </div>

        <!-- 执行流 -->
        <div class="trace">
          <div class="trace__head">
            <div class="trace__title">
              <span class="trace__eyebrow">EXECUTION TRACE</span>
              <h3>节点执行轨迹</h3>
            </div>
            <span class="trace__status">{{ timelineStatusText }}</span>
          </div>

          <!-- 节点流 -->
          <div v-if="orderedSteps.length" class="trace__body">
            <div
              v-for="(step, i) in orderedSteps"
              :key="step.id"
              :id="`node-${step.name}`"
              class="trace-row"
              :class="`trace-row--${NODE_META[step.name]?.phase}`"
            >
              <div class="trace-row__gut">
                <span class="trace-row__idx">
                  {{ String(NODE_META[step.name]?.idx ?? i + 1).padStart(2, '0') }}
                </span>
                <span
                  v-if="i < orderedSteps.length - 1"
                  class="trace-row__line"
                  aria-hidden="true"
                ></span>
              </div>
              <div class="trace-row__card">
                <component
                  v-if="NODE_COMPONENTS[step.name]"
                  :is="NODE_COMPONENTS[step.name]"
                  :name="step.name"
                  :content="step.content"
                  :data="step.data"
                  :status="step.status"
                />
              </div>
            </div>
          </div>

          <!-- 人工输入面板 -->
          <div v-if="awaitingConfirmation" class="human-panel">
            <div class="human-panel__head">
              <div class="human-panel__icon">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div>
                <h3>需要人工输入</h3>
                <p>请填写确认结果和反馈，提交后会继续执行后续节点。</p>
              </div>
            </div>
            <div class="human-panel__field">
              <span class="human-panel__label">确认结果</span>
              <el-radio-group v-model="confirmationApproved">
                <el-radio :value="true">确认继续</el-radio>
                <el-radio :value="false">取消执行</el-radio>
              </el-radio-group>
            </div>
            <div class="human-panel__field">
              <span class="human-panel__label">反馈内容</span>
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

          <!-- 空状态 -->
          <div v-else-if="!orderedSteps.length" class="empty">
            <div class="empty__orb">
              <el-icon><Cpu /></el-icon>
              <span class="empty__ring"></span>
            </div>
            <div class="empty__title">Awaiting Pipeline</div>
            <div class="empty__desc">
              选择示例问题或输入自定义需求，点击 <strong>Run Pipeline</strong> 即可观察 13 个节点的实时执行轨迹。
            </div>
            <div class="empty__phases">
              <span class="empty__phase empty__phase--cyan">Retrieval</span>
              <span class="empty__phase empty__phase--violet">Planning</span>
              <span class="empty__phase empty__phase--amber">Execution</span>
              <span class="empty__phase empty__phase--emerald">Output</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ====== 右 Side：DAG MiniMap + Metrics ====== -->
      <aside class="side">
        <div class="metric-grid">
          <div class="metric metric--primary">
            <span class="metric__label">PROGRESS</span>
            <strong class="metric__value">{{ progressPercentage }}<span>%</span></strong>
            <div class="metric__bar">
              <span :style="{ width: `${progressPercentage}%` }"></span>
            </div>
          </div>
          <div class="metric">
            <span class="metric__label">TRIGGERED</span>
            <strong class="metric__value">{{ orderedSteps.length }}</strong>
          </div>
          <div class="metric">
            <span class="metric__label">DONE</span>
            <strong class="metric__value">{{ completedStepCount }}</strong>
          </div>
        </div>

        <!-- 阶段统计条 -->
        <div class="phase-bars">
          <div class="phase-bars__head">
            <span class="phase-bars__label">PHASE BREAKDOWN</span>
          </div>
          <template v-for="phase in (['recall', 'plan', 'exec', 'output'] as const)" :key="phase">
            <div class="phase-bar" :class="`phase-bar--${phase}`">
              <span class="phase-bar__name">
                {{ phase === 'recall' ? 'Retrieval' : phase === 'plan' ? 'Planning' : phase === 'exec' ? 'Execution' : 'Output' }}
              </span>
              <span class="phase-bar__nums">
                {{ orderedSteps.filter(s => NODE_META[s.name]?.phase === phase && s.status === 'success').length }}
                /
                {{ DATA_AGENT_NODE_ORDER.filter(n => NODE_META[n]?.phase === phase).length }}
              </span>
              <div class="phase-bar__track">
                <span
                  :style="{
                    width: `${
                      (orderedSteps.filter(s => NODE_META[s.name]?.phase === phase && s.status === 'success').length /
                        Math.max(1, DATA_AGENT_NODE_ORDER.filter(n => NODE_META[n]?.phase === phase).length)) * 100
                    }%`,
                  }"
                ></span>
              </div>
            </div>
          </template>
        </div>

        <!-- DAG 缩略图 -->
        <div class="minimap">
          <div class="minimap__head">
            <span class="minimap__label">DAG MINI-MAP</span>
            <span class="minimap__legend">
              <span class="minimap__lg minimap__lg--idle"></span>idle
              <span class="minimap__lg minimap__lg--run"></span>running
              <span class="minimap__lg minimap__lg--ok"></span>done
            </span>
          </div>
          <svg viewBox="0 0 200 320" class="minimap__svg">
            <!-- Phase I -->
            <rect x="60" y="6" width="80" height="76" rx="6" fill="rgba(26,115,232,0.06)" stroke="rgba(26,115,232,0.4)" stroke-width="0.6" />
            <text x="100" y="18" class="mm-phase mm-phase--recall">RETRIEVAL</text>

            <rect
              v-for="(node, i) in [DATA_AGENT_GRAPH_NODE.EVIDENCE_RECALL, DATA_AGENT_GRAPH_NODE.SCHEMA_RECALL, DATA_AGENT_GRAPH_NODE.TABLE_RELATION]"
              :key="`r-${i}`"
              :x="70" :y="24 + i * 18" width="60" height="14" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[node]}`, { 'mm-node--active': activeNode === node }]"
            />
            <text v-for="(node, i) in ['Evidence', 'Schema', 'Relation']" :key="`rt-${i}`" :x="100" :y="34 + i * 18" class="mm-text">
              {{ node }}
            </text>

            <!-- Phase II -->
            <rect x="60" y="92" width="80" height="76" rx="6" fill="rgba(155,114,203,0.06)" stroke="rgba(155,114,203,0.4)" stroke-width="0.6" />
            <text x="100" y="104" class="mm-phase mm-phase--plan">PLANNING</text>

            <rect
              v-for="(node, i) in [DATA_AGENT_GRAPH_NODE.FEASIBILITY_ASSESSMENT, DATA_AGENT_GRAPH_NODE.PLANNER, DATA_AGENT_GRAPH_NODE.HUMAN_FEEDBACK]"
              :key="`p-${i}`"
              :x="70" :y="110 + i * 18" width="60" height="14" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[node]}`, { 'mm-node--active': activeNode === node }]"
            />
            <text v-for="(node, i) in ['Feasibility', 'Planner', 'Human']" :key="`pt-${i}`" :x="100" :y="120 + i * 18" class="mm-text">
              {{ node }}
            </text>

            <!-- Phase III -->
            <rect x="6" y="178" width="188" height="100" rx="6" fill="rgba(249,171,0,0.06)" stroke="rgba(249,171,0,0.4)" stroke-width="0.6" />
            <text x="100" y="190" class="mm-phase mm-phase--exec">EXECUTION</text>

            <!-- Dispatch -->
            <rect :x="70" :y="196" width="60" height="14" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[DATA_AGENT_GRAPH_NODE.PLAN_EXECUTION]}`, { 'mm-node--active': activeNode === DATA_AGENT_GRAPH_NODE.PLAN_EXECUTION }]"
            />
            <text x="100" y="206" class="mm-text">Dispatch</text>

            <!-- SQL Branch -->
            <rect :x="14" :y="220" width="50" height="12" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[DATA_AGENT_GRAPH_NODE.SQL_GENERATION]}`, { 'mm-node--active': activeNode === DATA_AGENT_GRAPH_NODE.SQL_GENERATION }]"
            />
            <text x="39" y="229" class="mm-text mm-text--sm">SQL Gen</text>
            <rect :x="14" :y="236" width="50" height="12" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[DATA_AGENT_GRAPH_NODE.SQL_EXECUTION]}`, { 'mm-node--active': activeNode === DATA_AGENT_GRAPH_NODE.SQL_EXECUTION }]"
            />
            <text x="39" y="245" class="mm-text mm-text--sm">SQL Exec</text>

            <!-- Python Branch -->
            <rect :x="76" :y="220" width="48" height="12" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[DATA_AGENT_GRAPH_NODE.PYTHON_GENERATION]}`, { 'mm-node--active': activeNode === DATA_AGENT_GRAPH_NODE.PYTHON_GENERATION }]"
            />
            <text x="100" y="229" class="mm-text mm-text--sm">Py Gen</text>
            <rect :x="76" :y="236" width="48" height="12" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[DATA_AGENT_GRAPH_NODE.PYTHON_EXECUTION]}`, { 'mm-node--active': activeNode === DATA_AGENT_GRAPH_NODE.PYTHON_EXECUTION }]"
            />
            <text x="100" y="245" class="mm-text mm-text--sm">Py Exec</text>
            <rect :x="76" :y="252" width="48" height="12" rx="3"
              :class="['mm-node', `mm-node--${stepStatusMap[DATA_AGENT_GRAPH_NODE.PYTHON_ANALYSIS]}`, { 'mm-node--active': activeNode === DATA_AGENT_GRAPH_NODE.PYTHON_ANALYSIS }]"
            />
            <text x="100" y="261" class="mm-text mm-text--sm">Py Analyze</text>

            <!-- Report Branch -->
            <rect :x="136" :y="220" width="50" height="12" rx="3"
              :class="['mm-node mm-node--out', `mm-node--${stepStatusMap[DATA_AGENT_GRAPH_NODE.REPORT_GENERATION]}`, { 'mm-node--active': activeNode === DATA_AGENT_GRAPH_NODE.REPORT_GENERATION }]"
            />
            <text x="161" y="229" class="mm-text mm-text--sm">Report</text>

            <!-- 简化连线 -->
            <line x1="100" y1="38" x2="100" y2="92" stroke="rgba(60,64,67,0.28)" stroke-width="0.6" stroke-dasharray="2 2" />
            <line x1="100" y1="124" x2="100" y2="178" stroke="rgba(60,64,67,0.28)" stroke-width="0.6" stroke-dasharray="2 2" />
            <line x1="100" y1="210" x2="100" y2="220" stroke="rgba(60,64,67,0.28)" stroke-width="0.6" />
            <line x1="100" y1="216" x2="40" y2="220" stroke="rgba(60,64,67,0.28)" stroke-width="0.6" />
            <line x1="100" y1="216" x2="160" y2="220" stroke="rgba(60,64,67,0.28)" stroke-width="0.6" />
          </svg>
        </div>

        <div class="ws-footer">
          <span>Built with Vue · Element Plus</span>
        </div>
      </aside>
    </div>
  </main>
</template>

<style scoped>
/* ============= 整体布局 ============= */
.ws {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 14px clamp(14px, 2vw, 26px) 18px;
}

/* ============= AppBar ============= */
.appbar {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  box-shadow: var(--shadow-sm);
  margin-bottom: 14px;
}

.appbar__l {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: opacity 0.18s;
}

.appbar__l:hover {
  opacity: 0.85;
}

.appbar__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  color: #ffffff;
  background: linear-gradient(135deg, #4285f4 0%, #9b72cb 50%, #d96570 100%);
  box-shadow: 0 2px 8px rgba(66, 133, 244, 0.32);
}

.appbar__brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.appbar__name {
  color: var(--text-1);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.012em;
}

.appbar__sub {
  color: var(--text-3);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.appbar__c {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: var(--bg-1);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 12px;
}

.appbar__crumb {
  color: var(--text-2);
  font-weight: 600;
}

.appbar__crumb--muted { color: var(--text-3); }
.appbar__crumb--accent { color: var(--color-accent); }

.appbar__crumb-sep {
  color: var(--text-3);
  opacity: 0.6;
}

.appbar__r {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
}

.appbar__status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: var(--bg-1);
  color: var(--text-1);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9aa0a6;
  box-shadow: 0 0 0 3px rgba(154, 160, 166, 0.18);
}

.dot--ok {
  background: #1e8e3e;
  box-shadow: 0 0 0 3px rgba(30, 142, 62, 0.20);
}

.dot--run {
  background: #f9ab00;
  box-shadow: 0 0 0 3px rgba(249, 171, 0, 0.22);
  animation: dot-pulse 1.4s infinite;
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.55; transform: scale(0.78); }
}

.appbar__home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--line-strong);
  border-radius: 999px;
  background: #ffffff;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s;
}

.appbar__home:hover {
  background: rgba(26, 115, 232, 0.06);
  border-color: var(--color-primary);
}

/* ============= 三栏 Grid ============= */
.grid {
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr) 320px;
  gap: 14px;
  flex: 1;
  min-height: 0;
}

/* ============= 左 Rail ============= */
.rail {
  position: sticky;
  top: 14px;
  align-self: start;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 32px);
  padding: 16px 12px 14px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-xl);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.rail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 6px 10px;
  border-bottom: 1px dashed var(--line-2);
  margin-bottom: 10px;
}

.rail__label {
  color: var(--text-3);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.rail__count {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-2);
  padding: 3px 8px;
  border-radius: 6px;
  background: var(--bg-2);
}

.rail__list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  padding-right: 2px;
}

.rail-item {
  --rail-c: var(--text-3);
  display: grid;
  grid-template-columns: 28px 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: var(--text-2);
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.rail-item:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.rail-item:not(:disabled):hover {
  background: var(--bg-1);
  border-color: var(--line-2);
}

.rail-item--active {
  background: var(--rail-soft);
  border-color: var(--rail-c) !important;
  box-shadow: none;
}

.rail-item--recall { --rail-c: #1a73e8; --rail-soft: rgba(26, 115, 232, 0.08); }
.rail-item--plan   { --rail-c: #9b72cb; --rail-soft: rgba(155, 114, 203, 0.08); }
.rail-item--exec   { --rail-c: #f9ab00; --rail-soft: rgba(249, 171, 0, 0.10); }
.rail-item--output { --rail-c: #d96570; --rail-soft: rgba(217, 101, 112, 0.08); }

.rail-item__idx {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-3);
  text-align: center;
}

.rail-item--active .rail-item__idx,
.rail-item--success .rail-item__idx {
  color: var(--rail-c);
}

.rail-item__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.rail-item__name {
  color: var(--text-1);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: -0.005em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rail-item__phase {
  color: var(--text-3);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.10em;
  text-transform: uppercase;
}

.rail-item__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--bg-3);
  flex-shrink: 0;
}

.rail-item--pending .rail-item__dot {
  background: var(--rail-c);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--rail-c) 18%, transparent);
  animation: dot-pulse 1.4s infinite;
}

.rail-item--success .rail-item__dot {
  background: #1e8e3e;
  box-shadow: 0 0 0 3px rgba(30, 142, 62, 0.18);
}

.rail__foot {
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px dashed var(--line-2);
}

.rail-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rail-progress__bar {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: var(--bg-2);
  overflow: hidden;
}

.rail-progress__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4285f4, #9b72cb, #d96570);
  transition: width 0.4s ease;
}

.rail-progress__num {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-1);
  min-width: 36px;
  text-align: right;
}

/* ============= 中央 Stage ============= */
.stage {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

/* ----- Composer ----- */
.composer {
  padding: 22px 24px 20px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-xl);
  background:
    radial-gradient(700px 220px at 100% 0%, rgba(155, 114, 203, 0.06), transparent 60%),
    #ffffff;
  box-shadow: var(--shadow-sm);
}

.composer__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 14px;
}

.composer__eyebrow {
  display: inline-block;
  margin-bottom: 6px;
  padding: 3px 9px;
  border: 1px solid rgba(26, 115, 232, 0.24);
  border-radius: 6px;
  background: rgba(26, 115, 232, 0.08);
  color: var(--color-primary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.composer__title h2 {
  margin: 0;
  color: var(--text-1);
  font-size: clamp(22px, 2.4vw, 28px);
  font-weight: 700;
  letter-spacing: -0.022em;
  line-height: 1.2;
}

.composer__db {
  flex-shrink: 0;
  width: 220px;
}

.composer__db-label {
  display: block;
  margin-bottom: 6px;
  color: var(--text-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.composer__chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.composer__chips-label {
  color: var(--text-3);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
  margin-right: 4px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: min(100%, 540px);
  padding: 7px 13px;
  border: 1px solid var(--line-strong);
  border-radius: 999px;
  background: #ffffff;
  color: var(--text-2);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s, color 0.18s;
}

.chip:hover {
  background: rgba(26, 115, 232, 0.06);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.chip span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 460px;
}

.composer__box {
  display: grid;
  gap: 12px;
}

.composer__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.composer__hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-3);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.composer__hint kbd {
  padding: 2px 7px;
  border: 1px solid var(--line-strong);
  border-radius: 5px;
  background: var(--bg-1);
  color: var(--text-2);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
}

.composer__sep {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--text-3);
  opacity: 0.5;
}

.composer :deep(.el-button) {
  height: 44px;
  padding: 0 22px;
  border-radius: 22px;
  font-weight: 600;
}

/* ----- Trace ----- */
.trace {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 22px 22px 20px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-xl);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
  min-height: 480px;
}

.trace__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--line-2);
}

.trace__eyebrow {
  display: inline-block;
  margin-bottom: 4px;
  color: var(--color-accent);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.trace__title h3 {
  margin: 0;
  color: var(--text-1);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.018em;
}

.trace__status {
  padding: 7px 13px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: var(--bg-1);
  color: var(--text-1);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.trace__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.trace-row {
  --row-c: var(--color-primary);
  position: relative;
  display: grid;
  grid-template-columns: 36px 1fr;
  gap: 12px;
  padding-bottom: 18px;
}

.trace-row--recall { --row-c: #1a73e8; }
.trace-row--plan   { --row-c: #9b72cb; }
.trace-row--exec   { --row-c: #f9ab00; }
.trace-row--output { --row-c: #d96570; }

.trace-row__gut {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.trace-row__idx {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: 1.5px solid var(--row-c);
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 1px 3px color-mix(in srgb, var(--row-c) 18%, transparent);
  color: var(--row-c);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: -0.02em;
  z-index: 1;
}

.trace-row__line {
  position: absolute;
  top: 36px;
  left: 50%;
  transform: translateX(-50%);
  width: 1.5px;
  height: calc(100% - 36px);
  background: linear-gradient(180deg, var(--row-c), transparent);
  opacity: 0.5;
}

.trace-row__card {
  min-width: 0;
}

/* ----- Human Panel ----- */
.human-panel {
  margin-top: 18px;
  padding: 22px;
  border: 1px solid rgba(155, 114, 203, 0.32);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(420px 200px at 0% 0%, rgba(155, 114, 203, 0.10), transparent 60%),
    #ffffff;
  box-shadow: var(--shadow-sm);
}

.human-panel__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.human-panel__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(155, 114, 203, 0.4);
  background: rgba(155, 114, 203, 0.10);
  color: #9b72cb;
}

.human-panel h3 {
  margin: 0;
  color: var(--text-1);
  font-size: 17px;
  font-weight: 700;
}

.human-panel p {
  margin: 4px 0 0;
  color: var(--text-2);
  font-size: 13px;
  line-height: 1.7;
}

.human-panel__field {
  margin-bottom: 14px;
}

.human-panel__label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-2);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

/* ----- Empty ----- */
.empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 50% 100%, rgba(26, 115, 232, 0.05), transparent 60%),
    var(--bg-1);
}

.empty__orb {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  margin-bottom: 18px;
  border: 1px solid rgba(26, 115, 232, 0.32);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(66, 133, 244, 0.10), rgba(155, 114, 203, 0.08));
  color: var(--color-primary);
  font-size: 30px;
  box-shadow: 0 4px 14px rgba(26, 115, 232, 0.18);
}

.empty__ring {
  position: absolute;
  inset: -8px;
  border: 1px dashed rgba(26, 115, 232, 0.4);
  border-radius: 28px;
  animation: ring-spin 14s linear infinite;
}

@keyframes ring-spin {
  to { transform: rotate(360deg); }
}

.empty__title {
  color: var(--text-1);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.empty__desc {
  max-width: 460px;
  margin: 8px 0 18px;
  color: var(--text-2);
  font-size: 13px;
  line-height: 1.7;
}

.empty__desc strong {
  color: var(--text-1);
  font-weight: 700;
}

.empty__phases {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.empty__phase {
  --c: var(--text-3);
  padding: 5px 12px;
  border: 1px solid var(--c);
  border-radius: 999px;
  color: var(--c);
  background: color-mix(in srgb, var(--c) 8%, transparent);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.empty__phase--cyan    { --c: #1a73e8; }
.empty__phase--violet  { --c: #9b72cb; }
.empty__phase--amber   { --c: #f9ab00; }
.empty__phase--emerald { --c: #d96570; }

/* ============= 右 Side ============= */
.side {
  position: sticky;
  top: 14px;
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: calc(100vh - 32px);
  overflow-y: auto;
  padding-right: 2px;
}

.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-xl);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.metric {
  padding: 12px 14px;
  border: 1px solid var(--line-2);
  border-radius: 12px;
  background: var(--bg-1);
}

.metric--primary {
  grid-column: 1 / -1;
  background: linear-gradient(135deg, rgba(66, 133, 244, 0.10), rgba(155, 114, 203, 0.08));
  border-color: rgba(26, 115, 232, 0.32);
}

.metric__label {
  display: block;
  color: var(--text-3);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.metric__value {
  display: block;
  margin-top: 6px;
  color: var(--text-1);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1;
}

.metric__value span {
  color: var(--text-2);
  font-size: 16px;
  font-weight: 600;
}

.metric__bar {
  margin-top: 10px;
  height: 5px;
  border-radius: 999px;
  background: var(--bg-2);
  overflow: hidden;
}

.metric__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4285f4, #9b72cb);
  transition: width 0.4s ease;
}

/* ----- Phase Bars ----- */
.phase-bars {
  padding: 16px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-xl);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.phase-bars__head {
  margin-bottom: 12px;
}

.phase-bars__label {
  color: var(--text-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.phase-bar {
  --c: var(--text-3);
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 6px 8px;
  margin-top: 10px;
}

.phase-bar--recall  { --c: #1a73e8; }
.phase-bar--plan    { --c: #9b72cb; }
.phase-bar--exec    { --c: #f9ab00; }
.phase-bar--output  { --c: #d96570; }

.phase-bar__name {
  color: var(--text-1);
  font-size: 12px;
  font-weight: 600;
}

.phase-bar__nums {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  color: var(--c);
  font-size: 11px;
  font-weight: 700;
}

.phase-bar__track {
  grid-column: 1 / -1;
  height: 4px;
  border-radius: 999px;
  background: var(--bg-2);
  overflow: hidden;
}

.phase-bar__track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--c);
  transition: width 0.4s ease;
}

/* ----- MiniMap ----- */
.minimap {
  padding: 14px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-xl);
  background: var(--bg-1);
  box-shadow: var(--shadow-sm);
}

.minimap__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.minimap__label {
  color: var(--text-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.minimap__legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 9px;
  font-weight: 600;
  color: var(--text-3);
  letter-spacing: 0.06em;
}

.minimap__lg {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin-left: 6px;
}

.minimap__lg--idle { background: var(--bg-3); }
.minimap__lg--run  { background: #f9ab00; }
.minimap__lg--ok   { background: #1e8e3e; }

.minimap__svg {
  width: 100%;
  height: auto;
}

.mm-phase {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 6px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-anchor: middle;
}

.mm-phase--recall { fill: #1a73e8; }
.mm-phase--plan   { fill: #9b72cb; }
.mm-phase--exec   { fill: #f9ab00; }

.mm-node {
  fill: #ffffff;
  stroke: var(--line-strong);
  stroke-width: 0.5;
  transition: all 0.3s;
}

.mm-node--pending {
  fill: rgba(249, 171, 0, 0.16);
  stroke: #f9ab00;
  stroke-width: 0.7;
}

.mm-node--success {
  fill: rgba(30, 142, 62, 0.14);
  stroke: #1e8e3e;
  stroke-width: 0.7;
}

.mm-node--active {
  filter: drop-shadow(0 0 2px currentColor);
}

.mm-text {
  fill: var(--text-2);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 5.5px;
  font-weight: 600;
  text-anchor: middle;
  pointer-events: none;
}

.mm-text--sm { font-size: 5px; }

.ws-footer {
  margin-top: auto;
  padding-top: 4px;
  text-align: center;
  color: var(--text-3);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  letter-spacing: 0.04em;
}

/* ============= 响应式 ============= */
@media (max-width: 1280px) {
  .grid {
    grid-template-columns: 240px minmax(0, 1fr) 280px;
  }
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: auto auto auto;
  }

  .rail,
  .side {
    position: static;
    height: auto;
    max-height: 540px;
  }

  .rail {
    flex-direction: column;
  }

  .side {
    overflow-y: visible;
  }

  .appbar {
    grid-template-columns: 1fr auto;
  }

  .appbar__c {
    display: none;
  }
}

@media (max-width: 720px) {
  .ws {
    padding: 10px 10px 16px;
  }

  .composer__top {
    flex-direction: column;
  }

  .composer__db {
    width: 100%;
  }

  .composer__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .composer :deep(.el-button) {
    width: 100%;
    justify-content: center;
  }

  .chip {
    width: 100%;
  }

  .chip span {
    max-width: none;
  }

  .trace-row {
    grid-template-columns: 28px 1fr;
    gap: 10px;
  }

  .trace-row__idx {
    width: 26px;
    height: 26px;
    font-size: 10px;
  }
}
</style>
