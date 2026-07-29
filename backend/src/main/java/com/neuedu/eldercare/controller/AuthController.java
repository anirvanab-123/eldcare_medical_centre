package com.neuedu.eldercare.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neuedu.eldercare.common.*;
import com.neuedu.eldercare.entity.SysUser;
import com.neuedu.eldercare.mapper.SysUserMapper;
import com.neuedu.eldercare.security.JwtUtil;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserMapper users;
    private final JwtUtil jwt;

    public AuthController(SysUserMapper users, JwtUtil jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(
            @RequestBody @Validated LoginRequest x) {
        SysUser u = users.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, x.username())
                        .eq(SysUser::getStatus, 1)
        );
        if (u == null || !u.getPassword().equals(x.password())) {
            throw new BusinessException("账号或密码错误");
        }
        return ApiResponse.ok(Map.of(
                "token", jwt.create(
                        u.getId(), u.getUsername(),
                        u.getRole(), u.getRealName()
                ),
                "user", Map.of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "realName", u.getRealName(),
                        "role", u.getRole()
                )
        ));
    }
}
