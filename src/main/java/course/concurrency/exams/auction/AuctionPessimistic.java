package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(1L, 1L, 1L);

    public boolean propose(Bid bid) {
        if (bid.getPrice() <= latestBid.getPrice())
            return false;
        synchronized (this) {
            if (bid.getPrice() > latestBid.getPrice()) {
                latestBid = bid;
                notifier.sendOutdatedMessage(latestBid);
            }
        }
        return true;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
