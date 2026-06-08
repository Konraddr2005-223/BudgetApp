package com.zadsoft.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Rate limit configuration: max 100 requests per 1 minute
    private static final int MAX_REQUESTS = 100;
    private static final long TIME_WINDOW_MS = 60 * 1000L;

    private final Map<String, List<Long>> requestTimesByIp = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        List<Long> timestamps = requestTimesByIp.computeIfAbsent(ip, k -> new ArrayList<>());

        synchronized (timestamps) {
            // Remove timestamps outside of the sliding window
            timestamps.removeIf(time -> now - time > TIME_WINDOW_MS);

            if (timestamps.size() >= MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Przekroczono limit żądań. Spróbuj ponownie później.\"}");
                return false;
            }

            timestamps.add(now);
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
