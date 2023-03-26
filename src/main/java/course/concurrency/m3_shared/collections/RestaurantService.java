package course.concurrency.m3_shared.collections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class RestaurantService {

    private Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>() {{
        put("A", new Restaurant("A"));
        put("B", new Restaurant("B"));
        put("C", new Restaurant("C"));
    }};

    private Map<String, Long> stat = new ConcurrentHashMap<>() ;

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public void addToStat(String restaurantName) {
        stat.merge(restaurantName, 1L, Long::sum);
    }

    public Set<String> printStat() {
        Set<String> print = new CopyOnWriteArraySet<>();
        for (Map.Entry<String, Long> entry: stat.entrySet()) {
            print.add(entry.getKey() + " - " + entry.getValue().toString());
        }
        return print;
    }
}
