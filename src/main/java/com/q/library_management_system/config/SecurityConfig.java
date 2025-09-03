package com.q.library_management_system.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity // 启用Web安全配置（建议添加，明确标识安全配置类）
public class SecurityConfig {

    // 定义PasswordEncoder Bean，供依赖注入使用
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 配置安全过滤链（核心）
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭CSRF（前后端分离项目通常不需要）
                .csrf(csrf -> csrf.disable())

                // 2. 配置跨域（允许前端指定域名访问）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 配置会话管理（前后端分离通常用JWT，设置为无状态）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. 配置URL访问权限
                .authorizeHttpRequests(auth -> auth
                        // 放行登录、注册接口
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        // 放行Swagger所有相关路径
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs/swagger-config", // Swagger 配置接口（关键漏配项）
                                "/api-docs/**"
                        ).permitAll()
                        // 管理员接口需要ADMIN角色
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 其他所有接口需要认证
                        .anyRequest().authenticated()
                )

                // 5. 配置表单登录（适配前后端分离）
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login") // 登录接口地址
                        .successHandler((req, res, auth) -> {
                            // 登录成功：返回JSON格式响应
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":200,\"message\":\"登录成功\",\"data\":\"" + auth.getName() + "\"}");
                        })
                        .failureHandler((req, res, e) -> {
                            // 登录失败：返回JSON格式错误信息
                            res.setContentType("application/json;charset=UTF-8");
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401状态码
                            res.getWriter().write("{\"code\":401,\"message\":\"登录失败：" + e.getMessage() + "\"}");
                        })
                )

                // 6. 配置登出功能
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout") // 登出接口地址
                        .logoutSuccessHandler((req, res, auth) -> {
                            // 登出成功：返回JSON响应
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":200,\"message\":\"登出成功\"}");
                        })
                        .invalidateHttpSession(true) // 清除会话
                        .deleteCookies("JSESSIONID") // 删除会话Cookie
                );

        return http.build();
    }

    // 跨域配置（根据实际前端域名修改）
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许前端域名访问（生产环境需指定具体域名，如 "http://localhost:8081"）
        configuration.setAllowedOrigins(Arrays.asList("*"));
        // 允许的请求方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        // 允许前端获取响应头中的Authorization等信息
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        // 允许携带Cookie
        configuration.setAllowCredentials(true);
        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用跨域配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
