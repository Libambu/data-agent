<script setup lang="ts">
</script>

<template>
  <router-view v-slot="{ Component }">
    <Transition name="page-fade" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>
</template>

<style>
:root {
  color-scheme: dark;

  /* ===== 主色：靛紫 + 青蓝 ===== */
  --color-primary: #818cf8;          /* indigo-400 */
  --color-primary-strong: #6366f1;   /* indigo-500 */
  --color-primary-deep: #4f46e5;     /* indigo-600 */
  --color-accent: #22d3ee;           /* cyan-400 */
  --color-accent-strong: #06b6d4;    /* cyan-500 */

  /* ===== 阶段语义色（4 大阶段）===== */
  --phase-recall: #22d3ee;     /* cyan   - Phase I  Retrieval */
  --phase-plan: #a78bfa;       /* violet - Phase II Planning  */
  --phase-exec: #fbbf24;       /* amber  - Phase III Execution */
  --phase-output: #34d399;     /* emerald - Phase IV Output    */

  /* ===== 状态色 ===== */
  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-danger: #f87171;
  --color-info: #60a5fa;

  /* ===== 文字 / 表面 ===== */
  --text-1: #e2e8f0;            /* 主文 */
  --text-2: #94a3b8;            /* 次文 */
  --text-3: #64748b;            /* 弱化 */
  --text-inverse: #0f172a;

  --bg-base: #0b1020;
  --bg-1: rgba(255, 255, 255, 0.04);
  --bg-2: rgba(255, 255, 255, 0.06);
  --bg-3: rgba(255, 255, 255, 0.09);
  --bg-elev: rgba(15, 23, 42, 0.72);

  --line-1: rgba(148, 163, 184, 0.14);
  --line-2: rgba(148, 163, 184, 0.22);
  --line-strong: rgba(148, 163, 184, 0.36);

  /* ===== 阴影 ===== */
  --shadow-sm: 0 4px 12px rgba(0, 0, 0, 0.24);
  --shadow-md: 0 12px 36px rgba(0, 0, 0, 0.36);
  --shadow-lg: 0 24px 64px rgba(0, 0, 0, 0.48);
  --shadow-glow: 0 0 0 1px rgba(129, 140, 248, 0.4), 0 16px 48px rgba(99, 102, 241, 0.32);

  /* ===== 圆角 ===== */
  --radius-xs: 8px;
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 22px;
  --radius-xl: 28px;
  --radius-2xl: 34px;
}

/* 全局重置 */
*,
*::before,
*::after {
  box-sizing: border-box;
}

html {
  min-height: 100%;
  background: var(--bg-base);
  scroll-behavior: smooth;
}

body {
  position: relative;
  min-width: 320px;
  min-height: 100vh;
  margin: 0;
  overflow-x: hidden;
  font-family:
    'Inter', 'PingFang SC', 'Hiragino Sans GB', ui-sans-serif, system-ui, -apple-system,
    BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: var(--text-1);
  background:
    /* Aurora 光斑 */
    radial-gradient(1100px 620px at 8% -4%, rgba(99, 102, 241, 0.34), transparent 60%),
    radial-gradient(900px 540px at 96% 6%, rgba(34, 211, 238, 0.22), transparent 58%),
    radial-gradient(720px 480px at 60% 100%, rgba(167, 139, 250, 0.22), transparent 60%),
    linear-gradient(180deg, #08091a 0%, #0b1020 48%, #050816 100%);
  background-attachment: fixed;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 噪点纹理叠加 */
body::before {
  position: fixed;
  z-index: 0;
  inset: 0;
  pointer-events: none;
  content: '';
  background-image:
    radial-gradient(circle at 25% 25%, rgba(255, 255, 255, 0.024) 1px, transparent 1px),
    radial-gradient(circle at 75% 75%, rgba(255, 255, 255, 0.024) 1px, transparent 1px);
  background-size: 22px 22px, 22px 22px;
  background-position: 0 0, 11px 11px;
  opacity: 0.6;
}

body::selection,
::selection {
  color: #ffffff;
  background: var(--color-primary-strong);
}

button,
input,
textarea,
select {
  font: inherit;
}

button {
  -webkit-tap-highlight-color: transparent;
}

#app {
  position: relative;
  z-index: 1;
  min-height: 100vh;
}

a {
  color: inherit;
  text-decoration: none;
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.03);
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(129, 140, 248, 0.6), rgba(34, 211, 238, 0.5));
  border: 2px solid transparent;
  border-radius: 999px;
  background-clip: padding-box;
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(129, 140, 248, 0.85), rgba(34, 211, 238, 0.75));
  background-clip: padding-box;
}

/* ===== Element Plus 深色玻璃态 ===== */
.el-button {
  border-radius: 14px;
  font-weight: 700;
  letter-spacing: 0.005em;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease,
    border-color 0.18s ease;
}

.el-button:hover {
  transform: translateY(-1px);
}

.el-button--primary {
  border: none;
  color: #ffffff;
  background: linear-gradient(135deg, var(--color-primary-strong), var(--color-primary-deep));
  box-shadow: 0 10px 28px rgba(79, 70, 229, 0.46);
}

.el-button--primary:hover,
.el-button--primary:focus {
  background: linear-gradient(135deg, #818cf8, #4f46e5);
  box-shadow: var(--shadow-glow);
}

.el-button--default,
.el-button.is-plain {
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.05);
  border-color: var(--line-2);
  backdrop-filter: blur(12px);
}

.el-button--default:hover,
.el-button.is-plain:hover {
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(129, 140, 248, 0.5);
}

.el-input__wrapper,
.el-textarea__inner,
.el-select__wrapper {
  border-radius: 14px;
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.04);
  box-shadow:
    0 0 0 1px var(--line-2) inset,
    0 6px 18px rgba(0, 0, 0, 0.18);
  transition:
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.el-textarea__inner {
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.04);
}

.el-textarea__inner::placeholder,
.el-input__inner::placeholder {
  color: var(--text-3);
}

.el-input__wrapper.is-focus,
.el-textarea__inner:focus,
.el-select__wrapper.is-focused {
  background: rgba(255, 255, 255, 0.07);
  box-shadow:
    0 0 0 1px rgba(129, 140, 248, 0.7) inset,
    0 0 0 4px rgba(129, 140, 248, 0.18),
    0 12px 28px rgba(0, 0, 0, 0.3);
}

.el-textarea__inner {
  padding: 16px 18px;
  line-height: 1.7;
}

.el-input__inner,
.el-select__placeholder {
  color: var(--text-1) !important;
}

.el-radio__label {
  color: var(--text-2);
}

.el-radio__input.is-checked + .el-radio__label {
  color: var(--text-1);
}

/* select 下拉浮层 */
.el-popper.is-light,
.el-select__popper.el-popper {
  background: rgba(15, 23, 42, 0.95) !important;
  border-color: var(--line-2) !important;
  backdrop-filter: blur(24px);
}

.el-select-dropdown__item {
  color: var(--text-2);
}

.el-select-dropdown__item.is-hovering,
.el-select-dropdown__item.is-selected {
  color: var(--text-1);
  background: rgba(129, 140, 248, 0.16) !important;
}

/* ===== 路由切换动画 ===== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.32s ease,
    transform 0.32s ease,
    filter 0.32s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.995);
  filter: blur(6px);
}

/* ============================================== */
/* ===== 节点卡片：按阶段 4 色相统一重塑      ===== */
/* ============================================== */

/* —— 通用基底 —— */
body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) {
  --node-color: var(--color-primary);
  --node-color-soft: rgba(129, 140, 248, 0.18);

  position: relative;
  overflow: hidden;
  border: 1px solid var(--line-2) !important;
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.015)) !important;
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(18px);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

/* 顶部色带 + 角落光晕 */
body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
)::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background:
    linear-gradient(90deg, var(--node-color), transparent 56%) top / 100% 2px no-repeat,
    radial-gradient(420px 180px at 0% 0%, var(--node-color-soft), transparent 62%);
}

body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
):hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.18) !important;
  box-shadow: var(--shadow-md);
}

/* —— 阶段 I: Retrieval (cyan) —— */
body :is(.evidence-card, .scheme-card, .relation-card) {
  --node-color: var(--phase-recall);
  --node-color-soft: rgba(34, 211, 238, 0.16);
}

/* —— 阶段 II: Planning (violet) —— */
body :is(.feasibility-card, .planner-card, .human-card) {
  --node-color: var(--phase-plan);
  --node-color-soft: rgba(167, 139, 250, 0.18);
}

/* —— 阶段 III: Execution (amber) —— */
body :is(
  .plan-execution-card,
  .sql-generation-card,
  .sql-execution-card,
  .python-generation-card,
  .python-execution-card,
  .python-analysis-card
) {
  --node-color: var(--phase-exec);
  --node-color-soft: rgba(251, 191, 36, 0.18);
}

/* —— 阶段 IV: Output (emerald) —— */
body :is(.report-generation-card) {
  --node-color: var(--phase-output);
  --node-color-soft: rgba(52, 211, 153, 0.18);
}

/* 头部内层 */
body :is(
  .evidence-card__header,
  .feasibility-card__header,
  .human-card__header,
  .plan-execution-card__header,
  .planner-card__header,
  .python-analysis-card__header,
  .python-execution-card__header,
  .python-generation-card__header,
  .report-generation-card__header,
  .scheme-card__header,
  .sql-execution-card__header,
  .sql-generation-card__header,
  .relation-card__header
) {
  position: relative;
  z-index: 1;
}

body :is(
  .evidence-card__eyebrow,
  .feasibility-card__eyebrow,
  .human-card__eyebrow,
  .plan-execution-card__eyebrow,
  .planner-card__eyebrow,
  .python-analysis-card__eyebrow,
  .python-execution-card__eyebrow,
  .python-generation-card__eyebrow,
  .report-generation-card__eyebrow,
  .scheme-card__eyebrow,
  .sql-execution-card__eyebrow,
  .sql-generation-card__eyebrow,
  .relation-card__eyebrow
) {
  color: var(--node-color) !important;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-weight: 800 !important;
}

body :is(
  .evidence-card__title,
  .feasibility-card__title,
  .human-card__title,
  .plan-execution-card__title,
  .planner-card__title,
  .python-analysis-card__title,
  .python-execution-card__title,
  .python-generation-card__title,
  .report-generation-card__title,
  .scheme-card__title,
  .sql-execution-card__title,
  .sql-generation-card__title,
  .relation-card__title
) {
  color: var(--text-1) !important;
  font-size: 17px;
  letter-spacing: -0.012em;
  font-weight: 800 !important;
}

body :is(
  .evidence-card__status,
  .feasibility-card__status,
  .human-card__status,
  .plan-execution-card__status,
  .planner-card__status,
  .python-analysis-card__status,
  .python-execution-card__status,
  .python-generation-card__status,
  .report-generation-card__status,
  .scheme-card__status,
  .sql-execution-card__status,
  .sql-generation-card__status,
  .relation-card__status
) {
  border: 1px solid var(--line-2) !important;
  background: rgba(255, 255, 255, 0.04) !important;
  color: var(--text-2) !important;
  box-shadow: none;
  font-weight: 800;
  letter-spacing: 0.04em;
}

/* 成功态：统一为 emerald */
body :is(
  .evidence-card--success,
  .feasibility-card--success,
  .human-card--success,
  .plan-execution-card--success,
  .planner-card--success,
  .python-analysis-card--success,
  .python-execution-card--success,
  .python-generation-card--success,
  .report-generation-card--success,
  .scheme-card--success,
  .sql-execution-card--success,
  .sql-generation-card--success,
  .relation-card--success
) :is(
  .evidence-card__status,
  .feasibility-card__status,
  .human-card__status,
  .plan-execution-card__status,
  .planner-card__status,
  .python-analysis-card__status,
  .python-execution-card__status,
  .python-generation-card__status,
  .report-generation-card__status,
  .scheme-card__status,
  .sql-execution-card__status,
  .sql-generation-card__status,
  .relation-card__status
) {
  border-color: rgba(52, 211, 153, 0.3) !important;
  background: rgba(52, 211, 153, 0.12) !important;
  color: #6ee7b7 !important;
}

/* 内部分区 */
body :is(
  .evidence-card__section,
  .feasibility-card__section,
  .human-card__section,
  .plan-execution-card__section,
  .planner-card__section,
  .python-analysis-card__section,
  .python-execution-card__section,
  .python-generation-card__section,
  .report-generation-card__section,
  .scheme-card__section,
  .sql-execution-card__section,
  .sql-generation-card__section,
  .relation-card__section,
  .planner-step,
  .scheme-item,
  .relation-item
) {
  position: relative;
  z-index: 1;
  border-color: var(--line-2) !important;
  background: rgba(255, 255, 255, 0.03) !important;
  box-shadow: none;
  color: var(--text-2);
}

/* 通用文字降亮 */
body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) :is(p, span, li, td, th, label, dt, dd) {
  color: var(--text-2);
}

body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) :is(h1, h2, h3, h4, strong, b) {
  color: var(--text-1);
}

/* 代码块与表格 */
body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) pre,
body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) code {
  color: #e0e7ff;
  background: rgba(2, 6, 23, 0.7) !important;
  border-radius: var(--radius-sm);
}

body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) table {
  border-radius: var(--radius-sm);
  overflow: hidden;
}

body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) th {
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: var(--line-2) !important;
}

body :is(
  .evidence-card,
  .feasibility-card,
  .human-card,
  .plan-execution-card,
  .planner-card,
  .python-analysis-card,
  .python-execution-card,
  .python-generation-card,
  .report-generation-card,
  .scheme-card,
  .sql-execution-card,
  .sql-generation-card,
  .relation-card
) td {
  border-color: var(--line-2) !important;
}

/* markdown 内容 */
.md-editor-preview-wrapper,
.md-editor-preview {
  color: var(--text-2) !important;
  background: transparent !important;
}

.md-editor-preview :is(h1, h2, h3, h4, h5, h6, strong) {
  color: var(--text-1) !important;
}

.md-editor-preview a {
  color: var(--color-accent) !important;
}
</style>
