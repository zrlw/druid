package com.alibaba.druid.spring.boot3.demo.configurer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable()) // disable CsrfFilter
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/*.html")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/**.html")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/**.css")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/**.js")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/druid/**")).permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
