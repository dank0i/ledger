import { FormEvent, useState } from "react";
import { Account, AccountType, api } from "./api";

interface Props {
  accounts: Account[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  onChanged: () => void;
}

export default function AccountList({ accounts, selectedId, onSelect, onChanged }: Props) {
  const [name, setName] = useState("");
  const [type, setType] = useState<AccountType>("ASSET");
  const [error, setError] = useState<string | null>(null);

  async function create(e: FormEvent) {
    e.preventDefault();
    try {
      await api.createAccount(name.trim(), type);
      setName("");
      setError(null);
      onChanged();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div>
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th className="num">Balance</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((a) => (
            <tr
              key={a.id}
              className={a.id === selectedId ? "selected" : ""}
              onClick={() => onSelect(a.id)}
            >
              <td>{a.name}</td>
              <td>{a.type}</td>
              <td className="num">{Number(a.balance).toFixed(2)}</td>
            </tr>
          ))}
          {accounts.length === 0 && (
            <tr>
              <td colSpan={3} className="muted">
                No accounts yet.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <form onSubmit={create} className="inline-form">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Account name"
          required
        />
        <select value={type} onChange={(e) => setType(e.target.value as AccountType)}>
          <option>ASSET</option>
          <option>LIABILITY</option>
          <option>INCOME</option>
          <option>EXPENSE</option>
        </select>
        <button type="submit">Add</button>
      </form>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
