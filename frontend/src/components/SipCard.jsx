import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { sipService } from '../services/sipService'
import ProgressBar from './ProgressBar'

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

// ── Status badge ────────────────────────────────────────────────────
function StatusBadge({ status }) {
  const styles = {
    ACTIVE:    'bg-blue-100 text-blue-700',
    PAUSED:    'bg-amber-100 text-amber-700',
    COMPLETED: 'bg-green-100 text-green-700',
  }
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
        styles[status] ?? 'bg-gray-100 text-gray-600'
      }`}
    >
      {status === 'ACTIVE' && (
        <span className="w-1.5 h-1.5 rounded-full bg-blue-500 mr-1.5 animate-pulse" />
      )}
      {status === 'PAUSED' && (
        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mr-1.5" />
      )}
      {status === 'COMPLETED' && (
        <span className="w-1.5 h-1.5 rounded-full bg-green-500 mr-1.5" />
      )}
      {status}
    </span>
  )
}

// ── Main SipCard ─────────────────────────────────────────────────────
export default function SipCard({ sip: initialSip, onUpdate }) {
  const navigate = useNavigate()
  const [sip, setSip] = useState(initialSip)
  const [pauseLoading, setPauseLoading] = useState(false)
  const [resumeLoading, setResumeLoading] = useState(false)
  const [error, setError] = useState('')

  // ── Pause handler ──────────────────────────────────────────────────
  const handlePause = async (e) => {
    e.stopPropagation() // prevent card click → navigate
    setError('')
    setPauseLoading(true)
    try {
      const updated = await sipService.pauseSip(sip.id)
      setSip(updated)          // update card immediately
      onUpdate?.(updated)      // bubble up to dashboard if provided
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to pause SIP')
    } finally {
      setPauseLoading(false)
    }
  }

  // ── Resume handler ─────────────────────────────────────────────────
  const handleResume = async (e) => {
    e.stopPropagation()
    setError('')
    setResumeLoading(true)
    try {
      const updated = await sipService.resumeSip(sip.id)
      setSip(updated)
      onUpdate?.(updated)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to resume SIP')
    } finally {
      setResumeLoading(false)
    }
  }

  return (
    <div
      className={`card p-5 hover:shadow-md transition-all cursor-pointer border-l-4 ${
        sip.status === 'ACTIVE'
          ? 'border-l-blue-500'
          : sip.status === 'PAUSED'
          ? 'border-l-amber-400'
          : 'border-l-green-500'
      }`}
      onClick={() => navigate(`/sip/${sip.id}`)}
    >
      {/* ── Header ── */}
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

      {/* ── Installment tracking ── */}
      <div className="grid grid-cols-3 gap-2 mb-3">
        <div className="bg-gray-50 rounded-lg p-2 text-center">
          <p className="text-xs text-gray-400">Total</p>
          <p className="text-base font-bold text-gray-800">{sip.totalInstallments}</p>
        </div>
        <div className="bg-green-50 rounded-lg p-2 text-center">
          <p className="text-xs text-gray-400">Done</p>
          <p className="text-base font-bold text-green-700">{sip.completedInstallments}</p>
        </div>
        <div
          className={`rounded-lg p-2 text-center ${
            sip.status === 'PAUSED' ? 'bg-amber-50' : 'bg-blue-50'
          }`}
        >
          <p className="text-xs text-gray-400">Left</p>
          <p
            className={`text-base font-bold ${
              sip.status === 'PAUSED' ? 'text-amber-700' : 'text-blue-700'
            }`}
          >
            {sip.remainingInstallments}
          </p>
        </div>
      </div>

      {/* ── Progress bar ── */}
      <div className="mb-3">
        <ProgressBar
          completed={sip.completedInstallments}
          total={sip.totalInstallments}
        />
      </div>

      {/* ── Financial row ── */}
      <div className="flex gap-3 mb-3 text-xs">
        <div>
          <span className="text-gray-400">Contributed </span>
          <span className="font-medium text-gray-700">{fmt(sip.totalContribution)}</span>
        </div>
        <div>
          <span className="text-gray-400">Interest </span>
          <span className="font-medium text-green-600">{fmt(sip.totalInterest)}</span>
        </div>
        <div className="ml-auto">
          <span className="text-gray-400">Rate </span>
          <span className="font-medium text-blue-700">{sip.interestRate}%</span>
        </div>
      </div>

      {/* ── Next execution date ── */}
      {sip.status !== 'COMPLETED' && (
        <div
          className={`text-xs rounded-lg px-3 py-1.5 mb-3 ${
            sip.status === 'PAUSED'
              ? 'bg-amber-50 text-amber-700'
              : 'bg-blue-50 text-blue-700'
          }`}
        >
          {sip.status === 'PAUSED' ? (
            <>Paused on {fmtDate(sip.pausedAt)} · next date recalculated on resume</>
          ) : (
            <>Next execution: <span className="font-medium">{fmtDate(sip.nextExecutionDate)}</span></>
          )}
        </div>
      )}

      {/* ── Error ── */}
      {error && (
        <div className="text-xs text-red-600 bg-red-50 rounded px-2 py-1 mb-2">
          {error}
        </div>
      )}

      {/* ── Action buttons ── */}
      <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
        {/* Pause — only visible when ACTIVE */}
        {sip.status === 'ACTIVE' && (
          <button
            onClick={handlePause}
            disabled={pauseLoading}
            className="flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-lg
                       bg-amber-50 text-amber-700 hover:bg-amber-100 border border-amber-200
                       transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {pauseLoading ? (
              <span className="w-3 h-3 border border-amber-600 border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                <rect x="2" y="1" width="3" height="10" rx="1"/>
                <rect x="7" y="1" width="3" height="10" rx="1"/>
              </svg>
            )}
            Pause
          </button>
        )}

        {/* Resume — only visible when PAUSED */}
        {sip.status === 'PAUSED' && (
          <button
            onClick={handleResume}
            disabled={resumeLoading}
            className="flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-lg
                       bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200
                       transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {resumeLoading ? (
              <span className="w-3 h-3 border border-blue-600 border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                <path d="M3 1.5L10 6L3 10.5V1.5Z"/>
              </svg>
            )}
            Resume
          </button>
        )}

        {/* Completed — no action buttons */}
        {sip.status === 'COMPLETED' && (
          <span className="text-xs text-green-600 font-medium">
            All installments completed
          </span>
        )}

        <button
          onClick={(e) => { e.stopPropagation(); navigate(`/sip/${sip.id}`) }}
          className="ml-auto text-xs text-gray-400 hover:text-blue-600 font-medium transition-colors"
        >
          View Details →
        </button>
      </div>
    </div>
  )
}
