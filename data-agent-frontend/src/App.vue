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
  --color-primary: #f97316;
  --color-primary-dark: #ea580c;
  --color-primary-soft: #fff7ed;
  --color-accent: #2563eb;
  --color-success: #16a34a;
  --color-warning: #d97706;
  --color-danger: #dc2626;
  --color-ink: #0f172a;
  --color-muted: #64748b;
  --color-subtle: #94a3b8;
  --color-line: rgba(148, 163, 184, 0.24);
  --surface-card: rgba(255, 255, 255, 0.88);
  --surface-card-solid: #ffffff;
  --shadow-sm: 0 10px 30px rgba(15, 23, 42, 0.08);
  --shadow-md: 0 20px 60px rgba(15, 23, 42, 0.12);
  --shadow-glow: 0 18px 60px rgba(249, 115, 22, 0.22);
  --radius-sm: 12px;
  --radius-md: 18px;
  --radius-lg: 28px;
  --radius-xl: 36px;
}

/* 全局重置 */
*,
*::before,
*::after {
  box-sizing: border-box;
}

html {
  min-height: 100%;
  background: #f8fafc;
  scroll-behavior: smooth;
}

body {
  min-width: 320px;
  min-height: 100vh;
  margin: 0;
  overflow-x: hidden;
  font-family:
    Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  color: var(--color-ink);
  background:
    radial-gradient(circle at 8% -10%, rgba(249, 115, 22, 0.16), transparent 34%),
    radial-gradient(circle at 92% 0%, rgba(37, 99, 235, 0.12), transparent 30%),
    linear-gradient(180deg, #fffaf4 0%, #f8fafc 46%, #eef4ff 100%);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

body::selection {
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
  background: rgba(226, 232, 240, 0.6);
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(249, 115, 22, 0.8), rgba(37, 99, 235, 0.66));
  border: 2px solid rgba(248, 250, 252, 0.85);
  border-radius: 999px;
}

.el-button {
  border-radius: 999px;
  font-weight: 700;
  letter-spacing: 0.01em;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.el-button:hover {
  transform: translateY(-1px);
}

.el-button--primary {
  border: none;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  box-shadow: 0 14px 32px rgba(249, 115, 22, 0.24);
}

.el-button--primary:hover,
.el-button--primary:focus {
  background: linear-gradient(135deg, #fb923c, var(--color-primary));
  box-shadow: var(--shadow-glow);
}

.el-input__wrapper,
.el-textarea__inner,
.el-select__wrapper {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 0 0 1px rgba(148, 163, 184, 0.22) inset,
    0 12px 28px rgba(15, 23, 42, 0.04);
  transition:
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.el-input__wrapper.is-focus,
.el-textarea__inner:focus,
.el-select__wrapper.is-focused {
  background: #ffffff;
  box-shadow:
    0 0 0 1px rgba(249, 115, 22, 0.52) inset,
    0 0 0 4px rgba(249, 115, 22, 0.1),
    0 16px 36px rgba(15, 23, 42, 0.08);
}

.el-textarea__inner {
  padding: 16px 18px;
  line-height: 1.7;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.28s ease,
    filter 0.28s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.99);
  filter: blur(4px);
}

body .timeline-panel :is(
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
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18) !important;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.88)),
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.13), transparent 34%) !important;
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.08);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

body .timeline-panel :is(
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
    linear-gradient(90deg, rgba(249, 115, 22, 0.34), rgba(37, 99, 235, 0.22), transparent 58%) top / 100% 3px no-repeat,
    radial-gradient(circle at 92% 12%, rgba(37, 99, 235, 0.08), transparent 20%);
}

body .timeline-panel :is(
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
  border-color: rgba(249, 115, 22, 0.26) !important;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.12);
}

body .timeline-panel :is(
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

body .timeline-panel :is(
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
  color: var(--color-primary-dark) !important;
}

body .timeline-panel :is(
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
  font-size: 17px;
  letter-spacing: -0.01em;
}

body .timeline-panel :is(
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
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.78) !important;
  color: var(--color-muted) !important;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

body .timeline-panel :is(
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
  border-color: rgba(22, 163, 74, 0.22);
  background: rgba(240, 253, 244, 0.86) !important;
  color: #15803d !important;
}

body .timeline-panel :is(
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
  border-color: rgba(148, 163, 184, 0.18) !important;
  background: rgba(255, 255, 255, 0.76) !important;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.04);
}

body .timeline-panel pre {
  overflow-x: auto;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
}

body .timeline-panel table {
  border-radius: 12px;
  overflow: hidden;
}

body .timeline-panel th {
  background: linear-gradient(180deg, #f8fafc, #eef2ff) !important;
}
</style>
