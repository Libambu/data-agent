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
  color-scheme: light;

  /* ===== 主色：Google Blue ===== */
  --color-primary: #1a73e8;          /* Google Blue */
  --color-primary-strong: #1967d2;   /* Hover */
  --color-primary-deep: #185abc;     /* Pressed */
  --color-accent: #9b72cb;           /* Gemini Purple */
  --color-accent-strong: #7c3fb6;

  /* ===== Gemini 阶段色（4 大阶段）===== */
  --phase-recall: #1a73e8;     /* Blue   - Phase I  Retrieval */
  --phase-plan: #9b72cb;       /* Purple - Phase II Planning  */
  --phase-exec: #f9ab00;       /* Amber  - Phase III Execution */
  --phase-output: #d96570;     /* Coral  - Phase IV Output    */

  /* ===== 状态色 ===== */
  --color-success: #1e8e3e;
  --color-warning: #f9ab00;
  --color-danger: #d93025;
  --color-info: #1a73e8;

  /* ===== 文字 / 表面 ===== */
  --text-1: #202124;            /* 主文 */
  --text-2: #5f6368;            /* 次文 */
  --text-3: #80868b;            /* 弱化 */
  --text-inverse: #ffffff;

  --bg-base: #ffffff;
  --bg-1: #f8f9fa;              /* surface-1 */
  --bg-2: #f1f3f4;              /* surface-2 */
  --bg-3: #e8eaed;              /* surface-3 */
  --bg-elev: #ffffff;           /* 弹层 */

  --line-1: #f1f3f4;
  --line-2: #e8eaed;
  --line-strong: #dadce0;

  /* ===== 阴影（轻、克制） ===== */
  --shadow-sm: 0 1px 2px rgba(60, 64, 67, 0.08), 0 1px 3px rgba(60, 64, 67, 0.04);
  --shadow-md: 0 1px 3px rgba(60, 64, 67, 0.12), 0 4px 8px rgba(60, 64, 67, 0.08);
  --shadow-lg: 0 4px 12px rgba(60, 64, 67, 0.14), 0 10px 24px rgba(60, 64, 67, 0.1);
  --shadow-glow: 0 0 0 3px rgba(26, 115, 232, 0.18), 0 6px 18px rgba(26, 115, 232, 0.22);

  /* ===== 圆角 ===== */
  --radius-xs: 6px;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 22px;
  --radius-2xl: 28px;
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
    'Google Sans', 'Google Sans Text', 'Inter', 'PingFang SC', 'Hiragino Sans GB',
    ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: var(--text-1);
  background:
    /* Gemini 三色淡晕：蓝→紫→橙 */
    radial-gradient(900px 540px at 6% -2%, rgba(26, 115, 232, 0.10), transparent 60%),
    radial-gradient(820px 480px at 96% 4%, rgba(155, 114, 203, 0.10), transparent 58%),
    radial-gradient(720px 460px at 60% 100%, rgba(249, 171, 0, 0.06), transparent 60%),
    linear-gradient(180deg, #ffffff 0%, #f8f9fa 60%, #ffffff 100%);
  background-attachment: fixed;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 噪点纹理叠加（极淡） */
body::before {
  position: fixed;
  z-index: 0;
  inset: 0;
  pointer-events: none;
  content: '';
  background-image:
    radial-gradient(circle at 25% 25%, rgba(60, 64, 67, 0.018) 1px, transparent 1px),
    radial-gradient(circle at 75% 75%, rgba(60, 64, 67, 0.018) 1px, transparent 1px);
  background-size: 24px 24px, 24px 24px;
  background-position: 0 0, 12px 12px;
  opacity: 0.5;
}

body::selection,
::selection {
  color: #ffffff;
  background: var(--color-primary);
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
  width: 12px;
  height: 12px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #dadce0;
  border: 3px solid transparent;
  border-radius: 999px;
  background-clip: padding-box;
}

::-webkit-scrollbar-thumb:hover {
  background: #bdc1c6;
  background-clip: padding-box;
}

/* ===== Element Plus 浅色 Material 风 ===== */
.el-button {
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease,
    border-color 0.18s ease;
}

.el-button:hover {
  transform: none;
}

.el-button--primary {
  border: none;
  color: #ffffff;
  background: var(--color-primary);
  box-shadow: 0 1px 2px rgba(26, 115, 232, 0.28), 0 1px 3px rgba(26, 115, 232, 0.18);
}

.el-button--primary:hover,
.el-button--primary:focus {
  background: var(--color-primary-strong);
  box-shadow: 0 1px 3px rgba(26, 115, 232, 0.4), 0 4px 10px rgba(26, 115, 232, 0.24);
}

.el-button--default,
.el-button.is-plain {
  color: var(--color-primary);
  background: #ffffff;
  border-color: var(--line-strong);
}

.el-button--default:hover,
.el-button.is-plain:hover {
  color: var(--color-primary);
  background: rgba(26, 115, 232, 0.06);
  border-color: var(--color-primary);
}

.el-input__wrapper,
.el-textarea__inner,
.el-select__wrapper {
  border-radius: 8px;
  color: var(--text-1);
  background: #ffffff;
  box-shadow: 0 0 0 1px var(--line-strong) inset;
  transition:
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.el-textarea__inner {
  color: var(--text-1);
  background: #ffffff;
}

.el-textarea__inner::placeholder,
.el-input__inner::placeholder {
  color: var(--text-3);
}

.el-input__wrapper.is-focus,
.el-textarea__inner:focus,
.el-select__wrapper.is-focused {
  background: #ffffff;
  box-shadow:
    0 0 0 2px var(--color-primary) inset,
    0 1px 3px rgba(26, 115, 232, 0.18);
}

.el-textarea__inner {
  padding: 14px 16px;
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
  color: var(--color-primary);
}

/* select 下拉浮层 */
.el-popper.is-light,
.el-select__popper.el-popper {
  background: #ffffff !important;
  border-color: var(--line-strong) !important;
  box-shadow: var(--shadow-md) !important;
  backdrop-filter: none;
}

.el-select-dropdown__item {
  color: var(--text-2);
}

.el-select-dropdown__item.is-hovering,
.el-select-dropdown__item.is-selected {
  color: var(--color-primary);
  background: rgba(26, 115, 232, 0.08) !important;
}

/* ===== 路由切换动画 ===== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.32s ease,
    transform 0.32s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* ============================================== */
/* ===== 节点卡片：Material You 浅色卡片重塑 ===== */
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
  --node-color-soft: rgba(26, 115, 232, 0.08);

  position: relative;
  overflow: hidden;
  border: 1px solid var(--line-2) !important;
  border-radius: var(--radius-md);
  background: #ffffff !important;
  box-shadow: var(--shadow-sm);
  backdrop-filter: none;
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

/* 顶部色带 + 角落淡晕 */
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
    linear-gradient(90deg, var(--node-color), transparent 56%) top / 100% 3px no-repeat,
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
  transform: none;
  border-color: var(--line-strong) !important;
  box-shadow: var(--shadow-md);
}

/* —— 阶段 I: Retrieval (Blue) —— */
body :is(.evidence-card, .scheme-card, .relation-card) {
  --node-color: var(--phase-recall);
  --node-color-soft: rgba(26, 115, 232, 0.08);
}

/* —— 阶段 II: Planning (Purple) —— */
body :is(.feasibility-card, .planner-card, .human-card) {
  --node-color: var(--phase-plan);
  --node-color-soft: rgba(155, 114, 203, 0.10);
}

/* —— 阶段 III: Execution (Amber) —— */
body :is(
  .plan-execution-card,
  .sql-generation-card,
  .sql-execution-card,
  .python-generation-card,
  .python-execution-card,
  .python-analysis-card
) {
  --node-color: var(--phase-exec);
  --node-color-soft: rgba(249, 171, 0, 0.10);
}

/* —— 阶段 IV: Output (Coral) —— */
body :is(.report-generation-card) {
  --node-color: var(--phase-output);
  --node-color-soft: rgba(217, 101, 112, 0.10);
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
  letter-spacing: 0.08em;
  font-weight: 700 !important;
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
  font-size: 16px;
  letter-spacing: -0.01em;
  font-weight: 700 !important;
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
  border: 1px solid var(--line-strong) !important;
  background: var(--bg-1) !important;
  color: var(--text-2) !important;
  box-shadow: none;
  font-weight: 700;
  letter-spacing: 0.04em;
}

/* 成功态：统一为 Google Green */
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
  border-color: rgba(30, 142, 62, 0.4) !important;
  background: rgba(30, 142, 62, 0.10) !important;
  color: #1e8e3e !important;
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
  background: var(--bg-1) !important;
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
  color: #202124;
  background: #f1f3f4 !important;
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
  background: var(--bg-2) !important;
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
  color: var(--color-primary) !important;
}
</style>
