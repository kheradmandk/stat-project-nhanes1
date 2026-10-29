# Data Source

The data used in this project is from the CDC NHANES 2021-2023 survey.
- Official Source: https://wwwn.cdc.gov/nchs/nhanes/continuousnhanes/default.aspx?Cycle=2021-2023
- Files Used: DEMO_L.xpt, BMX_L.xpt, BPXO_L.xpt
- Date Downloaded:  October 2026

## Reconstruction Steps
1. Download the `.xpt` files from the official CDC website.
2. Convert the `.xpt` files to `.csv` format.
3. Note: The converter added an extra row index column, which is handled in `LoadData.java` by ignoring the first column.