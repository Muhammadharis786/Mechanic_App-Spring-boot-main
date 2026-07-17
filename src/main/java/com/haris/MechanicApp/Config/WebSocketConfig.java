package com.haris.MechanicApp.Config;

import com.haris.MechanicApp.Service.CustomUserDetailsService_Final;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
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
import java.util.concurrent.Executors;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@Configuration
@EnableWebSocketMessageBroker // Iska matlab hai: "WebSocket Messaging chalu kar do"
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Autowired
    private CustomUserDetailsService_Final userDetailsService;

//    @Autowired
//    private WebSocketSessionRegistry sessionRegistry;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. 'topic' wo jagah hai jahan server messages phenkega (Broadcasting)s
        //yha data moojod hoga phir yha say /topic say data server phenkaiga client ko unko phenkaiga
        //jinho nay  /topic ko subscirbe kya hoga
        // Heartbeat: {server->client every 10s, client->server every 10s}.
        // Requires a real TaskScheduler — bina iske heartbeat values silently
        // ignore ho jate hain aur zombie sessions wapis miss hone lagti hain.
        config.enableSimpleBroker("/topic") ;


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

//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        // CONNECT frame ke waqt Flutter client "mechanicId" header bhejta hai
//        // (websocket_service.dart -> connect()). Yahan usay pakad kar
//        // sessionId ke against store kar lete hain, taake disconnect hone
//        // pe (crash/force-kill/net gone) hume pata chale konsa mechanic tha.
//        registration.interceptors(new ChannelInterceptor() {
//            @Override
//            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
//                StompHeaderAccessor accessor =
//                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//
//                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
//                    String mechanicIdHeader = accessor.getFirstNativeHeader("mechanicId");
//                    String sessionId = accessor.getSessionId();
//
//                    if (mechanicIdHeader != null && sessionId != null) {
//                        try {
//                            Long mechanicId = Long.parseLong(mechanicIdHeader);
//                            sessionRegistry.register(sessionId, mechanicId);
//                        } catch (NumberFormatException ignored) {
//                            // Header malformed tha, skip — presence sirf
//                            // best-effort backup hai, request ko fail
//                            // karne ki zaroorat nahi.
//                        }
//                    }
//                }
//                return message;
//            }
//        });
//    }

}