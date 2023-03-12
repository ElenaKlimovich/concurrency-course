package course.concurrency.m2_async.cf.min_price;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        List<CompletableFuture<Double>> futureList = shopIds.stream()
                .map(shopId ->
                        CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                                .exceptionally(e -> {
                                    System.out.println("Error!!! -> " + e);
                                    return Double.NaN;
                                })
                                .completeOnTimeout(Double.NaN, 2900, TimeUnit.MILLISECONDS)
                )
                .collect(Collectors.toList());

        List<Double> prices = CompletableFuture.allOf(futureList
                        .toArray(CompletableFuture[]::new))
                .thenApply(r -> futureList.stream()
                        .map(CompletableFuture::join)
                        .filter(price -> !Double.isNaN(price))
                        .collect(Collectors.toList()))
                .join();

        return prices.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(Double.NaN);
    }
}
