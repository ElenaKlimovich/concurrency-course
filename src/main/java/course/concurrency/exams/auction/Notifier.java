package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {

    private final int poolSize = Runtime.getRuntime().availableProcessors();
    private ExecutorService executor = Executors.newFixedThreadPool(poolSize);

    public void sendOutdatedMessage(Bid bid) {
        executor.execute(this::imitateSending);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
