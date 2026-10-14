# Bankproject

[![Build](https://github.com/alm7h/Bankproject/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/alm7h/Bankproject/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-22-blue)
[![License: MIT](https://img.shields.io/github/license/alm7h/Bankproject)](LICENSE)

A Java 21 + JavaFX learning project that models a small banking domain and includes a JavaFX back-office application ("Bankverwaltung") with login for managing customers, accounts and staff users. The project demonstrates clean domain modeling (accounts, customers, money, currencies), design patterns (Template Method, Abstract Factory, Observer), collections/streams, concurrency with `ExecutorService`, persistence via Java serialization, a thorough unit test suite (JUnit 5 + Mockito), and code quality tooling (SpotBugs, JaCoCo, Javadoc).

This repository is intended for teaching/learning purposes but is structured like a production Maven project so you can build, test, and package it easily.


## Contents
- Overview
- Features
- Domain rules
- Verwaltung app (login, customers, accounts, users)
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
- `bankprojekt.verwaltung` — The `Bank` aggregate which manages accounts, transfers, reports and persistence, and the back-office layer on top of it: `Bankverwaltung` (customer records, account records, staff users, audit log), `Anmeldung` (login with lockout), `Passwortschutz` (PBKDF2 password hashing) and `Datenspeicher` (saves everything to a file).
- `bankprojekt.fabriken` — Abstract-Factory layer (`Kontofabrik` + one factory per account type) used by `Bank.kontoErstellen`.
- `bankprojekt.aktienhandel` — Stock trading with concurrency: `Aktie` changes its price in the background, `Aktienkonto` places buy/sell orders that wait for the right price.
- `bankprojekt.exceptions` — Domain exceptions `GesperrtException` and `UngueltigeKontonummerException`.
- `bankprojekt.nuetzliches` — Helpers: `Kalender` (injectable "today") and `EinAusgabe` (console input).
- `bankprojekt.oberflaeche` — The JavaFX Verwaltung app (FXML + controllers): login, main window with Kunden, Konten, Benutzer and Protokoll views, launched through `Start`.

The `spielereien` package holds runnable `main` demo programs (the presentation layer) that exercise the domain and print to the console / write files — the business classes themselves never print.


## Features
- Multiple account types with deposits, withdrawals, and transfers
- Withdrawal rules per account type via a Template Method (`Konto.abheben` with the hook `pruefeAbhebung`)
- Account factories to encapsulate creation logic
- Money in several currencies (EUR, MKD, KMF, STN), compared by euro value and rounded to cents
- Bank-level queries with streams: totals by customer, customers with a negative balance, birthdays, seniors count, a gift for new adults
- Transfers between accounts with rollback if crediting fails
- Stock trading: shares change their price on a `ScheduledExecutorService`, orders return a `Future` and wait for the price without `sleep()`
- JavaFX back-office app (FXML + controllers) with login: manage customers, accounts and staff users, audit log of every action
- Passwords stored only as salted PBKDF2 hashes; login locked for 60 s after 5 failed attempts
- Persistence: save/load a whole bank via Java serialization, several banks in one zip file; the Verwaltung app saves its data automatically after every change
- Unit tests with JUnit 5 and Mockito (about 530 tests)
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


## Verwaltung app (login, customers, accounts, users)
`./mvnw javafx:run` starts the Bankverwaltung, a back-office application for bank staff.

**Login.** Staff members log in with a username and password. Wrong credentials show an error and clear the password field; after 5 failed attempts the login is locked for 60 seconds. Every Verwaltung screen is only reachable after a successful login, and every action is executed in the name of the logged-in user.

On the very first start (no data file yet) one user is created:

| Username | Password |
|---|---|
| `m.alharethi` | `1234` |

> **⚠️ Development only:** the seed password `1234` is meant for development and testing. Change it before any real use: log in, open **Benutzer**, select `m.alharethi` and click **Passwort zurücksetzen**.

**Views** (sidebar on the left, **Abmelden** logs out):
- **Kunden** — search by name or customer number, filter by status; create and edit customers (customer number `K000001`… is assigned automatically; first/last name, date of birth, address, e-mail required, phone optional and validated); deactivate (with confirmation) and reactivate. Customers are never deleted; a deactivated customer gets no new accounts.
- **Konten** — search by account number, IBAN or owner, filter by customer and status; open an account for a customer (Girokonto with dispo, Sparbuch, Festgeldkonto, Kinderkonto, Aktienkonto; currency; opening balance 0; account number and German IBAN assigned automatically); details with live balance; block (with confirmation), unblock, close (with confirmation, only at a balance of 0 — the account then stays locked and is kept). A customer can have several accounts.
- **Benutzer** — staff users: create, edit the display name, reset a password, deactivate (with confirmation; not yourself) and reactivate.
- **Protokoll** — read-only audit log: who did what and when (logins, failed logins, lockouts, logouts, every create/edit/block/close).

**Data file.** All data (bank, customers, accounts, users, audit log) is saved with Java serialization after every change to `~/.bankprojekt/verwaltung.ser`. Another file can be chosen with `-Dbankprojekt.datei=/path/to/file.ser`. Deleting the file resets the app to the seed user (all data is lost).


## Tech stack
- Language: Java 21
- Build: Apache Maven (Maven wrapper included)
- CI: GitHub Actions
- UI: JavaFX (OpenJFX `javafx-controls`, `javafx-fxml`)
- Money rounding: decimal4j
- Testing: JUnit 5 (Jupiter) + Mockito
- Analysis: SpotBugs, JaCoCo, Javadoc

See `pom.xml` for exact versions and plugins.


## Project layout
```
/ (project root)
├─ .github/workflows/build.yml   # CI: build and test on every push and pull request
├─ mvnw, mvnw.cmd, .mvn/         # Maven wrapper
├─ pom.xml
├─ src
│  ├─ main
│  │  ├─ java
│  │  │  ├─ bankprojekt
│  │  │  │  ├─ basisdaten/        # Core domain entities (accounts, money, currency, customer)
│  │  │  │  ├─ verwaltung/        # Bank aggregate, Bankverwaltung, login, password hashing, data file
│  │  │  │  ├─ fabriken/          # Factories for accounts
│  │  │  │  ├─ aktienhandel/      # Shares and stock account (concurrency)
│  │  │  │  ├─ exceptions/        # Domain exceptions
│  │  │  │  ├─ nuetzliches/       # Kalender, console input
│  │  │  │  └─ oberflaeche/       # JavaFX Verwaltung app (Start, BankAnwendung, controllers)
│  │  │  └─ spielereien/          # Runnable demo programs
│  │  └─ resources
│  │     └─ bankprojekt/oberflaeche/  # FXML views and forms + verwaltung.css
│  └─ test
│     └─ java/bankprojekt/        # Unit tests, same packages as the classes they test
├─ LICENSE
└─ README.md
```


## Getting started

### Prerequisites
- Java Development Kit (JDK) 21 or newer
- No Maven installation needed: the repository contains the Maven wrapper (`mvnw` / `mvnw.cmd`), which downloads the right Maven version (3.9.9) on first use. The commands below use `./mvnw` (Windows: `mvnw.cmd`); with Maven 3.9+ installed you can write `mvn` instead.

Verify your environment:
```
java -version
./mvnw -version
```

### Build
```
./mvnw -q -DskipTests package
```
This compiles sources and packages the project. Use `./mvnw package` (without `-DskipTests`) to include running tests.

### Run (JavaFX UI)
The easiest way is the JavaFX Maven plugin:
```
./mvnw javafx:run
```
Alternatively, launch the non-`Application` bootstrap class `Start` from the classpath:
```
./mvnw -q -Dexec.mainClass=bankprojekt.oberflaeche.Start exec:java
```
Notes:
- Run `Start` (not `BankAnwendung`) to avoid the classic "JavaFX runtime components are missing" error when JavaFX is resolved via Maven on the classpath.
- Log in with `m.alharethi` / `1234` (development only, see [Verwaltung app](#verwaltung-app-login-customers-accounts-users)).
- To try things out without touching your real data, start with a separate data file:
  ```
  ./mvnw -q -Dexec.mainClass=bankprojekt.oberflaeche.Start -Dbankprojekt.datei=/tmp/test.ser exec:java
  ```

### Run demo programs
The demo programs live in `spielereien`. The default main class for `exec:java` is `spielereien.KontenSpielereien` (set as the `exec.mainClass` property in `pom.xml`). To run it:
```
./mvnw -q exec:java
```
Or run any other main class, e.g.:
```
./mvnw -q -Dexec.mainClass=spielereien.Bankspielereien exec:java
```
Available demos:
- `spielereien.KontenSpielereien` — interactive: creates an account and asks how much to withdraw
- `spielereien.Bankspielereien` — the bank's stream reports, with the expected results printed alongside
- `spielereien.BankPersistenzSpielereien` — saves two banks into `bankdatei.zip` and reads them back (writes the file into the current directory)
- `spielereien.Aktienspielereien` — several buy/sell orders running at the same time (waits for real price changes, can take a minute or two)
- `spielereien.KundenUndKontenSortieren` — interactive: sorting customers and accounts

### Run tests and coverage
```
./mvnw -q test
```
A JaCoCo coverage report is generated during tests. It covers all of `bankprojekt` except the JavaFX UI (`bankprojekt.oberflaeche`). Find the HTML report in:
```
./target/site/jacoco/index.html
```
Every push and pull request is also built and tested by GitHub Actions (`.github/workflows/build.yml`) with JDK 21, once with the default and once with a German locale. The JaCoCo report of each run can be downloaded from the run's page (artifact `jacoco-report`).
Run a single test class or method:
```
./mvnw -q test -Dtest=BankTest
./mvnw -q test -Dtest=BankTest#methodName
```

### Generate static analysis and docs
- SpotBugs HTML report:
  ```
  ./mvnw -q compile
  open target/site/spotbugs.html   # or view the file in your browser
  ```
- Javadoc (package-level visibility and above):
  ```
  ./mvnw -q package
  open target/site/apidocs/index.html
  ```

### Package an archive (assembly)
A simple assembly is configured for submissions/packaging and runs as part of `package`:
```
./mvnw -q package
```
The archive `target/Bankproject-1.0-SNAPSHOT-submission.zip` contains the sources, `pom.xml` and the generated reports (SpotBugs, JaCoCo and Javadoc).


## Tests
The test suite lives in `src/test/java/bankprojekt`, in the same packages as the classes under test, and uses only JUnit 5 and Mockito.

- **About 530 tests**, around 97 % line and branch coverage of `bankprojekt` (without the UI).
- Every account type, `Geldbetrag`, `Waehrung`, `Kunde`, the factories, the `Bank` (including transfers, reports and persistence), the exceptions and `Kalender` have their own test classes, as do the back-office classes `Bankverwaltung`, `Kundendaten`, `Anmeldung` (lockout with an adjustable test clock), `Passwortschutz` and `Datenspeicher`.
- Test names and comments are in German, e.g. `geldAbheben_gibtFalse_wennUeberDispo`.
- The suite does not depend on the computer's language settings: locale-dependent output is tested with `Locale.GERMANY`, set and restored around each test. To double-check:
  ```
  ./mvnw -q test -DargLine="-Duser.language=de -Duser.country=DE"
  ```
- The stock trading tests contain no `sleep()`: they wait for results with `Future.get(timeout)` and use price limits that are met immediately, so they don't depend on random price changes.


## Usage notes (JavaFX specifics)
- Entry point: `bankprojekt.oberflaeche.Start` calls `Application.launch(BankAnwendung.class, args)`, ensuring JavaFX starts even when OpenJFX is provided via Maven dependencies (classpath) rather than module path.
- If you run the `BankAnwendung` class directly without JavaFX modules on the module path, the JVM may exit early with: "JavaFX runtime components are missing". Always run `Start` instead when launching via Maven.
- The warning "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'" is expected when JavaFX runs from the classpath (`exec:java`) and can be ignored; `javafx:run` puts JavaFX on the module path and avoids it.


## Troubleshooting
- JavaFX runtime components are missing
  - Symptom: JVM exits before your code runs.
  - Fix: Start the UI with `./mvnw javafx:run`, or launch `bankprojekt.oberflaeche.Start` instead of `BankAnwendung`:
    ```
    ./mvnw -q -Dexec.mainClass=bankprojekt.oberflaeche.Start exec:java
    ```
- Java not found or wrong version
  - Ensure JDK 21 or newer is installed and active: `java -version` should show `21` or higher.
- Number and date formats look different
  - Amounts and dates are printed in the format of your computer's language settings (e.g. `1,234.50 EUR` or `1.234,50 EUR`). This is intended; the tests pass with any setting.
- Login is locked ("Zu viele Fehlversuche")
  - After 5 failed attempts the login is locked for 60 seconds; the countdown shows the remaining time. Restarting the app also clears the lock.
- Forgot the password / start over
  - Another active user can reset it under **Benutzer → Passwort zurücksetzen**. If no one can log in, delete `~/.bankprojekt/verwaltung.ser`; the next start creates the seed user again (all data is lost).
- An interactive demo stops at the second question when input is piped in
  - `KontenSpielereien` reads its two answers with two separate readers. Type the answers yourself instead of piping them in all at once.


## Contributing
Contributions are welcome for educational improvements and additional examples. Suggested areas:
- Additional account operations and edge cases (with tests)
- Extended Verwaltung features (bookings, reports, customer portal)
- Documentation and comments (the code comments and Javadoc are in German)

Please open an issue or pull request.


## License
Released under the [MIT License](LICENSE) — © 2026 Mohameed Al-Harethi.
