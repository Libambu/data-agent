<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'

const router = useRouter()

const goToWorkspace = () => {
  router.push('/workspace')
}

const isVisible = ref(false)

// 4 个阶段亮点（对应 SVG 图）
const phases = [
  { key: 'I', name: 'Retrieval', label: '智能召回', color: 'cyan', desc: 'Evidence · Schema · Relation 三维召回' },
  { key: 'II', name: 'Planning', label: '任务规划', color: 'violet', desc: '可行性评估 · 步骤拆解 · 人工确认' },
  { key: 'III', name: 'Execution', label: '双引擎执行', color: 'amber', desc: 'SQL / Python / Report 多分支调度' },
  { key: 'IV', name: 'Output', label: '报告输出', color: 'emerald', desc: '自动生成 Markdown 分析报告' },
]

// ========== DAG 活体演示：按真实工作流顺序激活节点 / 边 ==========
// step: { node, edges, phase, label } —— 每一步都对应一个被点亮的节点 + 进入它的边
type WalkStep = {
  node: string
  edges: string[]
  phase: 'I' | 'II' | 'III' | 'IV'
  label: string
}

const walk: WalkStep[] = [
  { node: 'n-start',     edges: [],                                phase: 'I',   label: 'Start' },
  { node: 'n-evidence',  edges: ['e-s-evidence'],                  phase: 'I',   label: 'Evidence Recall' },
  { node: 'n-schema',    edges: ['e-evidence-schema'],             phase: 'I',   label: 'Schema Recall' },
  { node: 'n-relation',  edges: ['e-schema-relation'],             phase: 'I',   label: 'Table Relation' },
  { node: 'n-feasible',  edges: ['e-relation-feasible'],           phase: 'II',  label: 'Feasibility' },
  { node: 'n-planner',   edges: ['e-feasible-planner'],            phase: 'II',  label: 'Planner' },
  { node: 'n-human',     edges: ['e-planner-human'],               phase: 'II',  label: 'Human-in-Loop' },
  { node: 'n-dispatch',  edges: ['e-human-dispatch'],              phase: 'III', label: 'Dispatch' },
  { node: 'n-sqlgen',    edges: ['e-dispatch-sqlgen'],             phase: 'III', label: 'SQL Gen' },
  { node: 'n-sqlexec',   edges: ['e-sqlgen-sqlexec'],              phase: 'III', label: 'SQL Exec' },
  { node: 'n-pygen',     edges: ['e-dispatch-pygen'],              phase: 'III', label: 'Python Gen' },
  { node: 'n-pyexec',    edges: ['e-pygen-pyexec'],                phase: 'III', label: 'Python Exec' },
  { node: 'n-report',    edges: ['e-dispatch-report'],             phase: 'III', label: 'Report Gen' },
  { node: 'n-end',       edges: ['e-report-end'],                  phase: 'IV',  label: 'Done' },
]

const stepIndex = ref(0)
const activeNode = computed(() => walk[stepIndex.value]!.node)
const activeEdges = computed(() => walk[stepIndex.value]!.edges)
const activePhase = computed(() => walk[stepIndex.value]!.phase)
const activeLabel = computed(() => walk[stepIndex.value]!.label)

// 已经走过的节点（用于轻量描色，区分 "running" / "visited" / "idle"）
const visitedNodes = computed(() => {
  const set = new Set<string>()
  for (let i = 0; i < stepIndex.value; i++) set.add(walk[i]!.node)
  return set
})

const nodeState = (id: string) => {
  if (id === activeNode.value) return 'running'
  if (visitedNodes.value.has(id)) return 'visited'
  return 'idle'
}
const edgeState = (id: string) => (activeEdges.value.includes(id) ? 'running' : 'idle')

let timer: number | null = null
const startWalker = () => {
  if (timer) return
  timer = window.setInterval(() => {
    stepIndex.value = (stepIndex.value + 1) % walk.length
  }, 1100)
}
const stopWalker = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(() => {
  nextTick(() => {
    isVisible.value = true
  })
  startWalker()
})

onBeforeUnmount(() => {
  stopWalker()
})
</script>

<template>
  <main class="home" :class="{ 'home--ready': isVisible }">
    <!-- 顶部导航 -->
    <nav class="topbar">
      <div class="topbar__brand" @click="goToWorkspace">
        <span class="topbar__logo">
          <el-icon><DataAnalysis /></el-icon>
        </span>
        <span class="topbar__name">Data&nbsp;Agent</span>
        <span class="topbar__tag">v1.0</span>
      </div>
      <div class="topbar__nav">
        <span class="topbar__link">Architecture</span>
        <span class="topbar__link">Pipeline</span>
        <span class="topbar__link">Docs</span>
        <span class="topbar__divider"></span>
        <button class="topbar__cta" @click="goToWorkspace">
          Launch Workspace
          <el-icon><Right /></el-icon>
        </button>
      </div>
    </nav>

    <!-- 主舞台 -->
    <section class="stage">
      <!-- 左：Hero -->
      <div class="hero">
        <div class="hero__pill">
          <span class="hero__pill-dot"></span>
          <span class="hero__pill-text">Intelligent Data Agent</span>
        </div>

        <h1 class="hero__title">
          <span class="hero__title-line">Multi-Agent Analytics,</span>
          <span class="hero__title-line hero__title-line--gradient">End-to-End Automated.</span>
        </h1>

        <p class="hero__lead">
          基于多节点 AI Agent 协作，全流程自动化，节点级可观测、人工可介入。
        </p>

        <ul class="hero__flow" aria-label="工作流程">
          <li class="flow-chip">需求理解</li>
          <li class="flow-arrow" aria-hidden="true">→</li>
          <li class="flow-chip">SQL / Python 生成</li>
          <li class="flow-arrow" aria-hidden="true">→</li>
          <li class="flow-chip">数据分析</li>
          <li class="flow-arrow" aria-hidden="true">→</li>
          <li class="flow-chip">报告输出</li>
        </ul>

        <div class="hero__cta">
          <el-button type="primary" size="large" @click="goToWorkspace">
            <el-icon><Promotion /></el-icon>
            <span>Start Analyzing</span>
          </el-button>
          <button class="ghost-btn" @click="goToWorkspace">
            <el-icon><CaretRight /></el-icon>
            <span>View Live Trace</span>
          </button>
        </div>

        <!-- 阶段卡 -->
        <div class="phases">
          <div
            v-for="(phase, idx) in phases"
            :key="phase.key"
            class="phase"
            :class="`phase--${phase.color}`"
            :style="{ animationDelay: `${idx * 80}ms` }"
          >
            <span class="phase__rail" aria-hidden="true"></span>
            <div class="phase__head">
              <span class="phase__idx">{{ phase.key }}</span>
              <span class="phase__name">{{ phase.name }}</span>
            </div>
            <div class="phase__label">{{ phase.label }}</div>
            <div class="phase__desc">{{ phase.desc }}</div>
          </div>
        </div>

        <div class="hero__stats">
          <div class="stat">
            <strong>4</strong>
            <span>Phases</span>
          </div>
          <div class="stat">
            <strong>13</strong>
            <span>Nodes</span>
          </div>
          <div class="stat">
            <strong>SQL · Py</strong>
            <span>Engines</span>
          </div>
          <div class="stat">
            <strong>Live</strong>
            <span>Streaming</span>
          </div>
        </div>
      </div>

      <!-- 右：玻璃面板架构图 -->
      <aside class="arch-pane">
        <div class="arch-pane__head">
          <div class="arch-pane__head-l">
            <span class="arch-pane__badge arch-pane__badge--live">
              <span class="arch-pane__badge-dot"></span>
              LIVE&nbsp;WORKFLOW&nbsp;MAP
            </span>
            <h2 class="arch-pane__title">System DAG</h2>
          </div>
          <div class="arch-pane__dots">
            <span></span><span></span><span></span>
          </div>
        </div>

        <div class="arch-pane__status">
          <span class="status-line">
            <span class="status-line__pulse"></span>
            <span class="status-line__phase">PHASE&nbsp;{{ activePhase }}</span>
            <span class="status-line__sep">›</span>
            <span class="status-line__label">{{ activeLabel }}</span>
          </span>
          <span class="status-line__progress">
            {{ String(stepIndex + 1).padStart(2, '0') }} / {{ String(walk.length).padStart(2, '0') }}
          </span>
        </div>

        <div class="arch-graph">
          <svg viewBox="0 0 520 680" class="arch-svg" preserveAspectRatio="xMidYMid meet">
            <defs>
              <marker id="arr" markerWidth="6" markerHeight="5" refX="6" refY="2.5" orient="auto">
                <polygon points="0 0, 6 2.5, 0 5" fill="#9aa0a6" />
              </marker>
              <marker id="arr-c" markerWidth="6" markerHeight="5" refX="6" refY="2.5" orient="auto">
                <polygon points="0 0, 6 2.5, 0 5" fill="#bdc1c6" />
              </marker>
              <marker id="arr-l" markerWidth="5" markerHeight="4" refX="5" refY="2" orient="auto">
                <polygon points="0 0, 5 2, 0 4" fill="#bdc1c6" />
              </marker>
              <marker id="arr-live" markerWidth="7" markerHeight="6" refX="7" refY="3" orient="auto">
                <polygon points="0 0, 7 3, 0 6" fill="#1a73e8" />
              </marker>
              <linearGradient id="grad-recall" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#1a73e8" stop-opacity="0.10" />
                <stop offset="1" stop-color="#1a73e8" stop-opacity="0.02" />
              </linearGradient>
              <linearGradient id="grad-plan" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#9b72cb" stop-opacity="0.10" />
                <stop offset="1" stop-color="#9b72cb" stop-opacity="0.02" />
              </linearGradient>
              <linearGradient id="grad-exec" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#f9ab00" stop-opacity="0.10" />
                <stop offset="1" stop-color="#f9ab00" stop-opacity="0.02" />
              </linearGradient>
              <linearGradient id="grad-out" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#d96570" stop-opacity="0.12" />
                <stop offset="1" stop-color="#d96570" stop-opacity="0.03" />
              </linearGradient>
              <radialGradient id="glow-blue" cx="0.5" cy="0.5" r="0.5">
                <stop offset="0" stop-color="#1a73e8" stop-opacity="0.55" />
                <stop offset="1" stop-color="#1a73e8" stop-opacity="0" />
              </radialGradient>
              <filter id="f-glow" x="-40%" y="-40%" width="180%" height="180%">
                <feGaussianBlur stdDeviation="2.4" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>

            <!-- Phase 分组背景 -->
            <rect x="155" y="18" width="210" height="175" rx="14"
              class="phase-rect" :class="{ 'phase-rect--active': activePhase === 'I' }"
              fill="url(#grad-recall)" stroke="#1a73e8" />
            <text x="260" y="38" class="phase-label phase-label--recall">PHASE I · RETRIEVAL</text>

            <rect x="155" y="208" width="210" height="162" rx="14"
              class="phase-rect" :class="{ 'phase-rect--active': activePhase === 'II' }"
              fill="url(#grad-plan)" stroke="#9b72cb" />
            <text x="260" y="228" class="phase-label phase-label--plan">PHASE II · PLANNING</text>

            <rect x="55" y="385" width="410" height="175" rx="14"
              class="phase-rect" :class="{ 'phase-rect--active': activePhase === 'III' }"
              fill="url(#grad-exec)" stroke="#f9ab00" />
            <text x="260" y="405" class="phase-label phase-label--exec">PHASE III · EXECUTION</text>

            <rect x="355" y="575" width="110" height="50" rx="14"
              class="phase-rect" :class="{ 'phase-rect--active': activePhase === 'IV' }"
              fill="url(#grad-out)" stroke="#d96570" />
            <text x="410" y="592" class="phase-label phase-label--out">PHASE IV</text>

            <!-- ===== 连线（先画，节点后画以便置顶） ===== -->
            <line id="e-s-evidence" x1="260" y1="66" x2="260" y2="78"
              class="edge" :class="`edge--${edgeState('e-s-evidence')}`" marker-end="url(#arr)" />
            <line id="e-evidence-schema" x1="260" y1="104" x2="260" y2="116"
              class="edge" :class="`edge--${edgeState('e-evidence-schema')}`" marker-end="url(#arr)" />
            <line id="e-schema-relation" x1="260" y1="142" x2="260" y2="154"
              class="edge" :class="`edge--${edgeState('e-schema-relation')}`" marker-end="url(#arr)" />
            <line id="e-relation-feasible" x1="260" y1="180" x2="260" y2="230"
              class="edge" :class="`edge--${edgeState('e-relation-feasible')}`" marker-end="url(#arr)" />

            <line id="e-feasible-planner" x1="260" y1="266" x2="260" y2="286"
              class="edge-cond" :class="`edge-cond--${edgeState('e-feasible-planner')}`" marker-end="url(#arr-c)" />
            <text x="266" y="280" class="label-edge">yes</text>

            <path d="M 295 248 L 480 248 L 480 600 L 421 600" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="484" y="320" class="label-edge">no</text>

            <line id="e-planner-human" x1="260" y1="312" x2="260" y2="332"
              class="edge" :class="`edge--${edgeState('e-planner-human')}`" marker-end="url(#arr)" />

            <line id="e-human-dispatch" x1="260" y1="368" x2="260" y2="415"
              class="edge-cond" :class="`edge-cond--${edgeState('e-human-dispatch')}`" marker-end="url(#arr-c)" />
            <text x="266" y="395" class="label-edge">ok</text>

            <path d="M 225 350 L 190 350 L 190 299 L 205 299" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="168" y="328" class="label-edge">redo</text>

            <path d="M 295 350 L 470 350 L 470 600 L 421 600" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="474" y="420" class="label-edge">cancel</text>

            <path id="e-dispatch-sqlgen" d="M 225 433 L 115 433 L 115 475"
              class="edge-cond" :class="`edge-cond--${edgeState('e-dispatch-sqlgen')}`" marker-end="url(#arr-c)" />
            <text x="148" y="428" class="label-edge">sql</text>

            <line id="e-dispatch-pygen" x1="260" y1="451" x2="260" y2="475"
              class="edge-cond" :class="`edge-cond--${edgeState('e-dispatch-pygen')}`" marker-end="url(#arr-c)" />
            <text x="266" y="467" class="label-edge">py</text>

            <path id="e-dispatch-report" d="M 295 433 L 405 433 L 405 475"
              class="edge-cond" :class="`edge-cond--${edgeState('e-dispatch-report')}`" marker-end="url(#arr-c)" />
            <text x="340" y="428" class="label-edge">report</text>

            <line id="e-sqlgen-sqlexec" x1="115" y1="501" x2="115" y2="515"
              class="edge" :class="`edge--${edgeState('e-sqlgen-sqlexec')}`" marker-end="url(#arr)" />
            <path d="M 70 528 L 55 528 L 55 433 L 225 433" class="edge-loop" marker-end="url(#arr-l)" />

            <line id="e-pygen-pyexec" x1="260" y1="501" x2="260" y2="515"
              class="edge" :class="`edge--${edgeState('e-pygen-pyexec')}`" marker-end="url(#arr)" />
            <path d="M 305 528 L 320 528 L 320 460 L 260 460 L 260 451" class="edge-loop" marker-end="url(#arr-l)" />

            <line id="e-report-end" x1="405" y1="501" x2="405" y2="589"
              class="edge" :class="`edge--${edgeState('e-report-end')}`" marker-end="url(#arr)" />

            <!-- ===== Phase I 节点 ===== -->
            <g :class="`node node--${nodeState('n-start')}`">
              <circle cx="260" cy="55" r="16" class="node-aura" />
              <circle cx="260" cy="55" r="11" class="node-terminal" />
              <text x="260" y="59" class="label-terminal">S</text>
            </g>

            <g :class="`node node--${nodeState('n-evidence')}`">
              <rect x="203" y="76" width="114" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="205" y="78" width="110" height="26" rx="8" class="node-box node-box--recall" />
              <text x="260" y="95" class="label-node">Evidence Recall</text>
            </g>

            <g :class="`node node--${nodeState('n-schema')}`">
              <rect x="203" y="114" width="114" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="205" y="116" width="110" height="26" rx="8" class="node-box node-box--recall" />
              <text x="260" y="133" class="label-node">Schema Recall</text>
            </g>

            <g :class="`node node--${nodeState('n-relation')}`">
              <rect x="203" y="152" width="114" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="205" y="154" width="110" height="26" rx="8" class="node-box node-box--recall" />
              <text x="260" y="171" class="label-node">Table Relation</text>
            </g>

            <!-- ===== Phase II 节点 ===== -->
            <g :class="`node node--${nodeState('n-feasible')}`">
              <polygon points="260,226 299,248 260,270 221,248" class="node-aura node-aura--diamond" />
              <polygon points="260,230 295,248 260,266 225,248" class="node-decision node-decision--plan" />
              <text x="260" y="252" class="label-decision">Feasibility</text>
            </g>

            <g :class="`node node--${nodeState('n-planner')}`">
              <rect x="203" y="284" width="114" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="205" y="286" width="110" height="26" rx="8" class="node-box node-box--plan" />
              <text x="260" y="303" class="label-node">Planner</text>
            </g>

            <g :class="`node node--${nodeState('n-human')}`">
              <polygon points="260,328 299,350 260,372 221,350" class="node-aura node-aura--diamond" />
              <polygon points="260,332 295,350 260,368 225,350" class="node-decision node-decision--plan" />
              <text x="260" y="354" class="label-decision">Human</text>
            </g>

            <!-- ===== Phase III 节点 ===== -->
            <g :class="`node node--${nodeState('n-dispatch')}`">
              <polygon points="260,411 299,433 260,455 221,433" class="node-aura node-aura--diamond" />
              <polygon points="260,415 295,433 260,451 225,433" class="node-decision node-decision--exec" />
              <text x="260" y="437" class="label-decision">Dispatch</text>
            </g>

            <g :class="`node node--${nodeState('n-sqlgen')}`">
              <rect x="68" y="473" width="94" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="70" y="475" width="90" height="26" rx="8" class="node-box node-box--exec" />
              <text x="115" y="492" class="label-node">SQL Gen</text>
            </g>

            <g :class="`node node--${nodeState('n-sqlexec')}`">
              <rect x="68" y="513" width="94" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="70" y="515" width="90" height="26" rx="8" class="node-box node-box--exec" />
              <text x="115" y="532" class="label-node">SQL Exec</text>
            </g>

            <g :class="`node node--${nodeState('n-pygen')}`">
              <rect x="213" y="473" width="94" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="215" y="475" width="90" height="26" rx="8" class="node-box node-box--exec" />
              <text x="260" y="492" class="label-node">Python Gen</text>
            </g>

            <g :class="`node node--${nodeState('n-pyexec')}`">
              <rect x="213" y="513" width="94" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="215" y="515" width="90" height="26" rx="8" class="node-box node-box--exec" />
              <text x="260" y="532" class="label-node">Python Exec</text>
            </g>

            <g :class="`node node--${nodeState('n-report')}`">
              <rect x="358" y="473" width="94" height="30" rx="9" class="node-aura node-aura--rect" />
              <rect x="360" y="475" width="90" height="26" rx="8" class="node-box node-box--out" />
              <text x="405" y="492" class="label-node">Report Gen</text>
            </g>

            <!-- ===== Phase IV ===== -->
            <g :class="`node node--${nodeState('n-end')}`">
              <circle cx="410" cy="600" r="16" class="node-aura" />
              <circle cx="410" cy="600" r="11" class="node-terminal node-terminal--end" />
              <text x="410" y="604" class="label-terminal">E</text>
            </g>
          </svg>
        </div>

        <div class="arch-legend">
          <div class="leg"><span class="leg-sw leg-sw--node"></span>Process</div>
          <div class="leg"><span class="leg-sw leg-sw--decision"></span>Decision</div>
          <div class="leg"><span class="leg-sw leg-sw--flow"></span>Flow</div>
          <div class="leg"><span class="leg-sw leg-sw--cond"></span>Conditional</div>
          <div class="leg"><span class="leg-sw leg-sw--loop"></span>Loop-back</div>
        </div>
      </aside>
    </section>

    <!-- 底部尾签 -->
    <footer class="foot">
      <span>© Data Agent · Multi-Agent DAG Orchestration</span>
      <span class="foot__sep"></span>
      <span>Built with Vue · Element Plus · A2A SDK</span>
    </footer>
  </main>
</template>

<style scoped>
.home {
  position: relative;
  min-height: 100vh;
  padding: 22px clamp(20px, 4vw, 56px) 28px;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 26px;
}

/* ========== Topbar ========== */
.topbar {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1480px;
  margin: 0 auto;
  width: 100%;
  padding: 10px 12px 10px 16px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  box-shadow: var(--shadow-sm);
  opacity: 0;
  transform: translateY(-10px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.home--ready .topbar {
  opacity: 1;
  transform: none;
}

.topbar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.topbar__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  color: #ffffff;
  background: linear-gradient(135deg, #4285f4 0%, #9b72cb 50%, #d96570 100%);
  box-shadow: 0 2px 8px rgba(66, 133, 244, 0.32);
}

.topbar__name {
  font-weight: 900;
  letter-spacing: -0.01em;
  color: var(--text-1);
}

.topbar__tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-2);
  color: var(--text-2);
  font-size: 11px;
  font-weight: 600;
}

.topbar__nav {
  display: flex;
  align-items: center;
  gap: 18px;
}

.topbar__link {
  color: var(--text-2);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.18s;
}

.topbar__link:hover {
  color: var(--text-1);
}

.topbar__divider {
  width: 1px;
  height: 16px;
  background: var(--line-2);
}

.topbar__cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 999px;
  color: #ffffff;
  background: var(--color-primary);
  box-shadow: 0 1px 2px rgba(26, 115, 232, 0.28), 0 1px 3px rgba(26, 115, 232, 0.18);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s, box-shadow 0.18s;
}

.topbar__cta:hover {
  background: var(--color-primary-strong);
  box-shadow: 0 1px 3px rgba(26, 115, 232, 0.4), 0 4px 10px rgba(26, 115, 232, 0.24);
}

/* ========== Stage ========== */
.stage {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.9fr);
  gap: clamp(32px, 4vw, 64px);
  align-items: start;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.7s ease 0.1s, transform 0.7s ease 0.1s;
}

.home--ready .stage {
  opacity: 1;
  transform: none;
}

/* ========== Hero ========== */
.hero {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding: clamp(20px, 3.4vw, 40px) 0 clamp(8px, 2vw, 20px);
  max-width: 720px;
  width: 100%;
}

.hero__pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 6px 14px 6px 10px;
  border: 1px solid rgba(26, 115, 232, 0.22);
  border-radius: 999px;
  background: rgba(26, 115, 232, 0.06);
  color: var(--color-primary);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.hero__pill-text {
  font-family: 'Google Sans', 'Inter', system-ui, sans-serif;
}

.hero__pill-dot {
  position: relative;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}

.hero__pill-dot::after {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: rgba(26, 115, 232, 0.22);
  content: '';
  animation: pulse 1.6s infinite;
}

@keyframes pulse {
  0%   { opacity: 0.5; transform: scale(0.6); }
  100% { opacity: 0;   transform: scale(1.6); }
}

.hero__title {
  margin: 20px 0 0;
  display: flex;
  flex-direction: column;
  font-size: clamp(40px, 4.6vw, 62px);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -0.035em;
  color: var(--text-1);
}

.hero__title-line--gradient {
  background: linear-gradient(120deg, #4285f4 0%, #9b72cb 50%, #d96570 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero__lead {
  max-width: 560px;
  margin: 18px 0 0;
  color: var(--text-2);
  font-size: 15px;
  line-height: 1.7;
}

/* ========== 流程胶囊 ========== */
.hero__flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.flow-chip {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 12px;
  border: 1px solid rgba(26, 115, 232, 0.22);
  border-radius: 999px;
  background: rgba(26, 115, 232, 0.06);
  color: var(--color-primary);
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.01em;
  white-space: nowrap;
}

.flow-arrow {
  color: var(--text-3);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 12px;
  font-weight: 500;
  user-select: none;
}

.hero__cta {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}

.hero__cta :deep(.el-button) {
  height: 44px;
  padding: 0 22px;
  border-radius: 22px;
  font-weight: 600;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 20px;
  border: 1px solid var(--line-strong);
  border-radius: 22px;
  background: #ffffff;
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s, box-shadow 0.18s;
}

.ghost-btn .el-icon {
  font-size: 14px;
}

.ghost-btn:hover {
  background: rgba(26, 115, 232, 0.06);
  border-color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(26, 115, 232, 0.18);
}

/* ========== 阶段卡 ========== */
.phases {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 32px;
}

.phase {
  position: relative;
  overflow: hidden;
  padding: 18px 18px 16px 22px;
  border: 1px solid var(--line-2);
  border-radius: 14px;
  background: #ffffff;
  cursor: default;
  opacity: 0;
  animation: phase-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
  box-shadow: var(--shadow-sm);
}

@keyframes phase-in {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: none; }
}

.phase__rail {
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: 10px;
  width: 3px;
  border-radius: 999px;
  background: var(--phase-c);
  opacity: 0.85;
}

.phase::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background: radial-gradient(280px 160px at 0% 0%, var(--phase-c-soft), transparent 62%);
}

.phase:hover {
  transform: translateY(-1px);
  border-color: var(--phase-c);
  box-shadow: var(--shadow-md);
}

/* Gemini 阶段色：Blue / Purple / Amber / Coral */
.phase--cyan    { --phase-c: #1a73e8; --phase-c-soft: rgba(26, 115, 232, 0.10); }
.phase--violet  { --phase-c: #9b72cb; --phase-c-soft: rgba(155, 114, 203, 0.10); }
.phase--amber   { --phase-c: #f9ab00; --phase-c-soft: rgba(249, 171, 0, 0.10); }
.phase--emerald { --phase-c: #d96570; --phase-c-soft: rgba(217, 101, 112, 0.10); }

.phase__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.phase__idx {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--phase-c-soft);
  color: var(--phase-c);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.phase__name {
  color: var(--phase-c);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0.9;
}

.phase__label {
  position: relative;
  z-index: 1;
  color: var(--text-1);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.phase__desc {
  position: relative;
  z-index: 1;
  margin-top: 6px;
  color: var(--text-2);
  font-size: 12.5px;
  line-height: 1.55;
}

/* ========== 横向数据条 ========== */
.hero__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: stretch;
  gap: 0;
  margin-top: 28px;
  padding: 16px 4px;
  border: 1px solid var(--line-2);
  border-radius: 14px;
  background:
    linear-gradient(180deg, #ffffff 0%, var(--bg-1) 100%);
  box-shadow: var(--shadow-sm);
}

.stat {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 4px 18px;
}

.stat + .stat::before {
  position: absolute;
  top: 8px;
  bottom: 8px;
  left: 0;
  width: 1px;
  background: var(--line-2);
  content: '';
}

.stat strong {
  display: block;
  color: var(--text-1);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.1;
  background: linear-gradient(120deg, #4285f4 0%, #9b72cb 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat span {
  display: block;
  margin-top: 6px;
  color: var(--text-3);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

/* ========== Arch Pane ========== */
.arch-pane {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 18px 20px 16px;
  border: 1px solid var(--line-2);
  border-radius: var(--radius-2xl);
  background:
    radial-gradient(600px 400px at 92% 0%, rgba(155, 114, 203, 0.08), transparent 50%),
    #ffffff;
  box-shadow: var(--shadow-md);
}

.arch-pane::before {
  position: absolute;
  inset: 1px;
  pointer-events: none;
  border: 1px solid rgba(60, 64, 67, 0.04);
  border-radius: calc(var(--radius-2xl) - 1px);
  content: '';
}

.arch-pane__head {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.arch-pane__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid rgba(26, 115, 232, 0.24);
  border-radius: 999px;
  background: rgba(26, 115, 232, 0.08);
  color: var(--color-primary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.arch-pane__badge--live {
  border-color: rgba(52, 168, 83, 0.36);
  background: rgba(52, 168, 83, 0.08);
  color: #1e8e3e;
}

.arch-pane__badge-dot {
  position: relative;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34a853;
  box-shadow: 0 0 0 0 rgba(52, 168, 83, 0.6);
  animation: live-dot 1.4s ease-out infinite;
}

@keyframes live-dot {
  0%   { box-shadow: 0 0 0 0 rgba(52, 168, 83, 0.55); }
  70%  { box-shadow: 0 0 0 7px rgba(52, 168, 83, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 168, 83, 0); }
}

/* 实时状态条 */
.arch-pane__status {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 12px;
  padding: 8px 12px;
  border: 1px solid var(--line-2);
  border-radius: 10px;
  background:
    linear-gradient(90deg, rgba(26, 115, 232, 0.05), rgba(155, 114, 203, 0.04));
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 11px;
}

.status-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  overflow: hidden;
}

.status-line__pulse {
  position: relative;
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.18);
  animation: status-pulse 1.1s ease-in-out infinite;
}

@keyframes status-pulse {
  0%, 100% { transform: scale(1);   box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.18); }
  50%      { transform: scale(1.3); box-shadow: 0 0 0 6px rgba(26, 115, 232, 0.05); }
}

.status-line__phase {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(26, 115, 232, 0.1);
  color: var(--color-primary);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.status-line__sep {
  flex-shrink: 0;
  color: var(--text-3);
}

.status-line__label {
  color: var(--text-1);
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  animation: label-fade 0.4s ease;
}

@keyframes label-fade {
  from { opacity: 0; transform: translateY(2px); }
  to   { opacity: 1; transform: none; }
}

.status-line__progress {
  flex-shrink: 0;
  margin-left: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--bg-2);
  color: var(--text-2);
  font-weight: 700;
  letter-spacing: 0.06em;
}

.arch-pane__title {
  margin: 8px 0 0;
  color: var(--text-1);
  font-size: clamp(22px, 2.4vw, 30px);
  font-weight: 700;
  letter-spacing: -0.025em;
}

.arch-pane__dots {
  display: flex;
  gap: 6px;
}

.arch-pane__dots span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--bg-3);
}

.arch-pane__dots span:nth-child(1) { background: #ea4335; }
.arch-pane__dots span:nth-child(2) { background: #f9ab00; }
.arch-pane__dots span:nth-child(3) { background: #34a853; }

.arch-pane__caption {
  position: relative;
  z-index: 1;
  margin: 6px 0 12px;
  color: var(--text-3);
  font-size: 12px;
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  letter-spacing: 0.02em;
}

.arch-graph {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 50% 50%, rgba(26, 115, 232, 0.04), transparent 60%),
    var(--bg-1);
  box-shadow: inset 0 0 0 1px var(--line-2);
}

.arch-svg {
  width: 100%;
  height: auto;
  max-height: 640px;
}

/* SVG 样式（AI Studio 明亮版） */
.phase-rect {
  stroke-width: 1.2;
  stroke-opacity: 0.5;
  transition: stroke-opacity 0.3s ease, stroke-width 0.3s ease, filter 0.3s ease;
}

.phase-rect--active {
  stroke-opacity: 1;
  stroke-width: 1.8;
  filter: drop-shadow(0 0 8px rgba(26, 115, 232, 0.18));
}

.phase-label {
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.10em;
  text-anchor: middle;
}

.phase-label--recall { fill: #1a73e8; }
.phase-label--plan   { fill: #9b72cb; }
.phase-label--exec   { fill: #f9ab00; }
.phase-label--out    { fill: #d96570; }

.node-box {
  fill: #ffffff;
  stroke: var(--line-strong);
  stroke-width: 1;
}

.node-box--recall { stroke: #1a73e8; }
.node-box--plan   { stroke: #9b72cb; }
.node-box--exec   { stroke: #f9ab00; }
.node-box--out    { stroke: #d96570; }

.node-terminal {
  fill: #1a73e8;
  stroke: #ffffff;
  stroke-width: 1.4;
}

.node-terminal--end {
  fill: #d96570;
}

.node-decision {
  fill: #ffffff;
  stroke-width: 1.1;
}

.node-decision--plan { stroke: #9b72cb; }
.node-decision--exec { stroke: #f9ab00; }

.label-node {
  fill: var(--text-1);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: -0.02em;
  text-anchor: middle;
}

.label-terminal {
  fill: #ffffff;
  font-family: 'Google Sans', 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 700;
  text-anchor: middle;
}

.label-decision {
  fill: var(--text-1);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 9.5px;
  font-weight: 600;
  text-anchor: middle;
}

.label-edge {
  fill: var(--text-3);
  font-family: 'Google Sans', 'Inter', sans-serif;
  font-size: 9px;
  font-weight: 600;
  text-anchor: start;
}

.edge {
  fill: none;
  stroke: #9aa0a6;
  stroke-width: 1.2;
  transition: stroke 0.3s ease, stroke-width 0.3s ease;
}

.edge--running {
  stroke: #1a73e8;
  stroke-width: 2;
  stroke-dasharray: 6 4;
  animation: edge-flow 0.8s linear infinite;
  filter: drop-shadow(0 0 3px rgba(26, 115, 232, 0.55));
}

.edge-cond {
  fill: none;
  stroke: #bdc1c6;
  stroke-dasharray: 5 3;
  stroke-width: 1;
  transition: stroke 0.3s ease, stroke-width 0.3s ease;
}

.edge-cond--running {
  stroke: #1a73e8;
  stroke-width: 1.8;
  stroke-dasharray: 6 4;
  animation: edge-flow 0.8s linear infinite;
  filter: drop-shadow(0 0 3px rgba(26, 115, 232, 0.55));
}

@keyframes edge-flow {
  to { stroke-dashoffset: -20; }
}

.edge-loop {
  fill: none;
  stroke: #bdc1c6;
  stroke-dasharray: 2 3;
  stroke-width: 0.9;
}

/* ===== 节点状态：aura 呼吸圈 + visited / running 染色 ===== */
.node-aura {
  fill: rgba(26, 115, 232, 0.55);
  stroke: none;
  opacity: 0;
  transform-origin: center;
  transform-box: fill-box;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.node-aura--rect,
.node-aura--diamond {
  fill: rgba(26, 115, 232, 0.35);
}

.node--running .node-aura {
  opacity: 1;
  animation: aura-breath 1.1s ease-in-out infinite;
}

@keyframes aura-breath {
  0%, 100% {
    opacity: 0.45;
    transform: scale(1);
    filter: blur(2px);
  }
  50% {
    opacity: 0.85;
    transform: scale(1.18);
    filter: blur(3px);
  }
}

/* 已走过的节点染上淕淡背景，表示 "已完成" */
.node--visited .node-box,
.node--visited .node-decision {
  fill: rgba(26, 115, 232, 0.06);
  transition: fill 0.4s ease, stroke-width 0.3s ease;
}

.node--visited .node-terminal {
  filter: saturate(0.8) brightness(1.05);
}

/* 运行中的节点描边加粗 + 高亮 */
.node--running .node-box,
.node--running .node-decision {
  stroke-width: 2;
  fill: rgba(26, 115, 232, 0.10);
  filter: drop-shadow(0 0 6px rgba(26, 115, 232, 0.4));
  transition: filter 0.3s ease, stroke-width 0.3s ease, fill 0.3s ease;
}

.node--running .node-terminal {
  filter: drop-shadow(0 0 8px rgba(26, 115, 232, 0.7));
  animation: terminal-bounce 1.1s ease-in-out infinite;
}

@keyframes terminal-bounce {
  0%, 100% { transform: scale(1); transform-origin: center; transform-box: fill-box; }
  50%      { transform: scale(1.12); transform-origin: center; transform-box: fill-box; }
}

.node--running .label-node,
.node--running .label-decision {
  fill: var(--color-primary);
  font-weight: 700;
}

/* Legend */
.arch-legend {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--line-2);
}

.leg {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: var(--bg-1);
  color: var(--text-2);
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 600;
}

.leg-sw {
  display: inline-block;
  width: 14px;
  height: 8px;
}

.leg-sw--node { border: 1px solid var(--text-2); border-radius: 2px; }
.leg-sw--decision {
  width: 0; height: 0; border: 4px solid transparent;
  border-left-color: var(--text-2); border-right-color: var(--text-2);
}
.leg-sw--flow { border-top: 1.4px solid var(--text-2); height: 1px; }
.leg-sw--cond { border-top: 1.4px dashed var(--text-2); height: 1px; }
.leg-sw--loop { border-top: 1.4px dotted var(--text-2); height: 1px; }

/* ========== Foot ========== */
.foot {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  max-width: 1480px;
  width: 100%;
  margin: 0 auto;
  padding-top: 8px;
  color: var(--text-3);
  font-size: 11px;
  font-family: 'Roboto Mono', 'SF Mono', ui-monospace, monospace;
  letter-spacing: 0.04em;
}

.foot__sep {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--text-3);
  opacity: 0.5;
}

/* ========== 响应式 ========== */
@media (max-width: 1280px) {
  .stage {
    grid-template-columns: minmax(0, 1fr) minmax(380px, 0.78fr);
    gap: 36px;
  }

  .hero__title {
    font-size: clamp(40px, 5vw, 60px);
  }
}

@media (max-width: 1080px) {
  .stage {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .hero {
    max-width: 760px;
    margin: 0 auto;
    text-align: left;
    padding-top: 16px;
  }

  .arch-pane {
    min-height: 600px;
    max-width: 760px;
    width: 100%;
    margin: 0 auto;
  }
}

@media (max-width: 860px) {
  .topbar__divider {
    display: none;
  }

  .topbar__nav .topbar__link {
    display: none;
  }

  .phases {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .arch-graph {
    overflow-x: auto;
  }

  .arch-svg {
    min-width: 460px;
  }

  .hero__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 12px;
  }

  .stat:nth-child(3)::before {
    display: none;
  }
}

@media (max-width: 560px) {
  .hero__title {
    font-size: clamp(40px, 13vw, 60px);
  }

  .hero__cta {
    flex-direction: column;
  }

  .hero__cta :deep(.el-button),
  .ghost-btn {
    width: 100%;
    justify-content: center;
  }

  .phases {
    grid-template-columns: 1fr;
  }

  .hero__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
