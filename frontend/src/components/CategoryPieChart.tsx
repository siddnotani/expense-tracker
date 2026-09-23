import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import type { PieLabelRenderProps } from 'recharts'
import type { CategoryBreakdown } from '../types/analytics'
import { formatCurrency } from '../utils/format'

const COLORS = [
  '#4f6df5',
  '#f5a04f',
  '#3cba8f',
  '#e0546c',
  '#9b6ff5',
  '#f5d24f',
  '#4fc3f5',
  '#8a8f99',
]

function renderLabel(props: PieLabelRenderProps) {
  const entry = props.payload as CategoryBreakdown | undefined
  if (!entry) return null
  const { cx, cy, midAngle, outerRadius } = props
  if (cx === undefined || cy === undefined || midAngle === undefined || outerRadius === undefined) {
    return null
  }
  const radius = Number(outerRadius) + 24
  const angle = (-Number(midAngle) * Math.PI) / 180
  const x = Number(cx) + radius * Math.cos(angle)
  const y = Number(cy) + radius * Math.sin(angle)
  return (
    <text
      x={x}
      y={y}
      fill="#1f2430"
      fontSize={12}
      textAnchor={x > Number(cx) ? 'start' : 'end'}
      dominantBaseline="central"
    >
      {`${entry.displayName} ${entry.percentage.toFixed(1)}%`}
    </text>
  )
}

interface Props {
  data: CategoryBreakdown[]
}

export function CategoryPieChart({ data }: Props) {
  return (
    <ResponsiveContainer width="100%" height={320}>
      <PieChart>
        <Pie
          data={data}
          dataKey="total"
          nameKey="displayName"
          cx="50%"
          cy="50%"
          outerRadius={100}
          label={renderLabel}
          isAnimationActive={false}
        >
          {data.map((entry, index) => (
            <Cell key={entry.category} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip formatter={(value) => formatCurrency(Number(value))} />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  )
}
