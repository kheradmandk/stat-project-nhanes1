# Statistical Project - NHANES 2021-2023

Phase-by-phase statistical analysis project for the Statistics course (Track A: NHANES 2021-2023).

## Project card
See `PROJECT_CARD.md` for the statistical question, variables, population and limitations.

## Dataset
The data comes from the CDC NHANES August 2021 - August 2023 survey (files `DEMO_L`, `BMX_L`, `BPXO_L`).
Raw and converted data files are **not** in this repository because of size limits.
See `data/README.md` for the download and conversion steps.

## Requirements
- Java 23 (see `pom.xml`)
- Maven (dependencies: commons-csv, commons-math3, xchart are downloaded automatically)

## How to reproduce Phase 1
1. Put `DEMO_L.csv`, `BMX_L.csv` and `BPXO_L.csv` in the `data/` folder (see `data/README.md`).
2. Open the project in IntelliJ IDEA and run `org.example.Main`.
   It runs, in order:
   1. `Audit` - data audit, analytic-population flow and descriptive tables (printed to the console)
   2. `Analysis` - crude vs weighted estimates
   3. `Plots` - saves the four figures into `figures/`
3. Each class can also be run alone (each has its own `main`).

The console output of `Audit` is saved in `reports/phase1/audit_output.txt`.

## Structure
- `src/main/java/org/example/`: Java source code
  - `LoadData`: reads the CSV files
  - `MergeData`: merges the three files by `SEQN`
  - `Audit`: data-quality audit and analytic population
  - `Analysis`: descriptive statistics, crude vs weighted estimates
  - `Plots`: figures
  - `Main`: runs the steps above in order
- `data/`: data description only (`README.md`)
- `figures/`: generated plots
- `reports/phase1/`: Phase 1 report (PDF) and audit output
- `PROJECT_CARD.md`: project card

## Status
- Phase 1: done (tag `phase-1`)
