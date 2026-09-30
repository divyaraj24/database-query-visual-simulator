# Research Notes — Database Query Visual Simulator

Research phase output, per `requirements_for_work.txt` step 1. Covers: base paper analysis, presentation analysis, literature review of the 10 reference papers, and the research gap. This is the raw research material the paper draft will be built from — not the draft itself.

---

## 1. Base Paper Analysis

**"When Temporary Results Meet Intermediate Index: An Optimization Technique of Procedural SQL Query Processing"**
Rahman, Hashmi, Gnanajothy, Lee — *Data & Knowledge Engineering*, 164 (2026) 102577.

### Problem
Procedural SQL (stored procedures, UDFs) mixes declarative SELECTs with imperative control flow. Intermediate results from one statement (a "producer") get stored in a temp table and reused by later statements ("consumers"). Existing auto-indexing tools (AutoAdmin, HypoPG, Oracle Auto Indexing) only index **base tables** — none of them index these **temporary/intermediate** tables, so consumers of intermediate results are stuck doing full scans.

### Proposed solution — AutoCox
- Models the program as a **statement graph** (a control-flow graph where nodes are SQL assignment statements, tracking producer→consumer data-flow).
- Defines an **intermediate index**: `IIndex(π_producer, θ_consumer)` — built on the producer's output columns, but only for columns consumer statements actually filter/join on.
- Uses a **what-if cost model** (no real index is built) that estimates: full-scan cost of consumers *without* the index vs. (index-creation cost + index-scan cost) *with* it. Index is only created if the net benefit is positive.
- The index is scoped to a single program execution — created after the producer statement, dropped at the end, so it never risks stale data.
- Algorithm: extract producer/consumer statements → find filter/join columns on the producer's projected columns → decide composite vs. shared vs. per-consumer index → run what-if cost comparison → rewrite the physical plan if beneficial.

### Results
- Tested on S-SOAP (125 synthetic stored procedures) and P-TPC-DS (procedural TPC-DS queries), compared against a state-of-the-art auto-indexing baseline.
- Up to **261× speedup**, ~**67% cost reduction** on dependent queries, on the highest-reuse S-SOAP programs.
- Cost model validated against real measured execution time (predicted vs. actual costs tracked closely).
- Benefit scales with the number of times a consumer re-accesses the same intermediate result — below a workload-specific access-count threshold, the technique correctly declines to create an index (avoids overhead on cheap cases).

### Why it's the base paper for this project
It is exactly a **hypothetical/what-if indexing cost model that compares "with index" vs "without index" cost and decides whether to apply it** — which is structurally identical to what the "What-If Index Toggle" feature in our simulator needs to visualize. The simulator's Cost Engine can mirror AutoCox's full-scan-vs-index-scan cost equations (Eq. 1–3 in the paper) as its own baseline heuristic formulas.

### Key limitation the project should be honest about
AutoCox operates on real database statistics obtained from a live optimizer (PostgreSQL catalogs, cardinality/selectivity estimates). Our simulator has **no live database at all** — it uses assumed row counts and flat selectivity heuristics. So we are not reproducing AutoCox's intermediate-index mechanism; we're borrowing its *what-if cost-comparison methodology* and applying it to a simpler, fully hypothetical, single-query (not multi-statement procedural) setting aimed at teaching/visualization rather than production optimization.

---

## 2. Presentation Analysis (Review-1 deck, already used in class)

15 slides, title: *"Database Query Visual Simulator"* (working title **QueryScope**). Structure already followed the required Review-1 format:

1. Problem — EXPLAIN output is hard to read, cost is opaque, live testing is risky.
2. Audience — students, junior devs, DBAs, backend engineers.
3. Introduction — what query optimization is, why it's invisible, where simulation helps.
4. Proposed solution — 3-stage pipeline: ANTLR4 parsing → Java cost engine → React Flow visualization, with what-if index toggles.
5. Base paper tie-in (slide 6) — frames AutoCox's what-if cost model as the thing the simulator visualizes.
6. Literature review summary (4 clustered themes, not per-paper).
7. Research gap (3 gaps, see below — same shape needed for the paper).
8. Objectives (5 listed).
9. Scope/MVP (in/out lists — matches `requirements_for_work.txt` team doc).
10. Methodology (6-step pipeline).
11. Expected outcomes.
12. Risks and mitigation.
13. Conclusion.
14. References (uses the **first** reference list variant — see note in §4 below).

**Takeaway:** the presentation already has working versions of Introduction, Objectives, Literature Review, and Research Gap content. The paper draft should tighten/expand these into full prose, not invent new positioning.

---

## 3. Literature Review — Reference Papers

**Reference list source of truth: the presentation's References slide (slide 15)** — confirmed as the latest/authoritative version. In that list, item [8] is the base paper itself (Rahman, Hashmi, Gnanajothy & Lee, DKE 2026), cited again in the numbered bibliography alongside the 9 other related-work papers. So the literature review below covers **9 distinct related-work papers**; the base paper is analyzed separately in §1 and keeps its slot as reference [8] in the final numbered References section.

Each entry below was checked against the actual paper/abstract (not just the group chat's ChatGPT summary), via publisher pages or the PDF itself where accessible.

| # | Paper | Verified summary |
|---|-------|-------------------|
| 1 | Rahman, Islam, Kamruzzaman & Joy, "Advanced Query Optimization in SQL Databases for Real-Time Big Data Analytics," *AJBAIS* 4(3), 2024. [doi:10.69593/ajbais.v4i3.77](https://doi.org/10.69593/ajbais.v4i3.77) | Studies multi-level indexing, query rewriting, and dynamic execution plans on large SQL databases. Reports roughly 40% lower retrieval time from multi-level indexing, 35% lower execution time from rewriting, 25% better resource utilization from dynamic plans. Main point: combining several optimization techniques beats relying on any single static one. |
| 2 | Uzzaman, Jim, Nishat & Nahar, "Optimizing SQL Databases for Big Data Workloads: Techniques and Best Practices," *AJBAIS* 4(3), 2024. [doi:10.69593/ajbais.v4i3.78](https://doi.org/10.69593/ajbais.v4i3.78) | Practitioner-style survey of indexing, partitioning, sharding, and caching for big-data SQL workloads, using literature analysis plus case studies/expert interviews. Conclusion: no single technique suffices; effective scaling needs several combined. |
| 3 | V. Panwar, "AI-Driven Query Optimization: Revolutionizing Database Performance and Efficiency," *IJCTT* 72(3), 2024. [doi:10.14445/22312803/IJCTT-V72I3P103](https://doi.org/10.14445/22312803/IJCTT-V72I3P103) | Reviews ML-based query optimization (decision trees, random forests, neural nets) trained on historical query workloads, applied to SQL and NoSQL systems. Cites gains of ~40% execution time, ~25% CPU, ~30% memory, >90% cost-prediction accuracy vs. traditional optimizers, but flags model overhead, retraining cost, and legacy-DBMS integration as open problems. |
| 4 | Ikram & Mazher, "Query Optimization Techniques in SQL: A Performance-Driven Perspective," *PRJCS* 2(2), 2025. | Performance-oriented survey covering indexing, join optimization, subquery transforms, cost-based optimization, materialized views, and caching. Explicitly notes the index trade-off: faster reads, but extra storage and write-maintenance overhead — directly relevant to our what-if index toggle framing. |
| 5 | Gretscher & Dittrich, "How to Optimize SQL Queries? A Comparison Between Split, Holistic, and Hybrid Approaches," *PVLDB* 18(11), 2025. [doi:10.14778/3749646.3749663](https://doi.org/10.14778/3749646.3749663) — **read in full from the VLDB PDF.** | Frames query optimization as choosing a physical execution plan (QOP) in two possible ways: **split** (algebraic plan chosen first, physical operators chosen after — fast but only locally optimal) vs **holistic** (both chosen together in one search — globally optimal but exponentially slower). Proposes **top-k**, a hybrid that carries the top-k algebraic plans into physical optimization, empirically shown to reach near-holistic plan quality at close to split's speed. Directly useful: gives our cost engine formal vocabulary (algebraic vs. physical optimization, QOP) and a concrete justification for why our MVP's simpler heuristic approach is a reasonable, deliberate simplification of a genuinely hard search problem. |
| 6 | S. P. Panda, "Dynamic Cost-Aware SQL Rewriting Algorithm for Multi-Cloud Query Optimization," *ICICNCT* 2025. [doi:10.1109/ICICNCT66124.2025.11233011](https://doi.org/10.1109/ICICNCT66124.2025.11233011) | Proposes DCAS, a rewriting algorithm for cross-cloud query optimization that factors in CPU, I/O, network egress, and cloud pricing (not just latency), using predicate pushdown, join reordering, and Q-learning over historical execution feedback. Relevant mainly as evidence that "cost" in modern optimizers is multi-dimensional — a nuance our MVP intentionally scopes out (single-node, heuristic cost only). |
| 7 | T. Juopperi, "AI-Driven SQL Query Optimization Techniques," Bachelor's thesis, Haaga-Helia UAS, 2024. | Compares traditional vs. AI-assisted SQL optimization (quantitative + qualitative), including ChatGPT-based query rewriting. Concludes indexes and execution plans remain foundational even as AI tooling is layered on top, and flags integration/reliability concerns with AI-generated optimizations. |
| 9 | Shajeena, Shiny, Gayathri Devi & Vespa, "A Review of Query Optimization Techniques: From Traditional to Reinforcement Learning Approaches," *ICEARS* 2026. [doi:10.1109/ICEARS67481.2026.11416629](https://doi.org/10.1109/ICEARS67481.2026.11416629) | Traces the field from rule-/cost-based optimizers to RL-based ones; their own PPO-based implementation on PostgreSQL + IMDB benchmark reports 30–50% cost/performance improvement and ~40% average execution-time reduction over PostgreSQL's default planner across 100 IMDB queries. Notes scalability, generalization, and real-time inference overhead as unresolved issues with RL optimizers — a good concrete example of the "AI overhead" gap we cite. |
| 10 | Hettiarachchi & Yapa, "OptimAIzerSQL: Optimizing SQL Queries with Heuristic and ML-Based Multi-Agent Systems," *MLISE* 2025. [doi:10.1109/MLISE66443.2025.11100226](https://doi.org/10.1109/MLISE66443.2025.11100226) | Two-agent system: a heuristic Join Order Agent (branch-and-bound) plus a DQN-based Index Selection Agent, evaluated on IMDb/JOB. Reports improved plans/costs, with the index agent hitting ~72% selection accuracy. Limitations: training-data dependence, compute overhead, limited testing beyond IMDb/JOB. Reinforces the "hybrid multi-agent is powerful but opaque and heavy" pattern across papers 3, 6, 9, 10. |

*(Reference [8] is the base paper — see §1 for its full analysis. It is not re-summarized here to avoid duplication, but keeps number [8] in the final References list.)*

### Cross-cutting pattern across the 9 related-work papers
Two clusters emerge cleanly:
- **Papers 1, 2, 4** — traditional/heuristic techniques (indexing, partitioning, rewriting, caching, materialized views). Reliable and explainable, but static and manually tuned.
- **Papers 3, 6, 9, 10** — AI/ML/RL-driven or multi-agent optimizers. Better adaptability and measurable performance gains (25–50% ranges recur across papers 1, 3, 9), but consistently reported downsides: training/retraining cost, integration difficulty with existing DBMSs, and reduced interpretability of *why* a plan was chosen.
- **Paper 5** (Gretscher & Dittrich) and the base paper sit slightly apart — both are about the *mechanics* of plan search/cost evaluation itself (split vs. holistic search; what-if indexing) rather than being "traditional vs AI" — they're the most directly technically relevant to building a cost engine.
- **Paper 7** (Juopperi) bridges both clusters directly and is useful for framing.

None of the 9 related-work papers, nor the base paper, produce an **interactive, explorable visualization** of a query plan or of what-if index effects. They report numbers in tables/text or, at best, static plan diagrams in a paper. That absence is the seed of the research gap below.

---

## 4. Reference list — resolved

Two slightly different 10-paper lists existed across the source material (the presentation's References slide / `requirements_for_work.txt`'s first list, vs. a revised list in the WhatsApp chat literature-review table). **Confirmed: the presentation's list is the latest and authoritative one.** Its item [8] is the base paper itself, appearing twice by design — once as the anchor "base paper" (§1) and once with a reference number in the bibliography, since it is also formally cited in-text. The chat's alternate item [8] (a second Hettiarachchi & Yapa paper on join order/index selection review) is **not** used. The table in §3 above reflects this.

---

## 5. Research Gap (draft — to be refined into final "Research Gap Identified" section)

**Gap 1 — Lack of visual/interactive explanation.**
Every reviewed paper (traditional or AI-driven) reports costs as numbers or static EXPLAIN-style plans. None give an interactive way to *see* why a plan was chosen or how changing an index would change it.

**Gap 2 — AI/ML/RL optimizers trade interpretability for performance.**
Papers 3, 6, 9, 10 consistently show good performance gains from ML/RL approaches, but just as consistently report overhead, retraining cost, and reduced transparency into individual decisions — a genuine tension the field hasn't resolved.

**Gap 3 — No safe, data-free way to test hypothetical indexes.**
The base paper's AutoCox is the closest match (what-if cost modeling for indexes) but requires a live database and real optimizer statistics. Developers/DBAs otherwise have to either run `EXPLAIN` against production-scale data (risky, costly) or reason manually — there's no lightweight sandbox for exploring "what would this index do" before committing to schema/data.

**Summary — how this project addresses the gaps:**
The Database Query Visual Simulator (QueryScope) directly targets all three: it turns query cost analysis into an interactive node-graph (Gap 1); it deliberately uses transparent, fixed heuristic cost formulas instead of an opaque ML model, trading some accuracy for full explainability (Gap 2); and it lets users toggle hypothetical indexes and see the cost impact instantly, without a live database or real data (Gap 3, directly extending the base paper's what-if methodology into a zero-data, visual context).

---

## 6. Open items before the paper draft can be finalized
1. ~~Confirm reference list~~ — resolved: presentation's list is authoritative (§4).
2. Team/course details still needed for the Title & Author page: full team member names, course name, guide name (placeholders only in the current deck).
3. Everything above is written in the team's own words, cross-checked against primary sources — safe to build the Abstract/Introduction/Literature Review directly from this file.
