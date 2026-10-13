package vn.edu.hcmuaf.fit.ttltw_nhom6.service.cache;

import vn.edu.hcmuaf.fit.ttltw_nhom6.model.CartItem;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheService {
    // cache product detail
    public static final Map<Integer, Comic> comicCache = new ConcurrentHashMap<>();

    // cache product list
    public static final Map<String, List<Comic>> comicListCache = new ConcurrentHashMap<>();

    // cache cart
    public static final Map<Integer, List<CartItem>> cartCache = new ConcurrentHashMap<>();
}
