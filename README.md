# Ledger

A small double-entry transaction ledger: Spring Boot backend, React frontend.
Every transaction is recorded as a set of legs (debits and credits) that must
balance, so the books can never drift out of sync by construction.

## Running the backend

Requires Java 21 and Maven. The default profile uses an in-memory H2 database,
so no setup is needed:

```
cd backend
mvn spring-boot:run
```

The API is served on http://localhost:8080. Schema is managed by Flyway
(`src/main/resources/db/migration`).

To run against Postgres instead:

```
docker compose up -d          # from the repo root, starts postgres:16
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Tests:

```
cd backend
mvn test
```

## Running the frontend

Requires Node 18+.

```
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` to the backend
on port 8080, so start the backend first.

## API

| Method | Path                                        | Description                                        |
| ------ | ------------------------------------------- | -------------------------------------------------- |
| POST   | /api/accounts                               | Create an account (name, type)                     |
| GET    | /api/accounts                               | List accounts with cached balances                 |
| GET    | /api/accounts/{id}/balance                  | Balance recomputed from legs                       |
| GET    | /api/accounts/{id}/statement?from=&to=      | Legs in [from, to) with running balance (ISO 8601) |
| POST   | /api/transactions                           | Post a transaction (2+ balanced legs, optional category) |
| GET    | /api/transactions                           | List transactions with legs                        |
| POST   | /api/imports/csv                            | Multipart CSV import (field name `file`)           |

Error mapping: validation failures are 400, unknown resources are 404, and
transactions that violate the double-entry invariant are 422.

CSV format for imports (header row optional):

```
date,description,amount,debitAccountId,creditAccountId
2026-07-01,July pay,2500.00,1,2
```

## Design notes

**Double entry.** Money is never created or destroyed, only moved. Each
transaction has at least two legs, and the sum of debit amounts must equal the
sum of credit amounts; the service rejects anything else with a 422 before it
touches the database. An account's balance is defined as debits minus credits
over its legs, so asset and expense accounts carry positive balances while
liability and income accounts naturally go negative. Amounts are `BigDecimal`
end to end and compared with `compareTo`, because `0.1 + 0.2` is not money.

**Idempotency.** POSTs are retried in the real world: timeouts, flaky wifi,
double clicks. A transaction can carry an `idempotencyKey`, backed by a unique
column. Replaying a known key returns the original transaction with a 200
instead of creating a duplicate, so a client can safely retry until it gets an
answer. The CSV import builds its keys by hashing each raw row, which makes
re-uploading the same file (or overlapping exports) a no-op rather than a
double-booking.

**Reconciliation.** Each account keeps a `cachedBalance` that is updated inside
the same database transaction as the posting, with the affected account rows
locked in id order to keep concurrent postings from interleaving. The legs
remain the source of truth. An hourly scheduled job recomputes every account's
balance from its legs and logs a warning for any account where the cache has
drifted, which would indicate a bug or an out-of-band data edit. Cheap to run,
and it turns a silent corruption into a loud log line.
