import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import SipCard from '../components/SipCard'
import { sipService } from '../services/sipService'

const fmt = (n) => `₹${Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`

function SummaryCard({ label, value, color = 'text-gray-900', bg = 'bg-white' }) {
  return (
    <div className={`${bg} card p-5`}>
      <p className="text-xs font-medium text-gray-400 uppercase tracking-wide mb-1">{label}</p>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
    </div>
  )
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    sipService.getDashboard()
      .then(setDashboard)
      .catch(() => setError('Failed to load dashboard'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <>
      <Navbar />
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    </>
  )

  return (
    <>
      <Navbar />
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8">

        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-gray-400 text-sm mt-0.5">Track all your SIP investments</p>
          </div>
          <button onClick={() => navigate('/sip/create')} className="btn-primary flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Create SIP
          </button>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 text-red-600 rounded-xl text-sm">{error}</div>
        )}

        {dashboard && (
          <>
            {/* Summary cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
              <SummaryCard label="Total Contribution" value={fmt(dashboard.totalContribution)} />
              <SummaryCard label="Total Interest Earned" value={fmt(dashboard.totalInterest)} color="text-green-600" />
              <SummaryCard label="Current Value" value={fmt(dashboard.currentAmount)} color="text-blue-700" />
            </div>

            {/* SIP stats row */}
            <div className="flex items-center gap-6 mb-6">
              <p className="text-lg font-semibold text-gray-800">
                Your SIPs
                <span className="ml-2 text-sm font-normal text-gray-400">({dashboard.totalSips} total)</span>
              </p>
              <div className="flex gap-3 text-xs">
                <span className="bg-blue-50 text-blue-700 px-2.5 py-1 rounded-full font-medium">
                  {dashboard.activeSips} Active
                </span>
                <span className="bg-green-50 text-green-700 px-2.5 py-1 rounded-full font-medium">
                  {dashboard.completedSips} Completed
                </span>
              </div>
            </div>

            {/* SIP cards grid */}
            {dashboard.sips.length === 0 ? (
              <div className="card p-16 text-center">
                <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-4">
                  <svg className="w-8 h-8 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <h3 className="text-lg font-semibold text-gray-700 mb-2">No SIPs yet</h3>
                <p className="text-gray-400 text-sm mb-6">Start your investment journey by creating your first SIP</p>
                <button onClick={() => navigate('/sip/create')} className="btn-primary mx-auto">
                  Create your first SIP
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {dashboard.sips.map(sip => <SipCard key={sip.id} sip={sip} />)}
              </div>
            )}
          </>
        )}
      </div>
    </>
  )
}
