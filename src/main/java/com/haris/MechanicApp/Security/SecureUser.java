package com.haris.MechanicApp.Security;


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecureUser {
    @Autowired
    UserDetailsService userDetailsService;



    @Bean
public SecurityFilterChain  securityFilterChain (HttpSecurity http){


        http
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .logout(logout->logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((request, response,
                                               authentication) -> {

                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write("Logged out Successfully");

                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")

                )
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.addAllowedOriginPattern("*"); // allows all origins safely (Spring 5.3+)
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                // 1. CSRF ko bilkul disable kar dein (Testing ke liye )
                .csrf(csrf -> csrf.disable())
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth->auth
                        .requestMatchers("/api/user/register").permitAll()
                        .requestMatchers("/api/user/login").permitAll()
                        .requestMatchers("/api/verify/user/token").permitAll()
                        .requestMatchers("/api/user/forgot").permitAll()
                        .requestMatchers("/uploads/**" ).permitAll()
                        .requestMatchers("/api/user/forget/verify").permitAll()
                        .requestMatchers("/api/mechanic/forget/newPassword").permitAll()
                        .requestMatchers( "/api/mechanic/forget/verifytoken").permitAll()
                        .requestMatchers("/api/mechanic/forget").permitAll()
                        .requestMatchers("/api/kyc/verify/**").permitAll()
                        .requestMatchers("/create").permitAll()






                        .requestMatchers("/api/user/newPassword").permitAll()
                        .requestMatchers("/api/mechanic/register").permitAll()
                        .requestMatchers("/api/mechanic/checknumber").permitAll()
                        .requestMatchers("/api/mechanic/login").permitAll()
                        .requestMatchers("/api/mechanic/allmechanic").permitAll()
                        .requestMatchers("/ws-notifications").permitAll()
                        .requestMatchers("/api/login").permitAll()
                                .requestMatchers("/api/mechanic/registerwithotp").permitAll()
                                .requestMatchers("/api/mechanic/register/verify").permitAll()
                        .requestMatchers("/api/payment/callback").permitAll()
                                .requestMatchers("/api/admin/**").permitAll()

                        













                        .requestMatchers("/api/user/allusers").permitAll()



                        .anyRequest().authenticated()
                )

                .httpBasic(Customizer.withDefaults());

//                .oauth2Login(Customizer.withDefaults());
//                .formLogin(Customizer.withDefaults());
             return http.build();
 }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Is se Spring Security is raste ko bilkul ignore kar degi
        return (web) -> web.ignoring().requestMatchers("/ws-notifications/**");
    }

    @Bean
    public AuthenticationProvider authenticationProvider (){

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        return provider;

    }

    @Bean
    public AuthenticationManager authenticationManager (AuthenticationConfiguration authconfig)
            throws Exception {
        return   authconfig.getAuthenticationManager();

    }


}
