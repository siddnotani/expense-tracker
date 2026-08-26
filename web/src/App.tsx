import { NavLink, Route, Routes } from 'react-router-dom';

type PageProps = { title: string; description: string };
const pages: Record<string, PageProps> = {
  dashboard: { title: 'Dashboard', description: 'Your expense overview will appear here.' },
  transactions: { title: 'Transactions', description: 'Track income and expenses in one place.' },
  accounts: { title: 'Accounts', description: 'Manage your accounts and balances.' },
  budgets: { title: 'Budgets', description: 'Plan monthly spending by category.' },
};

function Placeholder({ title, description }: PageProps) { return <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm"><h2 className="text-xl font-semibold">{title}</h2><p className="mt-2 text-slate-600">{description}</p></section>; }
function Login() { return <div className="mx-auto max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm"><h1 className="text-2xl font-semibold">Sign in</h1><p className="mt-2 text-slate-600">Authentication will be added in the next phase.</p></div>; }
function Shell() { return <div className="min-h-screen"><header className="border-b border-slate-200 bg-white"><div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4"><h1 className="font-semibold">Expense Tracker</h1><span className="text-sm text-slate-500">Personal finances</span></div></header><div className="mx-auto flex max-w-6xl gap-8 px-6 py-8"><nav className="w-44 shrink-0"><ul className="space-y-1">{[['/', 'Dashboard'], ['/transactions', 'Transactions'], ['/accounts', 'Accounts'], ['/budgets', 'Budgets']].map(([to, label]) => <li key={to}><NavLink to={to} className={({ isActive }) => `block rounded px-3 py-2 text-sm ${isActive ? 'bg-slate-200 font-medium' : 'text-slate-600 hover:bg-slate-100'}`}>{label}</NavLink></li>)}</ul></nav><main className="min-w-0 flex-1"><Routes><Route path="/" element={<Placeholder {...pages.dashboard} />} /><Route path="/transactions" element={<Placeholder {...pages.transactions} />} /><Route path="/accounts" element={<Placeholder {...pages.accounts} />} /><Route path="/budgets" element={<Placeholder {...pages.budgets} />} /></Routes></main></div></div>; }
export default function App() { return <Routes><Route path="/login" element={<Login />} /><Route path="*" element={<Shell />} /></Routes>; }
