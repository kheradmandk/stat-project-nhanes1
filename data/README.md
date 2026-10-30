# Data Source

The data used in this project is from the CDC NHANES August 2021 - August 2023 survey (public-use files, no access restrictions).

- Official source: https://wwwn.cdc.gov/nchs/nhanes/continuousnhanes/default.aspx?Cycle=2021-2023
- Files used: `DEMO_L.xpt` (demographics and sampling weights), `BMX_L.xpt` (body measures), `BPXO_L.xpt` (oscillometric blood pressure)
- Date downloaded: October 2026
- Merge key: `SEQN` (participant ID)

## Variables used

| File | Variable | Meaning |
|------|----------|---------|
| DEMO_L | `SEQN` | participant ID |
| DEMO_L | `RIDAGEYR` | age in years (80 = "80 and over", top-coded) |
| DEMO_L | `RIAGENDR` | sex (1 = male, 2 = female) |
| DEMO_L | `WTMEC2YR` | MEC exam sampling weight |
| DEMO_L | `SDMVSTRA`, `SDMVPSU` | design stratum and primary sampling unit (not yet used) |
| BMX_L | `BMXBMI` | body mass index (kg/m^2) |
| BPXO_L | `BPXOSY1` | systolic blood pressure, 1st reading (mmHg) |

## Reconstruction steps
1. Download the three `.xpt` files from the official CDC website above.
2. Convert each `.xpt` file to `.csv` and save them in this folder as `DEMO_L.csv`, `BMX_L.csv`, `BPXO_L.csv`.
3. Note: the converter adds an unnamed first column (row index). `LoadData.java` detects and handles it, and `SEQN` values like `130378.0` are normalised to `130378`.
4. Run `org.example.Main` (see the main `README.md`).
Expected sizes: DEMO_L 11933 rows, BMX_L 8860 rows, BPXO_L 7801 rows.
