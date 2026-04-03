export default function StatusBadge({ status }) {
  const styles = {
    COMPLETED: 'bg-green-100 text-green-700',
    PENDING:   'bg-yellow-100 text-yellow-700',
    ACTIVE:    'bg-blue-100 text-blue-700',
  }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status] ?? 'bg-gray-100 text-gray-600'}`}>
      {status}
    </span>
  )
}
