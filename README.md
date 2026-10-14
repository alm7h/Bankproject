# Bankproject

A Java 21 + JavaFX learning project that models a small banking domain and includes a simple JavaFX UI. The project demonstrates clean domain modeling (accounts, customers, money, currencies), design patterns (Template Method, Abstract Factory, Observer), collections/streams, concurrency with `ExecutorService`, persistence via Java serialization, a thorough unit test suite (JUnit 5 + Mockito), and code quality tooling (SpotBugs, JaCoCo, Javadoc).

This repository is intended for teaching/learning purposes but is structured like a production Maven project so you can build, test, and package it easily.


## Contents
- Overview
- Features
- Domain rules
- Tech stack
- Project layout
- Getting started
  - Prerequisites
  - Build
  - Run (JavaFX UI)
  - Run demo programs
  - Run tests and coverage
  - Generate static analysis and docs
  - Package an archive
- Tests
- Usage notes (JavaFX specifics)
- Troubleshooting
- Contributing
- License


## Overview
Core domain packages live under `bankprojekt.*`:
- `bankprojekt.basisdaten` — Core banking entities: the abstract `Konto` (account) with `Girokonto`, `Sparbuch`, `Festgeldkonto` and `Kinderkonto`, plus `Geldbetrag` (money), `Waehrung` (currency) and `Kunde` (customer).
- `bankprojekt.verwaltung` — The `Bank` aggregate which manages accounts, transfers, reports and persistence.
- `bankprojekt.fabriken` — Abstract-Factory layer (`Kontofabrik` + one factory per account type) used by `Bank.kontoErstellen`.
- `bankprojekt.aktienhandel` — Stock trading with concurrency: `Aktie` changes its price in the background, `Aktienkonto` places buy/sell orders that wait for the right price.
- `bankprojekt.exceptions` — Domain exceptions `GesperrtException` and `UngueltigeKontonummerException`.
- `bankprojekt.nuetzliches` — Helpers: `Kalender` (injectable "today") and `EinAusgabe` (console input).
- `bankprojekt.oberflaeche` — A small JavaFX UI (FXML) connecting view and model via `KontoController`, launched through `Start`.

The `spielereien` package holds runnable `main` demo programs (the presentation layer) that exercise the domain and print to the console / write files — the business classes themselves never print.


## Features
- Multiple account types with deposits, withdrawals, and transfers
- Withdrawal rules per account type via a Template Method (`Konto.abheben` with the hook `pruefeAbhebung`)
- Account factories to encapsulate creation logic
- Money in several currencies (EUR, MKD, KMF, STN), compared by euro value and rounded to cents
- Bank-level queries with streams: totals by customer, customers with a negative balance, birthdays, seniors count, a gift for new adults
- Transfers between accounts with rollback if crediting fails
- Stock trading: shares change their price on a `ScheduledExecutorService`, orders return a `Future` and wait for the price without `sleep()`
- JavaFX UI (FXML + Controller) with property bindings to perform account operations and visualize state
- Persistence: save/load a whole bank via Java serialization, several banks in one zip file
- Unit tests with JUnit 5 and Mockito (about 400 tests)
- Code quality: SpotBugs static analysis, JaCoCo coverage, Javadoc generation


## Domain rules
A few rules the code enforces (and the tests check):
- **A locked account (`gesperrt`) allows no bookings:** withdrawing, depositing, sending or receiving transfers, cancelling fixed-term money and stock orders all throw `GesperrtException`.
- **Girokonto:** may be overdrawn down to `-dispo`; the dispo must be greater than 0.
- **Sparbuch:** at most 2,000 € per calendar month, and at least 0.50 € must remain.
- **Kinderkonto:** at most 50 € per withdrawal, no overdraft.
- **Festgeldkonto:** only the amount cancelled beforehand (`kuendigen`) can be withdrawn, at most the balance.
- **Aktienkonto:** no overdraft; a buy order without enough balance or for an unknown share buys nothing (0 €).
- **Account numbers** are handed out by the `Bank`; a failed account creation uses up no number. `getKonten()` returns a read-only view.
- **`Geldbetrag`** is immutable; `plus`, `minus`, `mal` and `umrechnen` round to cents.


## Tech stack
- Language: Java 21
- Build: Apache Maven
- UI: JavaFX (OpenJFX `javafx-controls`, `javafx-fxml`)
- Money rounding: decimal4j
- Testing: JUnit 5 (Jupiter) + Mockito
- Analysis: SpotBugs, JaCoCo, Javadoc

See `pom.xml` for exact versions and plugins.


## Project layout
```
/ (project root)
├─ pom.xml
├─ src
│  ├─ main
│  │  ├─ java
│  │  │  ├─ bankprojekt
│  │  │  │  ├─ basisdaten/        # Core domain entities (accounts, money, currency, customer)
│  │  │  │  ├─ verwaltung/        # Bank aggregate and operations
│  │  │  │  ├─ fabriken/          # Factories for accounts
│  │  │  │  ├─ aktienhandel/      # Shares and stock account (concurrency)
│  │  │  │  ├─ exceptions/        # Domain exceptions
│  │  │  │  ├─ nuetzliches/       # Kalender, console input
│  │  │  │  └─ oberflaeche/       # JavaFX UI (Start, KontoAnwendung, KontoController)
│  │  │  └─ spielereien/          # Runnable demo programs
│  │  └─ resources
│  │     └─ bankprojekt/oberflaeche/  # FXML + CSS + images
│  └─ test
│     └─ java/bankprojekt/        # Unit tests, same packages as the classes they test
└─ README.md
```


## Getting started

### Prerequisites
- Java Development Kit (JDK) 21
- Apache Maven 3.9+

Verify your environment:
```
java -version
mvn -version
```

### Build
```
mvn -q -DskipTests package
```
This compiles sources and packages the project. Use `mvn package` (without `-DskipTests`) to include running tests.

### Run (JavaFX UI)
This project uses a non-`Application` bootstrap class to simplify launching JavaFX from Maven/classpath. Run the JavaFX UI by invoking the `Start` class:
```
mvn -q -Dexec.mainClass=bankprojekt.oberflaeche.Start exec:java
```
Notes:
- Run `Start` (not `KontoAnwendung`) to avoid the classic "JavaFX runtime components are missing" error when JavaFX is resolved via Maven on the classpath.
- The UI loads `src/main/resources/bankprojekt/oberflaeche/KontoOberflaeche.fxml`.
- In the window you can choose an amount and "Abheben" (withdraw) or "Einzahlen" (deposit), lock/unlock the account and edit the owner's address. A picture appears when the balance is negative.

### Run demo programs
The demo programs live in `spielereien`. The default main class for `exec:java` is `spielereien.KontenSpielereien` (set as the `exec.mainClass` property in `pom.xml`). To run it:
```
mvn -q exec:java
```
Or run any other main class, e.g.:
```
mvn -q -Dexec.mainClass=spielereien.Bankspielereien exec:java
```
Available demos:
- `spielereien.KontenSpielereien` — interactive: creates an account and asks how much to withdraw
- `spielereien.Bankspielereien` — the bank's stream reports, with the expected results printed alongside
- `spielereien.BankPersistenzSpielereien` — saves two banks into `bankdatei.zip` and reads them back (writes the file into the current directory)
- `spielereien.Aktienspielereien` — several buy/sell orders running at the same time (waits for real price changes, can take a minute or two)
- `spielereien.KundenUndKontenSortieren` — interactive: sorting customers and accounts

### Run tests and coverage
```
mvn -q test
```
A JaCoCo coverage report is generated during tests. It covers all of `bankprojekt` except the JavaFX UI (`bankprojekt.oberflaeche`). Find the HTML report in:
```
./target/site/jacoco/index.html
```
Run a single test class or method:
```
mvn -q test -Dtest=BankTest
mvn -q test -Dtest=BankTest#methodName
```

### Generate static analysis and docs
- SpotBugs HTML report:
  ```
  mvn -q compile
  open target/site/spotbugs.html   # or view the file in your browser
  ```
- Javadoc (package-level visibility and above):
  ```
  mvn -q package
  open target/site/apidocs/index.html
  ```

### Package an archive (assembly)
A simple assembly is configured for submissions/packaging and runs as part of `package`:
```
mvn -q package
```
The archive `target/Bankproject-1.0-SNAPSHOT-submission.zip` contains the sources, `pom.xml` and the SpotBugs report.


## Tests
The test suite lives in `src/test/java/bankprojekt`, in the same packages as the classes under test, and uses only JUnit 5 and Mockito.

- **About 400 tests**, around 97 % line and branch coverage of `bankprojekt` (without the UI).
- Every account type, `Geldbetrag`, `Waehrung`, `Kunde`, the factories, the `Bank` (including transfers, reports and persistence), the exceptions and `Kalender` have their own test classes.
- Test names and comments are in German, e.g. `geldAbheben_gibtFalse_wennUeberDispo`.
- The suite does not depend on the computer's language settings: locale-dependent output is tested with `Locale.GERMANY`, set and restored around each test. To double-check:
  ```
  mvn -q test -DargLine="-Duser.language=de -Duser.country=DE"
  ```
- The stock trading tests contain no `sleep()`: they wait for results with `Future.get(timeout)` and use price limits that are met immediately, so they don't depend on random price changes.


## Usage notes (JavaFX specifics)
- Entry point: `bankprojekt.oberflaeche.Start` calls `Application.launch(KontoAnwendung.class, args)`, ensuring JavaFX starts even when OpenJFX is provided via Maven dependencies (classpath) rather than module path.
- If you run the `KontoAnwendung` class directly without JavaFX modules on the module path, the JVM may exit early with: "JavaFX runtime components are missing". Always run `Start` instead when launching via Maven.
- The warning "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'" is expected when JavaFX runs from the classpath and can be ignored.


## Troubleshooting
- JavaFX runtime components are missing
  - Symptom: JVM exits before your code runs.
  - Fix: Launch `bankprojekt.oberflaeche.Start` instead of `KontoAnwendung`:
    ```
    mvn -q -Dexec.mainClass=bankprojekt.oberflaeche.Start exec:java
    ```
- Java not found or wrong version
  - Ensure JDK 21 is installed and active: `java -version` should show `21`.
- Number and date formats look different
  - Amounts and dates are printed in the format of your computer's language settings (e.g. `1,234.50 EUR` or `1.234,50 EUR`). This is intended; the tests pass with any setting.
- An interactive demo stops at the second question when input is piped in
  - `KontenSpielereien` reads its two answers with two separate readers. Type the answers yourself instead of piping them in all at once.


## Contributing
Contributions are welcome for educational improvements and additional examples. Suggested areas:
- Additional account operations and edge cases (with tests)
- Extended JavaFX UI interactions
- Documentation and comments (the code comments and Javadoc are in German)

Please open an issue or pull request.


## License
Released under the [MIT License](LICENSE) — © 2026 Mohameed Al-Harethi.
