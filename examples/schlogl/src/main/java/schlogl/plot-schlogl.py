#  STARK: Software Tool for the Analysis of Robustness in the unKnown environment
#
#                 Copyright (C) 2023.
#
#  See the NOTICE file distributed with this work for additional information
#  regarding copyright ownership.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#              http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
#  or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Plotting script for the Schlogl bistability STARK example.
#
# CSVs written by Main.java (all headerless):
#   schlogl_nominal_<basin>.csv                       avg_x, p_low, p_mid, p_high
#   schlogl_<scenario>_alpha<a>_<basin>.csv           same
#   schlogl_finalx_nominal_<basin>.csv                500 rows x 1 col (final X per sample)
#   schlogl_finalx_<scenario>_alpha<a>_<basin>.csv    same
#   schlogl_traces_nominal_mid.csv                    STEPS x 500 cols (full trajectories)
#   schlogl_traces_<scenario>_alpha<a>_mid.csv        same
#   schlogl_evalR_<scenario>_alpha<a>_mid.csv         threshold, truth_value (20 rows)
#   schlogl_adapt_<scenario>_alpha<a>_<low|high>.csv  eta_1, truth_value (1 row)
#
# Figures produced:
#   1. schlogl_hist_final_X_{low,mid,high,all}.png   per-sample final-X histograms
#   2. schlogl_sample_traces_{scenario}.png           10 individual traces per alpha
#   3. schlogl_heatmap_pct_{high,low}.png             basin-ending fraction heatmaps
#   4. schlogl_robustness_{scenario}_alpha{a}_mid.png RobTL three-valued truth
#   5. schlogl_adaptability.png                       adaptability evaluation heatmap
#
#   <scenario> in {scenario1_k1, scenario2_k3}
#   <basin>    in {low, mid, high}
#   <a>        in {1p05, 1p10, 1p20} (S1) and {0p95, 0p90, 0p80} (S2)

from pathlib import Path
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import numpy as np
import pandas as pd


GRANULARITY    = 0.01
LOW_THRESHOLD  = 150
HIGH_THRESHOLD = 400
N_SAMPLES      = 500
N_TRACES       = 10

SCENARIO_TAGS = ["scenario1_k1", "scenario2_k3"]
BASINS        = ["low", "mid", "high"]

SCENARIO_PRETTY = {
    "scenario1_k1": r"Scenario 1: $k_1 \to \alpha\,k_1$",
    "scenario2_k3": r"Scenario 2: $k_3 \to \alpha\,k_3$",
}
SCENARIO_SHORT = {"scenario1_k1": "S1", "scenario2_k3": "S2"}

SCENARIO_ALPHAS = {
    "scenario1_k1": [1.05, 1.10, 1.20],
    "scenario2_k3": [0.95, 0.90, 0.80],
}

# Column order: S2 (most->least push-low) | Nominal | S1 (least->most push-high),
# giving a single monotonic stress sweep along the x-axis.
HEATMAP_ORDER = [
    ("scenario2_k3", 0.80),
    ("scenario2_k3", 0.90),
    ("scenario2_k3", 0.95),
    ("nominal",      None),
    ("scenario1_k1", 1.05),
    ("scenario1_k1", 1.10),
    ("scenario1_k1", 1.20),
]

# Adaptability is only evaluated for low and high basins under perturbation (no nominal).
ADAPT_ORDER = [
    ("scenario2_k3", 0.80),
    ("scenario2_k3", 0.90),
    ("scenario2_k3", 0.95),
    ("scenario1_k1", 1.05),
    ("scenario1_k1", 1.10),
    ("scenario1_k1", 1.20),
]
ADAPT_BASINS = ["low", "high"]

BASIN_PRETTY = {
    "low":  "start_low (X0=50)",
    "mid":  "start_mid (X0=250)",
    "high": "start_high (X0=600)",
}
BASIN_SHORT  = {"low": "start_low", "mid": "start_mid", "high": "start_high"}
BASIN_COLORS = {"low": "#1f77b4", "mid": "#ff7f0e", "high": "#2ca02c"}

TRACE_COLORS = {
    "scenario1_k1": {1.05: "#fdae6b", 1.10: "#e6550d", 1.20: "#a63603"},
    "scenario2_k3": {0.95: "#9ecae1", 0.90: "#3182bd", 0.80: "#08519c"},
}

TRUTH_LABEL = {-1.0: "false", 0.0: "unknown", 1.0: "true"}

VALUES_COLS = ["avg_x", "p_low", "p_mid", "p_high"]
EVALR_COLS  = ["threshold", "truth_value"]
ADAPT_COLS  = ["eta_1", "truth_value"]

SCRIPT_DIR = Path(__file__).resolve().parent


def _search_dirs():
    dirs = [Path.cwd().resolve()]
    for p in [SCRIPT_DIR, *SCRIPT_DIR.parents]:
        if p not in dirs:
            dirs.append(p)
    return dirs


_DATA_DIR = None

def _data_dir():
    global _DATA_DIR
    if _DATA_DIR is None:
        for d in _search_dirs():
            if (d / "schlogl_nominal_mid.csv").is_file():
                _DATA_DIR = d
                break
        else:
            _DATA_DIR = Path.cwd().resolve()
    return _DATA_DIR


def find_file(name):
    primary = _data_dir() / name
    if primary.is_file():
        return primary
    for d in _search_dirs():
        cand = d / name
        if cand.is_file():
            return cand
    return None


# Filename builders
def _alpha_tag(alpha):
    return f"{alpha:.2f}".replace(".", "p")

def values_name(scenario, alpha, basin):
    return f"schlogl_{scenario}_alpha{_alpha_tag(alpha)}_{basin}.csv"

def evalr_name(scenario, alpha, basin):
    return f"schlogl_evalR_{scenario}_alpha{_alpha_tag(alpha)}_{basin}.csv"

def finalx_name(scenario, alpha, basin):
    if scenario == "nominal":
        return f"schlogl_finalx_nominal_{basin}.csv"
    return f"schlogl_finalx_{scenario}_alpha{_alpha_tag(alpha)}_{basin}.csv"

def traces_name(scenario, alpha, basin):
    if scenario == "nominal":
        return f"schlogl_traces_nominal_{basin}.csv"
    return f"schlogl_traces_{scenario}_alpha{_alpha_tag(alpha)}_{basin}.csv"

def adapt_name(scenario, alpha, basin):
    return f"schlogl_adapt_{scenario}_alpha{_alpha_tag(alpha)}_{basin}.csv"


# Loaders
def _read_csv(path, names=None):
    if path is None or not path.is_file():
        return None
    df = pd.read_csv(path, header=None, names=names, skipinitialspace=True)
    return df.apply(pd.to_numeric, errors="coerce")


def load_values(scenario, alpha, basin):
    return _read_csv(find_file(values_name(scenario, alpha, basin)), VALUES_COLS)

def load_nominal(basin):
    return _read_csv(find_file(f"schlogl_nominal_{basin}.csv"), VALUES_COLS)

def load_evalr(scenario, alpha, basin):
    return _read_csv(find_file(evalr_name(scenario, alpha, basin)), EVALR_COLS)

def load_finalx(scenario, alpha, basin):
    df = _read_csv(find_file(finalx_name(scenario, alpha, basin)))
    return df.to_numpy().ravel() if df is not None else None

def load_traces(scenario, alpha, basin):
    df = _read_csv(find_file(traces_name(scenario, alpha, basin)))
    return df.to_numpy() if df is not None else None

def load_adapt(scenario, alpha, basin):
    """Returns (eta_1, truth_value) or None."""
    df = _read_csv(find_file(adapt_name(scenario, alpha, basin)), ADAPT_COLS)
    if df is None or df.empty:
        return None
    return float(df.iloc[0]["eta_1"]), float(df.iloc[0]["truth_value"])


def _time_axis(n):
    return np.arange(n) * GRANULARITY


def save_fig(fig, output_dir, name):
    fig.tight_layout()
    out = output_dir / name
    fig.savefig(out, dpi=300)
    plt.close(fig)
    print(f"  wrote {out}")


# Figure 1: per-sample final-X histograms (nominal, all three initial conditions)
_HIST_BINS = np.linspace(0.0, 900.0, 46)

def _draw_thresholds(ax):
    ax.axvline(LOW_THRESHOLD, color="black", linestyle="--", linewidth=1.3,
               label=f"Low threshold = {LOW_THRESHOLD}")
    ax.axvline(HIGH_THRESHOLD, color="dimgray", linestyle="--", linewidth=1.3,
               label=f"High threshold = {HIGH_THRESHOLD}")


def plot_final_x_histograms(output_dir):
    finals = {b: load_finalx("nominal", None, b) for b in BASINS}
    finals = {b: a for b, a in finals.items() if a is not None and a.size}
    if not finals:
        print("Fig 1: no schlogl_finalx_nominal_*.csv found.", file=sys.stderr)
        return

    for basin, vals in finals.items():
        fig, ax = plt.subplots(figsize=(9, 5.5))
        ax.hist(vals, bins=_HIST_BINS, color=BASIN_COLORS[basin], alpha=0.75,
                edgecolor="white", linewidth=0.4, label=BASIN_SHORT[basin])
        _draw_thresholds(ax)
        ax.set_xlim(0, 900)
        ax.set_xlabel("Final X population")
        ax.set_ylabel(f"Number of samples (of {N_SAMPLES})")
        ax.set_title(f"Nominal final-X distribution — {BASIN_PRETTY[basin]}")
        ax.grid(True, axis="y", alpha=0.3)
        ax.legend(loc="best", fontsize=9)
        save_fig(fig, output_dir, f"schlogl_hist_final_X_{basin}.png")

    fig, ax = plt.subplots(figsize=(10, 6))
    for basin in BASINS:
        if basin in finals:
            ax.hist(finals[basin], bins=_HIST_BINS, color=BASIN_COLORS[basin],
                    alpha=0.45, edgecolor="white", linewidth=0.3, label=BASIN_SHORT[basin])
    _draw_thresholds(ax)
    ax.set_xlim(0, 900)
    ax.set_xlabel("Final X population")
    ax.set_ylabel(f"Number of samples (of {N_SAMPLES})")
    ax.set_title("Nominal final-X distribution — all initial conditions")
    ax.grid(True, axis="y", alpha=0.3)
    ax.legend(loc="best", fontsize=9)
    save_fig(fig, output_dir, "schlogl_hist_final_X_all.png")


# Figure 2: individual sample traces per scenario (mid initial condition)
def _representative_indices(traces, n):
    """Pick n columns spread across the final-X distribution so both basins are represented."""
    order = np.argsort(traces[-1, :])
    positions = np.linspace(0, len(order) - 1, n).round().astype(int)
    return order[positions]


def plot_sample_traces(output_dir):
    nominal_traces = load_traces("nominal", None, "mid")
    if nominal_traces is not None:
        nominal_mean = nominal_traces.mean(axis=1)
    else:
        df = load_nominal("mid")
        nominal_mean = df["avg_x"].to_numpy() if df is not None else None

    for scenario in SCENARIO_TAGS:
        fig, ax = plt.subplots(figsize=(10, 6))
        handles = []
        plotted = False

        if nominal_mean is not None:
            ax.plot(_time_axis(len(nominal_mean)), nominal_mean,
                    color="black", linewidth=1.8, linestyle="--", zorder=5)
            handles.append(Line2D([0], [0], color="black", linestyle="--",
                                  linewidth=1.8, label="Nominal (mean, mid)"))

        for alpha in SCENARIO_ALPHAS[scenario]:
            color = TRACE_COLORS[scenario][alpha]
            traces = load_traces(scenario, alpha, "mid")
            if traces is not None:
                t = _time_axis(traces.shape[0])
                for col in _representative_indices(traces, min(N_TRACES, traces.shape[1])):
                    ax.plot(t, traces[:, col], color=color, linewidth=0.9, alpha=0.55, zorder=2)
                handles.append(Line2D([0], [0], color=color, linewidth=2.0,
                                      label=rf"$\alpha={alpha:g}$ (mid, {N_TRACES} samples)"))
                plotted = True
            else:
                # Fall back to aggregate mean-X if per-sample traces are not available.
                df = load_values(scenario, alpha, "mid")
                if df is None:
                    continue
                ax.plot(_time_axis(len(df)), df["avg_x"], color=color, linewidth=1.8, zorder=3)
                handles.append(Line2D([0], [0], color=color, linewidth=2.0,
                                      label=rf"$\alpha={alpha:g}$ (mid, mean — no traces)"))
                plotted = True

        if not plotted:
            plt.close(fig)
            print(f"Fig 2: no data for {scenario}.", file=sys.stderr)
            continue

        ax.axhline(LOW_THRESHOLD, color="black", linestyle=":", linewidth=1.0)
        ax.axhline(HIGH_THRESHOLD, color="dimgray", linestyle=":", linewidth=1.0)
        handles.append(Line2D([0], [0], color="black", linestyle=":", linewidth=1.0,
                              label=f"Low threshold = {LOW_THRESHOLD}"))
        handles.append(Line2D([0], [0], color="dimgray", linestyle=":", linewidth=1.0,
                              label=f"High threshold = {HIGH_THRESHOLD}"))
        ax.set_xlabel("Time")
        ax.set_ylabel("X population")
        ax.set_title(f"{SCENARIO_PRETTY[scenario]} — sample traces (start_mid)")
        ax.grid(True, alpha=0.3)
        ax.legend(handles=handles, loc="best", fontsize=8, ncol=2)
        save_fig(fig, output_dir, f"schlogl_sample_traces_{scenario}.png")


# Figure 3: basin-ending fraction heatmaps (recomputed from per-sample final X)
def _basin_frac(scenario, alpha, basin, which):
    """Fraction of samples ending in the high (which='high') or low basin.
    Prefers per-sample finalx CSVs; falls back to last-step aggregate probability."""
    vals = load_finalx(scenario, alpha, basin)
    if vals is not None and vals.size:
        return float(np.mean(vals >= HIGH_THRESHOLD) if which == "high" else np.mean(vals < LOW_THRESHOLD))
    df = load_nominal(basin) if scenario == "nominal" else load_values(scenario, alpha, basin)
    if df is None or df.empty:
        return np.nan
    return float(df.iloc[-1]["p_high" if which == "high" else "p_low"])


def _heatmap_col_labels():
    return [
        "Nominal" if s == "nominal" else rf"{SCENARIO_SHORT[s]} $\alpha={a:g}$"
        for s, a in HEATMAP_ORDER
    ]


def _plot_one_heatmap(output_dir, which, title, fname):
    grid = np.full((len(BASINS), len(HEATMAP_ORDER)), np.nan)
    for i, basin in enumerate(BASINS):
        for j, (scenario, alpha) in enumerate(HEATMAP_ORDER):
            grid[i, j] = _basin_frac(scenario, alpha, basin, which)

    fig, ax = plt.subplots(figsize=(1.05 * len(HEATMAP_ORDER) + 2.2, 1.25 * len(BASINS) + 1.8))
    cmap = plt.get_cmap("Greys").copy()
    cmap.set_bad("lightblue")
    im = ax.imshow(np.ma.masked_invalid(grid), aspect="auto", vmin=0.0, vmax=1.0, cmap=cmap)

    ax.set_xticks(range(len(HEATMAP_ORDER)))
    ax.set_xticklabels(_heatmap_col_labels(), rotation=35, ha="right", fontsize=9)
    ax.set_yticks(range(len(BASINS)))
    ax.set_yticklabels([BASIN_SHORT[b] for b in BASINS], fontsize=10)
    ax.set_title(title, fontsize=11)

    # Mark the nominal column — the centre of the monotonic sweep.
    nom_j = next(j for j, (s, _) in enumerate(HEATMAP_ORDER) if s == "nominal")
    ax.axvline(nom_j - 0.5, color="#d62728", linewidth=1.0, alpha=0.5)
    ax.axvline(nom_j + 0.5, color="#d62728", linewidth=1.0, alpha=0.5)

    for i in range(len(BASINS)):
        for j in range(len(HEATMAP_ORDER)):
            v = grid[i, j]
            if np.isnan(v):
                ax.text(j, i, "n/a", ha="center", va="center", fontsize=8)
            else:
                ax.text(j, i, f"{v:.2f}", ha="center", va="center", fontsize=9,
                        color="white" if v > 0.5 else "black")

    cbar = fig.colorbar(im, ax=ax, fraction=0.045, pad=0.04)
    cbar.set_label(f"fraction of {N_SAMPLES} samples (final state)")
    save_fig(fig, output_dir, fname)


def plot_basin_heatmaps(output_dir):
    _plot_one_heatmap(
        output_dir, "high",
        f"Final-state occupancy: fraction ending in HIGH basin (X≥{HIGH_THRESHOLD})\n(dark = all high)",
        "schlogl_heatmap_pct_high.png")
    _plot_one_heatmap(
        output_dir, "low",
        f"Final-state occupancy: fraction ending in LOW basin (X<{LOW_THRESHOLD})\n(dark = all low)",
        "schlogl_heatmap_pct_low.png")


# Figure 4: RobTL three-valued truth vs threshold (mid initial condition)
def plot_robustness(output_dir):
    for scenario in SCENARIO_TAGS:
        for alpha in SCENARIO_ALPHAS[scenario]:
            df = load_evalr(scenario, alpha, "mid")
            if df is None or df.empty:
                print(f"  Fig 4: missing {evalr_name(scenario, alpha, 'mid')}", file=sys.stderr)
                continue
            df = df.sort_values("threshold")
            color = TRACE_COLORS[scenario][alpha]

            fig, ax = plt.subplots(figsize=(9, 5))
            ax.step(df["threshold"], df["truth_value"], where="post",
                    color=color, linewidth=1.8, zorder=2)
            ax.scatter(df["threshold"], df["truth_value"], color=color, s=42,
                       zorder=3, edgecolor="black", linewidth=0.5)
            ax.axhline(0, color="grey", linewidth=0.8, alpha=0.6)
            ax.set_yticks([-1, 0, 1])
            ax.set_yticklabels(["−false (−1)", "unknown (0)", "true (1)"])
            ax.set_ylim(-1.3, 1.3)
            ax.set_xlabel("Threshold η (max normalised d(X))")
            ax.set_ylabel("RobTL truth value")
            ax.set_title(f"{SCENARIO_PRETTY[scenario]}, "
                         rf"$\alpha={alpha:g}$ — Robustness (start_mid)")
            ax.grid(True, alpha=0.3)
            save_fig(fig, output_dir,
                     f"schlogl_robustness_{scenario}_alpha{_alpha_tag(alpha)}_mid.png")


# Figure 5: adaptability evaluation heatmap (low and high basins, all perturbed conditions)
def plot_adaptability(output_dir):
    grid     = np.full((len(ADAPT_BASINS), len(ADAPT_ORDER)), np.nan)
    eta_grid = np.full_like(grid, np.nan)

    for i, basin in enumerate(ADAPT_BASINS):
        for j, (scenario, alpha) in enumerate(ADAPT_ORDER):
            result = load_adapt(scenario, alpha, basin)
            if result is not None:
                eta_grid[i, j], grid[i, j] = result

    if np.all(np.isnan(grid)):
        print("Fig 5: no schlogl_adapt_*.csv found.", file=sys.stderr)
        return

    col_labels = [rf"{SCENARIO_SHORT[s]} $\alpha={a:g}$" for s, a in ADAPT_ORDER]

    cmap = plt.get_cmap("RdYlGn")
    cmap.set_bad("lightblue")

    fig, ax = plt.subplots(figsize=(11, 3.8))
    im = ax.imshow(np.ma.masked_invalid(grid), aspect="auto", vmin=-1.0, vmax=1.0, cmap=cmap)

    ax.set_xticks(range(len(ADAPT_ORDER)))
    ax.set_xticklabels(col_labels, rotation=30, ha="right", fontsize=9)
    ax.set_yticks(range(len(ADAPT_BASINS)))
    ax.set_yticklabels([BASIN_SHORT[b] for b in ADAPT_BASINS], fontsize=10)
    ax.set_title(
        r"Adaptability: $\phi^{\mathrm{basin}} = \rho^{\mathrm{basin}} \Rightarrow "
        r"\sup_t\, d(X)(t) \leq \eta_1$",
        fontsize=11)

    # Divider between S2 columns (0-2) and S1 columns (3-5).
    ax.axvline(2.5, color="black", linewidth=1.5, alpha=0.6)

    for i in range(len(ADAPT_BASINS)):
        for j in range(len(ADAPT_ORDER)):
            v, eta = grid[i, j], eta_grid[i, j]
            if np.isnan(v):
                ax.text(j, i, "n/a", ha="center", va="center", fontsize=9)
            else:
                label = TRUTH_LABEL.get(v, f"{v:g}")
                eta_str = f"\nη₁={eta:.4f}" if not np.isnan(eta) else ""
                text_color = "black" if v == 0.0 else "white"
                ax.text(j, i, label + eta_str, ha="center", va="center",
                        fontsize=8, color=text_color, linespacing=1.5)

    cbar = fig.colorbar(im, ax=ax, fraction=0.045, pad=0.04, ticks=[-1, 0, 1])
    cbar.set_ticklabels(["false (−1)", "unknown (0)", "true (1)"])
    cbar.set_label("adaptability truth value")
    save_fig(fig, output_dir, "schlogl_adaptability.png")


def main():
    output_dir = _data_dir()
    print(f"Schlogl plots — reading and writing to {output_dir}")

    print("Figure 1: final-X histograms")
    plot_final_x_histograms(output_dir)
    print("Figure 2: sample traces")
    plot_sample_traces(output_dir)
    print("Figure 3: basin-ending heatmaps")
    plot_basin_heatmaps(output_dir)
    print("Figure 4: robustness vs threshold")
    plot_robustness(output_dir)
    print("Figure 5: adaptability evaluation")
    plot_adaptability(output_dir)
    print("Done.")


if __name__ == "__main__":
    main()
