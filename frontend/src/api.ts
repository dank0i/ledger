export type AccountType = "ASSET" | "LIABILITY" | "INCOME" | "EXPENSE";
export type Direction = "DEBIT" | "CREDIT";

export interface Account {
  id: number;
  name: string;
  type: AccountType;
  createdAt: string;
  balance: string;
}

export interface Leg {
  id: number;
  accountId: number;
  accountName: string;
  direction: Direction;
  amount: string;
}

export interface Transaction {
  id: number;
  description: string;
  category: string | null;
  occurredAt: string;
  idempotencyKey: string | null;
  legs: Leg[];
}

export interface StatementLine {
  occurredAt: string;
  description: string;
  category: string | null;
  direction: Direction;
  amount: string;
  runningBalance: string;
}

export interface Statement {
  accountId: number;
  from: string;
  to: string;
  openingBalance: string;
  closingBalance: string;
  lines: StatementLine[];
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details: Record<string, string> = {}
  ) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init);
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new ApiError(res.status, body?.error ?? res.statusText, body?.details);
  }
  return res.json();
}

function postJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

export const api = {
  listAccounts: () => request<Account[]>("/api/accounts"),
  createAccount: (name: string, type: AccountType) =>
    postJson<Account>("/api/accounts", { name, type }),
  listTransactions: () => request<Transaction[]>("/api/transactions"),
  postTransaction: (body: {
    description: string;
    category?: string;
    legs: { accountId: number; direction: Direction; amount: string }[];
  }) => postJson<Transaction>("/api/transactions", body),
  statement: (accountId: number, from: string, to: string) =>
    request<Statement>(
      `/api/accounts/${accountId}/statement?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    ),
};
