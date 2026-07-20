import { FormEvent, useState } from "react";
import { Account, api } from "./api";

interface Props {
  accounts: Account[];
  onPosted: () => void;
}

export default function TransactionForm({ accounts, onPosted }: Props) {
  const [description, setDescription] = useState("");
  const [debitId, setDebitId] = useState("");
  const [creditId, setCreditId] = useState("");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [posted, setPosted] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (debitId === creditId) {
      setError("Debit and credit accounts must differ.");
      return;
    }
    try {
      const tx = await api.postTransaction({
        description: description.trim(),
        legs: [
          { accountId: Number(debitId), direction: "DEBIT", amount },
          { accountId: Number(creditId), direction: "CREDIT", amount },
        ],
      });
      setPosted(`Posted transaction #${tx.id}`);
      setError(null);
      setDescription("");
      setAmount("");
      onPosted();
    } catch (err) {
      setPosted(null);
      setError((err as Error).message);
    }
  }

  return (
    <form onSubmit={submit} className="stack">
      <label>
        Description
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          required
        />
      </label>
      <label>
        Debit account
        <select value={debitId} onChange={(e) => setDebitId(e.target.value)} required>
          <option value="">Choose...</option>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        Credit account
        <select value={creditId} onChange={(e) => setCreditId(e.target.value)} required>
          <option value="">Choose...</option>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        Amount
        <input
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
        />
      </label>
      <button type="submit">Post</button>
      {posted && <p className="ok">{posted}</p>}
      {error && <p className="error">{error}</p>}
    </form>
  );
}
