import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { MonthBreakdown } from '../types/analytics'
import { formatCurrency, formatMonth } from '../utils/format'

interface Props {
  data: MonthBreakdown[]
}

export function MonthlyBarChart({ data }: Props) {
  return (
    <ResponsiveContainer width="100%" height={320}>
      <BarChart data={data} margin={{ top: 8, right: 16, left: 8, bottom: 8 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="month" tickFormatter={formatMonth} />
        <YAxis tickFormatter={(v) => formatCurrency(Number(v))} width={90} />
        <Tooltip
          formatter={(value) => formatCurrency(Number(value))}
          labelFormatter={(label) => formatMonth(String(label))}
        />
        <Bar dataKey="total" fill="#4f6df5" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}
