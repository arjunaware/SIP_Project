export default function ProgressBar({ completed, total, showLabel = true }) {
  const pct = total > 0 ? Math.round((completed / total) * 100) : 0

  return (
    <div className="w-full">
      {showLabel && (
        <div className="flex justify-between text-xs text-gray-500 mb-1.5">
          <span>{completed} of {total} installments</span>
          <span className="font-medium text-blue-600">{pct}%</span>
        </div>
      )}
      <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
        <div
          className="bg-blue-600 h-2.5 rounded-full transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}
