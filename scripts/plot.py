#!/usr/bin/env python3
"""Reads results/*.csv and writes the PNG charts to results/plots/.

Usage:  python3 scripts/plot.py        (needs pandas + matplotlib)
"""
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "results")
OUT = os.path.join(RES, "plots")
os.makedirs(OUT, exist_ok=True)

STYLE = {  # (colour, marker) per structure
    "DynamicArray": ("#1f77b4", "o"),
    "MyLinkedList": ("#d62728", "s"),
    "MinHeap": ("#2ca02c", "^"),
}
LINESTYLE = {"random": "-", "half_hit": "-", "insert_extract": "-", "head": "-", "middle": "--"}
TITLES = {
    "W1": "W1 - random access: 10 000 get(index)",
    "W2": "W2 - search: 1 000 contains(x), half hits / half misses",
    "W3": "W3 - insert & remove: 1 000 + 1 000 operations",
    "W4": "W4 - priority processing: n insert + n extractMin",
}
METRICS = [("steps", "steps (cell reads / node visits)"),
           ("moves", "moves (element shifts / pointer updates)"),
           ("comparisons", "comparisons")]


def label(structure, variant, multi_variant):
    return f"{structure} ({variant})" if multi_variant else structure


def time_plot(df, w):
    sub = df[df.workload == w]
    multi = sub.variant.nunique() > 1
    fig, ax = plt.subplots(figsize=(6.4, 4.4))
    for (structure, variant), g in sub.groupby(["structure", "variant"], sort=False):
        c, m = STYLE[structure]
        ax.plot(g.n, g.time_ms, marker=m, color=c, linestyle=LINESTYLE[variant],
                label=label(structure, variant, multi))
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("n (elements in the structure)")
    ax.set_ylabel("median time of 5 runs [ms]")
    ax.set_title(TITLES[w], fontsize=10)
    ax.grid(True, which="both", alpha=0.3)
    ax.legend(fontsize=8)
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, f"{w}_time.png"), dpi=150)
    plt.close(fig)


def ops_plot(df, w):
    sub = df[df.workload == w]
    multi = sub.variant.nunique() > 1
    fig, axes = plt.subplots(1, 3, figsize=(12, 4))
    for ax, (metric, title) in zip(axes, METRICS):
        if (sub[metric] == 0).all():
            ax.text(0.5, 0.5, f"always 0\nfor every structure\nand every n", ha="center",
                    va="center", fontsize=11, transform=ax.transAxes)
            ax.set_xticks([])
            ax.set_yticks([])
            ax.set_title(title, fontsize=9)
            continue
        for (structure, variant), g in sub.groupby(["structure", "variant"], sort=False):
            if (g[metric] == 0).all():
                continue  # log axis cannot show zero; listed in the legend note below
            c, m = STYLE[structure]
            ax.plot(g.n, g[metric], marker=m, color=c, linestyle=LINESTYLE[variant],
                    label=label(structure, variant, multi))
        zero = [label(s, v, multi) for (s, v), g in sub.groupby(["structure", "variant"], sort=False)
                if (g[metric] == 0).all()]
        ax.set_xscale("log")
        ax.set_yscale("log")
        ax.set_xlabel("n")
        ax.set_title(title, fontsize=9)
        ax.grid(True, which="both", alpha=0.3)
        ax.legend(fontsize=7)
        if zero:
            ax.text(0.02, 0.02, "0 for: " + ", ".join(zero), fontsize=7, transform=ax.transAxes,
                    va="bottom", bbox=dict(facecolor="white", alpha=0.8, edgecolor="none"))
    fig.suptitle(TITLES[w] + "  -  counted operations", fontsize=10)
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, f"{w}_ops.png"), dpi=150)
    plt.close(fig)


def slopes(df):
    print("Empirical log-log slope of time vs n (n = 1 000 .. 100 000):")
    import numpy as np
    for (w, v, s), g in df.groupby(["workload", "variant", "structure"], sort=False):
        g = g[g.n >= 1000]
        k = np.polyfit(np.log10(g.n), np.log10(g.time_ms), 1)[0]
        ks = np.polyfit(np.log10(g.n), np.log10(g.steps + g.moves + g.comparisons), 1)[0]
        print(f"  {w:3s} {v:15s} {s:13s} time slope {k:5.2f}   ops slope {ks:5.2f}")


def bonus_plots():
    path = os.path.join(RES, "buildheap.csv")
    if os.path.exists(path):
        b = pd.read_csv(path)
        fig, axes = plt.subplots(1, 2, figsize=(11, 4))
        for inp, ls in (("random", "-"), ("descending", "--")):
            for method, c, m in (("n_inserts", "#d62728", "s"), ("buildHeap", "#2ca02c", "^")):
                g = b[(b.input == inp) & (b.method == method)]
                axes[0].plot(g.n, g.comparisons_per_n, color=c, marker=m, linestyle=ls,
                             label=f"{method} ({inp})")
                axes[1].plot(g.n, g.time_ms, color=c, marker=m, linestyle=ls,
                             label=f"{method} ({inp})")
        axes[0].set_ylabel("comparisons / n")
        axes[0].set_title("Bonus B: comparisons per element", fontsize=10)
        axes[1].set_ylabel("median time [ms]")
        axes[1].set_yscale("log")
        axes[1].set_title("Bonus B: build time", fontsize=10)
        for ax in axes:
            ax.set_xscale("log")
            ax.set_xlabel("n")
            ax.grid(True, which="both", alpha=0.3)
            ax.legend(fontsize=8)
        fig.tight_layout()
        fig.savefig(os.path.join(OUT, "bonus_buildheap.png"), dpi=150)
        plt.close(fig)

    path = os.path.join(RES, "memory.csv")
    if os.path.exists(path):
        m = pd.read_csv(path)
        fig, ax = plt.subplots(figsize=(6.4, 4.4))
        for s, g in m.groupby("structure", sort=False):
            if s == "MinHeap":
                continue  # identical layout to DynamicArray (int[] + header), see report
            c, mk = STYLE[s]
            ax.plot(g.n, g.bytes_per_element, color=c, marker=mk, label=s + (" = MinHeap" if s == "DynamicArray" else ""))
        ax.axhline(4, color="gray", linestyle=":", label="payload: 4 bytes / int")
        ax.set_xscale("log")
        ax.set_xlabel("n")
        ax.set_ylabel("bytes per element (JOL, whole object graph)")
        ax.set_title("Bonus A: memory footprint", fontsize=10)
        ax.grid(True, which="both", alpha=0.3)
        ax.legend(fontsize=8)
        fig.tight_layout()
        fig.savefig(os.path.join(OUT, "bonus_memory.png"), dpi=150)
        plt.close(fig)


def main():
    df = pd.read_csv(os.path.join(RES, "results.csv"))
    for w in ("W1", "W2", "W3", "W4"):
        time_plot(df, w)
        ops_plot(df, w)
    bonus_plots()
    slopes(df)
    print("Plots written to", OUT)


if __name__ == "__main__":
    main()
