package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(new Bid(1L, 1L, 1L), true);

    public boolean propose(Bid bid) {
        Bid current;
        do {
            current = latestBid.getReference();
            if (bid.getPrice() <= current.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(current, bid, true, true));

        notifier.sendOutdatedMessage(current);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        boolean markChanged;
        do {
            markChanged = latestBid.attemptMark(latestBid.getReference(), false);
        } while (!markChanged);
        return latestBid.getReference();
    }
}
