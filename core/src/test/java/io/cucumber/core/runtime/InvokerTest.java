package io.cucumber.core.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvokerTest {

    @Test
    public void doesnt_time_out_if_it_doesnt_take_too_long() throws Throwable {
        final Slow slow = new Slow();
        String what = Invoker.timeout(new Invoker.Callback<String>() {
            @Override
            public String call() throws Throwable {
                return slow.slow(10);
            }
        }, 50);
        assertThat(what, is(equalTo("slept 10ms")));
    }

    @Test
    public void times_out_if_it_takes_too_long() {
        final Slow slow = new Slow();
        Executable testMethod = () -> Invoker.timeout(() -> slow.slow(100), 50);
        TimeoutException expectedThrown = assertThrows(TimeoutException.class, testMethod);
        assertThat(expectedThrown.getMessage(), is(equalTo("Timed out after 50ms.")));
    }

    @Test
    public void times_out_infinite_loop_if_it_takes_too_long() {
        final Slow slow = new Slow();
        Executable testMethod = () -> Invoker.timeout((Invoker.Callback<Void>) () -> {
            slow.infinite();
            return null;
        }, 10);
        TimeoutException expectedThrown = assertThrows(TimeoutException.class, testMethod);
        assertThat(expectedThrown.getMessage(), is(equalTo("Timed out after 10ms.")));
    }

    @Test
    public void times_out_infinite_latch_wait_if_it_takes_too_long() {
        final Slow slow = new Slow();
        Executable testMethod = () -> Invoker.timeout((Invoker.Callback<Void>) () -> {
            slow.infiniteLatchWait();
            return null;
        }, 10);
        TimeoutException expectedThrown = assertThrows(TimeoutException.class, testMethod);
        assertThat(expectedThrown.getMessage(), is(equalTo("Timed out after 10ms.")));
    }


    @Test
    public void times_out_busy_wait_if_it_takes_too_long() {
        final Slow slow = new Slow();
        Executable testMethod = () -> Invoker.timeout((Invoker.Callback<Void>) () -> {
            slow.busyWait();
            return null;
        }, 1);
        TimeoutException expectedThrown = assertThrows(TimeoutException.class, testMethod);
        assertThat(expectedThrown.getMessage(), is(equalTo("Timed out after 1ms.")));
    }

    @Test
    public void doesnt_leak_threads() throws Throwable {

        long initialNumberOfThreads = Thread.getAllStackTraces().size();
        long currentNumberOfThreads = Long.MAX_VALUE;

        boolean cleanedUp = false;
        for (int i = 0; i < 1000; i++) {
            Invoker.timeout(new Invoker.Callback<String>() {
                @Override
                public String call() throws Throwable {
                    return null;
                }
            }, 10);
            Thread.sleep(5);
            currentNumberOfThreads = Thread.getAllStackTraces().size();
            if (i > 20 && currentNumberOfThreads <= initialNumberOfThreads) {
                cleanedUp = true;
                break;
            }
        }
        assertThat(String.format("Threads weren't cleaned up, initial count: %d current count: %d",
            initialNumberOfThreads, currentNumberOfThreads),
            cleanedUp, is(equalTo(true)));
    }

    public static class Slow {
        int busyCounter = Integer.MIN_VALUE;

        public String slow(int millis) throws InterruptedException {
            sleep(millis);
            return String.format("slept %sms", millis);
        }

        public void infinite() throws InterruptedException {
            while (true) {
                sleep(1);
            }
        }

        public void infiniteLatchWait() throws InterruptedException {
            new CountDownLatch(1).await();
        }

        public int busyWait() throws InterruptedException {
            while (busyCounter < Integer.MAX_VALUE) {
                busyCounter += 1;
            }

            return busyCounter;
        }
    }

}
