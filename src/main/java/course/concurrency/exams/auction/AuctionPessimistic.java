package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(1L, 1L, 1L);

    public boolean propose(Bid bid) {
        synchronized (this) {
            if (bid.getPrice() > latestBid.getPrice()) {
                latestBid = bid;
                return true;
            }
            notifier.sendOutdatedMessage(latestBid);
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
