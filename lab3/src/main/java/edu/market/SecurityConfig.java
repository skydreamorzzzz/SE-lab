package edu.market;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AccountRepository accounts) {
        return username -> {
            Account account = accounts.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(username));
            return User.withUsername(account.getUsername()).password(account.getPasswordHash())
                    .roles(account.getRoles().toArray(String[]::new)).build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountAccessFilter accountAccess) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/search", "/api/catalog", "/products/*", "/sellers/*", "/login", "/register", "/recover", "/ranking", "/availability/*", "/js/**", "/css/**", "/images/**", "/error")
                .permitAll().requestMatchers(org.springframework.http.HttpMethod.GET, "/media/*", "/api/products/*/comments").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/**").hasRole("SELLER").anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"));
        http.addFilterAfter(accountAccess, org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        return http.build();
    }
}
