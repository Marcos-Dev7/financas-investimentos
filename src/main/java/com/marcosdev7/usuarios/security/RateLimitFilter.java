package com.marcosdev7.usuarios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L; // Janela de 1 minuto
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 15;
    private static final int MAX_API_REQUESTS_PER_MINUTE = 120;
    private static final int MAX_TRACKED_BUCKETS = 5_000;

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/")
                || path.equals("/index.html")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".svg")
                || path.endsWith(".ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extrairIpCliente(request);
        String path = request.getRequestURI();
        boolean isAuthWrite = path.startsWith("/auth/") && "POST".equalsIgnoreCase(request.getMethod());

        String bucketKey = clientIp + (isAuthWrite ? ":AUTH" : ":API");
        int limit = isAuthWrite ? MAX_AUTH_REQUESTS_PER_MINUTE : MAX_API_REQUESTS_PER_MINUTE;

        long now = System.currentTimeMillis();
        if (buckets.size() > MAX_TRACKED_BUCKETS) {
            buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS);
        }

        RateBucket bucket = buckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                return new RateBucket(now, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        int currentCount = bucket.counter.get();
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));

        if (currentCount > limit) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensagem\":\"Muitas requisições em pouco tempo. Aguarde 1 minuto e tente novamente.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extrairIpCliente(HttpServletRequest request) {
        // Confia exclusivamente no IP resolvido pelo container/proxy reverso confiável
        // (server.forward-headers-strategy=framework), impedindo spoofing via cabeçalhos X-Forwarded-For / X-Real-IP.
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private record RateBucket(long windowStart, AtomicInteger counter) {}
}
