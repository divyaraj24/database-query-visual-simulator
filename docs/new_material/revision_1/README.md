# Revision 1 — UI polish screenshots

Taken after the visual redesign pass (header legend, cost-comparison banner, section
grouping, richer node colors) and five bug fixes found while testing: fitView over-zoom
on sparse trees, a misleading "no change" bar that still rendered full green, a backend
gap where querying an undeclared table silently fabricated a plan instead of erroring,
a leftover Vite template stylesheet (`index.css`) that was rendering the header title in
near-black instead of white, and the recommend-panel screenshot originally being taken
without running Analyze first (so the plan graphs showed empty).

Not yet added to the paper. Kept here for review before deciding what goes in.

| File | Scenario | Paper status |
|---|---|---|
| `01_default_join.png` | Join scan→nested-loop switch (small_table/big_table, −64.3%) | Already in paper (Figure 5 / Table 1) — same numbers, restyled |
| `02_scan_equality.png` | Single-table equality scan switch (customers.city, −98.0%) | Already in paper (Figure 4 / Table 1) — same numbers, restyled |
| `03_scan_range_no_benefit.png` | Range filter (`price > 500`), modest −17.5% gain | **New** — not in paper; didn't hit the intended "no benefit" case |
| `04_three_table_join_sort.png` | 3-table join + ORDER BY → Sort/Hash/Nested-Loop, 4-level tree | **New** — not in paper; shows the system beyond the two canonical examples |
| `05_recommend_panel.png` | Automatic index recommendation, 1 candidate (PK excluded) | Already in paper (Figure 7 / Table 2) — restyled, now correctly 1 row |
| `06_sensitivity_panel.png` | Sensitivity/crossover panel (~281 rows) | Already in paper (Figure 8 / Table 3) — same number, restyled |
| `07_no_index_available.png` | `SELECT * FROM logs`, nothing to index — honest "no change" | **New** — not in paper; shows the tool doesn't always claim a win |
| `08_error_state.png` | Query references an undeclared table → clear error | **New** — not in paper; robustness/validation fix, not a results figure |

Rows 01, 02, 05, 06 are safe straight swaps for their current paper counterparts.
Rows 03, 04, 07, 08 are optional additions, pending a decision on scope.
