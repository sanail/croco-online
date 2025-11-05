package com.crocodile.service;

import com.crocodile.util.SessionIdGenerator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@Slf4j
public class SessionService {

    @Value("${game.session.cookie-name}")
    private String cookieName;

    @Value("${game.session.cookie-max-age}")
    private int cookieMaxAge;

    public String getOrCreateSessionId(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> existingSessionId = getSessionIdFromRequest(request);
        
        if (existingSessionId.isPresent()) {
            log.debug("Found existing session: {}", existingSessionId.get());
            return existingSessionId.get();
        }
        
        String newSessionId = SessionIdGenerator.generate();
        log.info("Created new session: {}", newSessionId);
        
        Cookie cookie = new Cookie(cookieName, newSessionId);
        cookie.setMaxAge(cookieMaxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        
        return newSessionId;
    }

    public Optional<String> getSessionIdFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        
        return Arrays.stream(request.getCookies())
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
}

