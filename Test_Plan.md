# Phase 1 Testing Plan — Bankprojekt

## Context
Test coverage is thin and lopsided. `Bank` is well covered (BankTest has about 40 tests, plus BankMockitoTest), and `Sparbuch` is partly covered. `Girokonto` has only 3 tests, and `Aktienkonto` has only its observer tests. **These classes have no tests at all:** `Geldbetrag`, `Waehrung`, `Konto` (the Template Method `abheben`, IBAN, properties, serialization), `Kunde`, `Kinderkonto`, `Festgeldkonto`, every factory, `Aktie`, the exceptions, and `Bank` persistence and stream reports (`speichern`/`einlesen`, `getKundenMitLeeremKonto`, `getKundengeburtstage`, `getAnzahlSenioren`, `schenkungAnNeuerwachsene`).
Goal: work through the domain bottom-up, one class at a time, so each layer is tested before the layers that depend on it.

## Status
| Step | State | Tests | Commit(s) |
|---|---|---|---|
| 0 Baseline | ✅ done | 69 existing, all green (locale en_001) | — |
| JaCoCo scope | ✅ done | — | `build: widen JaCoCo report scope…` |
| 1 `GeldbetragTest` | ✅ done | 52 | 2× `fix(Geldbetrag)`, `test: add unit tests for Geldbetrag` |
| 2 `WaehrungTest` | ✅ done | 13 | `test: add unit tests for Waehrung` |
| 3 `KundeTest` | ✅ done | 22 | `fix(Kunde)`, `test: add unit tests for Kunde` |
| 4 `KontoTest` | ✅ done | 48 | `test: add unit tests for Konto`, `test: call equals directly…` |
| 5 `GirokontoTest` | ✅ done | 23 (was 3, duplicate removed) | `test: extend Girokonto tests for dispo, transfers and currency switch` |
| 6 `SparbuchTest` / `SparbuchMockitoTest` | ⏭ next | | |
| 7–16 | open | | |

**Suite total: 225 tests, all green** — on the default locale and with `-Duser.language=de -Duser.country=DE`.

## Bugs found and fixed
| # | Class | Bug | Fix |
|---|---|---|---|
| 1 | `Geldbetrag` | `umrechnen(null)` threw `NullPointerException`, JavaDoc promises `IllegalArgumentException` | explicit null check |
| 2 | `Geldbetrag` | `hashCode` hashed the raw amount while `equals` compares the euro value → 1 EUR equals 61.5 MKD but hash codes differed (Object contract broken) | private `inEuro()` shared by `compareTo` and `hashCode` |
| 3 | `Kunde` | String constructor used the locale's SHORT date format: German `"15.03.85"` → 2085 (rejected as future), en_001 `"01/01/90"` → year 90 AD | fixed `dd.MM.yy` pattern, two-digit year resolved into the last 100 years; `GirokontoTest`/`SparbuchTest` literals changed to `"01.01.90"` |

**Coverage (JaCoCo, branches):** `Geldbetrag` 30/30, `Konto` 40/40, `Kunde` 16/18 (the 2 missing are the locale branch in the static initializer, which can't be reached without reloading the class).

**Lessons for the remaining tests:**
- Check `equals` against `null` and foreign types by calling it directly (`assertFalse(x.equals(null))`). `assertNotEquals(null, x)` never calls `x.equals`.
- `Konto.ausgebenAufDerKonsole()` prints to the console from the business layer, against the course convention. It stays untested; it's reported, not fixed.

## Decisions (confirmed with you)
- **Tools:** only JUnit 5 (Jupiter, including `@Nested`, `@ParameterizedTest`, which is part of `junit-jupiter`) and Mockito 5.15 (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `ArgumentCaptor`). No new dependencies.
- **When a test finds a bug:** I write the failing test, **stop and report it**, and don't change production code until you decide. Once you decide, the production fix gets its own `fix(Klasse): …` commit, and the test class follows in a `test: …` commit.
- **JaCoCo:** widen the existing report's `<includes>` in `pom.xml` from `Bank.class` to `bankprojekt/**`, and add `<excludes>` for `bankprojekt/oberflaeche/**` and `bankprojekt/aktienhandel/Aktienspielereien*`. No new plugin.
- **Out of scope:** `bankprojekt.oberflaeche` (JavaFX UI), `spielereien.*`, `aktienhandel.Aktienspielereien`, and `EinAusgabe` (interactive console input). These are presentation layer.
- **Existing tests:** extend them, and fix clear defects. Each change gets reported.
- **Conventions:** German comments and test names (match the existing style, e.g. `geldAbheben_gibtFalse_wennUeberDispo`). No `@author` tags in new files. No `Thread.sleep` in tests. Locale-dependent assertions run under `Locale.GERMANY`, which is saved and restored around each test.
- **AI attribution:** nothing in the code or comments says the work was AI-generated. Every commit message ends with the trailer `Assisted-by: Claude Code (Claude Opus 5.5)`. All earlier branch commits were rewritten to carry it as well.
- **Commits:** on branch `test/testsuite`. One commit per test class or fix. Conventional-commit subjects (`test:`, `fix(Klasse):`, `docs:`, `build:`) with an explanatory body. Never commit a red build.

## Prioritized order (one class at a time, run the tests after each one)

**Step 0 – Baseline.** ✅ Run `mvn test` with IntelliJ's bundled Maven and record which tests pass or fail today. Any red test gets reported before anything else.

### Priority 1 — Value types: everything else depends on them (`basisdaten`)
1. ✅ **`GeldbetragTest`** (new). This is the most important class: every balance and rule goes through it. Cover the constructors (NaN/Infinity/null currency → IAE), `plus`/`minus` (null or negative → IAE, currency conversion into this object's currency), `mal`, `umrechnen` (same currency returns the same instance, rounding to 2 decimal places), `isNegativ`, `compareTo` across currencies, `equals`/`hashCode`, `toString`, and immutability (the original is unchanged after an operation).
   - ⚠️ Expected finding: equal amounts in different currencies give different `hashCode` values → **stop and report**. *Confirmed and fixed (bugs #1 and #2).*
2. ✅ **`WaehrungTest`** (new, small). Exchange rates, codes (`kuerzel`), `toString`.
3. ✅ **`KundeTest`** (new). All 4 constructors, null checks in the setters, future birthday → IAE, `getName`, `equals`/`hashCode`/`compareTo` by name, `adresseProperty`, and a serialization round trip (the address is restored).
   - ⚠️ The String-date constructor and `getAnrede()` depend on the locale. I'll test them with `Locale` saved and restored in `@BeforeEach`/`@AfterEach`. If that isn't enough, I'll ask you. *The String constructor turned out to be buggy (bug #3, fixed).*

### Priority 2 — The core abstraction (`Konto`, Template Method)
4. ✅ **`KontoTest`** (new). Uses a small concrete test subclass or a Mockito spy (`mock(Konto.class, CALLS_REAL_METHODS)`) to test the **Template Method** in isolation. It checks the order validate → lock → `pruefeAbhebung` → deduct → `nachAbhebung`, and confirms the hooks are not called on rejection or when the account is locked (Mockito `inOrder`/`never`). Also: constructor limits (−1, 0, 9 999 999 999, 10 000 000 000), `einzahlen`, `sperren`/`entsperren`/`setInhaber` while locked, `getIban` (a known-valid IBAN, BLZ out of range → ""), `getKontonummerFormatiert`, `equals`/`hashCode`/`compareTo`, `waehrungswechsel`, the JavaFX properties (`kontostandProperty`, `imPlusProperty` binding, `gesperrtProperty`; no FX toolkit needed), and a serialization round trip (balance and lock state survive).

### Priority 3 — Concrete account types (each has its own `pruefeAbhebung` rule)
5. **`GirokontoTest`** (extend and clean up). ~~Fix the locale-dependent `"01/01/90"`~~ (done with bug #3). Remove the duplicate test. Add the dispo boundary (exactly −dispo is allowed, 0.01 more is rejected), `setDispo` validation, `ueberweisungAbsenden`/`ueberweisungEmpfangen` (locked, null parameters, cover check), and `waehrungswechsel` converting the dispo too.
6. **`SparbuchTest` / `SparbuchMockitoTest`** (extend). Exact boundaries: MINIMUM 0.50 €, 2000 €/month, a year change with the same month number (Jan 2026 → Jan 2027 must reset), a rejected withdrawal must not count toward the monthly total, and the `Kalender(null)` constructor → IAE.
7. **`KinderkontoTest`** (new). Exactly 50 € is allowed, 50.01 € is rejected, no overdraft.
8. **`FestgeldkontoTest`** (new). `kuendigen` (capped at the balance, null/negative → IAE), withdrawing at most the cancelled amount (`gekuendigt`), the cancelled amount shrinking after a withdrawal, and no second withdrawal of the same amount.

### Priority 4 — Factories (`fabriken`)
9. **`KontofabrikTest`** (new, one file with `@Nested` per factory). Each factory returns the right type with the given owner and number. `GirokontoFabrik` passes its dispo through; `GirokontoFabrik(null)` → IAE at `erstellen`.

### Priority 5 — `Bank` (fill the gaps; existing tests stay)
10. **`BankTest`** (extend). `kontoErstellen` with a null factory, and with Kinderkonto, Festgeldkonto and Aktienkonto factories. Plus the methods that have no tests yet:
    - `getKundenMitLeeremKonto` (no duplicates), `getKundengeburtstage` (sorted by month and day, year ignored), `getAnzahlSenioren` (exactly 67 years today versus one day short; birthdays calculated relative to `LocalDate.now()`, because `Bank` uses `now()` directly), `schenkungAnNeuerwachsene` (exactly one account per customer).
    - ⚠️ Expected finding: a factory that throws still uses up an account number (`naechsteFreieKontonummer++` runs before `erstellen`) → **stop and report**.
11. **`BankPersistenzTest`** (new). `speichern`→`einlesen` round trip using `ByteArrayOutputStream`/`ByteArrayInputStream`: the BLZ, every account type, balances and lock state are all preserved. Corrupt or empty input → an empty bank with BLZ 0. **The stream stays open**, checked with a Mockito `spy` on the stream (`verify(never()).close()`), and two banks are written to and read back from one `ZipOutputStream`.
12. **`BankMockitoTest`** (extend). The rollback in `geldUeberweisen` when `ueberweisungEmpfangen` throws: the sender gets `einzahlen(betrag)` and the method returns false.

### Priority 6 — Concurrency (`aktienhandel`), last because it is the hardest to make deterministic
13. **`AktieTest`** (new). Constructor validation (null, negative, duplicate WKN → IAE), `getAktie` lookup, and add/remove of listeners.
14. **`AktienkontoTest`** (new, separate from the observer test). `pruefeAbhebung` (no overdraft), a buy with too little balance → 0 €, an unknown WKN or `anzahl ≤ 0` → 0 €, selling something not in the depot → 0 €, depot stock after a buy and a sell, and the balance after trading.
    - **How the tests stay deterministic:** each test uses its own unique WKN (the `Aktie` registry is static), and the price limits are chosen so the condition is met **immediately** at the current price. Results come from `Future.get(timeout)`, never from `sleep`.
    - ⚠️ "Wait until the price reaches X" can't be tested deterministically, because `kursAendern` is private, random and on a 1–5 s timer. **I won't change production code to make it testable.** If you want that tested, I'll ask you first.

### Priority 7 — Small leftovers
15. **Exceptions** (one small test). The `GesperrtException` message contains the account number, and `UngueltigeKontonummerException` is a RuntimeException.
16. **`Kalender`**. Returns today's date (trivial).

## Files
- Modified: `pom.xml` (only the JaCoCo `<includes>`/`<excludes>`), `GirokontoTest`, `SparbuchTest`, `SparbuchMockitoTest`, `BankTest`, `BankMockitoTest`.
- New in `src/test/java/bankprojekt/…`: `basisdaten/{Geldbetrag,Waehrung,Kunde,Konto,Kinderkonto,Festgeldkonto}Test`, `fabriken/KontofabrikTest`, `verwaltung/BankPersistenzTest`, `aktienhandel/{Aktie,Aktienkonto}Test`, `exceptions/ExceptionsTest`.
- Reused: the `mockEinfuegen(Konto)` helper pattern from `BankMockitoTest` (a throwaway `Kontofabrik`) and the `Kalender` mock from `SparbuchMockitoTest`.

## Verification
- After each class: `"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" test -Dtest=<Klasse>Test`.
- After each priority block: a full `mvn test`, then check `target/site/jacoco/index.html` for line and branch coverage per class.
- Run the whole suite twice to catch flaky concurrency tests, plus once with `-Duser.language=de -Duser.country=DE` to catch locale dependence.
- I check in with you after each priority block, and right away for every "stop and report" bug.
