# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A German university Java course project ("Bankprojekt", author "Doro") built up over a series of
exercises (Übungen). Java 21, Maven. The codebase is a mix of the bank-management domain
(`bankprojekt.*`) and standalone exercise packages (`automat`, `generisch`, `sortiererei`,
`Nullstellen`). The bank domain is the substantive part; the rest are isolated practice files.

## Commands

```bash
mvn compile                 # compile (runs SpotBugs in the compile phase)
mvn test                    # run all tests (JaCoCo agent attached; coverage report scoped to Bank.class)
mvn test -Dtest=BankTest    # run a single test class
mvn test -Dtest=BankTest#methodName   # run a single test method
mvn exec:java               # run the configured main class (spielereien.KontenSpielereien)
mvn exec:java -Dexec.mainClass=spielereien.Aktienspielereien   # run a different demo/main
mvn package                 # builds javadoc + a `submission` zip (src + pom + SpotBugs site) via the assembly plugin
```

There is no separate lint step — SpotBugs runs automatically during `mvn compile` and writes HTML to
`target/site`.

**`mvn` is not on the PATH in this environment.** Options for a quick compile/run without it:
- IntelliJ's bundled Maven: `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn`
- Plain `javac`/`java` with the one external dependency from the local repo on the classpath:
  ```bash
  DEC=$HOME/.m2/repository/org/decimal4j/decimal4j/1.0.3/decimal4j-1.0.3.jar
  javac -d /tmp/out -cp "$DEC" $(find src/main/java/bankprojekt -name '*.java') src/main/java/spielereien/<Demo>.java
  java -cp "/tmp/out:$DEC" spielereien.<Demo>
  ```
  Run demos that write files from a scratch dir (e.g. `cd /tmp/run`) so output artifacts don't land in the repo.

## Architecture

The bank domain is layered. Keep that separation when extending it:

- **`bankprojekt.basisdaten`** — domain model. `Konto` is the abstract base (kontonummer, inhaber,
  kontostand, sperren); concrete subclasses are `Girokonto`, `Sparbuch`, `Aktienkonto`, `Kinderkonto`,
  and `Festgeldkonto`. **`abheben(Geldbetrag)` is a Template Method (Übung 11): it is `final` in
  `Konto` and fixes the algorithm (validate → check lock → `pruefeAbhebung` → deduct → `nachAbhebung`).
  Subclasses must NOT reimplement `abheben`; they override the `protected` primitive operations
  `pruefeAbhebung` (their withdrawal rule) and optionally `nachAbhebung` (post-booking, e.g. `Sparbuch`
  monthly total, `Festgeldkonto` remaining cancelled amount). The base `abheben` is `synchronized` so
  `Aktienkonto`'s trade methods stay thread-safe without overriding it.**
  `UeberweisungsfaehigesKonto` is an abstract class (extends `Konto`) marking accounts that can
  send/receive transfers (`Bank.geldUeberweisen` checks `instanceof UeberweisungsfaehigesKonto`).
  `Geldbetrag` is an **immutable** money value type (amount + `Waehrung`); use its
  `plus`/`minus`/`mal`/`umrechnen` methods and `Geldbetrag.NULL_EURO` rather than raw doubles.
  `Kunde` is the customer.
- **`bankprojekt.fabriken`** — Abstract-Factory layer (Übung 11). `Kontofabrik` is the abstract factory
  with `erstellen(Kunde, long)`; concrete factories (`GirokontoFabrik` carries the dispo,
  `SparbuchFabrik`, `KinderkontoFabrik`, `FestgeldkontoFabrik`, `AktienkontoFabrik`) each build one
  account type. `Bank` creates accounts only through `kontoErstellen(Kontofabrik, Kunde)` — adding a new
  account type needs a new factory, not a new `Bank` method.
- **`bankprojekt.verwaltung.Bank`** — the aggregate/service holding a `Map<Long, Konto>`, handing out
  account numbers, and implementing cross-account operations (transfers with rollback, stream-based
  reports over customers). Accounts are created via `kontoErstellen(Kontofabrik, Kunde)` (replaced the
  old `girokontoErstellen`/`sparbuchErstellen`/`mockEinfuegen` — Übung 11 b). Tests inject Mockito mocks
  by passing a throwaway `Kontofabrik` whose `erstellen` returns the mock.
  **Persistence:** `speichern(OutputStream)` / `einlesen(InputStream)` use Java serialization, so the
  whole object graph is `Serializable` (`Bank`, `Konto` + all subclasses, `Geldbetrag`, `Kunde`,
  `Kalender`). Serialization records each object's runtime type, so new `Konto` subtypes persist with
  no change to `Bank`. **These two methods deliberately do NOT close the passed stream** (`speichern`
  only `flush()`es; `einlesen` doesn't close) — this is required so multiple banks can be written to /
  read from successive `ZipOutputStream`/`ZipInputStream` entries. Don't "fix" this back to
  try-with-resources.
- **`bankprojekt.aktienhandel`** — the concurrency exercise (Übung 9); see its dedicated rules below.
- **`bankprojekt.exceptions`** — `GesperrtException` (account locked), `UngueltigeKontonummerException`.
- **`bankprojekt.nuetzliches`** — helpers (`Kalender` for injectable "today", `EinAusgabe`).
- **`spielereien`** — the **presentation layer**: `main` demo programs that print to the console /
  write files (`KontenSpielereien`, `Bankspielereien`, `Aktienspielereien`,
  `BankPersistenzSpielereien` — saves two banks to a multi-entry `bankdatei.zip` and reads them back,
  `FormatierungSpielereien` — `PrintWriter.printf` formatting demo, …). Business classes do not print;
  console output and demo I/O belong here.

Tests live under `src/test/java/bankprojekt`, JUnit 5 (Jupiter). Some use Mockito
(`BankMockitoTest`, `SparbuchMockitoTest`) — the Bank tests mock `Konto` and insert via
`mockEinfuegen` to test `Bank` in isolation.

## Conventions (course constraints — these are graded, not preferences)

- **Comments and JavaDoc are written in German.** Match that when editing.
- **The business layer (`bankprojekt.*`) must not print to the console.** Demos go in `spielereien`.
- **`Geldbetrag` is immutable** — never mutate; operations return new instances.

### Aktienhandel (`bankprojekt.aktienhandel`) — concurrency rules

This package has strict assignment constraints. When touching it:

- **Concurrency via the newer `ExecutorService` API only.** No `sleep()`, no `new Thread()`, no
  `wait`/`notify`. `Aktie` drives price changes with a `ScheduledExecutorService`
  (`scheduleAtFixedRate`). `Aktienkonto.kaufauftrag`/`verkaufauftrag` return the `Future` from
  `ExecutorService.submit(...)` and "wait" for the right price by blocking on a `BlockingQueue.take()`
  fed by the `Aktie`'s `PropertyChangeListener` (Observer pattern) — no busy-waiting. Daemon threads
  are created via `Executors.defaultThreadFactory().newThread(r)` + `setDaemon(true)` so no literal
  `new Thread()` appears.
- **Mark every change you make with a `// Claude changed it` comment** plus a German `Bemerkung`.
