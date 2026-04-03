import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import ProgressBar from '../components/ProgressBar'
import StatusBadge from '../components/StatusBadge'
import { sipService } from '../services/sipService'

const fmt = (n) => `₹${Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
const fmtDate = (d) => new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })

export default function SipDetailsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [sip, setSip] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    sipService.getSipById(id)
      .then(setSip)
      .catch(() => setError('Failed to load SIP details'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <>
      <Navbar />
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    </>
  )

  if (error || !sip) return (
    <>
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 py-12 text-center">
        <p className="text-red-500">{error || 'SIP not found'}</p>
        <button onClick={() => navigate('/dashboard')} className="btn-secondary mt-4">← Back</button>
      </div>
    </>
  )

  return (
    <>
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
        <button onClick={() => navigate('/dashboard')} className="text-sm text-gray-400 hover:text-gray-600 flex items-center gap-1 mb-6">
          ← Back to Dashboard
        </button>

        {/* Header card */}
        <div className="card p-6 mb-5">
          <div className="flex items-start justify-between mb-5">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <h1 className="text-2xl font-bold text-gray-900">{fmt(sip.amount)}</h1>
                <StatusBadge status={sip.status} />
              </div>
              <p className="text-sm text-gray-500">
                {sip.frequency} · {sip.isSip ? 'SIP' : 'Recurring Deposit'} · Passbook: <span className="font-mono font-medium">{sip.passbookId}</span>
              </p>
            </div>
            <div className="text-right">
              <p className="text-xs text-gray-400">Interest Rate</p>
              <p className="text-xl font-bold text-blue-700">{sip.interestRate}%</p>
              <p className="text-xs text-gray-400">{sip.trust ? 'Custom rate' : 'Default rate'}</p>
            </div>
          </div>

          {/* Stats row */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
            <div className="bg-gray-50 rounded-xl p-3 text-center">
              <p className="text-xs text-gray-400 mb-1">Total</p>
              <p className="text-xl font-bold text-gray-800">{sip.totalInstallments}</p>
              <p className="text-xs text-gray-400">installments</p>
            </div>
            <div className="bg-green-50 rounded-xl p-3 text-center">
              <p className="text-xs text-gray-400 mb-1">Completed</p>
              <p className="text-xl font-bold text-green-700">{sip.completedInstallments}</p>
              <p className="text-xs text-gray-400">done</p>
            </div>
            <div className="bg-yellow-50 rounded-xl p-3 text-center">
              <p className="text-xs text-gray-400 mb-1">Pending</p>
              <p className="text-xl font-bold text-yellow-700">{sip.pendingInstallments}</p>
              <p className="text-xs text-gray-400">remaining</p>
            </div>
            <div className="bg-blue-50 rounded-xl p-3 text-center">
              <p className="text-xs text-gray-400 mb-1">Started</p>
              <p className="text-sm font-bold text-blue-700">{fmtDate(sip.startDate)}</p>
              <p className="text-xs text-gray-400">start date</p>
            </div>
          </div>

          {/* Progress bar */}
          <ProgressBar completed={sip.completedInstallments} total={sip.totalInstallments} />
        </div>

        {/* Financial summary */}
        <div className="grid grid-cols-3 gap-4 mb-5">
          <div className="card p-4 text-center">
            <p className="text-xs text-gray-400 mb-1">Contribution</p>
            <p className="text-lg font-bold text-gray-900">{fmt(sip.totalContribution)}</p>
          </div>
          <div className="card p-4 text-center">
            <p className="text-xs text-gray-400 mb-1">Interest</p>
            <p className="text-lg font-bold text-green-600">{fmt(sip.totalInterest)}</p>
          </div>
          <div className="card p-4 text-center">
            <p className="text-xs text-gray-400 mb-1">Current Value</p>
            <p className="text-lg font-bold text-blue-700">{fmt(sip.currentAmount)}</p>
          </div>
        </div>

        {/* Transactions table */}
        <div className="card overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-50">
            <h2 className="text-base font-semibold text-gray-900">Transaction History</h2>
          </div>

          {!sip.transactions || sip.transactions.length === 0 ? (
            <div className="p-12 text-center text-gray-400 text-sm">No transactions yet</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 text-gray-400 text-xs uppercase tracking-wide">
                    <th className="px-6 py-3 text-left font-medium">#</th>
                    <th className="px-6 py-3 text-left font-medium">Date</th>
                    <th className="px-6 py-3 text-right font-medium">Amount</th>
                    <th className="px-6 py-3 text-right font-medium">Interest</th>
                    <th className="px-6 py-3 text-right font-medium">Total</th>
                    <th className="px-6 py-3 text-center font-medium">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {sip.transactions.map((t) => (
                    <tr key={t.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-3.5 text-gray-500 font-medium">{t.installmentNumber}</td>
                      <td className="px-6 py-3.5 text-gray-700">{fmtDate(t.transactionDate)}</td>
                      <td className="px-6 py-3.5 text-right text-gray-900 font-medium">{fmt(t.amount)}</td>
                      <td className="px-6 py-3.5 text-right text-green-600">{fmt(t.interest)}</td>
                      <td className="px-6 py-3.5 text-right text-blue-700 font-semibold">{fmt(t.totalAmount)}</td>
                      <td className="px-6 py-3.5 text-center">
                        <StatusBadge status={t.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
