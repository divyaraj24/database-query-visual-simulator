const COLUMN_WIDTH = 220;
const ROW_HEIGHT = 120;

// kept in sync with the legend swatches in App.jsx and the CSS custom properties in App.css
export const NODE_COLORS = {
  SeqScan: { bg: '#f3b64b', text: '#2b1c04' },
  IndexScan: { bg: '#34c77b', text: '#ffffff' },
  HashJoin: { bg: '#4f93f7', text: '#ffffff' },
  NestedLoop: { bg: '#ef6868', text: '#ffffff' },
  Sort: { bg: '#b075e8', text: '#ffffff' },
};

// converts a backend PlanNode tree into React Flow nodes/edges
export function planToFlow(root, idPrefix) {
  const nodes = [];
  const edges = [];
  let leafCounter = { count: 0 };
  let idCounter = { count: 0 };

  function visit(node, depth, parentId) {
    const id = `${idPrefix}-${idCounter.count++}`;
    let x;

    if (!node.children || node.children.length === 0) {
      x = leafCounter.count * COLUMN_WIDTH;
      leafCounter.count += 1;
    } else {
      const childXs = node.children.map((child) => visit(child, depth + 1, id));
      x = childXs.reduce((a, b) => a + b, 0) / childXs.length;
    }

    const colors = NODE_COLORS[node.type] || { bg: '#e5e7eb', text: '#1e293b' };
    nodes.push({
      id,
      position: { x, y: depth * ROW_HEIGHT },
      data: {
        label: `${node.label}\nrows≈${Math.round(node.estimatedRows)} · cost≈${node.cost.toFixed(2)}`,
      },
      style: {
        background: colors.bg,
        color: colors.text,
        border: '1.5px solid rgba(0, 0, 0, 0.18)',
        borderRadius: 10,
        padding: '10px 12px',
        fontSize: 12.5,
        fontWeight: 500,
        lineHeight: 1.5,
        whiteSpace: 'pre-line',
        width: 195,
        boxShadow: '0 2px 6px rgba(15, 23, 42, 0.12)',
      },
    });

    if (parentId) {
      edges.push({
        id: `${id}-e`,
        source: parentId,
        target: id,
        style: { stroke: '#94a3b8', strokeWidth: 1.5 },
      });
    }

    return x;
  }

  if (root) {
    visit(root, 0, null);
  }
  return { nodes, edges };
}
