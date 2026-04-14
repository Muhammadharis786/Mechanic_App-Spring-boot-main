package com.haris.MechanicApp.Config;

import com.haris.MechanicApp.Service.CustomUserDetailsService_Final;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Base64;

@Configuration
@EnableWebSocketMessageBroker // Iska matlab hai: "WebSocket Messaging chalu kar do"
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Autowired
    private CustomUserDetailsService_Final userDetailsService;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. 'topic' wo jagah hai jahan server messages phenkega (Broadcasting)s
        //yha data moojod hoga phir yha say /topic say data server phenkaiga client ko unko phenkaiga
        //jinho nay  /topic ko subscirbe kya hoga
        config.enableSimpleBroker("/topic");

        // 2. 'app' wo prefix hai jab client server ko kuch bhejega
        //ye wo jgha hay jha data aiga client say
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3. Ye wo "Address" hai jahan se browser server se connect hoga
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns("*")  // Taake kisi bhi browser se connection ho sake
                .withSockJS(); // Agar browser purana ho to ye madad karta hai
    }

}
