# Microfinance Loan Tracker

**Student:** Lim Jun Jie 
**ID:** 23067812  
**Programme:** Bachelor of Computer Science  
**Module:** PRG2104 Object-Oriented Programming  
**Theme:** UN SDG 1 — No Poverty

An offline ScalaFX desktop application for a small community lender. It manages borrowers, flat-interest loans, monthly instalments, repayments, and overdue-risk summaries.

## Run the project

Use Java 21 and run these commands from the project folder. On macOS, setting `JAVA_HOME` explicitly prevents Homebrew sbt from selecting a newer JDK:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) sbt clean test
JAVA_HOME=$(/usr/libexec/java_home -v 21) sbt run
```

The application saves local runtime data in `data/microfinance-state.json`. It does not use a network service.

## Main features

- Register and edit borrowers with validation.
- Create loans between RM100 and RM50,000 for 1–36 months.
- Calculate equal monthly instalments using flat interest.
- Record full or partial repayments.
- Show outstanding balances, due dates, loan status, and risk levels.
- Save and reload data as JSON.

Each borrower can have only one active loan. Repayments are applied to the oldest unpaid instalment first. Invalid, future-dated, pre-disbursement, and excessive payments are rejected.

## Project design

- Immutable Scala domain models and collections.
- Sealed types for loan status, risk level, and application errors.
- Generic repository for controlled data access.
- Separate validation, service, persistence, and JavaFX UI layers.
- `Either`, `Option`, and `Try` are used for error handling.

## Sample data

The fictional demo data is in `src/main/resources/sample-state.json`. To use it, copy it to `data/microfinance-state.json` before running the application. Do not use real borrower information.

## Testing

The test suite covers validation, loan calculations, repayments, risk thresholds, repository behavior, JSON errors, and restart persistence.

Verified with Eclipse Adoptium Java 21.0.7:

```text
16 tests passed; 0 failed; clean compile emitted 0 warnings
```

## AI use

Codex was used to review requirements, refine the design, generate and revise code, run tests, and prepare documentation. AI-assisted code is marked in the source files. The genuine interaction record is in `ai/interaction_log.md`, and the declaration is in `ai/declaration.md`.

## Third-party components

The project uses Scala, sbt, ScalaFX, OpenJFX, uPickle, and MUnit. License details and source links are recorded in `docs/citations.md`.
