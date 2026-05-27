<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, onMounted, nextTick } from 'vue'

const router = useRouter()

const goToWorkspace = () => {
  router.push('/workspace')
}

const isVisible = ref(false)
onMounted(() => {
  nextTick(() => {
    isVisible.value = true
  })
})

// 4 个阶段亮点（对应 SVG 图）
const phases = [
  { key: 'I', name: 'Retrieval', label: '智能召回', color: 'cyan', desc: 'Evidence · Schema · Relation 三维召回' },
  { key: 'II', name: 'Planning', label: '任务规划', color: 'violet', desc: '可行性评估 · 步骤拆解 · 人工确认' },
  { key: 'III', name: 'Execution', label: '双引擎执行', color: 'amber', desc: 'SQL / Python / Report 多分支调度' },
  { key: 'IV', name: 'Output', label: '报告输出', color: 'emerald', desc: '自动生成 Markdown 分析报告' },
]
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
          INTELLIGENT&nbsp;DATA&nbsp;ANALYSIS&nbsp;PLATFORM
        </div>

        <h1 class="hero__title">
          <span class="hero__title-line">Turn Questions</span>
          <span class="hero__title-line hero__title-line--gradient">into Data Stories.</span>
        </h1>

        <p class="hero__lead">
          基于多节点 AI Agent 协作，自动完成
          <em>需求理解 → SQL/Python 生成 → 数据分析 → 报告输出</em>
          全流程，节点级可观测、人工可介入。
        </p>

        <div class="hero__cta">
          <el-button type="primary" size="large" @click="goToWorkspace">
            <el-icon><Promotion /></el-icon>
            <span>Start Analyzing</span>
          </el-button>
          <button class="ghost-btn" @click="goToWorkspace">
            <el-icon><VideoPlay /></el-icon>
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
            <div class="phase__head">
              <span class="phase__idx">{{ phase.key }}</span>
              <span class="phase__name">Phase {{ phase.key }} · {{ phase.name }}</span>
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
          <span class="stat-sep"></span>
          <div class="stat">
            <strong>13</strong>
            <span>Nodes</span>
          </div>
          <span class="stat-sep"></span>
          <div class="stat">
            <strong>SQL · Python</strong>
            <span>Engines</span>
          </div>
          <span class="stat-sep"></span>
          <div class="stat">
            <strong>Streaming</strong>
            <span>Realtime</span>
          </div>
        </div>
      </div>

      <!-- 右：玻璃面板架构图 -->
      <aside class="arch-pane">
        <div class="arch-pane__head">
          <div class="arch-pane__head-l">
            <span class="arch-pane__badge">LIVE&nbsp;WORKFLOW&nbsp;MAP</span>
            <h2 class="arch-pane__title">System DAG</h2>
          </div>
          <div class="arch-pane__dots">
            <span></span><span></span><span></span>
          </div>
        </div>

        <p class="arch-pane__caption">
          DAG of Data Agent — Retrieval / Planning / Execution / Output.
        </p>

        <div class="arch-graph">
          <svg viewBox="0 0 520 680" class="arch-svg" preserveAspectRatio="xMidYMid meet">
            <defs>
              <marker id="arr" markerWidth="6" markerHeight="5" refX="6" refY="2.5" orient="auto">
                <polygon points="0 0, 6 2.5, 0 5" fill="#cbd5e1" />
              </marker>
              <marker id="arr-c" markerWidth="6" markerHeight="5" refX="6" refY="2.5" orient="auto">
                <polygon points="0 0, 6 2.5, 0 5" fill="#94a3b8" />
              </marker>
              <marker id="arr-l" markerWidth="5" markerHeight="4" refX="5" refY="2" orient="auto">
                <polygon points="0 0, 5 2, 0 4" fill="#64748b" />
              </marker>
              <linearGradient id="grad-recall" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#22d3ee" stop-opacity="0.18" />
                <stop offset="1" stop-color="#22d3ee" stop-opacity="0.02" />
              </linearGradient>
              <linearGradient id="grad-plan" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#a78bfa" stop-opacity="0.18" />
                <stop offset="1" stop-color="#a78bfa" stop-opacity="0.02" />
              </linearGradient>
              <linearGradient id="grad-exec" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#fbbf24" stop-opacity="0.18" />
                <stop offset="1" stop-color="#fbbf24" stop-opacity="0.02" />
              </linearGradient>
              <linearGradient id="grad-out" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#34d399" stop-opacity="0.22" />
                <stop offset="1" stop-color="#34d399" stop-opacity="0.04" />
              </linearGradient>
            </defs>

            <!-- Phase 分组背景 -->
            <rect x="155" y="18" width="210" height="175" rx="14" class="phase-rect" fill="url(#grad-recall)" stroke="#22d3ee" />
            <text x="260" y="38" class="phase-label phase-label--recall">PHASE I · RETRIEVAL</text>

            <rect x="155" y="208" width="210" height="162" rx="14" class="phase-rect" fill="url(#grad-plan)" stroke="#a78bfa" />
            <text x="260" y="228" class="phase-label phase-label--plan">PHASE II · PLANNING</text>

            <rect x="55" y="385" width="410" height="175" rx="14" class="phase-rect" fill="url(#grad-exec)" stroke="#fbbf24" />
            <text x="260" y="405" class="phase-label phase-label--exec">PHASE III · EXECUTION</text>

            <rect x="355" y="575" width="110" height="50" rx="14" class="phase-rect" fill="url(#grad-out)" stroke="#34d399" />
            <text x="410" y="592" class="phase-label phase-label--out">PHASE IV</text>

            <!-- ===== Phase I 节点 ===== -->
            <circle cx="260" cy="55" r="11" class="node-terminal" />
            <text x="260" y="59" class="label-terminal">S</text>

            <rect x="205" y="78" width="110" height="26" rx="8" class="node-box node-box--recall" />
            <text x="260" y="95" class="label-node">Evidence Recall</text>

            <rect x="205" y="116" width="110" height="26" rx="8" class="node-box node-box--recall" />
            <text x="260" y="133" class="label-node">Schema Recall</text>

            <rect x="205" y="154" width="110" height="26" rx="8" class="node-box node-box--recall" />
            <text x="260" y="171" class="label-node">Table Relation</text>

            <!-- ===== Phase II 节点 ===== -->
            <polygon points="260,230 295,248 260,266 225,248" class="node-decision node-decision--plan" />
            <text x="260" y="252" class="label-decision">Feasibility</text>

            <rect x="205" y="286" width="110" height="26" rx="8" class="node-box node-box--plan" />
            <text x="260" y="303" class="label-node">Planner</text>

            <polygon points="260,332 295,350 260,368 225,350" class="node-decision node-decision--plan" />
            <text x="260" y="354" class="label-decision">Human</text>

            <!-- ===== Phase III 节点 ===== -->
            <polygon points="260,415 295,433 260,451 225,433" class="node-decision node-decision--exec" />
            <text x="260" y="437" class="label-decision">Dispatch</text>

            <rect x="70" y="475" width="90" height="26" rx="8" class="node-box node-box--exec" />
            <text x="115" y="492" class="label-node">SQL Gen</text>

            <rect x="70" y="515" width="90" height="26" rx="8" class="node-box node-box--exec" />
            <text x="115" y="532" class="label-node">SQL Exec</text>

            <rect x="215" y="475" width="90" height="26" rx="8" class="node-box node-box--exec" />
            <text x="260" y="492" class="label-node">Python Gen</text>

            <rect x="215" y="515" width="90" height="26" rx="8" class="node-box node-box--exec" />
            <text x="260" y="532" class="label-node">Python Exec</text>

            <rect x="360" y="475" width="90" height="26" rx="8" class="node-box node-box--out" />
            <text x="405" y="492" class="label-node">Report Gen</text>

            <!-- ===== Phase IV ===== -->
            <circle cx="410" cy="600" r="11" class="node-terminal node-terminal--end" />
            <text x="410" y="604" class="label-terminal">E</text>

            <!-- ===== 连线 ===== -->
            <line x1="260" y1="66" x2="260" y2="78" class="edge" marker-end="url(#arr)" />
            <line x1="260" y1="104" x2="260" y2="116" class="edge" marker-end="url(#arr)" />
            <line x1="260" y1="142" x2="260" y2="154" class="edge" marker-end="url(#arr)" />
            <line x1="260" y1="180" x2="260" y2="230" class="edge" marker-end="url(#arr)" />

            <line x1="260" y1="266" x2="260" y2="286" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="266" y="280" class="label-edge">yes</text>

            <path d="M 295 248 L 480 248 L 480 600 L 421 600" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="484" y="320" class="label-edge">no</text>

            <line x1="260" y1="312" x2="260" y2="332" class="edge" marker-end="url(#arr)" />

            <line x1="260" y1="368" x2="260" y2="415" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="266" y="395" class="label-edge">ok</text>

            <path d="M 225 350 L 190 350 L 190 299 L 205 299" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="168" y="328" class="label-edge">redo</text>

            <path d="M 295 350 L 470 350 L 470 600 L 421 600" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="474" y="420" class="label-edge">cancel</text>

            <path d="M 225 433 L 115 433 L 115 475" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="148" y="428" class="label-edge">sql</text>

            <line x1="260" y1="451" x2="260" y2="475" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="266" y="467" class="label-edge">py</text>

            <path d="M 295 433 L 405 433 L 405 475" class="edge-cond" marker-end="url(#arr-c)" />
            <text x="340" y="428" class="label-edge">report</text>

            <line x1="115" y1="501" x2="115" y2="515" class="edge" marker-end="url(#arr)" />
            <path d="M 70 528 L 55 528 L 55 433 L 225 433" class="edge-loop" marker-end="url(#arr-l)" />

            <line x1="260" y1="501" x2="260" y2="515" class="edge" marker-end="url(#arr)" />
            <path d="M 305 528 L 320 528 L 320 460 L 260 460 L 260 451" class="edge-loop" marker-end="url(#arr-l)" />

            <line x1="405" y1="501" x2="405" y2="589" class="edge" marker-end="url(#arr)" />
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
  padding: 18px clamp(18px, 3vw, 40px) 24px;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 22px;
}

/* ========== Topbar ========== */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1480px;
  margin: 0 auto;
  width: 100%;
  padding: 10px 12px 10px 16px;
  border: 1px solid var(--line-2);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02));
  backdrop-filter: blur(22px);
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
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  box-shadow: 0 8px 22px rgba(99, 102, 241, 0.5);
}

.topbar__name {
  font-weight: 900;
  letter-spacing: -0.01em;
  color: var(--text-1);
}

.topbar__tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-2);
  font-size: 11px;
  font-weight: 700;
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
  background: linear-gradient(135deg, var(--color-primary-strong), var(--color-primary-deep));
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.5);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.18s, box-shadow 0.18s;
}

.topbar__cta:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-glow);
}

/* ========== Stage ========== */
.stage {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(440px, 0.95fr);
  gap: clamp(28px, 3.5vw, 56px);
  align-items: stretch;
  max-width: 1480px;
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
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(8px, 2vw, 24px) 0;
}

.hero__pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 7px 14px 7px 12px;
  border: 1px solid rgba(34, 211, 238, 0.32);
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.16), rgba(99, 102, 241, 0.12));
  color: #67e8f9;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  backdrop-filter: blur(8px);
}

.hero__pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 0 4px rgba(34, 211, 238, 0.22);
  animation: pulse 1.6s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}

.hero__title {
  margin: 22px 0 0;
  display: flex;
  flex-direction: column;
  font-size: clamp(48px, 6.4vw, 84px);
  font-weight: 900;
  line-height: 0.96;
  letter-spacing: -0.045em;
  color: var(--text-1);
}

.hero__title-line--gradient {
  background: linear-gradient(120deg, #818cf8 0%, #22d3ee 48%, #34d399 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 6px 24px rgba(99, 102, 241, 0.34));
}

.hero__lead {
  max-width: 560px;
  margin: 24px 0 0;
  color: var(--text-2);
  font-size: 16px;
  line-height: 1.75;
}

.hero__lead em {
  color: var(--text-1);
  font-style: normal;
  font-weight: 700;
  background: linear-gradient(120deg, rgba(99, 102, 241, 0.2), rgba(34, 211, 238, 0.18));
  padding: 1px 8px;
  border-radius: 6px;
}

.hero__cta {
  display: flex;
  gap: 14px;
  margin-top: 28px;
}

.hero__cta :deep(.el-button) {
  height: 52px;
  padding: 0 26px;
  border-radius: 14px;
  font-weight: 800;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 52px;
  padding: 0 22px;
  border: 1px solid var(--line-strong);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-1);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: all 0.18s;
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(129, 140, 248, 0.6);
  transform: translateY(-1px);
}

/* ========== 阶段卡 ========== */
.phases {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 32px;
}

.phase {
  position: relative;
  overflow: hidden;
  padding: 16px 16px 14px;
  border: 1px solid var(--line-2);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  backdrop-filter: blur(14px);
  cursor: default;
  opacity: 0;
  animation: phase-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  transition: transform 0.22s ease, border-color 0.22s ease;
}

@keyframes phase-in {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: none; }
}

.phase::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background: linear-gradient(90deg, var(--phase-c), transparent 50%) top / 100% 2px no-repeat,
              radial-gradient(260px 140px at 0% 0%, var(--phase-c-soft), transparent 60%);
}

.phase:hover {
  transform: translateY(-2px);
  border-color: var(--phase-c);
}

.phase--cyan    { --phase-c: #22d3ee; --phase-c-soft: rgba(34, 211, 238, 0.16); }
.phase--violet  { --phase-c: #a78bfa; --phase-c-soft: rgba(167, 139, 250, 0.18); }
.phase--amber   { --phase-c: #fbbf24; --phase-c-soft: rgba(251, 191, 36, 0.18); }
.phase--emerald { --phase-c: #34d399; --phase-c-soft: rgba(52, 211, 153, 0.18); }

.phase__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.phase__idx {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 6px;
  background: var(--phase-c-soft);
  color: var(--phase-c);
  font-family: 'SF Mono', ui-monospace, monospace;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.05em;
}

.phase__name {
  color: var(--text-3);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.phase__label {
  position: relative;
  z-index: 1;
  color: var(--text-1);
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.phase__desc {
  position: relative;
  z-index: 1;
  margin-top: 4px;
  color: var(--text-2);
  font-size: 12.5px;
  line-height: 1.55;
}

/* ========== 横向数据条 ========== */
.hero__stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px dashed var(--line-2);
}

.stat strong {
  display: block;
  color: var(--text-1);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat span {
  display: block;
  margin-top: 4px;
  color: var(--text-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.stat-sep {
  width: 1px;
  height: 28px;
  background: var(--line-2);
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
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015)),
    radial-gradient(600px 400px at 92% 0%, rgba(99, 102, 241, 0.18), transparent 50%);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(20px);
}

.arch-pane::before {
  position: absolute;
  inset: 1px;
  pointer-events: none;
  border: 1px solid rgba(255, 255, 255, 0.05);
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
  display: inline-block;
  padding: 5px 10px;
  border: 1px solid rgba(99, 102, 241, 0.36);
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.14);
  color: #a5b4fc;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.arch-pane__title {
  margin: 8px 0 0;
  color: var(--text-1);
  font-size: clamp(22px, 2.4vw, 30px);
  font-weight: 900;
  letter-spacing: -0.03em;
}

.arch-pane__dots {
  display: flex;
  gap: 6px;
}

.arch-pane__dots span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
}

.arch-pane__dots span:nth-child(1) { background: rgba(248, 113, 113, 0.7); }
.arch-pane__dots span:nth-child(2) { background: rgba(251, 191, 36, 0.7); }
.arch-pane__dots span:nth-child(3) { background: rgba(52, 211, 153, 0.7); }

.arch-pane__caption {
  position: relative;
  z-index: 1;
  margin: 6px 0 12px;
  color: var(--text-3);
  font-size: 12px;
  font-family: 'SF Mono', ui-monospace, monospace;
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
    linear-gradient(180deg, rgba(2, 6, 23, 0.5), rgba(2, 6, 23, 0.2)),
    radial-gradient(circle at 50% 50%, rgba(99, 102, 241, 0.06), transparent 60%);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.arch-svg {
  width: 100%;
  height: auto;
  max-height: 640px;
  filter: drop-shadow(0 8px 22px rgba(0, 0, 0, 0.4));
}

/* SVG 样式（深色版） */
.phase-rect {
  stroke-width: 1.2;
  stroke-opacity: 0.65;
}

.phase-label {
  font-family: 'SF Mono', ui-monospace, monospace;
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-anchor: middle;
}

.phase-label--recall { fill: #67e8f9; }
.phase-label--plan   { fill: #c4b5fd; }
.phase-label--exec   { fill: #fcd34d; }
.phase-label--out    { fill: #6ee7b7; }

.node-box {
  fill: rgba(15, 23, 42, 0.86);
  stroke: rgba(255, 255, 255, 0.18);
  stroke-width: 1;
}

.node-box--recall { stroke: #22d3ee; }
.node-box--plan   { stroke: #a78bfa; }
.node-box--exec   { stroke: #fbbf24; }
.node-box--out    { stroke: #34d399; }

.node-terminal {
  fill: #818cf8;
  stroke: rgba(255, 255, 255, 0.7);
  stroke-width: 1.4;
}

.node-terminal--end {
  fill: #34d399;
}

.node-decision {
  fill: rgba(15, 23, 42, 0.86);
  stroke-width: 1.1;
}

.node-decision--plan { stroke: #a78bfa; }
.node-decision--exec { stroke: #fbbf24; }

.label-node {
  fill: var(--text-1);
  font-family: 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: -0.02em;
  text-anchor: middle;
}

.label-terminal {
  fill: #ffffff;
  font-family: 'Inter', sans-serif;
  font-size: 11px;
  font-weight: 900;
  text-anchor: middle;
}

.label-decision {
  fill: #e2e8f0;
  font-family: 'SF Mono', ui-monospace, monospace;
  font-size: 9.5px;
  font-weight: 700;
  text-anchor: middle;
}

.label-edge {
  fill: #94a3b8;
  font-family: 'Inter', sans-serif;
  font-size: 9px;
  font-weight: 700;
  text-anchor: start;
}

.edge {
  fill: none;
  stroke: rgba(203, 213, 225, 0.6);
  stroke-width: 1.2;
}

.edge-cond {
  fill: none;
  stroke: rgba(148, 163, 184, 0.5);
  stroke-dasharray: 5 3;
  stroke-width: 1;
}

.edge-loop {
  fill: none;
  stroke: rgba(100, 116, 139, 0.55);
  stroke-dasharray: 2 3;
  stroke-width: 0.9;
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
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-2);
  font-family: 'SF Mono', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 700;
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
  font-family: 'SF Mono', ui-monospace, monospace;
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
@media (max-width: 1180px) {
  .stage {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .arch-pane {
    min-height: 600px;
  }

  .phases {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .topbar__nav .topbar__link,
  .topbar__divider {
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
    gap: 14px;
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

  .stat-sep:nth-of-type(odd) {
    display: none;
  }
}
</style>
