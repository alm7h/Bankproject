# Bankproject

A Java 21 + JavaFX learning project that models a small banking domain and includes a simple JavaFX UI. The project demonstrates clean domain modeling (accounts, customers, money, currencies), design patterns (factory), collections/streams, persistence via Java serialization, unit tests (JUnit 5 + Mockito), and code quality tooling (SpotBugs, JaCoCo, Javadoc).

This repository is intended for teaching/learning purposes but is structured like a production Maven project so you can build, test, and package it easily.


## Contents
- Overview
- Features
- Tech stack
- Project layout
- Getting started
  - Prerequisites
  - Build
  - Run (JavaFX UI)
  - Run demo snippets
  - Run tests and coverage
  - Generate static analysis and docs
  - Package an archive
- Usage notes (JavaFX specifics)
- Troubleshooting
- Contributing
- License


## Overview
Core domain packages live under `bankprojekt.*`:
- `bankprojekt.basisdaten` — Core banking entities such as `Konto` (account), `Girokonto`, `Sparbuch`, `Festgeldkonto`, `Kinderkonto`, `Geldbetrag` (money), `Waehrung` (currency), and `Kunde` (customer).
- `bankprojekt.verwaltung` — The `Bank` aggregate which manages accounts, transfers, and queries.
- `bankprojekt.fabriken` — Simple factory types to create accounts.
- `bankprojekt.exceptions` — Domain exceptions like `GesperrtException`, `UngueltigeKontonummerException`.
- `bankprojekt.oberflaeche` — A small JavaFX UI (FXML) connecting view and model via `KontoController`, launched through `Start`.
- `bankprojekt.aktienhandel` — Optional stock trading examples (`Aktie`, `Aktienkonto`).

Additional packages provide exercises and demos (`spielereien`, `sortiererei`, `automat`, `Nullstellen`, `generisch`).


## Features
- Multiple account types with deposits, withdrawals, and transfers
- Account factories to encapsulate creation logic
- Bank-level queries: list accounts/customers, totals by customer, customers with empty accounts, birthdays, seniors count, etc.
- JavaFX UI (FXML + Controller) to perform basic account operations and visualize state
- Serializable persistence helpers for saving/loading the bank state
- Unit tests with JUnit 5 and Mockito
- Code quality: SpotBugs static analysis, JaCoCo coverage, Javadoc generation


## Tech stack
- Language: Java 21
- Build: Apache Maven
- UI: JavaFX (OpenJFX `javafx-controls`, `javafx-fxml`)
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
│  │  │  └─ bankprojekt
│  │  │     ├─ basisdaten/        # Core domain entities
│  │  │     ├─ verwaltung/        # Bank aggregate and operations
│  │  │     ├─ fabriken/          # Factories for accounts
│  │  │     ├─ oberflaeche/       # JavaFX UI (Start, KontoAnwendung, Controller)
│  │  │     ├─ aktienhandel/      # Stock-related examples
│  │  │     └─ ... (other demo/exercise packages)
│  │  └─ resources
│  │     └─ bankprojekt/oberflaeche/  # FXML + CSS + images
│  └─ test
│     └─ java/ ...                 # Unit tests
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

### Run demo snippets
There are several small demo programs under `spielereien` and other packages. The `exec-maven-plugin` default main class is set to `spielereien.KontenSpielereien` in `pom.xml`. To run it:
```
mvn -q exec:java
```
Or run any other main class, e.g.:
```
mvn -q -Dexec.mainClass=spielereien.Bankspielereien exec:java
```

### Run tests and coverage
```
mvn -q test
```
A JaCoCo coverage report is generated during tests. By default, the report includes the `bankprojekt/verwaltung/Bank` class. Find the HTML report in:
```
./target/site/jacoco/index.html
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
A simple assembly is configured for submissions/packaging:
```
mvn -q -DskipTests package assembly:single
```
The archive will be created under `target/`.


## Usage notes (JavaFX specifics)
- Entry point: `bankprojekt.oberflaeche.Start` calls `Application.launch(KontoAnwendung.class, args)`, ensuring JavaFX starts even when OpenJFX is provided via Maven dependencies (classpath) rather than module path.
- If you run the `KontoAnwendung` class directly without JavaFX modules on the module path, the JVM may exit early with: "JavaFX runtime components are missing". Always run `Start` instead when launching via Maven.


## Troubleshooting
- JavaFX runtime components are missing
  - Symptom: JVM exits before your code runs.
  - Fix: Launch `bankprojekt.oberflaeche.Start` instead of `KontoAnwendung`:
    ```
    mvn -q -Dexec.mainClass=bankprojekt.oberflaeche.Start exec:java
    ```
- Java not found or wrong version
  - Ensure JDK 21 is installed and active: `java -version` should show `21`.
- Tests fail due to locale/formatting
  - Some demos/formatting exercises may be locale-sensitive. Set a predictable locale when running, e.g., `-Duser.language=en -Duser.country=US` if needed.


## Contributing
Contributions are welcome for educational improvements and additional examples. Suggested areas:
- More unit tests and Mockito examples
- Additional account operations and edge cases
- Extended JavaFX UI interactions
- Documentation and comments (English and German)

Please open an issue or pull request.


## License
No license has been specified yet. If you plan to publish and accept contributions, consider adding an open-source license (e.g., MIT, Apache-2.0). Until a license is added, all rights are reserved by the repository owner.
