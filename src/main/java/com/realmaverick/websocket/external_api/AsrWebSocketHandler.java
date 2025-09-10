package com.realmaverick.websocket.external_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realmaverick.websocket.ratelimiter.RateLimiterService;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@Component
public class AsrWebSocketHandler extends AbstractWebSocketHandler {

    private final RateLimiterService rateLimiterService;

    // Config
    private static final long IDLE_TIMEOUT_SECONDS = 60;   // auto-stop if no audio
    private static final long MAX_SESSION_MINUTES = 10;    // max session duration
    private static final int MAX_CONCURRENT_SESSIONS_PER_USER = 1;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    // Track user sessions
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public AsrWebSocketHandler(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }

        // Limit concurrent sessions per user
        userSessions.putIfAbsent(username, new CopyOnWriteArrayList<>());
        List<WebSocketSession> sessions = userSessions.get(username);
        if (sessions.size() >= MAX_CONCURRENT_SESSIONS_PER_USER) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Too many concurrent sessions"));
            return;
        }
        sessions.add(session);

        // Initialize rate limiter
        Bucket bucket = rateLimiterService.resolveBucket(username);
        session.getAttributes().put("bucket", bucket);
        session.getAttributes().put("startTime", System.currentTimeMillis());

        // Idle timeout
        ScheduledFuture<?> idleFuture = scheduler.schedule(
                () -> closeSession(session, "Idle timeout"),
                IDLE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );
        session.getAttributes().put("idleFuture", idleFuture);

        System.out.println("WebSocket connected: " + username);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Bucket bucket = (Bucket) session.getAttributes().get("bucket");

        if (!bucket.tryConsume(1)) {
            session.sendMessage(new TextMessage("{\"error\":\"Rate limit exceeded\"}"));
            closeSession(session, "Rate limit exceeded");
            return;
        }

        resetIdleTimer(session);

        long startTime = (long) session.getAttributes().get("startTime");
        if (System.currentTimeMillis() - startTime > TimeUnit.MINUTES.toMillis(MAX_SESSION_MINUTES)) {
            closeSession(session, "Max session duration reached");
            return;
        }

        AsrWebSocketClient rivaClient = AsrWebSocketClient.getOrCreate(session.getId());
        rivaClient.sendAudio(message.getPayload().array());
        rivaClient.setTranscriptListener(transcript -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(transcript));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (payload.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(payload);
                String event = node.get("event").asText();

                switch (event) {
                    case "lang":
                        String langCode = node.get("code").asText();
                        session.getAttributes().put("language", langCode);

                        AsrWebSocketClient rivaClient = AsrWebSocketClient.getOrCreate(session.getId());
                        rivaClient.setLanguage(langCode);

                        System.out.println("[AsrWebSocketHandler] User '"
                                + session.getAttributes().get("username")
                                + "' switched language to: " + langCode);

                        session.sendMessage(new TextMessage("Language set to: " + langCode));
                        break;

                    case "stop":
                        closeSession(session, "Stopped by user");
                        break;

                    default:
                        session.sendMessage(new TextMessage("Unknown event: " + event));
                }

            } catch (Exception e) {
                e.printStackTrace();
                session.sendMessage(new TextMessage("{\"error\":\"Invalid control message\"}"));
            }
        } else {
            session.sendMessage(new TextMessage("Server ACK: " + payload));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ScheduledFuture<?> idleFuture = (ScheduledFuture<?>) session.getAttributes().get("idleFuture");
        if (idleFuture != null) idleFuture.cancel(false);

        String username = (String) session.getAttributes().get("username");
        if (username != null && userSessions.containsKey(username)) {
            userSessions.get(username).remove(session);
        }

        AsrWebSocketClient client = AsrWebSocketClient.getOrCreate(session.getId());
        client.close();

        System.out.println("WebSocket disconnected: " + username + ", reason: " + status.getReason());
    }

    // ---------------- Helper Methods ----------------

    private void resetIdleTimer(WebSocketSession session) {
        try {
            ScheduledFuture<?> idleFuture = (ScheduledFuture<?>) session.getAttributes().get("idleFuture");
            if (idleFuture != null) idleFuture.cancel(false);

            ScheduledFuture<?> newIdleFuture = scheduler.schedule(
                    () -> closeSession(session, "Idle timeout"),
                    IDLE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            session.getAttributes().put("idleFuture", newIdleFuture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeSession(WebSocketSession session, String reason) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.NORMAL.withReason(reason));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
