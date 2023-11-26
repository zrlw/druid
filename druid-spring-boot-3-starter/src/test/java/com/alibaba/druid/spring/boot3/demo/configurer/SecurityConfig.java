package com.alibaba.druid.spring.boot3.demo.configurer;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {
    private static final String COOKIE_CSRF_TOKEN_KEY = "CSRF-TOKEN";
    private static final String HTTP_REQUEST_HEADER_CSRF_TOKEN_KEY = "X-CSRF-TOKEN";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                //.csrf(csrf -> csrf.disable()) // disable CsrfFilter
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()))
                .addFilterBefore(wrapReqWithCsrfHeaderFilter(), CsrfFilter.class)
                .addFilterAfter(setRespCsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/error")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/*.html")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/*.ico")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/**.html")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/**.css")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/**.js")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/druid/**")).permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    private Filter wrapReqWithCsrfHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                    HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                Cookie cookie = WebUtils.getCookie(request, COOKIE_CSRF_TOKEN_KEY);
                if (cookie == null) {
                    filterChain.doFilter(request, response);
                    return;
                }
                CustomHttpServletRequest customRequest = new CustomHttpServletRequest(request);
                customRequest.addHeader(HTTP_REQUEST_HEADER_CSRF_TOKEN_KEY, cookie.getValue());
                filterChain.doFilter(customRequest, response);
            }
        };
    }

    private Filter setRespCsrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                    HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrf != null) {
                    String token = csrf.getToken();
                    Cookie cookie = WebUtils.getCookie(request, COOKIE_CSRF_TOKEN_KEY);
                    if (cookie == null
                            || (token != null && !token.equals(cookie.getValue()))) {
                        cookie = new Cookie(COOKIE_CSRF_TOKEN_KEY, token);
                        cookie.setPath("/");
                        response.addCookie(cookie);
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName(HTTP_REQUEST_HEADER_CSRF_TOKEN_KEY);
        return repository;
    }
}
