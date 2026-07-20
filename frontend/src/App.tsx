import { useCallback, useEffect, useState } from "react";
import { Account, api } from "./api";
import AccountList from "./AccountList";
import TransactionForm from "./TransactionForm";
import Statement from "./Statement";

export default function App() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(() => {
    api
      .listAccounts()
      .then((list) => {
        setAccounts(list);
        setError(null);
      })
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const selected = accounts.find((a) => a.id === selectedId) ?? null;

  return (
    <main className="app">
      <h1>Ledger</h1>
      {error && <p className="error">{error}</p>}
      <div className="columns">
        <section>
          <h2>Accounts</h2>
          <AccountList
            accounts={accounts}
            selectedId={selectedId}
            onSelect={setSelectedId}
            onChanged={refresh}
          />
        </section>
        <section>
          <h2>New transaction</h2>
          <TransactionForm accounts={accounts} onPosted={refresh} />
        </section>
        <section>
          <h2>Statement</h2>
          {selected ? (
            <Statement account={selected} />
          ) : (
            <p className="muted">Select an account to see its statement.</p>
          )}
        </section>
      </div>
    </main>
  );
}
