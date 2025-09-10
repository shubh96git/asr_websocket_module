package com.realmaverick.websocket.external_api;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class AsrWebSocketClient {

    private static final Map<String, AsrWebSocketClient> clientMap = new ConcurrentHashMap<>();
    private WebSocketSession rivaSession;
    private Consumer<String> transcriptListener;
    private String language = "en-US";
    private static final String ASR_URL = "ws://10.208.8.109:8000/ws";

    private AsrWebSocketClient(String sessionId) {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            rivaSession = client.doHandshake(new WebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    System.out.println("[Asr WebSocketClient] Connected to ASR WS");
                    sendConfig();
                }

                @Override
                public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                    if (message instanceof TextMessage) {
                        String transcript = ((TextMessage) message).getPayload();
                        if (transcriptListener != null) transcriptListener.accept(transcript);
                    }
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    exception.printStackTrace();
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                    System.out.println("[RivaWebSocketClient] Riva connection closed: " + closeStatus);
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }
            }, ASR_URL).get();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AsrWebSocketClient getOrCreate(String sessionId) {
        return clientMap.computeIfAbsent(sessionId, AsrWebSocketClient::new);
    }

    public void setLanguage(String language) {
        this.language = language;
        System.out.println("[WebSocketClient] Switching Riva language to: " + language);
        sendConfig();
    }

    private void sendConfig() {
        if (rivaSession != null && rivaSession.isOpen()) {
            try {
                String configJson = String.format(
                        "{\"event\":\"lang\",\"code\":\"%s\"}",
                        language
                );
                log.info("configJson: {}",configJson);
                rivaSession.sendMessage(new TextMessage(configJson));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            log.error("rivaSession is null or : {}",rivaSession);
        }
    }

    public void sendAudio(byte[] pcm16) throws IOException {
        if (rivaSession != null && rivaSession.isOpen()) {
            rivaSession.sendMessage(new BinaryMessage(pcm16));
        }
    }

    public void setTranscriptListener(Consumer<String> listener) {
        this.transcriptListener = listener;
    }

    public void close() {
        try {
            if (rivaSession != null && rivaSession.isOpen()) rivaSession.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientMap.values().remove(this);
    }
}

