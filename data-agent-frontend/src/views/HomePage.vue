<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, onMounted, nextTick } from 'vue'

const router = useRouter()

const goToWorkspace = () => {
  router.push('/workspace')
}

// 动画相关
const isVisible = ref(false)
onMounted(() => {
  nextTick(() => {
    isVisible.value = true
  })
})
</script>

<template>
  <main class="home-page">
    <!-- 顶部双栏布局 -->
    <div class="top-layout" :class="{ 'top-layout--visible': isVisible }">
      <!-- 左侧 Hero 区域 -->
      <section class="hero">
        <div class="hero__badge">
          <el-icon><DataAnalysis /></el-icon>
          <span>Intelligent Data Analysis</span>
        </div>
        <h1 class="hero__title">Data Agent</h1>
        <h2 class="hero__title-sub">智能数据分析平台</h2>
        <ul class="hero__desc">
          <li>基于多节点协作的 AI Agent 架构，自动完成从需求理解、SQL/Python 代码生成到数据分析报告的全流程</li>
          <li>支持自然语言输入，智能规划执行路径</li>
          <li>实时可视化节点执行轨迹，全程可观测</li>
        </ul>
        <div class="hero__actions">
          <el-button type="primary" size="large" @click="goToWorkspace">
            <el-icon><Promotion /></el-icon>
            开始分析
          </el-button>
          <el-button size="large" plain @click="goToWorkspace">
            查看执行轨迹
          </el-button>
        </div>

        <!-- 特性卡片 -->
        <div class="features">
          <div class="feature-card">
            <div class="feature-card__icon feature-card__icon--blue">
              <el-icon><Search /></el-icon>
            </div>
            <h3>智能召回</h3>
            <p>自动召回相关证据、表结构和表关系</p>
          </div>
          <div class="feature-card">
            <div class="feature-card__icon feature-card__icon--green">
              <el-icon><SetUp /></el-icon>
            </div>
            <h3>自动规划</h3>
            <p>AI 自动拆解任务并规划执行步骤</p>
          </div>
          <div class="feature-card">
            <div class="feature-card__icon feature-card__icon--orange">
              <el-icon><Monitor /></el-icon>
            </div>
            <h3>多引擎执行</h3>
            <p>支持 SQL 和 Python 双引擎数据处理</p>
          </div>
          <div class="feature-card">
            <div class="feature-card__icon feature-card__icon--purple">
              <el-icon><Document /></el-icon>
            </div>
            <h3>报告生成</h3>
            <p>自动汇总分析结果生成 Markdown 报告</p>
          </div>
        </div>
      </section>

      <!-- 右侧架构图 -->
      <section class="arch-section">
        <div class="arch-section__header">
          <h2>System Architecture</h2>
          <p class="arch-section__caption">
            Figure 1. DAG architecture of Data Agent with four phases:
            <em>Retrieval</em>, <em>Planning</em>, <em>Execution</em>, and <em>Output</em>.
          </p>
        </div>

      <div class="arch-graph">
        <svg viewBox="0 0 520 680" class="arch-svg">
          <defs>
            <marker id="arrow" markerWidth="6" markerHeight="5" refX="6" refY="2.5" orient="auto">
              <polygon points="0 0, 6 2.5, 0 5" fill="#1a1a1a" />
            </marker>
            <marker id="arrow-cond" markerWidth="6" markerHeight="5" refX="6" refY="2.5" orient="auto">
              <polygon points="0 0, 6 2.5, 0 5" fill="#999" />
            </marker>
            <marker id="arrow-loop" markerWidth="5" markerHeight="4" refX="5" refY="2" orient="auto">
              <polygon points="0 0, 5 2, 0 4" fill="#aaa" />
            </marker>
          </defs>

          <!-- ===== Phase 分组背景 ===== -->
          <rect x="155" y="18" width="210" height="175" rx="3" class="phase-box phase-box--retrieval" />
          <text x="260" y="34" class="phase-label">Phase I: Retrieval</text>

          <rect x="155" y="208" width="210" height="162" rx="3" class="phase-box phase-box--planning" />
          <text x="260" y="224" class="phase-label">Phase II: Planning</text>

          <rect x="55" y="385" width="410" height="175" rx="3" class="phase-box phase-box--execution" />
          <text x="260" y="401" class="phase-label">Phase III: Execution</text>

          <rect x="355" y="575" width="110" height="50" rx="3" class="phase-box phase-box--output" />
          <text x="410" y="590" class="phase-label">Phase IV: Output</text>

          <!-- ===== Phase I 节点 ===== -->
          <circle cx="260" cy="55" r="11" class="node-terminal" />
          <text x="260" y="59" class="label-terminal">S</text>

          <rect x="205" y="78" width="110" height="26" rx="2" class="node-box" />
          <text x="260" y="95" class="label-node">Evidence Recall</text>

          <rect x="205" y="116" width="110" height="26" rx="2" class="node-box" />
          <text x="260" y="133" class="label-node">Schema Recall</text>

          <rect x="205" y="154" width="110" height="26" rx="2" class="node-box" />
          <text x="260" y="171" class="label-node">Table Relation</text>

          <!-- ===== Phase II 节点 ===== -->
          <polygon points="260,230 295,248 260,266 225,248" class="node-decision" />
          <text x="260" y="252" class="label-decision">Feasibility</text>

          <rect x="205" y="286" width="110" height="26" rx="2" class="node-box" />
          <text x="260" y="303" class="label-node">Planner</text>

          <polygon points="260,332 295,350 260,368 225,350" class="node-decision" />
          <text x="260" y="354" class="label-decision">Human</text>

          <!-- ===== Phase III 节点 ===== -->
          <polygon points="260,415 295,433 260,451 225,433" class="node-decision" />
          <text x="260" y="437" class="label-decision">Dispatch</text>

          <!-- SQL 分支 -->
          <rect x="70" y="475" width="90" height="26" rx="2" class="node-box node-box--sql" />
          <text x="115" y="492" class="label-node">SQL Gen</text>

          <rect x="70" y="515" width="90" height="26" rx="2" class="node-box node-box--sql" />
          <text x="115" y="532" class="label-node">SQL Exec</text>

          <!-- Python 分支 -->
          <rect x="215" y="475" width="90" height="26" rx="2" class="node-box node-box--python" />
          <text x="260" y="492" class="label-node">Python Gen</text>

          <rect x="215" y="515" width="90" height="26" rx="2" class="node-box node-box--python" />
          <text x="260" y="532" class="label-node">Python Exec</text>

          <!-- Report 分支 -->
          <rect x="360" y="475" width="90" height="26" rx="2" class="node-box node-box--report" />
          <text x="405" y="492" class="label-node">Report Gen</text>

          <!-- ===== Phase IV ===== -->
          <circle cx="410" cy="600" r="11" class="node-terminal" />
          <text x="410" y="604" class="label-terminal">E</text>

          <!-- ===== 连线 ===== -->

          <!-- Phase I 内部：垂直顺序 -->
          <line x1="260" y1="66" x2="260" y2="78" class="edge" marker-end="url(#arrow)" />
          <line x1="260" y1="104" x2="260" y2="116" class="edge" marker-end="url(#arrow)" />
          <line x1="260" y1="142" x2="260" y2="154" class="edge" marker-end="url(#arrow)" />

          <!-- Phase I -> Phase II -->
          <line x1="260" y1="180" x2="260" y2="230" class="edge" marker-end="url(#arrow)" />

          <!-- Feasibility -> Planner (feasible) -->
          <line x1="260" y1="266" x2="260" y2="286" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="266" y="280" class="label-edge">yes</text>

          <!-- Feasibility -> END (infeasible): 右侧外绕 -->
          <path d="M 295 248 L 480 248 L 480 600 L 421 600" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="484" y="320" class="label-edge">no</text>

          <!-- Planner -> Human -->
          <line x1="260" y1="312" x2="260" y2="332" class="edge" marker-end="url(#arrow)" />

          <!-- Human -> Dispatch (approved) -->
          <line x1="260" y1="368" x2="260" y2="415" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="266" y="395" class="label-edge">ok</text>

          <!-- Human -> Planner (revise): 左侧短回边 -->
          <path d="M 225 350 L 190 350 L 190 299 L 205 299" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="168" y="328" class="label-edge">redo</text>

          <!-- Human -> END (cancel): 与 infeasible 共用右侧通道但偏移 -->
          <path d="M 295 350 L 470 350 L 470 600 L 421 600" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="474" y="420" class="label-edge">cancel</text>

          <!-- Dispatch -> SQL Gen -->
          <path d="M 225 433 L 115 433 L 115 475" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="148" y="428" class="label-edge">sql</text>

          <!-- Dispatch -> Python Gen -->
          <line x1="260" y1="451" x2="260" y2="475" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="266" y="467" class="label-edge">py</text>

          <!-- Dispatch -> Report Gen -->
          <path d="M 295 433 L 405 433 L 405 475" class="edge-cond" marker-end="url(#arrow-cond)" />
          <text x="340" y="428" class="label-edge">report</text>

          <!-- SQL Gen -> SQL Exec -->
          <line x1="115" y1="501" x2="115" y2="515" class="edge" marker-end="url(#arrow)" />

          <!-- SQL Exec -> Dispatch (回边) -->
          <path d="M 70 528 L 55 528 L 55 433 L 225 433" class="edge-loop" marker-end="url(#arrow-loop)" />

          <!-- Python Gen -> Python Exec -->
          <line x1="260" y1="501" x2="260" y2="515" class="edge" marker-end="url(#arrow)" />

          <!-- Python Exec -> Dispatch (回边) -->
          <path d="M 305 528 L 320 528 L 320 460 L 260 460 L 260 451" class="edge-loop" marker-end="url(#arrow-loop)" />

          <!-- Report Gen -> END -->
          <line x1="405" y1="501" x2="405" y2="589" class="edge" marker-end="url(#arrow)" />
        </svg>
      </div>

        <!-- 图例 -->
        <div class="arch-legend">
          <div class="arch-legend__item">
            <svg width="20" height="14"><rect x="2" y="2" width="16" height="10" rx="2" fill="none" stroke="#1a1a1a" stroke-width="1.2"/></svg>
            <span>Process</span>
          </div>
          <div class="arch-legend__item">
            <svg width="20" height="14"><polygon points="10,1 19,7 10,13 1,7" fill="none" stroke="#1a1a1a" stroke-width="1.2"/></svg>
            <span>Decision</span>
          </div>
          <div class="arch-legend__item">
            <svg width="20" height="14"><line x1="2" y1="7" x2="18" y2="7" stroke="#1a1a1a" stroke-width="1.2"/></svg>
            <span>Flow</span>
          </div>
          <div class="arch-legend__item">
            <svg width="20" height="14"><line x1="2" y1="7" x2="18" y2="7" stroke="#666" stroke-width="1.2" stroke-dasharray="3 2"/></svg>
            <span>Conditional</span>
          </div>
          <div class="arch-legend__item">
            <svg width="20" height="14"><path d="M 2 7 C 6 2, 14 2, 18 7" fill="none" stroke="#1a1a1a" stroke-width="1.2"/></svg>
            <span>Loop-back</span>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 40px 40px 64px;
  background: #fafafa;
}

/* 顶部双栏布局 */
.top-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  max-width: 1400px;
  margin: 0 auto;
  align-items: center;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.top-layout--visible {
  opacity: 1;
  transform: translateY(0);
}

/* Hero */
.hero {
  text-align: center;
}

.hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, #fff7ed, #fef3c7);
  border: 1px solid rgba(249, 115, 22, 0.2);
  font-size: 12px;
  font-weight: 600;
  color: #ea580c;
  letter-spacing: 0.04em;
  margin-bottom: 4px;
}

.hero__title {
  margin: 12px 0 0;
  font-size: clamp(42px, 5vw, 58px);
  font-weight: 800;
  line-height: 1;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.hero__title-sub {
  margin: 10px 0 0;
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 700;
  line-height: 1.2;
  background: linear-gradient(135deg, #f97316, #ea580c, #dc2626);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero__desc {
  margin: 24px auto 32px;
  padding: 0;
  list-style: none;
  display: inline-block;
  text-align: left;
}

.hero__desc li {
  position: relative;
  padding-left: 20px;
  margin-bottom: 12px;
  font-size: 15px;
  line-height: 1.7;
  color: #475569;
}

.hero__desc li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f97316, #ea580c);
}

.hero__actions {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 40px;
}

/* 特性卡片 */
.features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-top: 0;
}

.feature-card {
  padding: 24px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(15, 23, 42, 0.08);
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.08);
}

.feature-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 22px;
  margin-bottom: 14px;
}

.feature-card__icon--blue { background: #eff6ff; color: #3b82f6; }
.feature-card__icon--green { background: #f0fdf4; color: #22c55e; }
.feature-card__icon--orange { background: #fff7ed; color: #f97316; }
.feature-card__icon--purple { background: #faf5ff; color: #a855f7; }

.feature-card h3 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.feature-card p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

/* ===== 科研风格架构图 ===== */
.arch-section {
  padding: 24px 20px 20px;
  background: #ffffff;
  border: 1px solid #d4d4d4;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.arch-section__header {
  text-align: center;
  margin-bottom: 12px;
}

.arch-section__header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  font-family: 'Georgia', 'Times New Roman', serif;
  letter-spacing: 0.02em;
}

.arch-section__caption {
  margin: 8px auto 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: #555;
  font-family: 'Georgia', 'Times New Roman', serif;
  font-style: normal;
}

.arch-section__caption em {
  font-style: italic;
  font-weight: 600;
}

/* SVG 图 */
.arch-graph {
  display: flex;
  justify-content: center;
  margin: 4px 0;
}

.arch-svg {
  width: 100%;
  height: auto;
}

/* Phase 分组框 */
.phase-box {
  fill: none;
  stroke-width: 1;
}

.phase-box--retrieval {
  stroke: #4a90d9;
  fill: rgba(74, 144, 217, 0.03);
}

.phase-box--planning {
  stroke: #5ba85b;
  fill: rgba(91, 168, 91, 0.03);
}

.phase-box--execution {
  stroke: #d4822d;
  fill: rgba(212, 130, 45, 0.03);
}

.phase-box--output {
  stroke: #8b5fc7;
  fill: rgba(139, 95, 199, 0.03);
}

.phase-label {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 11px;
  font-weight: 600;
  fill: #444;
  text-anchor: middle;
  letter-spacing: 0.03em;
}

/* 节点 */
.node-box {
  fill: #ffffff;
  stroke: #1a1a1a;
  stroke-width: 1.2;
}

.node-box--sql {
  fill: #f0f7ff;
  stroke: #4a90d9;
}

.node-box--python {
  fill: #f0fff4;
  stroke: #5ba85b;
}

.node-box--report {
  fill: #faf5ff;
  stroke: #8b5fc7;
}

.node-terminal {
  fill: #1a1a1a;
  stroke: none;
}

.node-decision {
  fill: #fffef5;
  stroke: #1a1a1a;
  stroke-width: 1.2;
}

/* 标签 */
.label-node {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 10px;
  font-weight: 500;
  fill: #1a1a1a;
  text-anchor: middle;
  letter-spacing: -0.02em;
}

.label-terminal {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 11px;
  font-weight: 700;
  fill: #ffffff;
  text-anchor: middle;
}

.label-decision {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 9.5px;
  font-weight: 500;
  fill: #333;
  text-anchor: middle;
}

.label-edge {
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 9px;
  font-style: italic;
  fill: #666;
  text-anchor: start;
}

.label-edge--vertical {
  writing-mode: tb;
  text-anchor: start;
}

/* 边 */
.edge {
  stroke: #1a1a1a;
  stroke-width: 1.2;
  fill: none;
}

.edge-cond {
  stroke: #888;
  stroke-width: 0.9;
  stroke-dasharray: 5 3;
  fill: none;
}

.edge-loop {
  stroke: #aaa;
  stroke-width: 0.8;
  stroke-dasharray: 2 2;
  fill: none;
}

/* 图例 */
.arch-legend {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #e5e5e5;
}

.arch-legend__item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: 'Georgia', 'Times New Roman', serif;
  font-size: 10px;
  color: #555;
}

@media (max-width: 1024px) {
  .top-layout {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .hero {
    text-align: center;
  }

  .hero__subtitle {
    margin: 0 auto 32px;
  }

  .hero__actions {
    justify-content: center;
  }

  .features {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 24px 16px 48px;
  }

  .features {
    grid-template-columns: repeat(2, 1fr);
  }

  .arch-section {
    padding: 16px 12px;
  }
}

@media (max-width: 480px) {
  .features {
    grid-template-columns: 1fr;
  }
}
</style>
