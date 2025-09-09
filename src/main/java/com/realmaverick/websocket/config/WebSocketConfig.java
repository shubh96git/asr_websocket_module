package com.realmaverick.websocket.config;

import com.realmaverick.websocket.external_api.AsrWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AsrWebSocketHandler asrWebSocketHandler;

    public WebSocketConfig(AsrWebSocketHandler asrWebSocketHandler ) {
        this.asrWebSocketHandler = asrWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(asrWebSocketHandler, "/api/asr-stream")
                .setAllowedOrigins("*"); // important for browser testing
    }
}