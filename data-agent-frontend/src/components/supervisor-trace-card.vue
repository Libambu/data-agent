<script setup lang="ts">
import { computed } from 'vue'
import { DATA_AGENT_STATE_KEY_PLANNING } from '@/constants/data-agent-graph-spec'

const props = defineProps<{
  name?: string
  content: string
  data?: Record<string, unknown>
  status: 'pending' | 'success'
}>()

const typedData = computed<Record<string, unknown>>(() => props.data ?? {})
const displayName = computed(() => props.name || 'SUPERVISOR_NODE')

interface ToolParameters {
  instruction?: string
  summary_and_recommendations?: string
}

interface ExecutionStep {
  step?: number
  tool_to_use?: string
  tool_parameters?: ToolParameters
}

interface PlanShape {
  thought_process?: string
  execution_plan?: ExecutionStep[]
}

const currentStep = computed(() => {
  const value = typedData.value[DATA_AGENT_STATE_KEY_PLANNING.CURRENT_STEP]
  return typeof value === 'number' ? value : null
})

const nextNode = computed(() => {
  const value = typedData.value[DATA_AGENT_STATE_KEY_PLANNING.NEXT_NODE]
  return typeof value === 'string' ? value : ''
})

const plan = computed<PlanShape | null>(() => {
  const raw = typedData.value[DATA_AGENT_STATE_KEY_PLANNING.PLAN]
  if (!raw) return null
  if (typeof raw === 'object') return raw as PlanShape
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) as PlanShape
    } catch {
      return null
    }
  }
  return null
})

const traceSteps = computed<ExecutionStep[]>(() => {
  const steps = plan.value?.execution_plan
  return Array.isArray(steps) ? steps : []
})

interface AgentSpec {
  key: string
  label: string
  role: string
}

const SUB_AGENTS: AgentSpec[] = [
  { key: 'SQL_GENERATE_NODE', label: 'SQL Sub-Agent', role: '数据查询' },
  { key: 'PYTHON_GENERATE_NODE', label: 'Python Sub-Agent', role: '数据分析' },
  { key: 'REPORT_GENERATOR_NODE', label: 'Report Sub-Agent', role: '报告撰写' },
]

const TOOL_LABEL: Record<string, string> = {
  SQL_GENERATE_NODE: 'SQL Sub-Agent',
  PYTHON_GENERATE_NODE: 'Python Sub-Agent',
  REPORT_GENERATOR_NODE: 'Report Sub-Agent',
}

const dispatchCount = computed<Record<string, number>>(() => {
  const counter: Record<string, number> = {}
  traceSteps.value.forEach((s) => {
    if (!s.tool_to_use) return
    counter[s.tool_to_use] = (counter[s.tool_to_use] || 0) + 1
  })
  return counter
})

const activeAgentKey = computed<string>(() => {
  if (props.status === 'success') return ''
  if (nextNode.value) return nextNode.value
  const last = traceSteps.value[traceSteps.value.length - 1]
  return last?.tool_to_use || ''
})

const supervisorPhase = computed(() => {
  if (props.status === 'success') return 'done'
  if (traceSteps.value.length === 0) return 'planning'
  return 'dispatching'
})

const totalRounds = computed(() => traceSteps.value.length)

const fallbackText = computed(() => props.content || '')

const agentStatusText = (key: string) => {
  if (activeAgentKey.value === key) return 'running'
  if ((dispatchCount.value[key] || 0) > 0) return 'used'
  return 'idle'
}
</script>

<template>
  <section class="sv-card">
    <!-- 顶部标题区：朴素 chip + 名称 -->
    <header class="sv-card__header">
      <div class="sv-card__brand">
        <span class="sv-card__chip">MULTI-AGENT · SUPERVISOR</span>
        <h3 class="sv-card__title">{{ displayName }}</h3>
        <p class="sv-card__subtitle">
          中枢节点动态决策派单，按需调度 Sub-Agent。
        </p>
      </div>
      <div class="sv-card__meta">
        <div class="sv-card__metaitem">
          <span class="sv-card__metalabel">phase</span>
          <span class="sv-card__metaval">{{ supervisorPhase }}</span>
        </div>
        <div class="sv-card__metaitem">
          <span class="sv-card__metalabel">rounds</span>
          <span class="sv-card__metaval">{{ totalRounds }}</span>
        </div>
        <div class="sv-card__metaitem">
          <span class="sv-card__metalabel">step</span>
          <span class="sv-card__metaval">#{{ currentStep ?? 0 }}</span>
        </div>
      </div>
    </header>

    <!-- 三列 Sub-Agent 状态条（纯文本 + 调度次数 + 状态点） -->
    <div class="sv-agents">
      <div
        v-for="agent in SUB_AGENTS"
        :key="agent.key"
        class="sv-agent"
        :class="`sv-agent--${agentStatusText(agent.key)}`"
      >
        <div class="sv-agent__head">
          <span class="sv-agent__dot"></span>
          <span class="sv-agent__name">{{ agent.label }}</span>
          <span class="sv-agent__count">×{{ dispatchCount[agent.key] || 0 }}</span>
        </div>
        <div class="sv-agent__foot">
          <span class="sv-agent__role">{{ agent.role }}</span>
          <span class="sv-agent__state">{{ agentStatusText(agent.key) }}</span>
        </div>
      </div>
    </div>

    <!-- 派单时间线 -->
    <div v-if="traceSteps.length" class="sv-timeline">
      <div class="sv-timeline__title">DISPATCH TRACE</div>
      <div class="sv-timeline__list">
        <article
          v-for="(step, idx) in traceSteps"
          :key="`tl-${idx}`"
          class="tl-item"
        >
          <div class="tl-item__rail">
            <div class="tl-item__node">{{ step.step ?? idx + 1 }}</div>
            <div v-if="idx < traceSteps.length - 1" class="tl-item__line"></div>
          </div>
          <div class="tl-item__body">
            <div class="tl-item__head">
              <div class="tl-item__route">
                <span class="tl-item__from">Supervisor</span>
                <span class="tl-item__arrow">→</span>
                <span class="tl-item__to">
                  {{ TOOL_LABEL[step.tool_to_use || ''] || step.tool_to_use || '-' }}
                </span>
              </div>
              <span class="tl-item__round">round #{{ step.step ?? idx + 1 }}</span>
            </div>
            <div v-if="step.tool_parameters?.instruction" class="tl-item__field">
              <span class="tl-item__label">instruction</span>
              <div class="tl-item__text">{{ step.tool_parameters.instruction }}</div>
            </div>
            <div
              v-if="step.tool_parameters?.summary_and_recommendations"
              class="tl-item__field"
            >
              <span class="tl-item__label">report outline</span>
              <div class="tl-item__text">
                {{ step.tool_parameters.summary_and_recommendations }}
              </div>
            </div>
          </div>
        </article>
      </div>
    </div>

    <div v-else-if="fallbackText" class="sv-fallback">
      <div class="sv-fallback__label">stream</div>
      <pre class="sv-fallback__text">{{ fallbackText }}</pre>
    </div>

    <div v-else class="sv-empty">Supervisor 正在思考首轮派单…</div>
  </section>
</template>

<style scoped>
.sv-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px 16px;
  background: #ffffff;
}

/* ========== Header ========== */
.sv-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.sv-card__chip {
  display: inline-block;
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #475569;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.sv-card__title {
  margin: 6px 0 2px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.sv-card__subtitle {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.sv-card__meta {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding-top: 2px;
}

.sv-card__metaitem {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  min-width: 48px;
}

.sv-card__metalabel {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 9px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #94a3b8;
}

.sv-card__metaval {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

/* ========== Sub-Agents (3 columns, flat) ========== */
.sv-agents {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.sv-agent {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  background: #f8fafc;
}

.sv-agent--running {
  border-color: #cbd5e1;
  background: #f1f5f9;
}

.sv-agent__head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sv-agent__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.sv-agent--used .sv-agent__dot {
  background: #22c55e;
}

.sv-agent--running .sv-agent__dot {
  background: #1a73e8;
}

.sv-agent__name {
  font-size: 12.5px;
  font-weight: 600;
  color: #0f172a;
  flex: 1;
}

.sv-agent__count {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 11px;
  color: #64748b;
}

.sv-agent__foot {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 6px;
}

.sv-agent__role {
  font-size: 11px;
  color: #94a3b8;
}

.sv-agent__state {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  color: #94a3b8;
  letter-spacing: 0.04em;
}

.sv-agent--used .sv-agent__state {
  color: #16a34a;
}

.sv-agent--running .sv-agent__state {
  color: #1a73e8;
}

/* ========== Timeline ========== */
.sv-timeline {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.sv-timeline__title {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #94a3b8;
}

.sv-timeline__list {
  margin-top: 8px;
}

.tl-item {
  display: grid;
  grid-template-columns: 26px 1fr;
  gap: 8px;
  padding-bottom: 10px;
}

.tl-item__rail {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.tl-item__node {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 11px;
  font-weight: 600;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
  z-index: 1;
}

.tl-item__line {
  position: absolute;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  width: 1px;
  height: calc(100% - 24px);
  background: #e2e8f0;
}

.tl-item__body {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  background: #ffffff;
}

.tl-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tl-item__route {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
}

.tl-item__from {
  color: #475569;
}

.tl-item__arrow {
  color: #cbd5e1;
  font-weight: 400;
}

.tl-item__to {
  color: #0f172a;
}

.tl-item__round {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  color: #94a3b8;
}

.tl-item__field {
  margin-top: 6px;
}

.tl-item__label {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #94a3b8;
}

.tl-item__text {
  margin-top: 2px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ========== Fallback / Empty ========== */
.sv-fallback {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  background: #f8fafc;
}

.sv-fallback__label {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #94a3b8;
}

.sv-fallback__text {
  margin: 4px 0 0;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #334155;
  font-size: 12.5px;
}

.sv-empty {
  margin-top: 12px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 10px;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}
</style>
