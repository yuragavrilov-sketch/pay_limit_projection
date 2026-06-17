package ru.copperside.paylimits.projection.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
public class InternalAdminKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Admin-Key";
    private final InternalAdminKeyProperties properties;
    private final Clock clock;

    public InternalAdminKeyFilter(InternalAdminKeyProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (properties.required()) {
            String provided = request.getHeader(HEADER);
            if (provided == null || !provided.equals(properties.apiKey())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.getWriter().write(
                        "{\"error\":{\"type\":\"https://contracts.newpay/errors/unauthorized\",\"title\":\"Unauthorized\",\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid internal admin key\",\"details\":null,\"traceId\":\"00000000-0000-0000-0000-000000000000\"},\"timestamp\":\"" + Instant.now(clock) + "\"}"
                );
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
