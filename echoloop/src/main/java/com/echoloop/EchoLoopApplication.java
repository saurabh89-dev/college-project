package com.echoloop;

import jakarta.servlet.http.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.*;
import org.springframework.web.socket.server.support.*;
import java.security.Principal;
import java.sql.*;
import java.time.*;
import java.util.*;

@SpringBootApplication
public class EchoLoopApplication {
    public static void main(String[] a) {
        SpringApplication.run(EchoLoopApplication.class, a);
    }

    @Bean
    BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}

@Configuration
@EnableWebSocketMessageBroker
class Ws implements WebSocketMessageBrokerConfigurer {
    public void configureMessageBroker(MessageBrokerRegistry r) {
        // /user is the client-facing destination prefix. Spring resolves it to
        // a session-specific /queue destination before the broker delivers it.
        r.enableSimpleBroker("/queue");
    }

    public void registerStompEndpoints(StompEndpointRegistry r) {
        r.addEndpoint("/ws").addInterceptors(new I()).setHandshakeHandler(new H()).setAllowedOriginPatterns("*");
    }

    static class I implements HandshakeInterceptor {
        public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest r,
                org.springframework.http.server.ServerHttpResponse s, WebSocketHandler h, Map<String, Object> a) {
            if (r instanceof org.springframework.http.server.ServletServerHttpRequest q) {
                HttpSession x = q.getServletRequest().getSession(false);
                if (x != null && x.getAttribute("username") != null) {
                    a.put("username", x.getAttribute("username"));
                    return true;
                }
            }
            return false;
        }

        public void afterHandshake(org.springframework.http.server.ServerHttpRequest a,
                org.springframework.http.server.ServerHttpResponse b, WebSocketHandler c, Exception e) {
        }
    }

    static class H extends DefaultHandshakeHandler {
        protected Principal determineUser(org.springframework.http.server.ServerHttpRequest r, WebSocketHandler h,
                Map<String, Object> a) {
            return () -> (String) a.get("username");
        }
    }
}

@RestController
@RequestMapping("/api")
class Api {
    final JdbcTemplate db;
    final BCryptPasswordEncoder enc;
    final SimpMessagingTemplate ws;
    final SimpUserRegistry connectedUsers;

    Api(JdbcTemplate d, BCryptPasswordEncoder e, SimpMessagingTemplate w, SimpUserRegistry u) {
        db = d;
        enc = e;
        ws = w;
        connectedUsers = u;
    }

    record Auth(String login, String password) {
    }

    record Reg(String username, String email, String password, String passwordConfirmation) {
    }

    record C(int userId) {
    }

    record Text(String text) {
    }

    record Bulk(List<Integer> messageIds) {
    }

    record Msg(int id, int conversationId, int senderId, String sender, String text, Instant sentAt, Instant editedAt) {
    }

    record E(String type, Object payload) {
    }

    int me(HttpServletRequest r) {
        Object x = r.getSession(false) == null ? null : r.getSession(false).getAttribute("id");
        if (x == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Log in first.");
        return (int) x;
    }

    Map<String, Object> one(String q, Object... p) {
        var z = db.queryForList(q, p);
        if (z.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found.");
        return z.get(0);
    }

    void text(String x) {
        if (x == null || x.trim().isEmpty() || x.trim().length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must contain 1–2000 characters.");
    }

    void member(int c, int u) {
        if (db.queryForObject("select count(*) from conversations where id=? and(user_one_id=? or user_two_id=?)",
                Integer.class, c, u, u) == 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversation access denied.");
    }

    void event(int c, String t, Object p) {
        for (String u : db.queryForList(
                "select username from users where id in(select user_one_id from conversations where id=? union select user_two_id from conversations where id=?)",
                String.class, c, c))
            ws.convertAndSendToUser(u, "/queue/events", new E(t, p));
    }

    Msg row(Map<String, Object> x) {
        return new Msg(((Number) x.get("id")).intValue(), ((Number) x.get("conversation_id")).intValue(),
                ((Number) x.get("sender_id")).intValue(), (String) x.get("sender"), (String) x.get("message_text"),
                ((Timestamp) x.get("sent_at")).toInstant(),
                x.get("edited_at") == null ? null : ((Timestamp) x.get("edited_at")).toInstant());
    }

    @PostMapping("/auth/register")
    Object register(@RequestBody Reg x, HttpServletRequest r) {
        if (x.username() == null || !x.username().matches("[A-Za-z0-9_]{3,30}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username must be 3–30 letters, numbers, or underscores.");
        if (x.email() == null || !x.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email.");
        if (x.password() == null || x.password().length() < 8
                || !Objects.equals(x.password(), x.passwordConfirmation()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Passwords must match and be at least 8 characters.");
        try {
            db.update("insert into users(username,email,password_hash)values(?,?,?)", x.username(),
                    x.email().toLowerCase(), enc.encode(x.password()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already registered.");
        }
        return login(new Auth(x.username(), x.password()), r);
    }

    @PostMapping("/auth/login")
    Object login(@RequestBody Auth x, HttpServletRequest r) {
        var u = one("select id,username,email,password_hash from users where username=? or email=?", x.login(),
                x.login().toLowerCase());
        String p = (String) u.get("password_hash");
        if (!(p.startsWith("$2") ? enc.matches(x.password(), p) : Objects.equals(x.password(), p)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login.");
        if (!p.startsWith("$2"))
            db.update("update users set password_hash=? where id=?", enc.encode(x.password()), u.get("id"));
        var s = r.getSession();
        s.setAttribute("id", ((Number) u.get("id")).intValue());
        s.setAttribute("username", u.get("username"));
        return Map.of("id", u.get("id"), "username", u.get("username"), "email", u.get("email"));
    }

    @PostMapping("/auth/logout")
    Object logout(HttpServletRequest r) {
        if (r.getSession(false) != null)
            r.getSession(false).invalidate();
        return Map.of("ok", true);
    }

    @DeleteMapping("/auth/account")
    @org.springframework.transaction.annotation.Transactional
    Object deleteAccount(HttpServletRequest r) {
        int userId = me(r);
        // Removing a conversation lets MySQL cascade to its messages and AI analysis.
        // This keeps no orphaned private data when its participant is removed.
        db.update("delete from ai_analysis where requested_by=?", userId);
        db.update("delete from conversations where user_one_id=? or user_two_id=?", userId, userId);
        db.update("delete from users where id=?", userId);
        r.getSession(false).invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/auth/me")
    Object mine(HttpServletRequest r) {
        return one("select id,username,email from users where id=?", me(r));
    }

    @GetMapping("/users")
    Object users(HttpServletRequest r) {
        int userId = me(r);
        return db.query("""
                select u.id,u.username,c.id conversation_id,lm.message_text,lm.sent_at
                from users u
                left join conversations c on (c.user_one_id=? and c.user_two_id=u.id)
                    or (c.user_two_id=? and c.user_one_id=u.id)
                left join messages lm on lm.id=(select m.id from messages m where m.conversation_id=c.id
                    and m.deleted_at is null order by m.sent_at desc,m.id desc limit 1)
                where u.id<>?
                order by coalesce(lm.sent_at,c.created_at,u.created_at) desc,u.username
                """, (z, n) -> {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", z.getInt("id"));
            user.put("username", z.getString("username"));
            user.put("conversationId", z.getObject("conversation_id"));
            user.put("lastMessage", z.getString("message_text"));
            user.put("lastMessageAt", z.getTimestamp("sent_at") == null ? null : z.getTimestamp("sent_at").toInstant());
            user.put("online", connectedUsers.getUser(z.getString("username")) != null);
            return user;
        }, userId, userId, userId);
    }

    @PostMapping("/conversations")
    Object conversation(@RequestBody C x, HttpServletRequest r) {
        int a = me(r), b = x.userId(), l = Math.min(a, b), h = Math.max(a, b);
        if (a == b)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose another user.");
        db.update("insert ignore into conversations(user_one_id,user_two_id)values(?,?)", l, h);
        return one("select id from conversations where user_one_id=? and user_two_id=?", l, h);
    }

    @GetMapping("/conversations/{c}/messages")
    Object history(@PathVariable int c, HttpServletRequest r) {
        member(c, me(r));
        return db.query(
                "select m.*,u.username sender from messages m join users u on u.id=m.sender_id where m.conversation_id=? and m.deleted_at is null order by m.sent_at",
                (z, n) -> new Msg(z.getInt("id"), z.getInt("conversation_id"), z.getInt("sender_id"),
                        z.getString("sender"), z.getString("message_text"), z.getTimestamp("sent_at").toInstant(),
                        z.getTimestamp("edited_at") == null ? null : z.getTimestamp("edited_at").toInstant()),
                c);
    }

    @PostMapping("/conversations/{c}/messages")
    Object send(@PathVariable int c, @RequestBody Text x, HttpServletRequest r) {
        int u = me(r);
        member(c, u);
        text(x.text());
        db.update("insert into messages(conversation_id,sender_id,message_text)values(?,?,?)", c, u, x.text().trim());
        Msg m = row(one(
                "select m.*,u.username sender from messages m join users u on u.id=m.sender_id where m.id=last_insert_id()"));
        event(c, "message", m);
        return m;
    }

    @DeleteMapping("/conversations/{c}/messages")
    Object clearChat(@PathVariable int c, HttpServletRequest r) {
        // Membership above ensures this affects only the currently authorized private chat.
        member(c, me(r));
        db.update("update messages set deleted_at=current_timestamp,message_text='' where conversation_id=? and deleted_at is null", c);
        event(c, "chat-cleared", Map.of("conversationId", c));
        return Map.of("conversationId", c, "cleared", true);
    }

    @PatchMapping("/messages/{id}")
    Object edit(@PathVariable int id, @RequestBody Text x, HttpServletRequest r) {
        text(x.text());
        var m = one("select * from messages where id=? and deleted_at is null", id);
        if (((Number) m.get("sender_id")).intValue() != me(r))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can edit only your own messages.");
        db.update("update messages set message_text=?,edited_at=current_timestamp where id=?", x.text().trim(), id);
        Msg o = row(
                one("select m.*,u.username sender from messages m join users u on u.id=m.sender_id where m.id=?", id));
        event(o.conversationId(), "message-edited", o);
        return o;
    }

    @DeleteMapping("/messages/{id}")
    Object del(@PathVariable int id, HttpServletRequest r) {
        var m = one("select * from messages where id=? and deleted_at is null", id);
        if (((Number) m.get("sender_id")).intValue() != me(r))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can delete only your own messages.");
        db.update("update messages set deleted_at=current_timestamp,message_text='' where id=?", id);
        int c = ((Number) m.get("conversation_id")).intValue();
        event(c, "message-deleted", Map.of("id", id, "conversationId", c));
        return Map.of("deletedId", id);
    }

    @DeleteMapping("/messages")
    @org.springframework.transaction.annotation.Transactional
    Object delBulk(@RequestBody Bulk x, HttpServletRequest r) {
        if (x.messageIds() == null || x.messageIds().isEmpty() || x.messageIds().size() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select 1–100 messages to delete.");
        List<Integer> ids = x.messageIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty() || ids.size() != x.messageIds().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message selection is invalid.");

        String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Map<String, Object>> messages = db.queryForList(
                "select id,conversation_id,sender_id from messages where id in(" + marks + ") and deleted_at is null",
                ids.toArray());
        if (messages.size() != ids.size() || messages.stream()
                .anyMatch(m -> ((Number) m.get("sender_id")).intValue() != me(r)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can delete only your own available messages.");

        db.update("update messages set deleted_at=current_timestamp,message_text='' where id in(" + marks + ")", ids.toArray());
        messages.stream().map(m -> ((Number) m.get("conversation_id")).intValue()).distinct()
                .forEach(c -> event(c, "messages-deleted", Map.of("messageIds", ids, "conversationId", c)));
        return Map.of("deletedIds", ids);
    }
}
