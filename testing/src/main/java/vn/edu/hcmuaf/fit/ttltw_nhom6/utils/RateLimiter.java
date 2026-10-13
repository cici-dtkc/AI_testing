package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
public class RateLimiter {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> clientRequests = new ConcurrentHashMap<>();
    public RateLimiter(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }
    public boolean tryAcquire(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowSizeMillis;
        ConcurrentLinkedQueue<Long> timestamps = clientRequests.computeIfAbsent(clientId, k -> new ConcurrentLinkedQueue<>());
        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
            timestamps.poll();
        }
        if (timestamps.size() < maxRequests) {
            timestamps.add(currentTime);
            return true;
        }
        return false;
    }
}
