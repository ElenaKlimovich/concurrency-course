package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicReference<Bid> latestBid = new AtomicReference<>(new Bid(1L, 1L, 1L));

    public boolean propose(Bid bid) {
        if (bid.getPrice() > latestBid.get().getPrice()) {
            boolean changed = false;
            while (!changed) {
                changed = latestBid.compareAndSet(latestBid.get(), bid);
            }
            notifier.sendOutdatedMessage(latestBid.get());
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
