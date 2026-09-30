"""Reads results/*.csv and writes the PNG charts to results/plots/.

Per workload: Wk_time.png (time vs n, all structures on one chart) and
Wk_ops.png (steps / moves / comparisons vs n). Plus two compact summary figures used in the
report, and the bonus charts.   Usage:  python3 scripts/plot.py   (needs pandas + matplotlib)
"""
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "results")
OUT = os.path.join(RES, "plots")
os.makedirs(OUT, exist_ok=True)

STYLE = {"DynamicArray": ("#1f77b4", "o"), "MyLinkedList": ("#d62728", "s"), "MinHeap": ("#2ca02c", "^")}
LINESTYLE = {"-": "-", "head": "-", "middle": "--"}
TITLES = {
    "W1": "W1 - random access: 10 000 get(index)",
    "W2": "W2 - search: 1 000 contains(x), half hits",
    "W3": "W3 - 1 000 inserts + 1 000 removes (head / middle)",
    "W4": "W4 - MinHeap: n insert + n extractMin",
}
METRICS = [("steps", "steps (cell reads / node visits)"),
           ("moves", "moves (shifts / pointer updates)"),
           ("comparisons", "comparisons")]


def label(structure, variant, multi):
    return f"{structure} ({variant})" if multi else structure


def draw_time(ax, df, w, fs=8):
    sub = df[df.workload == w]
    multi = sub.variant.nunique() > 1
    for (structure, variant), g in sub.groupby(["structure", "variant"], sort=False):
        c, m = STYLE[structure]
        ax.plot(g.n, g.time_ms, marker=m, color=c, linestyle=LINESTYLE[variant],
                label=label(structure, variant, multi))
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("n (elements in the structure)", fontsize=fs)
    ax.set_ylabel("median time of 5 runs [ms]", fontsize=fs)
    ax.set_title(TITLES[w], fontsize=fs + 1)
    ax.tick_params(labelsize=fs - 1)
    ax.grid(True, which="both", alpha=0.3)
    ax.legend(fontsize=fs - 1)


def draw_ops(axes, df, w, fs=8):
    sub = df[df.workload == w]
    multi = sub.variant.nunique() > 1
    for ax, (metric, title) in zip(axes, METRICS):
        ax.set_title(title, fontsize=fs)
        if (sub[metric] == 0).all():
            ax.text(0.5, 0.5, "always 0 for every\nstructure and every n", ha="center",
                    va="center", fontsize=fs + 1, transform=ax.transAxes)
            ax.set_xticks([])
            ax.set_yticks([])
            continue
        zero = []
        for (structure, variant), g in sub.groupby(["structure", "variant"], sort=False):
            if (g[metric] == 0).all():
                zero.append(label(structure, variant, multi))
                continue
            c, m = STYLE[structure]
            ax.plot(g.n, g[metric], marker=m, color=c, linestyle=LINESTYLE[variant],
                    label=label(structure, variant, multi))
        ax.set_xscale("log")
        ax.set_yscale("log")
        ax.set_xlabel("n", fontsize=fs)
        ax.set_ylabel("count of operations", fontsize=fs)
        ax.tick_params(labelsize=fs - 1)
        ax.grid(True, which="both", alpha=0.3)
        ax.legend(fontsize=fs - 2)
        if zero:
            ax.text(0.02, 0.02, "0 for: " + ", ".join(zero), fontsize=fs - 2, transform=ax.transAxes,
                    va="bottom", bbox=dict(facecolor="white", alpha=0.8, edgecolor="none"))


def save(fig, name):
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, name), dpi=150)
    plt.close(fig)


def workload_plots(df):
    for w in ("W1", "W2", "W3", "W4"):
        fig, ax = plt.subplots(figsize=(6.4, 4.4))
        draw_time(ax, df, w, fs=9)
        save(fig, f"{w}_time.png")
        fig, axes = plt.subplots(1, 3, figsize=(12, 4))
        draw_ops(axes, df, w, fs=9)
        fig.suptitle(TITLES[w] + "  -  counted operations", fontsize=10)
        save(fig, f"{w}_ops.png")

    fig, axes = plt.subplots(2, 2, figsize=(11, 7.4))
    for ax, w in zip(axes.flat, ("W1", "W2", "W3", "W4")):
        draw_time(ax, df, w, fs=8)
    save(fig, "summary_time.png")
    fig, axes = plt.subplots(4, 3, figsize=(12, 11))
    for row, w in zip(axes, ("W1", "W2", "W3", "W4")):
        draw_ops(row, df, w, fs=8)
        row[0].annotate(w, xy=(-0.32, 0.5), xycoords="axes fraction", fontsize=13,
                        fontweight="bold", va="center")
    save(fig, "summary_ops.png")


def slopes(df):
    print("Empirical log-log slope (n = 1 000 .. 100 000):")
    for (w, v, s), g in df.groupby(["workload", "variant", "structure"], sort=False):
        g = g[g.n >= 1000]
        k = np.polyfit(np.log10(g.n), np.log10(g.time_ms), 1)[0]
        ks = np.polyfit(np.log10(g.n), np.log10(g.steps + g.moves + g.comparisons), 1)[0]
        print(f"  {w:3s} {v:7s} {s:13s} time slope {k:5.2f}   ops slope {ks:5.2f}")


def bonus_plots():
    path = os.path.join(RES, "buildheap.csv")
    if os.path.exists(path):
        b = pd.read_csv(path)
        fig, axes = plt.subplots(1, 2, figsize=(11, 4))
        for inp, ls in (("random", "-"), ("descending", "--")):
            for method, c, m in (("n_inserts", "#d62728", "s"), ("buildHeap", "#2ca02c", "^")):
                g = b[(b.input == inp) & (b.method == method)]
                axes[0].plot(g.n, g.comparisons_per_n, color=c, marker=m, linestyle=ls, label=f"{method} ({inp})")
                axes[1].plot(g.n, g.time_ms, color=c, marker=m, linestyle=ls, label=f"{method} ({inp})")
        axes[0].set_ylabel("comparisons per element (comparisons / n)")
        axes[0].set_title("Bonus B: comparisons per element", fontsize=10)
        axes[1].set_ylabel("median time of 5 runs [ms]")
        axes[1].set_yscale("log")
        axes[1].set_title("Bonus B: build time", fontsize=10)
        for ax in axes:
            ax.set_xscale("log")
            ax.set_xlabel("n (number of keys)")
            ax.grid(True, which="both", alpha=0.3)
            ax.legend(fontsize=8)
        save(fig, "bonus_buildheap.png")

    path = os.path.join(RES, "memory.csv")
    if os.path.exists(path):
        m = pd.read_csv(path)

        fig, ax = plt.subplots(figsize=(6.4, 4.4))
        for s, g in m.groupby("structure", sort=False):
            c, mk = STYLE[s]
            ax.plot(g.n, g.total_bytes / 1e6, color=c, marker=mk,
                    linestyle="--" if s == "MinHeap" else "-", label=s)
        n = m.n.unique()
        ax.plot(n, 4 * n / 1e6, color="gray", linestyle=":", label="payload only (4 bytes per int)")
        ax.set_xscale("log")
        ax.set_yscale("log")
        ax.set_xlabel("n (elements)")
        ax.set_ylabel("memory of the object graph [MB = 10^6 bytes] (JOL)")
        ax.set_title("Bonus A: memory vs n (MinHeap overlaps DynamicArray)", fontsize=10)
        ax.grid(True, which="both", alpha=0.3)
        ax.legend(fontsize=8)
        save(fig, "bonus_memory_mb.png")
        fig, ax = plt.subplots(figsize=(6.4, 4.4))
        for s, g in m.groupby("structure", sort=False):
            if s == "MinHeap":
                continue
            c, mk = STYLE[s]
            ax.plot(g.n, g.bytes_per_element, color=c, marker=mk, label=s + (" = MinHeap" if s == "DynamicArray" else ""))
        ax.axhline(4, color="gray", linestyle=":", label="payload: 4 bytes per int")
        ax.set_xscale("log")
        ax.set_xlabel("n (elements)")
        ax.set_ylabel("bytes per element (JOL)")
        ax.set_title("Bonus A: bytes per element", fontsize=10)
        ax.grid(True, which="both", alpha=0.3)
        ax.legend(fontsize=8)
        save(fig, "bonus_memory_per_element.png")


def main():
    df = pd.read_csv(os.path.join(RES, "results.csv"))
    workload_plots(df)
    bonus_plots()
    slopes(df)
    print("Plots written to", OUT)


if __name__ == "__main__":
    main()
