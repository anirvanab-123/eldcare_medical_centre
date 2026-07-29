package com.neuedu.eldercare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.eldercare.common.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwt;
    private final ObjectMapper mapper;

    public AuthInterceptor(JwtUtil jwt, ObjectMapper mapper) {
        this.jwt = jwt;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest req,
            HttpServletResponse res,
            Object handler) throws Exception {

        String h = req.getHeader("Authorization");
        try {
            if (h == null || !h.startsWith("Bearer ")) {
                throw new IllegalArgumentException();
            }
            Claims c = jwt.parse(h.substring(7));
            req.setAttribute("userId", Long.valueOf(c.getSubject()));
            req.setAttribute("role", c.get("role", String.class));
            return true;
        } catch (Exception e) {
            res.setStatus(401);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(
                    mapper.writeValueAsString(
                            ApiResponse.fail("登录已失效，请重新登录")
                    )
            );
            return false;
        }
    }
}
