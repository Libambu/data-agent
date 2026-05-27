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
    <nav class="home-nav" :class="{ 'home-nav--visible': isVisible }">
      <div class="home-nav__brand" @click="goToWorkspace">
        <span class="home-nav__logo">
          <el-icon><DataAnalysis /></el-icon>
        </span>
        <span>Data Agent</span>
      </div>
      <div class="home-nav__links">
        <span>Retrieval</span>
        <span>Planning</span>
        <span>Execution</span>
        <el-button size="small" type="primary" @click="goToWorkspace">立即体验</el-button>
      </div>
    </nav>

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

        <div class="hero__stats" aria-label="平台能力指标">
          <div class="hero-stat">
            <strong>4</strong>
            <span>阶段式 DAG</span>
          </div>
          <div class="hero-stat">
            <strong>13</strong>
            <span>智能节点</span>
          </div>
          <div class="hero-stat">
            <strong>SQL + Python</strong>
            <span>双引擎执行</span>
          </div>
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
          <div class="arch-section__badge">Live Workflow Map</div>
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
  position: relative;
  min-height: 100vh;
  padding: 22px clamp(18px, 3.2vw, 48px) 72px;
  overflow: hidden;
  background:
    radial-gradient(circle at 4% 8%, rgba(249, 115, 22, 0.2), transparent 30%),
    radial-gradient(circle at 86% 4%, rgba(37, 99, 235, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(255, 251, 235, 0.9), rgba(248, 250, 252, 0.92) 46%, rgba(239, 246, 255, 0.95));
}

.home-page::before,
.home-page::after {
  position: absolute;
  pointer-events: none;
  content: '';
  border-radius: 999px;
  filter: blur(2px);
}

.home-page::before {
  top: 118px;
  left: -110px;
  width: 260px;
  height: 260px;
  background: rgba(249, 115, 22, 0.1);
}

.home-page::after {
  right: -150px;
  bottom: 6%;
  width: 360px;
  height: 360px;
  background: rgba(37, 99, 235, 0.1);
}

.home-nav {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1440px;
  margin: 0 auto 32px;
  padding: 12px 14px 12px 18px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.07);
  backdrop-filter: blur(20px);
  opacity: 0;
  transform: translateY(-12px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.home-nav--visible {
  opacity: 1;
  transform: translateY(0);
}

.home-nav__brand,
.home-nav__links {
  display: flex;
  align-items: center;
}

.home-nav__brand {
  gap: 10px;
  font-weight: 800;
  color: #0f172a;
  cursor: pointer;
}

.home-nav__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #f97316, #2563eb);
  box-shadow: 0 12px 28px rgba(249, 115, 22, 0.22);
}

.home-nav__links {
  gap: 18px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

/* 顶部双栏布局 */
.top-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(480px, 1.05fr);
  gap: clamp(28px, 4vw, 64px);
  max-width: 1440px;
  margin: 0 auto;
  align-items: center;
  opacity: 0;
  transform: translateY(24px);
  transition: all 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

.top-layout--visible {
  opacity: 1;
  transform: translateY(0);
}

/* Hero */
.hero {
  text-align: left;
}

.hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 15px;
  border: 1px solid rgba(249, 115, 22, 0.22);
  border-radius: 999px;
  margin-bottom: 10px;
  color: #ea580c;
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.96), rgba(239, 246, 255, 0.78));
  box-shadow: 0 10px 28px rgba(249, 115, 22, 0.1);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero__title {
  margin: 14px 0 0;
  font-size: clamp(54px, 7vw, 92px);
  font-weight: 900;
  line-height: 0.94;
  color: #0f172a;
  letter-spacing: -0.07em;
}

.hero__title-sub {
  margin: 14px 0 0;
  font-size: clamp(26px, 3.5vw, 44px);
  font-weight: 900;
  line-height: 1.12;
  background: linear-gradient(135deg, #f97316 0%, #ea580c 36%, #2563eb 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero__desc {
  display: grid;
  gap: 12px;
  max-width: 640px;
  margin: 28px 0 34px;
  padding: 0;
  list-style: none;
  text-align: left;
}

.hero__desc li {
  position: relative;
  padding: 14px 16px 14px 44px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  color: #475569;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.04);
  font-size: 15px;
  line-height: 1.7;
  backdrop-filter: blur(12px);
}

.hero__desc li::before {
  position: absolute;
  top: 18px;
  left: 18px;
  width: 13px;
  height: 13px;
  border: 3px solid rgba(255, 255, 255, 0.88);
  border-radius: 50%;
  background: linear-gradient(135deg, #f97316, #2563eb);
  box-shadow: 0 0 0 4px rgba(249, 115, 22, 0.12);
  content: '';
}

.hero__actions {
  display: flex;
  justify-content: flex-start;
  gap: 14px;
  margin-bottom: 26px;
}

.hero__actions :deep(.el-button) {
  min-width: 148px;
  height: 48px;
}

.hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.hero-stat {
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(16px);
}

.hero-stat strong,
.hero-stat span {
  display: block;
}

.hero-stat strong {
  color: #0f172a;
  font-size: clamp(20px, 2vw, 28px);
  line-height: 1;
}

.hero-stat span {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

/* 特性卡片 */
.features {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.feature-card {
  position: relative;
  overflow: hidden;
  min-height: 168px;
  padding: 24px 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.07);
  text-align: left;
  backdrop-filter: blur(18px);
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.feature-card::after {
  position: absolute;
  right: -44px;
  bottom: -44px;
  width: 118px;
  height: 118px;
  border-radius: 50%;
  background: rgba(249, 115, 22, 0.08);
  content: '';
}

.feature-card:hover {
  transform: translateY(-5px);
  border-color: rgba(249, 115, 22, 0.24);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.12);
}

.feature-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  border-radius: 18px;
  margin-bottom: 18px;
  font-size: 24px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.1);
}

.feature-card__icon--blue { background: linear-gradient(135deg, #dbeafe, #eff6ff); color: #2563eb; }
.feature-card__icon--green { background: linear-gradient(135deg, #dcfce7, #f0fdf4); color: #16a34a; }
.feature-card__icon--orange { background: linear-gradient(135deg, #ffedd5, #fff7ed); color: #f97316; }
.feature-card__icon--purple { background: linear-gradient(135deg, #f3e8ff, #faf5ff); color: #9333ea; }

.feature-card h3 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 17px;
  font-weight: 850;
}

.feature-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

/* ===== 科研风格架构图 ===== */
.arch-section {
  position: relative;
  overflow: hidden;
  padding: clamp(18px, 2.4vw, 30px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 34px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.82)),
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.12), transparent 34%);
  box-shadow: 0 26px 80px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(24px);
}

.arch-section::before {
  position: absolute;
  inset: 12px;
  pointer-events: none;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 26px;
  content: '';
}

.arch-section__header {
  position: relative;
  z-index: 1;
  margin-bottom: 14px;
  text-align: center;
}

.arch-section__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  padding: 6px 12px;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 999px;
  color: #2563eb;
  background: rgba(239, 246, 255, 0.74);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.arch-section__header h2 {
  margin: 0;
  color: #0f172a;
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  font-size: clamp(20px, 2.4vw, 28px);
  font-weight: 900;
  letter-spacing: -0.03em;
}

.arch-section__caption {
  max-width: 620px;
  margin: 10px auto 0;
  color: #64748b;
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  font-size: 12px;
  font-style: normal;
  line-height: 1.7;
}

.arch-section__caption em {
  color: #334155;
  font-style: normal;
  font-weight: 800;
}

/* SVG 图 */
.arch-graph {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  margin: 8px 0;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.58);
}

.arch-svg {
  width: 100%;
  height: auto;
  filter: drop-shadow(0 12px 24px rgba(15, 23, 42, 0.08));
}

/* Phase 分组框 */
.phase-box {
  fill: none;
  stroke-width: 1.2;
}

.phase-box--retrieval {
  stroke: #3b82f6;
  fill: rgba(59, 130, 246, 0.05);
}

.phase-box--planning {
  stroke: #16a34a;
  fill: rgba(22, 163, 74, 0.05);
}

.phase-box--execution {
  stroke: #f97316;
  fill: rgba(249, 115, 22, 0.05);
}

.phase-box--output {
  stroke: #8b5cf6;
  fill: rgba(139, 92, 246, 0.05);
}

.phase-label {
  fill: #334155;
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.02em;
  text-anchor: middle;
}

/* 节点 */
.node-box {
  fill: #ffffff;
  stroke: #334155;
  stroke-width: 1.15;
}

.node-box--sql {
  fill: #eff6ff;
  stroke: #2563eb;
}

.node-box--python {
  fill: #f0fdf4;
  stroke: #16a34a;
}

.node-box--report {
  fill: #faf5ff;
  stroke: #8b5cf6;
}

.node-terminal {
  fill: #0f172a;
  stroke: none;
}

.node-decision {
  fill: #fff7ed;
  stroke: #ea580c;
  stroke-width: 1.15;
}

/* 标签 */
.label-node {
  fill: #0f172a;
  font-family: 'SF Mono', 'Fira Code', Consolas, monospace;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: -0.02em;
  text-anchor: middle;
}

.label-terminal {
  fill: #ffffff;
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  font-size: 11px;
  font-weight: 900;
  text-anchor: middle;
}

.label-decision {
  fill: #7c2d12;
  font-family: 'SF Mono', 'Fira Code', Consolas, monospace;
  font-size: 9.5px;
  font-weight: 700;
  text-anchor: middle;
}

.label-edge {
  fill: #64748b;
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  font-size: 9px;
  font-style: normal;
  font-weight: 800;
  text-anchor: start;
}

.label-edge--vertical {
  writing-mode: tb;
  text-anchor: start;
}

/* 边 */
.edge {
  fill: none;
  stroke: #334155;
  stroke-width: 1.2;
}

.edge-cond {
  fill: none;
  stroke: #64748b;
  stroke-dasharray: 5 3;
  stroke-width: 0.95;
}

.edge-loop {
  fill: none;
  stroke: #94a3b8;
  stroke-dasharray: 2 2;
  stroke-width: 0.85;
}

/* 图例 */
.arch-legend {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.arch-legend__item {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 9px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 999px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.72);
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  font-size: 10px;
  font-weight: 700;
}

@media (max-width: 1180px) {
  .top-layout {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .hero {
    max-width: 860px;
    margin: 0 auto;
    text-align: center;
  }

  .hero__desc {
    margin-right: auto;
    margin-left: auto;
  }

  .hero__actions {
    justify-content: center;
  }

  .features {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 860px) {
  .home-page {
    padding: 16px 16px 48px;
  }

  .home-nav {
    border-radius: 24px;
  }

  .home-nav__links span {
    display: none;
  }

  .hero__stats,
  .features {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .arch-section {
    border-radius: 26px;
    padding: 16px;
  }

  .arch-graph {
    overflow-x: auto;
    justify-content: flex-start;
  }

  .arch-svg {
    min-width: 520px;
  }
}

@media (max-width: 560px) {
  .hero__actions,
  .hero__stats,
  .features {
    grid-template-columns: 1fr;
  }

  .hero__actions {
    flex-direction: column;
  }

  .hero__actions :deep(.el-button) {
    width: 100%;
  }

  .hero__title {
    font-size: clamp(46px, 17vw, 66px);
  }

  .hero__desc li {
    padding-right: 14px;
  }
}
</style>
