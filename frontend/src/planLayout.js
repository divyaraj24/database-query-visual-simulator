const COLUMN_WIDTH = 220;
const ROW_HEIGHT = 110;

const NODE_COLORS = {
  SeqScan: '#fde68a',
  IndexScan: '#86efac',
  HashJoin: '#93c5fd',
  NestedLoop: '#fca5a5',
  Sort: '#d8b4fe',
};

/**
 * Flattens a backend PlanNode tree (type/label/estimatedRows/cost/children)
 * into React Flow nodes + edges, laying leaves out left-to-right and
 * parents centered above their children.
 */
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

    nodes.push({
      id,
      position: { x, y: depth * ROW_HEIGHT },
      data: {
        label: `${node.label}\nrows≈${Math.round(node.estimatedRows)} · cost≈${node.cost.toFixed(2)}`,
      },
      style: {
        background: NODE_COLORS[node.type] || '#e5e7eb',
        border: '1px solid #4b5563',
        borderRadius: 8,
        padding: 8,
        fontSize: 12,
        whiteSpace: 'pre-line',
        width: 190,
      },
    });

    if (parentId) {
      edges.push({ id: `${id}-e`, source: id, target: parentId });
    }

    return x;
  }

  if (root) {
    visit(root, 0, null);
  }
  return { nodes, edges };
}
