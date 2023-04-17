package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private AtomicLong nextId = new AtomicLong();

    public long createOrder(List<Item> items) {
        final long id = nextId.getAndIncrement();
        final Order order = new Order(id, items);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        Order updated = currentOrders.compute(orderId,
                (k, v) -> new Order(orderId, v.getItems(), v.isPacked(), paymentInfo, Order.Status.IN_PROGRESS));
        if (updated.checkStatus()) {
            deliver(updated);
        }
    }

    public void setPacked(long orderId) {
        Order updated = currentOrders.compute(orderId,
                (k, v) -> new Order(orderId, v.getItems(), true, v.getPaymentInfo(), Order.Status.IN_PROGRESS));
        if (updated.checkStatus()) {
            deliver(updated);
        }
    }

    private void deliver(Order order) {
        final Order delivered = new Order(order.getId(), order.getItems(), order.isPacked(), order.getPaymentInfo(), Order.Status.DELIVERED);
        currentOrders.put(order.getId(), delivered);
    }

    public synchronized boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
