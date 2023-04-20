package course.concurrency.m3_shared.collections;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockingQueueTestClassTest {

    private int poolSize = 10;
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
    public void testBlockingWhenAdd() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        for (int it = 0; it < bq.getMaxCapacity(); it++) {
            bq.enqueue("" + it);
        }

        assertEquals(bq.getMaxCapacity(), bq.getCapacity());

        Thread producer = new Thread(() -> bq.enqueue("another element"));
        producer.start();
        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, producer.getState());

        bq.dequeue();
        Thread.sleep(100);
        assertEquals(Thread.State.TERMINATED, producer.getState());
    }

    @Test
    public void testBlockingWhenGet() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        bq.enqueue("some value");
        Thread consumer = new Thread(() -> {
            while (bq.getCapacity() != 0)
                bq.dequeue();

            bq.dequeue();
        });

        consumer.start();
        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, consumer.getState());

        bq.enqueue("another value");
        Thread.sleep(100);
        assertEquals(Thread.State.TERMINATED, consumer.getState());
    }

    @Test
    public void testSimultaneousEnqueueAndDequeue() throws InterruptedException {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < poolSize / 2; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < 1000; it++) {
                    bq.enqueue("" + it);
                }
            });

            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < 1000; it++) {
                    bq.dequeue();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10_000, TimeUnit.MILLISECONDS);
        assertEquals(0, bq.getCapacity());
    }

    @Test
    public void testOneThread() {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();

        for (int it = 0; it < 1_000; it++) {
            bq.enqueue("" + it);
            bq.dequeue();
        }

        assertEquals(0, bq.getCapacity());
    }

    @Test
    public void testFifo() {
        BlockingQueueClass<String> bq = new BlockingQueueClass<>();
        int counter = 1;
        for (int it = counter; it <= bq.getMaxCapacity(); it++) {
            bq.enqueue("" + it);
        }

        assertEquals(bq.getMaxCapacity(), bq.getCapacity());

        while (bq.getCapacity() != 0) {
            String current = bq.dequeue();
            assertEquals(String.valueOf(counter), current);
            counter++;
        }
    }
}