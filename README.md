# RES BAMOF Simulator

Java 17 Maven simulator for the paper "Burst-Aware Microservice Orchestration for Cost-Efficient Dynamic Cloud Workloads".

The simulator reproduces the manuscript experiments for the proposed BADP and BADP+ schedulers and the comparison baselines EDF, HPA, AmRP, CDAScaler, CEMA, and CEDULE. It generates raw CSV data, aggregate summaries, Wilcoxon signed-rank statistics, manuscript tables, running-text values, and publication-style PNG figures.

## Eclipse Import

1. Open Eclipse.
2. Choose `File > Import > Maven > Existing Maven Projects`.
3. Select this folder.
4. Import `res-bamof-simulator`.

## Requirements

- Java 17 or newer
- Maven 3.8 or newer

The implementation uses only the Java standard library. Maven is used for compilation and convenient execution.

## Run

From Eclipse, run `org.bamof.Main`. With no arguments it executes the full manuscript experiment suite and prints detailed progress and metrics to the console.

Useful program arguments:

- `quick`: execute a reduced smoke-test configuration for checking code changes.

From a terminal with Maven:

```powershell
mvn compile exec:java
```

For a reduced smoke-test run:

```powershell
mvn compile exec:java -Dexec.args="quick"
```

Full manuscript outputs are written under `results/manuscript-run/`.
Quick smoke-test outputs are written under `results/quick-run/`.

- `raw/`: per-run CSV files.
- `aggregate/`: means and 95% confidence intervals.
- `timeseries/`: active VM time series.
- `stats/`: Wilcoxon signed-rank summaries.
- `figures/`: manuscript-style PNG figures.
- `manuscript/tables/`: manuscript tables as `.tex` and `.csv`.
- `manuscript/text/`: running-text replacement values and LaTeX macros.

Generated figures:

- `fig_bamof_architecture.png`
- `fig_operational_workflow.png`
- `fig_cost_comparison.png`
- `fig_deadline_satisfaction.png`
- `fig_credit_utilization.png`
- `fig_active_vm_count.png`
- `fig_burst_sensitivity_cost.png`
- `fig_burst_sensitivity_dsr.png`
- `fig_credit_regen_cost.png`
- `fig_credit_regen_cue.png`
- `fig_runtime_scalability.png`
- `fig_prediction_stress_dsr.png`
- `fig_prediction_stress_starvation.png`
- `fig_prediction_stress_fallback.png`

Generated manuscript tables:

- `table_i_notation`
- `table_ii_simulation_environment`
- `table_iii_vm_configuration`
- `table_iv_compared_algorithms`
- `table_v_experiment_design`
- `table_vi_default_parameters`
- `table_vii_comparative_summary`
- `table_viii_statistical_significance`

Generated running-text artifacts:

- `placeholder_values.csv`
- `running_text_placeholders.md`
- `running_text_macros.tex`

## Repository Contents

- `src/main/java/org/bamof/`: simulator source code
- `pom.xml`: Maven project configuration
- `README.md`: build and execution instructions
- `.gitignore`: excludes generated build and experiment outputs

Generated directories such as `target/` and `results/` are intentionally not committed. Re-run the simulator to regenerate them.
