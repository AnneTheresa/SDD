function formatTimestamp(value) {
  if (!value) {
    return 'Not provided';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

function isCreditTransaction(transaction) {
  return String(transaction.direction || '').toUpperCase() === 'CREDIT';
}

function toTimestampValue(value) {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? Number.NEGATIVE_INFINITY : parsed.getTime();
}

function supportsCategoryEditing(transaction) {
  return !isCreditTransaction(transaction);
}

export function TransactionsTable({
  transactions,
  emptyTitle,
  emptyMessage,
  categoryOptions = [],
  onSaveCategory,
  savingTransactionId
}) {
  if (!Array.isArray(transactions) || transactions.length === 0) {
    return (
      <div className="panel empty-state stack tight-gap">
        <h3>{emptyTitle}</h3>
        <p className="muted">{emptyMessage}</p>
      </div>
    );
  }

  const sortedTransactions = [...transactions].sort(
    (left, right) => toTimestampValue(right.timestamp) - toTimestampValue(left.timestamp)
  );

  return (
    <div className="table-shell">
      <table>
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Description</th>
            <th>Type</th>
            <th>Outcome</th>
            <th>Amount</th>
            <th>Category</th>
          </tr>
        </thead>
        <tbody>
          {sortedTransactions.map((transaction) => {
            const currentCategory = String(transaction.category || '').trim();
            const isCredit = isCreditTransaction(transaction);
            const canEditCategory = typeof onSaveCategory === 'function' && supportsCategoryEditing(transaction);
            const isSaving = savingTransactionId === transaction.transactionId;

            return (
              <tr key={transaction.transactionId}>
                <td>{formatTimestamp(transaction.timestamp)}</td>
                <td>{transaction.description || 'No description'}</td>
                <td>{transaction.direction || 'Unknown'}</td>
                <td>
                  <span className={`status-pill ${String(transaction.status || '').toLowerCase() || 'neutral'}`}>
                    {transaction.status || 'Unknown'}
                  </span>
                </td>
                <td>{transaction.amount ?? '0.00'}</td>
                <td>
                  {canEditCategory ? (
                    <select
                      value={currentCategory}
                      disabled={isSaving}
                      onChange={(event) => onSaveCategory(transaction.transactionId, event.target.value)}
                    >
                      <option value="No category">No category</option>
                      {categoryOptions.map((category) => (
                        <option key={category} value={category}>{category}</option>
                      ))}
                    </select>
                  ) : (
                    currentCategory || (isCredit ? 'Not applicable for credits' : 'No category')
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}