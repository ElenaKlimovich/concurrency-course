package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(1L, 1L, 1L);
    private boolean isStopped = false;

    public boolean propose(Bid bid) {
        if (!isStopped) {
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
        return false;
    }

    public synchronized Bid getLatestBid() {
        return latestBid;
    }

    public synchronized Bid stopAuction() {
        isStopped = true;
        return latestBid;
    }
}
