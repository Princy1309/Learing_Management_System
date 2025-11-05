package com.example.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;

    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // public pages and assets
                        .requestMatchers("/", "/home", "/login", "/register", "/css/**", "/js/**", "/images/**")
                        .permitAll()

                        // allow public GET to browse approved courses
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/student/courses").permitAll()

                        // allow the course page HTML to be loaded by browser (page JS will call the
                        // protected API)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/student/course/**").permitAll()

                        // auth endpoints and test endpoints
                        .requestMatchers("/api/auth/**", "/api/test/**").permitAll()

                        // file upload must be authenticated
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/files/upload").authenticated()

                        // protected APIs by role
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/instructor/**").hasRole("INSTRUCTOR")
                        // student APIs require STUDENT role (enrollment checks are done inside
                        // controller)
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        // .requestMatchers("/mycourse").hasRole("STUDENT")
                        .requestMatchers("/api/student/my-courses").hasAuthority("STUDENT")

                        // dashboards (static pages) permitted so JS can run (JS will use JWT)
                        .requestMatchers("/dashboard-instructor", "/dashboard-student", "/dashboard-admin", "/mycourse",
                                "/up")
                        .permitAll()
                        .requestMatchers("/instructor/**", "/student/**", "/admin/**").permitAll()

                        .anyRequest().authenticated())

                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
