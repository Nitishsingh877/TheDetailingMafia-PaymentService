package com.thederailingmafia.carwash.paymentservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class UserDetailsConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            if ("abc1222@gmail.com".equals(username)) {
                return User.withUsername("abc1222@gmail.com")
                        .password("")
                        .authorities("ROLE_WASHER")
                        .build();
            }
            throw new UsernameNotFoundException("User not found: " + username);
        };
    }
}
