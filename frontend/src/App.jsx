import { useState } from 'react';
import ReactFlow, { Background, Controls } from 'reactflow';
import 'reactflow/dist/style.css';
import { planToFlow } from './planLayout';
import './App.css';

const API_URL = 'http://localhost:8080/api/analyze';
const RECOMMEND_URL = 'http://localhost:8080/api/recommend';
const SENSITIVITY_URL = 'http://localhost:8080/api/sensitivity';

const SAMPLE_DDL =
  'CREATE TABLE small_table (id INT PRIMARY KEY);\n' +
  'CREATE TABLE big_table (id INT PRIMARY KEY, small_id INT);';
const SAMPLE_QUERY =
  'SELECT * FROM small_table JOIN big_table ON small_table.id = big_table.small_id';

function PlanCanvas({ title, plan, cost }) {
  if (!plan) {
    return (
      <div className="plan-panel">
        <h3>{title}</h3>
        <p className="placeholder">Run an analysis to see this plan.</p>
      </div>
    );
  }
  const { nodes, edges } = planToFlow(plan, title.replace(/\s/g, ''));
  return (
    <div className="plan-panel">
      <h3>
        {title} — cost {cost.toFixed(2)}
      </h3>
      <div className="flow-canvas">
        <ReactFlow nodes={nodes} edges={edges} fitView proOptions={{ hideAttribution: true }}>
          <Background />
          <Controls />
        </ReactFlow>
      </div>
    </div>
  );
}

export default function App() {
  const [ddl, setDdl] = useState(SAMPLE_DDL);
  const [query, setQuery] = useState(SAMPLE_QUERY);
  const [tableStatsText, setTableStatsText] = useState('small_table=100\nbig_table=100000');
  const [indexTable, setIndexTable] = useState('big_table');
  const [indexColumn, setIndexColumn] = useState('small_id');
  const [useIndex, setUseIndex] = useState(true);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [recommendations, setRecommendations] = useState(null);
  const [recommendLoading, setRecommendLoading] = useState(false);
  const [sensitivity, setSensitivity] = useState(null);
  const [sensitivityLoading, setSensitivityLoading] = useState(false);

  function parseTableStats(text) {
    const stats = {};
    text.split('\n').forEach((line) => {
      const [table, rows] = line.split('=').map((s) => s.trim());
      if (table && rows) {
        stats[table] = Number(rows);
      }
    });
    return stats;
  }

  async function runAnalysis() {
    setLoading(true);
    setError(null);
    try {
      const hypotheticalIndexes = useIndex && indexTable && indexColumn
        ? [{ table: indexTable, column: indexColumn }]
        : [];
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ddl,
          query,
          tableStats: parseTableStats(tableStatsText),
          hypotheticalIndexes,
        }),
      });
      const body = await response.json();
      if (!response.ok) {
        throw new Error(body.error || 'Request failed');
      }
      setResult(body);
    } catch (err) {
      setError(err.message);
      setResult(null);
    } finally {
      setLoading(false);
    }
  }

  async function runRecommend() {
    setRecommendLoading(true);
    setError(null);
    try {
      const response = await fetch(RECOMMEND_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ddl, query, tableStats: parseTableStats(tableStatsText) }),
      });
      const body = await response.json();
      if (!response.ok) {
        throw new Error(body.error || 'Request failed');
      }
      setRecommendations(body);
    } catch (err) {
      setError(err.message);
      setRecommendations(null);
    } finally {
      setRecommendLoading(false);
    }
  }

  function applyRecommendation(rec) {
    setIndexTable(rec.table);
    setIndexColumn(rec.column);
    setUseIndex(true);
  }

  async function runSensitivity() {
    setSensitivityLoading(true);
    setError(null);
    try {
      const response = await fetch(SENSITIVITY_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ddl, query, tableStats: parseTableStats(tableStatsText) }),
      });
      const body = await response.json();
      if (!response.ok) {
        throw new Error(body.error || 'Request failed');
      }
      setSensitivity(body);
    } catch (err) {
      setError(err.message);
      setSensitivity(null);
    } finally {
      setSensitivityLoading(false);
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>QueryScope — Database Query Visual Simulator</h1>
        <p>Parse a hypothetical schema + query, estimate cost, and toggle a what-if index.</p>
      </header>

      <div className="app-body">
        <aside className="input-panel">
          <label>
            Schema (CREATE TABLE)
            <textarea rows={5} value={ddl} onChange={(e) => setDdl(e.target.value)} />
          </label>
          <label>
            Query (SELECT)
            <textarea rows={3} value={query} onChange={(e) => setQuery(e.target.value)} />
          </label>
          <label>
            Assumed row counts (one per line, table=rows)
            <textarea
              rows={3}
              value={tableStatsText}
              onChange={(e) => setTableStatsText(e.target.value)}
            />
          </label>

          <fieldset className="index-toggle">
            <legend>What-if index</legend>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={useIndex}
                onChange={(e) => setUseIndex(e.target.checked)}
              />
              Apply hypothetical index on
            </label>
            <div className="index-fields">
              <input
                placeholder="table"
                value={indexTable}
                onChange={(e) => setIndexTable(e.target.value)}
              />
              <input
                placeholder="column"
                value={indexColumn}
                onChange={(e) => setIndexColumn(e.target.value)}
              />
            </div>
          </fieldset>

          <div className="button-row">
            <button onClick={runAnalysis} disabled={loading}>
              {loading ? 'Analyzing…' : 'Analyze'}
            </button>
            <button onClick={runRecommend} disabled={recommendLoading} className="secondary">
              {recommendLoading ? 'Ranking…' : 'Recommend indexes'}
            </button>
          </div>

          <button onClick={runSensitivity} disabled={sensitivityLoading} className="secondary full-width">
            {sensitivityLoading ? 'Analyzing…' : 'Sensitivity analysis'}
          </button>

          {error && <p className="error">{error}</p>}

          {recommendations && (
            <div className="recommendations">
              <p className="recommendations-title">
                Ranked candidates (baseline cost {recommendations.baselineCost.toFixed(2)})
              </p>
              {recommendations.recommendations.length === 0 && (
                <p className="placeholder">No filter or join columns to index in this query.</p>
              )}
              {recommendations.recommendations.map((rec) => (
                <button
                  key={`${rec.table}.${rec.column}`}
                  className="recommendation-row"
                  onClick={() => applyRecommendation(rec)}
                >
                  <span>
                    {rec.table}.{rec.column}
                  </span>
                  <span className={rec.percentImprovement > 0 ? 'improvement' : ''}>
                    {rec.percentImprovement > 0 ? `-${rec.percentImprovement.toFixed(1)}%` : 'no change'}
                  </span>
                </button>
              ))}
            </div>
          )}

          {sensitivity && (
            <div className="recommendations">
              <p className="recommendations-title">Sensitivity (crossover points)</p>
              {sensitivity.scanCrossovers.map((c) => (
                <p key={`scan-${c.table}.${c.column}`} className="sensitivity-line">
                  <strong>
                    {c.table}.{c.column}
                  </strong>{' '}
                  ({Math.round(c.totalRows)} rows):{' '}
                  {c.crossoverSelectivityPercent != null
                    ? `index wins below ~${c.crossoverSelectivityPercent.toFixed(1)}% selectivity`
                    : c.indexAlwaysCheaper
                    ? 'index always wins in the tested range'
                    : 'seq scan always wins, table too small to benefit'}
                </p>
              ))}
              {sensitivity.joinCrossovers.map((j) => (
                <p key={`join-${j.outerTable}-${j.innerTable}`} className="sensitivity-line">
                  <strong>
                    {j.outerTable} ⋈ {j.innerTable}
                  </strong>{' '}
                  (inner {Math.round(j.innerRows)} rows):{' '}
                  {j.crossoverOuterRows != null
                    ? `indexed nested loop wins only while the outer side stays below ~${Math.round(
                        j.crossoverOuterRows
                      )} rows`
                    : 'one strategy dominates across the tested range'}
                </p>
              ))}
              {sensitivity.scanCrossovers.length === 0 && sensitivity.joinCrossovers.length === 0 && (
                <p className="placeholder">No filters or joins to analyze in this query.</p>
              )}
            </div>
          )}

          {result && (
            <div className="summary">
              <p>Baseline cost: {result.baselineCost.toFixed(2)}</p>
              <p>With-index cost: {result.withIndexCost.toFixed(2)}</p>
              <p className={result.percentImprovement > 0 ? 'improvement' : ''}>
                Change: {result.percentImprovement.toFixed(1)}%
              </p>
            </div>
          )}
        </aside>

        <main className="canvases">
          <PlanCanvas title="Baseline plan" plan={result?.baselinePlan} cost={result?.baselineCost ?? 0} />
          <PlanCanvas
            title="With-index plan"
            plan={result?.withIndexPlan}
            cost={result?.withIndexCost ?? 0}
          />
        </main>
      </div>
    </div>
  );
}
