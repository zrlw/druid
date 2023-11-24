//package com.alibaba.druid.spring.boot3.demo.configurer;
//
//import java.util.Arrays;
//import java.util.List;
//
//import org.springframework.boot.autoconfigure.security.SecurityProperties;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
// failure: strict-origin-when-cross-origin 403 Forbidden by CsrfFilter
//@Configuration
//public class CorsConfigForHttpServlet {
//    @Bean
//    public FilterRegistrationBean<CorsFilter> corsFilterBean() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        corsConfiguration.setAllowCredentials(true);
//        List<String> list = Arrays.asList("*");
//        corsConfiguration.setAllowedHeaders(list);
//        corsConfiguration.setAllowedMethods(list);
//        corsConfiguration.setAllowedOrigins(list);
//        source.registerCorsConfiguration("/**", corsConfiguration);
//        CorsFilter corsFilter = new CorsFilter(source);
//        FilterRegistrationBean<CorsFilter> filterRegistrationBean = new FilterRegistrationBean<>(corsFilter);
//        // prior to springSecurityFilterChain which created by SecurityFilterAutoConfiguration.
//        filterRegistrationBean.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
//        return filterRegistrationBean;
//    }
//}
