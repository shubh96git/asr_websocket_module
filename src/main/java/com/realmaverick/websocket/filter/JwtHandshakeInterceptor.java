package com.realmaverick.websocket.filter;

import com.realmaverick.websocket.security.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenService jwtTokenService;

    public JwtHandshakeInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        String token = null;

        // Try getting token from Authorization header first
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if(authHeaders != null && !authHeaders.isEmpty() && authHeaders.get(0).startsWith("Bearer ")) {
            token = authHeaders.get(0).substring(7);
        } else if (request.getURI().getQuery() != null) {
            // fallback: read token from query param
            String query = request.getURI().getQuery();
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.split("=")[1];
                    break;
                }
            }
        }

        if (token != null && jwtTokenService.validateToken(token)) {
            String username = jwtTokenService.extractUsername(token);
            attributes.put("username", username);
            return true;
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }


    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception ex) {
        // no-op
    }
}
