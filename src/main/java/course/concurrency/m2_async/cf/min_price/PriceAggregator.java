package course.concurrency.m2_async.cf.min_price;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private ExecutorService executor = Executors.newCachedThreadPool();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        CompletableFuture<Double> futureResult = CompletableFuture.supplyAsync(() -> {
            List<Double> prices = new ArrayList<>();
            long timeOut = 3000 / shopIds.size();
            for (long shopId : shopIds) {
                CompletableFuture<Double> futurePrice =
                        CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                                .exceptionally(e -> {
                                    System.out.println("Error!!! -> " + e);
                                    return Double.NaN;
                                });
                prices.add(futurePrice.completeOnTimeout(Double.NaN, timeOut, TimeUnit.MILLISECONDS).join());
            }

            return prices.stream().min(Double::compareTo).orElse(Double.NaN);
        });

        return futureResult.completeOnTimeout(Double.NaN, 2900, TimeUnit.MILLISECONDS).join();
    }
}
