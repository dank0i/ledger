import { useEffect, useState } from "react";
import { Account, Statement as StatementData, api } from "./api";

interface Props {
  account: Account;
}

function isoStartOfDay(date: string): string {
  return `${date}T00:00:00Z`;
}

const today = new Date().toISOString().slice(0, 10);
const monthAgo = new Date(Date.now() - 30 * 86400_000).toISOString().slice(0, 10);

export default function Statement({ account }: Props) {
  const [from, setFrom] = useState(monthAgo);
  const [to, setTo] = useState(today);
  const [data, setData] = useState<StatementData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // The "to" date is inclusive in the UI, so query up to the next day.
    const toExclusive = new Date(new Date(to).getTime() + 86400_000)
      .toISOString()
      .slice(0, 10);
    api
      .statement(account.id, isoStartOfDay(from), isoStartOfDay(toExclusive))
      .then((s) => {
        setData(s);
        setError(null);
      })
      .catch((e) => setError(e.message));
  }, [account.id, from, to]);

  return (
    <div>
      <p>
        <strong>{account.name}</strong> ({account.type})
      </p>
      <div className="inline-form">
        <label>
          From <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label>
          To <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Description</th>
              <th className="num">Amount</th>
              <th className="num">Balance</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={3}>Opening balance</td>
              <td className="num">{Number(data.openingBalance).toFixed(2)}</td>
            </tr>
            {data.lines.map((line, i) => (
              <tr key={i}>
                <td>{line.occurredAt.slice(0, 10)}</td>
                <td>{line.description}</td>
                <td className="num">
                  {line.direction === "DEBIT" ? "" : "-"}
                  {Number(line.amount).toFixed(2)}
                </td>
                <td className="num">{Number(line.runningBalance).toFixed(2)}</td>
              </tr>
            ))}
            <tr>
              <td colSpan={3}>Closing balance</td>
              <td className="num">{Number(data.closingBalance).toFixed(2)}</td>
            </tr>
          </tbody>
        </table>
      )}
    </div>
  );
}
