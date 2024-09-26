package course.concurrency.m3_shared.collections;

import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueueClass<T> {

    private Queue<T> queue = new LinkedList<>();
    private volatile int capacity = 0;
    private final int maxCapacity = 10;

    public int getCapacity() {
        return capacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public synchronized void enqueue(T value) {
        while (capacity >= maxCapacity) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        queue.add(value);
        capacity++;
        this.notify();
    }

    public synchronized T dequeue() {
        while (capacity == 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        capacity--;
        T value = queue.poll();
        this.notify();
        return value;
    }

}
