package com.alibaba.druid.spring.boot3.demo.configurer;
//package com.alibaba.druid.spring.boot3.demo.configurer;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
// failure: strict-origin-when-cross-origin 403 Forbidden by springSecurityFilterChain created at SecurityFilterAutoConfiguration.
//@Configuration
//public class CorsMvcConfig implements WebMvcConfigurer {
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 接口
//                .allowCredentials(true) // 是否发送 Cookie
//                .allowedOriginPatterns("*") // 支持域
//                .allowedMethods("*") // 支持方法 "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"
//                .allowedHeaders("*")
//                .exposedHeaders("*")
//                .maxAge(3600); // 允许跨域时间
//    }
//}
