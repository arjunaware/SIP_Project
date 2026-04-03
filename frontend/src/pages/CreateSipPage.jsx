import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Navbar from '../components/Navbar'
import { sipService } from '../services/sipService'

export default function CreateSipPage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [form, setForm] = useState({
    passbookId: user?.passbookId || '',
    amount: '',
    frequency: 'MONTHLY',
    totalInstallments: '',
    startDate: new Date().toISOString().split('T')[0],
    trust: false,
    interestRate: '',
    isSip: true,
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const validate = () => {
    if (!form.passbookId.trim()) return 'Passbook ID is required'
    if (!form.amount || isNaN(form.amount) || Number(form.amount) <= 0) return 'Enter a valid amount'
    if (!form.totalInstallments || isNaN(form.totalInstallments) || Number(form.totalInstallments) < 1)
      return 'Total installments must be at least 1'
    if (!form.startDate) return 'Start date is required'
    if (form.trust) {
      if (!form.interestRate) return 'Interest rate is required when trust is enabled'
      const rate = Number(form.interestRate)
      if (rate < 0 || rate > 15) return 'Interest rate must be between 0 and 15'
    }
    return null
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const err = validate()
    if (err) { setError(err); return }
    setError('')
    setLoading(true)

    try {
      const payload = {
        passbookId: form.passbookId,
        amount: parseFloat(form.amount),
        frequency: form.frequency,
        totalInstallments: parseInt(form.totalInstallments),
        startDate: form.startDate,
        trust: form.trust,
        isSip: form.isSip,
        ...(form.trust && { interestRate: parseFloat(form.interestRate) }),
      }
      const sip = await sipService.createSip(payload)
      navigate(`/sip/${sip.id}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create SIP. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <Navbar />
      <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8">
        <div className="mb-6">
          <button onClick={() => navigate('/dashboard')} className="text-sm text-gray-400 hover:text-gray-600 flex items-center gap-1 mb-4">
            ← Back to Dashboard
          </button>
          <h1 className="text-2xl font-bold text-gray-900">Create New SIP</h1>
          <p className="text-gray-400 text-sm mt-1">Set up a new Systematic Investment Plan</p>
        </div>

        <div className="card p-8">
          {error && (
            <div className="mb-5 p-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-lg">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Passbook ID */}
            <div>
              <label className="label">Passbook ID <span className="text-red-400">*</span></label>
              <input
                type="text"
                name="passbookId"
                value={form.passbookId}
                onChange={handleChange}
                className="input-field font-mono tracking-wider"
                placeholder="e.g. ALPHA123"
              />
              <p className="text-xs text-gray-400 mt-1">Your passbook ID from signup</p>
            </div>

            {/* Amount */}
            <div>
              <label className="label">Amount per Installment (₹) <span className="text-red-400">*</span></label>
              <input
                type="number"
                name="amount"
                value={form.amount}
                onChange={handleChange}
                className="input-field"
                placeholder="e.g. 5000"
                min="1"
              />
            </div>

            {/* Frequency + Installments */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">Frequency <span className="text-red-400">*</span></label>
                <select name="frequency" value={form.frequency} onChange={handleChange} className="input-field">
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </div>
              <div>
                <label className="label">Total Installments <span className="text-red-400">*</span></label>
                <input
                  type="number"
                  name="totalInstallments"
                  value={form.totalInstallments}
                  onChange={handleChange}
                  className="input-field"
                  placeholder="e.g. 12"
                  min="1"
                />
              </div>
            </div>

            {/* Start date */}
            <div>
              <label className="label">Start Date <span className="text-red-400">*</span></label>
              <input
                type="date"
                name="startDate"
                value={form.startDate}
                onChange={handleChange}
                className="input-field"
              />
            </div>

            {/* Type toggle */}
            <div>
              <label className="label">Investment Type</label>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setForm(f => ({ ...f, isSip: true }))}
                  className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${form.isSip ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'}`}
                >
                  SIP
                </button>
                <button
                  type="button"
                  onClick={() => setForm(f => ({ ...f, isSip: false }))}
                  className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${!form.isSip ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'}`}
                >
                  Recurring Deposit
                </button>
              </div>
            </div>

            {/* Trust checkbox */}
            <div className="bg-amber-50 border border-amber-100 rounded-xl p-4">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  name="trust"
                  checked={form.trust}
                  onChange={handleChange}
                  className="mt-0.5 w-4 h-4 accent-blue-600"
                />
                <div>
                  <p className="text-sm font-medium text-gray-800">Custom interest rate (Trust mode)</p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Enable to set a custom interest rate (0–15%). Otherwise, the system default rate applies.
                  </p>
                </div>
              </label>

              {form.trust && (
                <div className="mt-3 ml-7">
                  <label className="label">Interest Rate (%) <span className="text-red-400">*</span></label>
                  <input
                    type="number"
                    name="interestRate"
                    value={form.interestRate}
                    onChange={handleChange}
                    className="input-field"
                    placeholder="e.g. 8.5"
                    min="0"
                    max="15"
                    step="0.1"
                  />
                </div>
              )}
            </div>

            {/* Submit */}
            <div className="flex gap-3 pt-2">
              <button type="button" onClick={() => navigate('/dashboard')} className="btn-secondary flex-1">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-primary flex-1">
                {loading ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Creating...
                  </span>
                ) : 'Create SIP'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  )
}
