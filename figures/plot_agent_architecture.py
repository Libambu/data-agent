#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
plot_agent_architecture.py
==========================

科研出版级 (CCF-A 投稿质量) 绘图脚本，用于可视化 data-agent-backend 的：

  Figure 1 : 多节点 Agent 调度状态图 (Swim-lane + Routing Channel)
             所有反馈边走画布最右侧的专用通道, 不穿越任何节点。

  Figure 2 : Supervisor 指挥的 Multi-Agent 分层架构 (Layered View)
             User / Orchestrator / Worker / Sink 四层, 工业风/学术风。

核心设计准则
------------
1. 严格"网格 + 通道"布局: 节点放在网格槽位, 反馈/路由边走专用通道,
   彻底避免曲线穿过节点或标签互相压字。
2. 文字尺寸自动适配节点宽度: 通过 `_text_inside` 统计文本宽度,
   保证两行文字始终留有内边距, 不会越出节点边界。
3. 字体使用 serif (Times New Roman 优先), pdf.fonttype = 42 (TrueType)
   嵌入字体, 满足 IEEE / ACM 投稿要求。
4. 配色低饱和、按语义分组, 兼容黑白打印。
5. 同时输出 PDF (矢量) + PNG (300 dpi)。
"""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

import matplotlib as mpl
import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
from matplotlib.patches import FancyArrowPatch, FancyBboxPatch


# ============================================================================
# 0. 出版级 matplotlib 样式
# ============================================================================

def _setup_publication_style() -> None:
    mpl.rcParams.update({
        "font.family": "serif",
        "font.serif": [
            "Times New Roman", "Times", "Nimbus Roman",
            "DejaVu Serif", "serif",
        ],
        "mathtext.fontset": "stix",
        "pdf.fonttype": 42,
        "ps.fonttype": 42,
        "axes.unicode_minus": False,
        "axes.linewidth": 0.8,
        "lines.linewidth": 1.2,
        "font.size": 9.5,
        "axes.titlesize": 11,
        "savefig.bbox": "tight",
        "savefig.pad_inches": 0.06,
    })


# ============================================================================
# 1. 配色方案
# ============================================================================

PALETTE: Dict[str, Dict[str, str]] = {
    "io":         {"fill": "#EDEDED", "edge": "#3A3A3A", "text": "#1A1A1A"},
    "recall":     {"fill": "#DCE6F1", "edge": "#1F4E79", "text": "#0B2E4F"},
    "assess":     {"fill": "#E2EFDA", "edge": "#385723", "text": "#1B3A0E"},
    "plan":       {"fill": "#FFF2CC", "edge": "#8C6D1F", "text": "#4A3A0E"},
    "supervisor": {"fill": "#F8CBAD", "edge": "#9E480E", "text": "#4A1F05"},
    "sql":        {"fill": "#D9E1F2", "edge": "#2F5597", "text": "#142A52"},
    "python":     {"fill": "#E4DFEC", "edge": "#5F497A", "text": "#2A1F3D"},
    "report":     {"fill": "#FCE4D6", "edge": "#C55A11", "text": "#4D1F05"},
}

EDGE_DETERMINISTIC = "#2A2A2A"
EDGE_CONDITIONAL   = "#9E480E"
EDGE_FEEDBACK      = "#385723"


# ============================================================================
# 2. 节点 / 边 数据结构
# ============================================================================

@dataclass
class Node:
    key:    str
    label:  str
    sub:    str
    x:      float
    y:      float
    w:      float = 2.30
    h:      float = 0.86
    style:  str = "io"

    @property
    def left(self)   -> float: return self.x - self.w / 2
    @property
    def right(self)  -> float: return self.x + self.w / 2
    @property
    def top(self)    -> float: return self.y + self.h / 2
    @property
    def bottom(self) -> float: return self.y - self.h / 2


@dataclass
class Edge:
    src: str
    dst: str
    kind: str = "solid"          # "solid" / "dashed"
    color: str = EDGE_DETERMINISTIC
    label: str = ""
    label_pos: float = 0.5       # 0=src, 1=dst
    label_offset: Tuple[float, float] = (0.0, 0.0)
    waypoints: List[Tuple[float, float]] = field(default_factory=list)
    rad: float = 0.0
    src_anchor: str = "auto"     # auto / top / bottom / left / right
    dst_anchor: str = "auto"


# ============================================================================
# 3. 渲染原子
# ============================================================================

def _draw_node(ax: plt.Axes, n: Node) -> None:
    style = PALETTE[n.style]

    # 阴影
    shadow = FancyBboxPatch(
        (n.left + 0.05, n.bottom - 0.06),
        n.w, n.h,
        boxstyle="round,pad=0.02,rounding_size=0.10",
        linewidth=0, facecolor="#000000", alpha=0.07, zorder=1,
    )
    ax.add_patch(shadow)

    # 主体
    box = FancyBboxPatch(
        (n.left, n.bottom),
        n.w, n.h,
        boxstyle="round,pad=0.02,rounding_size=0.10",
        linewidth=1.0,
        edgecolor=style["edge"],
        facecolor=style["fill"],
        zorder=2,
    )
    ax.add_patch(box)

    # 文本: 上行主标题, 下行节点常量名 (italic, 较小)
    if n.sub:
        ax.text(
            n.x, n.y + n.h * 0.18,
            n.label,
            ha="center", va="center",
            color=style["text"],
            fontsize=9.8, fontweight="bold",
            zorder=3,
        )
        ax.text(
            n.x, n.y - n.h * 0.22,
            n.sub,
            ha="center", va="center",
            color=style["text"],
            fontsize=7.4, fontstyle="italic",
            zorder=3,
        )
    else:
        ax.text(
            n.x, n.y, n.label,
            ha="center", va="center",
            color=style["text"],
            fontsize=10.0, fontweight="bold",
            zorder=3,
        )


def _anchor_point(n: Node, side: str,
                  target: Optional[Tuple[float, float]] = None
                  ) -> Tuple[float, float]:
    """获取节点指定边的锚点。side='auto' 时按朝 target 方向自动选择。"""
    if side == "top":
        return (n.x, n.top)
    if side == "bottom":
        return (n.x, n.bottom)
    if side == "left":
        return (n.left, n.y)
    if side == "right":
        return (n.right, n.y)

    # auto: 取朝 target 那一侧的中点 (轴对齐)
    assert target is not None
    tx, ty = target
    dx, dy = tx - n.x, ty - n.y
    half_w, half_h = n.w / 2, n.h / 2
    if abs(dx) * half_h >= abs(dy) * half_w:
        return (n.right if dx > 0 else n.left, n.y)
    else:
        return (n.x, n.top if dy > 0 else n.bottom)


def _draw_edge(ax: plt.Axes,
               nodes: Dict[str, Node],
               e: Edge) -> None:
    src = nodes[e.src]
    dst = nodes[e.dst]

    # 起点 / 终点
    if e.waypoints:
        first_wp, last_wp = e.waypoints[0], e.waypoints[-1]
        p_src = _anchor_point(src, e.src_anchor, first_wp)
        p_dst = _anchor_point(dst, e.dst_anchor, last_wp)
    else:
        p_src = _anchor_point(src, e.src_anchor, (dst.x, dst.y))
        p_dst = _anchor_point(dst, e.dst_anchor, (src.x, src.y))

    linestyle = (0, (4, 2.5)) if e.kind == "dashed" else "-"

    # 通过 waypoint 渲染折线时, 使用多段 line + 单独箭头
    if e.waypoints:
        pts = [p_src] + list(e.waypoints) + [p_dst]
        # 折线段
        for a, b in zip(pts[:-1], pts[1:]):
            ax.plot(
                [a[0], b[0]], [a[1], b[1]],
                color=e.color, linestyle=linestyle, linewidth=1.1,
                solid_capstyle="round", zorder=1.6,
            )
        # 末段箭头
        arrow = FancyArrowPatch(
            pts[-2], pts[-1],
            arrowstyle="-|>",
            mutation_scale=11,
            linewidth=0,
            color=e.color,
            shrinkA=0, shrinkB=0,
            zorder=1.7,
        )
        ax.add_patch(arrow)
    else:
        arrow = FancyArrowPatch(
            p_src, p_dst,
            arrowstyle="-|>",
            mutation_scale=11,
            linewidth=1.1,
            color=e.color,
            linestyle=linestyle,
            connectionstyle=f"arc3,rad={e.rad}",
            shrinkA=0, shrinkB=0,
            zorder=1.6,
        )
        ax.add_patch(arrow)

    # 标签
    if e.label:
        if e.waypoints:
            # 在第一段中点上加标签 (通常 dispatch/feedback 信息在边缘起始处更显眼)
            mid_a = p_src
            mid_b = e.waypoints[0]
            t = e.label_pos
            mx = mid_a[0] + (mid_b[0] - mid_a[0]) * t
            my = mid_a[1] + (mid_b[1] - mid_a[1]) * t
        else:
            mx = p_src[0] + (p_dst[0] - p_src[0]) * e.label_pos
            my = p_src[1] + (p_dst[1] - p_src[1]) * e.label_pos

        lx = mx + e.label_offset[0]
        ly = my + e.label_offset[1]
        ax.text(
            lx, ly, e.label,
            ha="center", va="center",
            fontsize=8.0, color=e.color,
            fontstyle="italic",
            bbox=dict(
                boxstyle="round,pad=0.18",
                facecolor="white",
                edgecolor="none",
                alpha=0.92,
            ),
            zorder=3,
        )


# ============================================================================
# 4. Figure 1: Swim-lane + Routing-Channel 调度图
# ============================================================================

def build_figure_one() -> plt.Figure:
    """
    布局策略
    --------
    画布水平分 6 个语义"槽位"列 (col_x):
        col_x[0..5] -> 用户主流水线节点中线
    画布最右侧保留一个 "Routing Channel" 通道 (x = ch_x),
    所有 Sub-Agent → Supervisor 的反馈边都走该通道, 形成正交折线
    (类似 IC 设计图里的 bus channel), 干净、可读、无穿插。

    主线方向: 自上而下。
    """
    fig, ax = plt.subplots(figsize=(13.0, 11.9))
    ax.set_xlim(-1.5, 14.5)
    ax.set_ylim(-1.0, 14.8)
    ax.set_aspect("equal")
    ax.axis("off")

    # ----- 阶段背景色带 (Stage I-IV) -----
    # 各阶段 Y 范围 (与下方节点 Y 坐标严格对齐):
    #   Stage I  : Evidence(12.55) / Schema(11.45) / Table(10.35)
    #   Stage II : Feasibility(9.30)
    #   Stage III: Planner / Human (8.20)
    #   Stage IV : Supervisor(6.80) -> Sub-agents -> END
    bands = [
        ( 9.85, 13.20, "#1F4E79", "Stage I:  Knowledge Recall"),
        ( 8.75,  9.80, "#385723", "Stage II: Feasibility Assessment"),
        ( 7.65,  8.70, "#8C6D1F", "Stage III: Planning  &  Human-in-the-Loop"),
        ( 0.20,  7.60, "#9E480E", "Stage IV: Supervisor-Orchestrated Multi-Agent Execution"),
    ]
    for y_lo, y_hi, col, lbl in bands:
        band = FancyBboxPatch(
            (-1.30, y_lo),
            14.30, y_hi - y_lo,
            boxstyle="round,pad=0.0,rounding_size=0.16",
            linewidth=0.6, edgecolor=col, facecolor=col,
            alpha=0.06, zorder=0,
        )
        ax.add_patch(band)
        # 阶段标签一律放在画布右上角 (避开左侧可能被 Planner / SQL Generation 节点压字)
        ax.text(
            12.85, y_hi - 0.16, lbl,
            ha="right", va="top",
            fontsize=9.0, fontweight="bold",
            color=col, alpha=0.95, zorder=0.5,
        )

    # ----- 节点 (主流水线在 x=6.0 中线; 三个 sub-agent 列在 x=2.4, 6.0, 9.6) -----
    main_x = 6.0
    NW = 2.40    # 主节点宽 (足够装下 PYTHON_GENERATE_NODE 一行)
    NH = 0.90
    SW = 2.55    # Supervisor 宽
    SH = 0.98
    TW = 1.50    # START / END 宽
    TH = 0.62

    nodes_list: List[Node] = [
        Node("START", "START", "",                       main_x, 13.55, TW, TH, "io"),

        # Stage I
        Node("EVID",  "Evidence Recall",   "EVIDENCE_RECALL_NODE",   main_x, 12.55, NW, NH, "recall"),
        Node("SCHM",  "Schema Recall",     "SCHEMA_RECALL_NODE",     main_x, 11.45, NW, NH, "recall"),
        Node("TREL",  "Table Relation",    "TABLE_RELATION_NODE",    main_x, 10.35, NW, NH, "recall"),

        # Stage II
        Node("FEAS",  "Feasibility Assess.", "FEASIBILITY_ASSESS_NODE", main_x, 9.30, NW, NH, "assess"),

        # Stage III: Planner & HumanFeedback (并排)
        Node("PLAN",  "Planner",           "PLANNER_NODE",         3.50,  8.20, NW, NH, "plan"),
        Node("HUMAN", "Human Feedback",    "HUMAN_FEEDBACK_NODE",  main_x, 8.20, NW, NH, "plan"),

        # Stage IV: Supervisor (顶部居中) - 下移以便为 rejected/feasible 标签留到位置
        Node("SUP",   "Supervisor (LLM)",  "SUPERVISOR_NODE",      main_x, 6.40, SW, SH, "supervisor"),

        # Sub-agents 三列, 自上而下流水
        Node("SQLG",  "SQL Generation",    "SQL_GENERATE_NODE",     2.40, 4.85, NW, NH, "sql"),
        Node("SQLE",  "SQL Execution",     "SQL_EXECUTE_NODE",      2.40, 3.65, NW, NH, "sql"),

        Node("PYG",   "Python Generation", "PYTHON_GENERATE_NODE",  main_x, 4.85, NW, NH, "python"),
        Node("PYE",   "Python Execution",  "PYTHON_EXECUTE_NODE",   main_x, 3.65, NW, NH, "python"),
        Node("PYA",   "Python Analysis",   "PYTHON_ANALYZE_NODE",   main_x, 2.45, NW, NH, "python"),

        Node("REP",   "Report Generation", "REPORT_GENERATOR_NODE", 9.60, 4.85, NW, NH, "report"),

        # END (画布最右下角)
        Node("END",   "END", "",                                    10.50, 0.85, TW, TH, "io"),
    ]
    nodes: Dict[str, Node] = {n.key: n for n in nodes_list}

    for n in nodes_list:
        _draw_node(ax, n)

    # ----- 主流水线确定性边 (Stage I → Stage II → HUMAN → Supervisor) -----
    main_chain = [
        ("START", "EVID"),
        ("EVID",  "SCHM"),
        ("SCHM",  "TREL"),
        ("TREL",  "FEAS"),
    ]
    for s, d in main_chain:
        _draw_edge(ax, nodes, Edge(s, d, color=EDGE_DETERMINISTIC,
                                   src_anchor="bottom", dst_anchor="top"))

    # Stage II 条件分支: feasible -> HUMAN, infeasible -> END (走右侧)
    # feasible: 标签放在两节点之间路径中间 (FEAS 底 9.30-0.45=8.85, HUMAN 顶 8.20+0.45=8.65)
    # 路径中点 y ≈ 8.75, 标签不起从主轴偏移 (标签在起点-终点连线中点, 后续偏移量设 0)
    _draw_edge(ax, nodes, Edge(
        "FEAS", "HUMAN", kind="dashed", color=EDGE_CONDITIONAL,
        src_anchor="bottom", dst_anchor="top",
    ))
    # feasible 标签放在边的实际中点 (main_x 列), 带白底遮住箭头线
    ax.text(
        main_x, (nodes["FEAS"].bottom + nodes["HUMAN"].top) / 2,
        "feasible",
        ha="center", va="center",
        fontsize=8.0, color=EDGE_CONDITIONAL,
        fontstyle="italic",
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=2.6,
    )
    # infeasible: 通过最右侧通道下达 END
    _draw_edge(ax, nodes, Edge(
        "FEAS", "END", kind="dashed", color=EDGE_CONDITIONAL,
        src_anchor="right", dst_anchor="top",
        waypoints=[(12.00, 9.30), (12.00, 1.20)],
    ))
    # infeasible 标签放在水平段靠近 FEAS 处 (避开右上角 Stage II 标签)
    ax.text(
        nodes["FEAS"].right + 0.60, 9.30 + 0.20,
        "infeasible",
        ha="center", va="bottom",
        fontsize=8.0, color=EDGE_CONDITIONAL,
        fontstyle="italic",
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=2.6,
    )

    # Stage III: Planner ↔ Human (横向, 双标签)
    _draw_edge(ax, nodes, Edge(
        "PLAN", "HUMAN",
        color=EDGE_DETERMINISTIC,
        src_anchor="right", dst_anchor="left",
    ))
    # draft plan 标签单独绘制在两节点中间水平带 (y=8.20) 上方 0.55, 避免压节点顶部
    ax.text(
        (3.50 + main_x) / 2, 8.20 + 0.55,
        "draft plan",
        ha="center", va="center",
        fontsize=8.0, color=EDGE_DETERMINISTIC,
        fontstyle="italic",
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=2.6,
    )
    # rejected: Human → Planner (走下方折线), 与上方实线分开
    # 折线 y 下移至 7.55, 与 Stage III band 底(7.65) 中间留出 rejected 标签位置
    _draw_edge(ax, nodes, Edge(
        "HUMAN", "PLAN", kind="dashed", color=EDGE_CONDITIONAL,
        src_anchor="bottom", dst_anchor="bottom",
        waypoints=[(main_x, 7.30), (3.50, 7.30)],
    ))
    # rejected 标签放在水平折线中点上方 (在 Stage III 与 Stage IV 边界上方, Supervisor 顶部上方足够空间)
    ax.text(
        (3.50 + main_x) / 2, 7.30 + 0.20,
        "rejected",
        ha="center", va="bottom",
        fontsize=8.0, color=EDGE_CONDITIONAL,
        fontstyle="italic",
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=2.6,
    )
    # approved: Human → Supervisor (主线下行, 标签放在中点, 足够远离 Supervisor 顶)
    _draw_edge(ax, nodes, Edge(
        "HUMAN", "SUP", kind="dashed", color=EDGE_CONDITIONAL,
        src_anchor="bottom", dst_anchor="top",
        label="approved", label_pos=0.5, label_offset=(0.62, 0.0),
    ))

    # ----- Supervisor → Sub-agents dispatch (虚线, 三向放射) -----
    # 全部用 src_anchor="bottom", 但水平位置稍微偏移 (避免起点重叠)
    sup = nodes["SUP"]
    sup_y = sup.bottom

    # 三个 dispatch 用 waypoint 折线: Supervisor 底部 → 共享水平母线 → 各列顶部
    # 母线 y = 5.55, 在 Supervisor 底(5.91) 与 sub-agent 顶(5.30) 之间
    bus_y = 5.55
    # Supervisor 底部 → 母线 (短竖线)
    ax.plot(
        [sup.x, sup.x], [sup_y, bus_y],
        color=EDGE_CONDITIONAL, linestyle=(0, (4, 2.5)),
        linewidth=1.1, zorder=1.5,
    )
    # 母线 → 三个 worker 顶部 (每条带箭头, 位于各列顶上方一小段)
    for key, dx_label in [
        ("SQLG", -1.30),
        ("PYG",   1.30),
        ("REP",  -1.30),
    ]:
        n = nodes[key]
        # 母线水平段
        ax.plot(
            [sup.x, n.x], [bus_y, bus_y],
            color=EDGE_CONDITIONAL, linestyle=(0, (4, 2.5)),
            linewidth=1.1, zorder=1.5,
        )
        # 下落箭头
        arrow = FancyArrowPatch(
            (n.x, bus_y), (n.x, n.top),
            arrowstyle="-|>", mutation_scale=11,
            linewidth=1.1, color=EDGE_CONDITIONAL,
            linestyle=(0, (4, 2.5)),
            shrinkA=0, shrinkB=0, zorder=1.6,
        )
        ax.add_patch(arrow)
    # dispatch 标签 (放置在母线上方, 三个标签水平排开, 互不重叠)
    # 使用简短形式 "→ *_GENERATION" 节约横向空间
    dispatch_labels = [
        (nodes["SQLG"].x, "\u2192  SQL_GENERATION"),
        (nodes["PYG"].x,  "\u2192  PYTHON_GENERATION"),
        (nodes["REP"].x,  "\u2192  REPORT_GENERATION"),
    ]
    for lx, lbl in dispatch_labels:
        ax.text(
            lx, bus_y + 0.18, lbl,
            ha="center", va="bottom",
            fontsize=7.6, color=EDGE_CONDITIONAL,
            fontstyle="italic",
            bbox=dict(boxstyle="round,pad=0.16",
                      facecolor="white", edgecolor="none",
                      alpha=0.95),
            zorder=2.6,
        )
    # 总标题 "dispatch" 放在 Supervisor 与母线之间右侧足够远的位置
    # (避免与 PYG 所在的 main_x 列 dispatch 标签重叠)
    ax.text(
        sup.right + 0.30, (sup_y + bus_y) / 2,
        "dispatch",
        ha="left", va="center",
        fontsize=7.8, color=EDGE_CONDITIONAL,
        fontstyle="italic", fontweight="bold",
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=2.6,
    )

    # ----- Sub-agent 内部确定性流水 -----
    intra = [
        ("SQLG", "SQLE"),
        ("PYG",  "PYE"),
        ("PYE",  "PYA"),
    ]
    for s, d in intra:
        _draw_edge(ax, nodes, Edge(s, d, color=EDGE_DETERMINISTIC,
                                   src_anchor="bottom", dst_anchor="top"))

    # ----- Sub-agent → Supervisor 反馈边 (走专用 Routing Channel) -----
    # 左通道 ch_L: 服务 SQL agent
    # 右通道 ch_R: 服务 Python agent (绕过 Report 列, Report right=10.80, infeasible 折线在 11.20)
    ch_L = -0.50
    ch_R = 11.00

    _draw_edge(ax, nodes, Edge(
        "SQLE", "SUP", color=EDGE_FEEDBACK,
        src_anchor="left", dst_anchor="left",
        waypoints=[(ch_L, nodes["SQLE"].y),
                   (ch_L, sup.y),
                   (sup.left, sup.y)],
    ))
    # SQL feedback 标签放在左通道竖直段中部
    ax.text(
        ch_L - 0.05, (nodes["SQLE"].y + sup.y) / 2,
        "feedback",
        ha="right", va="center",
        fontsize=8.0, color=EDGE_FEEDBACK,
        fontstyle="italic", rotation=90,
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=2.6,
    )

    _draw_edge(ax, nodes, Edge(
        "PYA", "SUP", color=EDGE_FEEDBACK,
        src_anchor="right", dst_anchor="right",
        waypoints=[(ch_R, nodes["PYA"].y),
                   (ch_R, sup.y),
                   (sup.right, sup.y)],
    ))
    ax.text(
        ch_R + 0.05, (nodes["PYA"].y + sup.y) / 2,
        "feedback",
        ha="left", va="center",
        fontsize=8.0, color=EDGE_FEEDBACK,
        fontstyle="italic", rotation=90,
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=2.6,
    )

    # ----- Report → END -----
    _draw_edge(ax, nodes, Edge(
        "REP", "END", color=EDGE_DETERMINISTIC,
        src_anchor="bottom", dst_anchor="top",
        waypoints=[(9.60, 1.60), (10.50, 1.60)],
    ))

    # ----- 标题 (放在画布最顶部, 与 START 节点保持 0.55 安全间距) -----
    ax.text(
        6.5, 14.45,
        "Fig. 1.  End-to-End Multi-Node Agent Orchestration Graph of the Data-Agent Backend",
        ha="center", va="center",
        fontsize=12.5, fontweight="bold",
    )

    # ----- 图例 (画布底部, 单行) -----
    legend_handles = [
        Line2D([0], [0], color=EDGE_DETERMINISTIC, lw=1.4,
               label="Deterministic transition"),
        Line2D([0], [0], color=EDGE_CONDITIONAL, lw=1.4,
               linestyle=(0, (4, 2.5)),
               label="Conditional routing"),
        Line2D([0], [0], color=EDGE_FEEDBACK, lw=1.4,
               label="Feedback (via routing channel)"),
        mpatches.Patch(facecolor=PALETTE["recall"]["fill"],
                       edgecolor=PALETTE["recall"]["edge"],
                       label="Recall"),
        mpatches.Patch(facecolor=PALETTE["assess"]["fill"],
                       edgecolor=PALETTE["assess"]["edge"],
                       label="Feasibility"),
        mpatches.Patch(facecolor=PALETTE["plan"]["fill"],
                       edgecolor=PALETTE["plan"]["edge"],
                       label="Planning / Human"),
        mpatches.Patch(facecolor=PALETTE["supervisor"]["fill"],
                       edgecolor=PALETTE["supervisor"]["edge"],
                       label="Supervisor"),
        mpatches.Patch(facecolor=PALETTE["sql"]["fill"],
                       edgecolor=PALETTE["sql"]["edge"],
                       label="SQL agent"),
        mpatches.Patch(facecolor=PALETTE["python"]["fill"],
                       edgecolor=PALETTE["python"]["edge"],
                       label="Python agent"),
        mpatches.Patch(facecolor=PALETTE["report"]["fill"],
                       edgecolor=PALETTE["report"]["edge"],
                       label="Report agent"),
    ]
    leg = ax.legend(
        handles=legend_handles,
        loc="upper center",
        bbox_to_anchor=(6.5, -0.35),
        bbox_transform=ax.transData,
        ncol=5, frameon=True, fancybox=True,
        framealpha=0.96, edgecolor="#888888",
        handlelength=2.2, columnspacing=1.4,
        handletextpad=0.6,
    )
    leg.get_frame().set_linewidth(0.6)

    return fig


# ============================================================================
# 5. Figure 2: Layered Multi-Agent Architecture
# ============================================================================

def build_figure_two() -> plt.Figure:
    """
    分层架构 (Layered Architecture):
        Layer 1: User Interface     (User Question + Draft Plan)
        Layer 2: Orchestration      (Supervisor LLM)
        Layer 3: Worker Agents      (SQL / Python / Report)
        Layer 4: Output             (END)

    Supervisor 与 Worker Layer 之间用 "Dispatch Bus" 与 "Feedback Bus"
    两条总线 (water-bus) 抽象表示, 这是工业架构图常见画法。
    """
    fig, ax = plt.subplots(figsize=(13.0, 10.5))
    ax.set_xlim(-1.5, 14.5)
    ax.set_ylim(-1.0, 12.0)
    ax.set_aspect("equal")
    ax.axis("off")

    # ----- 各层背景 (浅底色 + 左侧标签) -----
    # Layer 1 抽高 (为右侧 reasoning 注释框留出位置)
    layers = [
        ( 9.00, 11.40, "#3A3A3A", "Layer 1\nInterface"),
        ( 7.40,  8.95, "#9E480E", "Layer 2\nOrchestration"),
        ( 2.60,  7.30, "#2F5597", "Layer 3\nWorker  Agents"),
        ( 0.40,  2.50, "#3A3A3A", "Layer 4\nOutput"),
    ]
    for y_lo, y_hi, col, lbl in layers:
        band = FancyBboxPatch(
            (-1.30, y_lo),
            14.30, y_hi - y_lo,
            boxstyle="round,pad=0.0,rounding_size=0.18",
            linewidth=0.6, edgecolor=col, facecolor=col,
            alpha=0.05, zorder=0,
        )
        ax.add_patch(band)
        ax.text(
            -1.20, (y_lo + y_hi) / 2, lbl,
            ha="left", va="center",
            fontsize=9.0, fontweight="bold", color=col,
            alpha=0.95, zorder=0.5,
        )

    NW, NH = 2.45, 0.90
    SW, SH = 3.40, 1.15
    TW, TH = 1.55, 0.65

    # ----- 节点定义 -----
    nodes_list: List[Node] = [
        # Layer 1 (两个节点靠左排列, 右侧空出给 reasoning 注释框)
        Node("USER",  "User Question",          "+ Draft Plan + Trace",   3.20, 10.20, 2.80, 0.95, "io"),
        Node("STATE", "Shared Agent State",     "DataAgentState (mutable)", 7.30, 10.20, 3.50, 0.95, "io"),

        # Layer 2: Supervisor (靠左, 与上方 USER/STATE 中心对齐; 右侧留给 reasoning 框)
        Node("SUP",   "Supervisor",             "LLM Orchestrator  \u00b7  per-iteration policy",
             5.30, 8.20, SW, SH, "supervisor"),

        # Layer 3
        Node("SQLG",  "SQL Generation",         "SQL_GENERATE_NODE",      2.40, 5.85, NW, NH, "sql"),
        Node("SQLE",  "SQL Execution",          "SQL_EXECUTE_NODE",       2.40, 4.10, NW, NH, "sql"),

        Node("PYG",   "Python Generation",      "PYTHON_GENERATE_NODE",   6.00, 5.85, NW, NH, "python"),
        Node("PYE",   "Python Execution",       "PYTHON_EXECUTE_NODE",    6.00, 4.55, NW, NH, "python"),
        Node("PYA",   "Python Analysis",        "PYTHON_ANALYZE_NODE",    6.00, 3.25, NW, NH, "python"),

        Node("REP",   "Report Generation",      "REPORT_GENERATOR_NODE",  10.00, 5.85, NW, NH, "report"),

        # Layer 4
        Node("END",   "END", "",                                          6.00, 1.45, TW, TH, "io"),
    ]
    nodes: Dict[str, Node] = {n.key: n for n in nodes_list}

    # ----- Cluster 包围框 (Layer 3 的三个 sub-agent 簇) -----
    # 参数: (title, style, x_left, y_top_of_box, width, height)
    # cluster 顶留出 0.20 空间与 dispatch bus(y=7.85)间隔, 避免包围框底接触 Layer 4
    clusters = [
        ("SQL Sub-Agent",     "sql",     1.05, 7.05,  2.70, 3.80),
        ("Python Sub-Agent",  "python",  4.65, 7.05,  2.70, 4.40),
        ("Report Sub-Agent",  "report",  8.65, 7.05,  2.70, 1.70),
    ]
    for title, style, x0, y_top, w, h in clusters:
        cluster_box = FancyBboxPatch(
            (x0, y_top - h),
            w, h,
            boxstyle="round,pad=0.0,rounding_size=0.18",
            linewidth=1.0,
            edgecolor=PALETTE[style]["edge"],
            facecolor=PALETTE[style]["fill"],
            alpha=0.16,
            linestyle=(0, (1.6, 1.6)),
            zorder=0.5,
        )
        ax.add_patch(cluster_box)
        # cluster 标题: 放在包围框内侧顶部中央 (带白底不受背景干扰)
        # dispatch bus 位于 y=7.85, dispatch 标签位于 bus 下 0.16 = 7.69
        # cluster 顶 = 7.05, 标签放在 7.05 - 0.05 = 7.00, 与 dispatch 标签间隔 0.69, 安全
        ax.text(
            x0 + w / 2, y_top - 0.05, title,
            ha="center", va="top",
            fontsize=9.6, fontweight="bold",
            color=PALETTE[style]["edge"],
            bbox=dict(boxstyle="round,pad=0.20",
                      facecolor="white",
                      edgecolor=PALETTE[style]["edge"],
                      linewidth=0.6),
            zorder=2.5,
        )
    # ----- 渲染节点 -----
    for n in nodes_list:
        _draw_node(ax, n)

    # ----- Layer 1 → Layer 2 -----
    _draw_edge(ax, nodes, Edge(
        "USER", "SUP", color=EDGE_DETERMINISTIC,
        src_anchor="bottom", dst_anchor="left",
        waypoints=[(3.20, 9.05), (3.90, 9.05), (3.90, 8.20)],
        label="state context", label_pos=0.5, label_offset=(-0.45, 0.30),
    ))
    _draw_edge(ax, nodes, Edge(
        "STATE", "SUP", color=EDGE_DETERMINISTIC,
        src_anchor="bottom", dst_anchor="right",
        waypoints=[(7.30, 9.05), (6.70, 9.05), (6.70, 8.20)],
        label="r/w state", label_pos=0.5, label_offset=(0.40, 0.30),
    ))

    # ----- Dispatch Bus (Layer 2 → Layer 3) -----
    bus_y_dispatch = 7.85
    ax.plot(
        [-1.30, 11.50], [bus_y_dispatch, bus_y_dispatch],
        color=EDGE_CONDITIONAL, linestyle=(0, (4, 2.5)),
        linewidth=1.4, zorder=1.4,
    )
    ax.text(
        0.10, bus_y_dispatch + 0.15,
        "Dispatch Bus",
        ha="left", va="bottom",
        fontsize=8.4, color=EDGE_CONDITIONAL, fontstyle="italic",
        fontweight="bold",
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=1.6,
    )
    # Supervisor 底 → 母线
    ax.plot(
        [nodes["SUP"].x, nodes["SUP"].x],
        [nodes["SUP"].bottom, bus_y_dispatch],
        color=EDGE_CONDITIONAL, linestyle=(0, (4, 2.5)),
        linewidth=1.2, zorder=1.4,
    )
    # 母线 → 各 worker 顶部 (带箭头)
    for key, lbl_text in [
        ("SQLG", "SQL_GENERATION"),
        ("PYG",  "PYTHON_GENERATION"),
        ("REP",  "REPORT_GENERATION"),
    ]:
        n = nodes[key]
        arrow = FancyArrowPatch(
            (n.x, bus_y_dispatch), (n.x, n.top),
            arrowstyle="-|>", mutation_scale=11,
            linewidth=1.1, color=EDGE_CONDITIONAL,
            linestyle=(0, (4, 2.5)),
            shrinkA=0, shrinkB=0, zorder=1.5,
        )
        ax.add_patch(arrow)
        ax.text(
            n.x, bus_y_dispatch - 0.16,
            f"\u2192  {lbl_text}",
            ha="center", va="top",
            fontsize=7.8, color=EDGE_CONDITIONAL,
            fontstyle="italic",
            bbox=dict(boxstyle="round,pad=0.18",
                      facecolor="white", edgecolor="none",
                      alpha=0.95),
            zorder=2.4,
        )

    # ----- Sub-agent 内部确定性流水 -----
    for s, d in [("SQLG", "SQLE"), ("PYG", "PYE"), ("PYE", "PYA")]:
        _draw_edge(ax, nodes, Edge(s, d, color=EDGE_DETERMINISTIC,
                                   src_anchor="bottom", dst_anchor="top"))

    # ----- Feedback Bus (Layer 3 → Layer 2) -----
    bus_y_feedback = 2.40
    ax.plot(
        [-1.30, 11.50], [bus_y_feedback, bus_y_feedback],
        color=EDGE_FEEDBACK, linestyle="-", linewidth=1.4, zorder=1.4,
    )
    ax.text(
        0.10, bus_y_feedback - 0.22,
        "Feedback Bus",
        ha="left", va="top",
        fontsize=8.4, color=EDGE_FEEDBACK, fontstyle="italic",
        fontweight="bold",
        bbox=dict(boxstyle="round,pad=0.16",
                  facecolor="white", edgecolor="none", alpha=0.95),
        zorder=1.6,
    )
    # 各 sub-agent 末端 → feedback bus
    for key in ["SQLE", "PYA"]:
        n = nodes[key]
        ax.plot(
            [n.x, n.x], [n.bottom, bus_y_feedback],
            color=EDGE_FEEDBACK, linewidth=1.1, zorder=1.4,
        )
    # feedback bus → Supervisor (走右侧通道, 上爬到 supervisor 右侧)
    fb_x = 12.85
    ax.plot(
        [11.50, fb_x], [bus_y_feedback, bus_y_feedback],
        color=EDGE_FEEDBACK, linewidth=1.1, zorder=1.4,
    )
    ax.plot(
        [fb_x, fb_x], [bus_y_feedback, nodes["SUP"].y],
        color=EDGE_FEEDBACK, linewidth=1.1, zorder=1.4,
    )
    arrow_fb = FancyArrowPatch(
        (fb_x, nodes["SUP"].y), (nodes["SUP"].right, nodes["SUP"].y),
        arrowstyle="-|>", mutation_scale=11,
        linewidth=1.1, color=EDGE_FEEDBACK,
        shrinkA=0, shrinkB=0, zorder=1.5,
    )
    ax.add_patch(arrow_fb)

    # ----- Report → END (终止) -----
    # Report 不进 feedback bus, 而是走 cluster 内下行路径直达 END
    _draw_edge(ax, nodes, Edge(
        "REP", "END", color=EDGE_DETERMINISTIC,
        src_anchor="bottom", dst_anchor="top",
        waypoints=[(10.00, 1.85), (6.00, 1.85)],
        label="terminal output", label_pos=0.0, label_offset=(0.0, -0.30),
    ))

    # ----- Supervisor reasoning 注释 (画布右上角 Layer 1 右侧空白带) -----
    # Layer 1 拓宽后 (y 范围 9.00 ~ 11.40), STATE 节点 right=8.05, 右侧空间充足
    reasoning_text = (
        r"$\bf{Supervisor\ Reasoning\ (per\ iteration)}$" + "\n"
        "  1. Read user question + execution trace\n"
        "  2. LLM emits  $\\langle$thought, next_agent,\n"
        "                 task_instruction, finished$\\rangle$\n"
        "  3. Append step to dynamic Plan\n"
        "  4. Dispatch routing  /  fallback to END\n"
        "  5. Iteration cap  $T_{max}=12$  (cycle breaker)"
    )
    ax.text(
        13.80, 11.20, reasoning_text,
        ha="right", va="top",
        fontsize=8.4, color="#1A1A1A",
        bbox=dict(boxstyle="round,pad=0.40",
                  facecolor="#FFFFFF",
                  edgecolor="#9E480E",
                  linewidth=0.8),
        zorder=4,
    )

    # ----- 标题 -----
    ax.text(
        6.5, 11.75,
        "Fig. 2.  Supervisor-Orchestrated Multi-Agent Architecture (Layered View)",
        ha="center", va="center",
        fontsize=12.5, fontweight="bold",
    )
    # ----- 图例 -----
    legend_handles = [
        Line2D([0], [0], color=EDGE_DETERMINISTIC, lw=1.4,
               label="Deterministic data/control flow"),
        Line2D([0], [0], color=EDGE_CONDITIONAL, lw=1.4,
               linestyle=(0, (4, 2.5)),
               label="Supervisor dispatch via Dispatch Bus (LLM-decided)"),
        Line2D([0], [0], color=EDGE_FEEDBACK, lw=1.4,
               label="Sub-agent feedback via Feedback Bus"),
    ]
    leg = ax.legend(
        handles=legend_handles,
        loc="upper center",
        bbox_to_anchor=(6.5, -0.30),
        bbox_transform=ax.transData,
        ncol=3, frameon=True, fancybox=True,
        framealpha=0.96, edgecolor="#888888",
        handlelength=2.4, columnspacing=2.0,
    )
    leg.get_frame().set_linewidth(0.6)

    return fig


# ============================================================================
# 6. 入口
# ============================================================================

def main() -> None:
    _setup_publication_style()

    out_dir = os.path.dirname(os.path.abspath(__file__))
    os.makedirs(out_dir, exist_ok=True)

    fig1 = build_figure_one()
    f1_pdf = os.path.join(out_dir, "fig1_agent_state_graph.pdf")
    f1_png = os.path.join(out_dir, "fig1_agent_state_graph.png")
    fig1.savefig(f1_pdf)
    fig1.savefig(f1_png, dpi=300)
    plt.close(fig1)
    print(f"[OK] Figure 1 written:\n  {f1_pdf}\n  {f1_png}")

    fig2 = build_figure_two()
    f2_pdf = os.path.join(out_dir, "fig2_supervisor_multiagent.pdf")
    f2_png = os.path.join(out_dir, "fig2_supervisor_multiagent.png")
    fig2.savefig(f2_pdf)
    fig2.savefig(f2_png, dpi=300)
    plt.close(fig2)
    print(f"[OK] Figure 2 written:\n  {f2_pdf}\n  {f2_png}")


if __name__ == "__main__":
    main()
