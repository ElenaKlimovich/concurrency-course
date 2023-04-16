package course.concurrency.m3_shared.collections;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockingQueueTestClassTest {

    private int poolSize = Runtime.getRuntime().availableProcessors() * 3;
    private int iterations = 100;

    @Test
    public void testEnqueueToMaxCapacity() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < iterations; it++) {
                    bq.enqueue("" + it);
                }
            });
        }

        assertEquals(0, bq.getCapacity());
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(20_000, TimeUnit.MILLISECONDS);
        assertEquals(bq.getMaxCapacity(), bq.getCapacity());
    }

    @Test
    public void testDequeue() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch latch = new CountDownLatch(1);

        // prepare blocking queue
        for (int it = 0; it < bq.getMaxCapacity(); it++) {
            bq.enqueue("" + it);
        }
        assertEquals(bq.getMaxCapacity(), bq.getCapacity());

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < iterations; it++) {
                    bq.dequeue();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(20_000, TimeUnit.MILLISECONDS);
        assertEquals(0, bq.getCapacity());
    }

    @Test
    public void testBlocking() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();

        for (int it = 0; it < bq.getMaxCapacity(); it++) {
            bq.enqueue("" + it);
        }

        assertEquals(bq.getMaxCapacity(), bq.getCapacity());

        Thread producer = new Thread(() -> bq.enqueue("another element"));
        producer.start();
        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, producer.getState());

        Thread consumer = new Thread(() -> {
            for (int it = 0; it < 100; it++)
                bq.dequeue();

        });
        consumer.start();
        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, consumer.getState());
    }

    @Test
    public void testSimultaneousEnqueueAndDequeue() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < 10_000; it++) {
                    bq.enqueue("" + it);
                }
            });

            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < 1_000; it++) {
                    bq.dequeue();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10_000, TimeUnit.MILLISECONDS);
        assertEquals(bq.getMaxCapacity(), bq.getCapacity());
    }

    @Test
    public void testOneThread() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        ExecutorService single = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < poolSize; i++) {
            single.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < 1_000; it++) {
                    bq.enqueue("" + it);
                    bq.dequeue();
                }
            });
        }

        latch.countDown();
        single.shutdown();
        single.awaitTermination(10_000, TimeUnit.MILLISECONDS);
        assertEquals(0, bq.getCapacity());
    }

    @Test
    public void testFifo() {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        for (int it = 1; it <= bq.getMaxCapacity(); it++) {
            bq.enqueue("" + it);
        }

        String firstElement = bq.dequeue();
        assertEquals(String.valueOf(1), firstElement);

        while (bq.getCapacity() > 1) {
            bq.dequeue();
        }

        String lastElement = bq.dequeue();
        assertEquals(String.valueOf(bq.getMaxCapacity()), lastElement);
    }
}