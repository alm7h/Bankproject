package bankprojekt.oberflaeche;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javafx.application.Platform;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * JUnit-Erweiterung für Tests der JavaFX-Oberfläche – ohne zusätzliche Testbibliothek.
 * <ul>
 *   <li>startet das JavaFX-Toolkit einmal pro JVM,</li>
 *   <li>führt jeden Test samt {@code @BeforeEach}/{@code @AfterEach} im JavaFX-Thread aus
 *       (mit Zeitlimit, damit ein versehentlich offener Dialog den Lauf nicht blockiert),</li>
 *   <li>lässt keine modalen Dialoge aufgehen: Ein Dialog, den der Test nicht mit
 *       {@link Dialogantworten} erwartet, lässt den Test scheitern.</li>
 * </ul>
 * Es werden keine Fenster angezeigt; im CI läuft der Build trotzdem unter xvfb, weil
 * das Toolkit einen Bildschirm braucht.
 */
class JavaFxErweiterung implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, InvocationInterceptor {

    /** Zeitlimit pro Testmethode */
    private static final long ZEITLIMIT_SEKUNDEN = 30;
    /** ob das Toolkit schon läuft */
    private static boolean gestartet;

    @Override
    public void beforeAll(ExtensionContext kontext) throws Exception {
        toolkitStarten();
    }

    @Override
    public void afterAll(ExtensionContext kontext) {
        Dialoge.setAnzeige(null);
    }

    /**
     * Startet das JavaFX-Toolkit, falls das noch nicht geschehen ist.
     * @throws InterruptedException wenn das Warten unterbrochen wird
     */
    private static synchronized void toolkitStarten() throws InterruptedException {
        if (gestartet)
            return;
        CountDownLatch bereit = new CountDownLatch(1);
        try {
            Platform.startup(bereit::countDown);
        } catch (IllegalStateException schonGestartet) {
            bereit.countDown();
        }
        Platform.setImplicitExit(false);
        if (!bereit.await(ZEITLIMIT_SEKUNDEN, TimeUnit.SECONDS))
            throw new IllegalStateException("JavaFX startet nicht");
        gestartet = true;
    }

    @Override
    public void beforeEach(ExtensionContext kontext) {
        // Standard: jeder Dialog ist unerwartet; Tests erwarten Dialoge mit Dialogantworten
        Dialoge.setAnzeige(dialog -> {
            throw new AssertionError("Unerwarteter Dialog: " + dialog.getHeaderText());
        });
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> aufruf, ReflectiveInvocationContext<Method> kontext,
                                          ExtensionContext erweiterung) throws Throwable {
        imFxThread(aufruf::proceed);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> aufruf, ReflectiveInvocationContext<Method> kontext,
                                    ExtensionContext erweiterung) throws Throwable {
        imFxThread(aufruf::proceed);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> aufruf, ReflectiveInvocationContext<Method> kontext,
                                            ExtensionContext erweiterung) throws Throwable {
        imFxThread(aufruf::proceed);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> aufruf, ReflectiveInvocationContext<Method> kontext,
                                         ExtensionContext erweiterung) throws Throwable {
        imFxThread(aufruf::proceed);
    }

    /**
     * Eine Aktion, die eine beliebige Ausnahme werfen darf.
     */
    @FunctionalInterface
    interface Aktion {
        void ausfuehren() throws Throwable;
    }

    /**
     * Führt eine Aktion im JavaFX-Thread aus und wartet auf ihr Ende.
     * @param aktion die Aktion
     * @throws Throwable jede Ausnahme der Aktion
     */
    private static void imFxThread(Aktion aktion) throws Throwable {
        // Falls ein BeforeEach ohne Interceptor vor beforeAll liefe
        toolkitStarten();
        if (Platform.isFxApplicationThread()) {
            aktion.ausfuehren();
            return;
        }
        CompletableFuture<Void> ergebnis = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                aktion.ausfuehren();
                ergebnis.complete(null);
            } catch (Throwable t) {
                ergebnis.completeExceptionally(t);
            }
        });
        try {
            ergebnis.get(ZEITLIMIT_SEKUNDEN, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        } catch (TimeoutException e) {
            throw new AssertionError("Der Test hängt im JavaFX-Thread (offener Dialog?)", e);
        }
    }
}
