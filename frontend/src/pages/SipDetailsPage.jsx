import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import ProgressBar from '../components/ProgressBar'
import { sipService } from '../services/sipService'

const fmt = (n) =>
  `₹${Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`

const fmtDate = (d) =>
  d
    ? new Date(d).toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      })
    : '—'

function StatusBadge({ status }) {
  const styles = {
    ACTIVE:    'bg-blue-100 text-blue-700',
    PAUSED:    'bg-amber-100 text-amber-700',
    COMPLETED: 'bg-green-100 text-green-700',
  }
  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${styles[status] ?? ''}`}>
      {status}
    </span>
  )
}

function TxStatusBadge({ status }) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
      status === 'COMPLETED' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
    }`}>
      {status}
    </span>
  )
}

export default function SipDetailsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [sip, setSip] = useState(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  useEffect(() => {
    sipService.getSipById(id)
      .then(setSip)
      .catch(() => setError('Failed to load SIP details'))
      .finally(() => setLoading(false))
  }, [id])

  const handlePause = async () => {
    setActionError('')
    setActionLoading(true)
    try {
      const updated = await sipService.pauseSip(sip.id)
      setSip(updated)
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to pause SIP')
    } finally {
      setActionLoading(false)
    }
  }

  const handleResume = async () => {
    setActionError('')
    setActionLoading(true)
    try {
      const updated = await sipService.resumeSip(sip.id)
      setSip(updated)
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to resume SIP')
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) return (
    <><Navbar /><div className="min-h-screen flex items-center justify-center">
      <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
    </div></>
  )

  if (error || !sip) return (
    <><Navbar /><div className="max-w-4xl mx-auto px-4 py-12 text-center">
      <p className="text-red-500">{error || 'SIP not found'}</p>
      <button onClick={() => navigate('/dashboard')} className="btn-secondary mt-4">← Back</button>
    </div></>
  )

  return (
    <>
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
        <button
          onClick={() => navigate('/dashboard')}
          className="text-sm text-gray-400 hover:text-gray-600 flex items-center gap-1 mb-6"
        >
          ← Back to Dashboard
        </button>

        {/* ── Header card ── */}
        <div className="card p-6 mb-5">
          <div className="flex items-start justify-between mb-5">
            <div>
              <div className="flex items-center gap-3 mb-1">
                <h1 className="text-2xl font-bold text-gray-900">{fmt(sip.amount)}</h1>
                <StatusBadge status={sip.status} />
              </div>
              <p className="text-sm text-gray-500">
                {sip.frequency} · {sip.isSip ? 'SIP' : 'RD'} ·{' '}
                Passbook: <span className="font-mono font-medium">{sip.passbookId}</span>
              </p>
            </div>

            {/* Pause / Resume buttons */}
            <div className="flex flex-col items-end gap-2">
              {sip.status === 'ACTIVE' && (
                <button
                  onClick={handlePause}
                  disabled={actionLoading}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
                             bg-amber-50 text-amber-700 hover:bg-amber-100 border border-amber-200
                             transition-colors disabled:opacity-50"
                >
                  {actionLoading
                    ? <span className="w-4 h-4 border-2 border-amber-600 border-t-transparent rounded-full animate-spin" />
                    : <svg width="14" height="14" viewBox="0 0 12 12" fill="currentColor">
                        <rect x="2" y="1" width="3" height="10" rx="1"/>
                        <rect x="7" y="1" width="3" height="10" rx="1"/>
                      </svg>
                  }
                  Pause SIP
                </button>
              )}
              {sip.status === 'PAUSED' && (
                <button
                  onClick={handleResume}
                  disabled={actionLoading}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
                             bg-blue-600 text-white hover:bg-blue-700
                             transition-colors disabled:opacity-50"
                >
                  {actionLoading
                    ? <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    : <svg width="14" height="14" viewBox="0 0 12 12" fill="currentColor">
                        <path d="M3 1.5L10 6L3 10.5V1.5Z"/>
                      </svg>
                  }
                  Resume SIP
                </button>
              )}
              {actionError && (
                <p className="text-xs text-red-600 bg-red-50 rounded px-2 py-1 max-w-[200px] text-right">
                  {actionError}
                </p>
              )}
            </div>
          </div>

          {/* Paused state banner */}
          {sip.status === 'PAUSED' && (
            <div className="bg-amber-50 border border-amber-100 rounded-xl px-4 py-3 mb-4 text-sm text-amber-700">
              SIP paused on <strong>{fmtDate(sip.pausedAt)}</strong>.
              Scheduler is skipping this SIP. Resume to continue remaining {sip.remainingInstallments} installments.
              Next execution date will be recalculated from the day you resume.
            </div>
          )}

          {/* Stats row */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5">
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
            <div className={`rounded-xl p-3 text-center ${sip.status === 'PAUSED' ? 'bg-amber-50' : 'bg-blue-50'}`}>
              <p className="text-xs text-gray-400 mb-1">Remaining</p>
              <p className={`text-xl font-bold ${sip.status === 'PAUSED' ? 'text-amber-700' : 'text-blue-700'}`}>
                {sip.remainingInstallments}
              </p>
              <p className="text-xs text-gray-400">left</p>
            </div>
            <div className="bg-purple-50 rounded-xl p-3 text-center">
              <p className="text-xs text-gray-400 mb-1">Next date</p>
              <p className="text-sm font-bold text-purple-700">
                {sip.status === 'COMPLETED'
                  ? 'Done'
                  : sip.status === 'PAUSED'
                  ? 'Paused'
                  : fmtDate(sip.nextExecutionDate)}
              </p>
              <p className="text-xs text-gray-400">execution</p>
            </div>
          </div>

          {/* Progress bar */}
          <ProgressBar completed={sip.completedInstallments} total={sip.totalInstallments} />
        </div>

        {/* ── Financial summary ── */}
        <div className="grid grid-cols-3 gap-4 mb-5">
          <div className="card p-4 text-center">
            <p className="text-xs text-gray-400 mb-1">Contribution</p>
            <p className="text-lg font-bold text-gray-900">{fmt(sip.totalContribution)}</p>
          </div>
          <div className="card p-4 text-center">
            <p className="text-xs text-gray-400 mb-1">Interest earned</p>
            <p className="text-lg font-bold text-green-600">{fmt(sip.totalInterest)}</p>
          </div>
          <div className="card p-4 text-center">
            <p className="text-xs text-gray-400 mb-1">Current value</p>
            <p className="text-lg font-bold text-blue-700">{fmt(sip.currentAmount)}</p>
          </div>
        </div>

        {/* ── Transactions table ── */}
        <div className="card overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-50 flex items-center justify-between">
            <h2 className="text-base font-semibold text-gray-900">Transaction History</h2>
            <span className="text-xs text-gray-400">{sip.completedInstallments} completed</span>
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
                      <td className="px-6 py-3.5 text-center"><TxStatusBadge status={t.status} /></td>
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
