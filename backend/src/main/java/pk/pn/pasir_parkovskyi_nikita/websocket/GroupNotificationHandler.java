package pk.pn.pasir_parkovskyi_nikita.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.pn.pasir_parkovskyi_nikita.security.JwtUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupNotificationHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GroupNotificationHandler.class);

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public GroupNotificationHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String query = uri.getQuery();
        String token = null;
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String email = jwtUtil.extractUsername(token);
        if (email == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessions.put(email, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.values().remove(session);
    }

    public void sendNotification(String targetEmail, Map<String, Object> notificationPayload) {
        WebSocketSession session = sessions.get(targetEmail);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(notificationPayload);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("Error sending WebSocket notification: {}", e.getMessage());
            }
        }
    }
}
