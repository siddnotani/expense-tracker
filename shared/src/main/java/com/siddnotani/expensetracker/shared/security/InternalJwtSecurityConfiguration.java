package com.siddnotani.expensetracker.shared.security;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Import(InternalJwt.class)
public class InternalJwtSecurityConfiguration {
    @Bean
    InternalJwtAuthenticationFilter internalJwtAuthenticationFilter(InternalJwt internalJwt) {
        return new InternalJwtAuthenticationFilter(internalJwt);
    }

    @Bean
    SecurityFilterChain internalJwtSecurityFilterChain(
            HttpSecurity http, InternalJwtAuthenticationFilter filter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
