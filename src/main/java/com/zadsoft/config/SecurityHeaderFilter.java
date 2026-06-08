package com.zadsoft.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // Ochrona przed Clickjackingiem
            httpResponse.setHeader("X-Frame-Options", "DENY");
            
            // Ochrona przed sniffowaniem typów Content-Type
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            
            // Aktywacja filtra XSS w starszych przeglądarkach
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Content Security Policy (CSP)
            httpResponse.setHeader("Content-Security-Policy", 
                    "default-src 'self'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                    "script-src 'self' 'unsafe-inline'; " + // Added 'unsafe-inline' for inline onclick event handlers in HTML
                    "connect-src 'self'");
        }
        chain.doFilter(request, response);
    }
}
