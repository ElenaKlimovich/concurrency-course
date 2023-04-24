package course.concurrency.exams.refactoring;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {

        final List<Others.RouterState> cachedRecords = routerStore.getCachedRecords();
        final List<MountTableRefresherThread> refreshThreads = cachedRecords.stream()
                .filter(routerState -> routerState != null && routerState.getAdminAddress() != null && routerState.getAdminAddress().length() != 0)
                .map(routerState -> createRefresher(routerState.getAdminAddress()))
                .collect(Collectors.toList());

        if (!refreshThreads.isEmpty()) {
            invokeRefresh(refreshThreads);
        }
    }

    private MountTableRefresherThread createRefresher(String adminAddress) {
        if (isLocalAdmin(adminAddress)) {
            adminAddress = "local";
        }
        return new MountTableRefresherThread(new Others.MountTableManager(adminAddress), adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<MountTableRefresherThread> refreshThreads) {
        // start all the threads
        List<CompletableFuture<Boolean>> futures = refreshThreads.stream()
                .map(refThread -> CompletableFuture.supplyAsync(() -> {
                            refThread.update();
                            return true;
                        })
                        .exceptionally(e -> {
                            log("Mount table cache refresher was interrupted.");
                            return null;
                        })
                        .completeOnTimeout(false, cacheUpdateTimeout, TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());


        final boolean allReqCompleted = CompletableFuture.allOf(futures
                        .toArray(CompletableFuture[]::new))
                .thenApply(r -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(isCompleted -> !isCompleted)
                        .findAny().orElse(true))
                .join();

        if (!allReqCompleted) {
            log("Not all router admins updated their cache");
        }
        logResult(refreshThreads);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<MountTableRefresherThread> refreshThreads) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        for (MountTableRefresherThread mountTableRefreshThread : refreshThreads) {
            if (mountTableRefreshThread.isSuccess()) {
                successCount.getAndIncrement();
            } else {
                failureCount.getAndIncrement();
                // remove RouterClient from cache so that new client is created
                removeFromCache(mountTableRefreshThread.getAdminAddress());
            }
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount.get(), failureCount.get()));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }
    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}