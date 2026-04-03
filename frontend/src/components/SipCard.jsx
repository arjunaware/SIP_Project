import { useNavigate } from 'react-router-dom'
import StatusBadge from './StatusBadge'
import ProgressBar from './ProgressBar'

const fmt = (n) => `₹${Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`

export default function SipCard({ sip }) {
  const navigate = useNavigate()

  return (
    <div className="card p-5 hover:shadow-md transition-shadow">
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div>
          <p className="text-xs text-gray-400 font-medium uppercase tracking-wide mb-0.5">
            {sip.frequency} · {sip.isSip ? 'SIP' : 'RD'}
          </p>
          <p className="text-xl font-semibold text-gray-900">{fmt(sip.amount)}</p>
          <p className="text-xs text-gray-400 mt-0.5">per installment</p>
        </div>
        <StatusBadge status={sip.status} />
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-3 gap-2 mb-4">
        <div className="bg-gray-50 rounded-lg p-2 text-center">
          <p className="text-xs text-gray-400">Contributed</p>
          <p className="text-sm font-semibold text-gray-800">{fmt(sip.totalContribution)}</p>
        </div>
        <div className="bg-green-50 rounded-lg p-2 text-center">
          <p className="text-xs text-gray-400">Interest</p>
          <p className="text-sm font-semibold text-green-700">{fmt(sip.totalInterest)}</p>
        </div>
        <div className="bg-blue-50 rounded-lg p-2 text-center">
          <p className="text-xs text-gray-400">Rate</p>
          <p className="text-sm font-semibold text-blue-700">{sip.interestRate}%</p>
        </div>
      </div>

      {/* Progress */}
      <div className="mb-4">
        <ProgressBar completed={sip.completedInstallments} total={sip.totalInstallments} />
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between">
        <p className="text-xs text-gray-400">
          Started {new Date(sip.startDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
        </p>
        <button
          onClick={() => navigate(`/sip/${sip.id}`)}
          className="text-sm text-blue-600 hover:text-blue-800 font-medium transition-colors"
        >
          View Details →
        </button>
      </div>
    </div>
  )
}
