package com.neuedu.eldercare.controller;

import com.neuedu.eldercare.common.ApiResponse;
import com.neuedu.eldercare.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/management")
public class ManagementController {

    private final JdbcTemplate jdbc;

    public ManagementController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record UserForm(
            @NotBlank String username,
            @NotBlank String realName,
            @NotBlank String phone,
            @NotBlank String role,
            Integer status,
            String password) {
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users(
            @RequestParam(defaultValue = "") String name,
            HttpServletRequest r) {
        admin(r);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT id, username, real_name, phone, role, status, created_at " +
                        "FROM sys_user WHERE deleted = 0 " +
                        "AND (real_name LIKE ? OR username LIKE ?) " +
                        "ORDER BY id",
                "%" + name + "%", "%" + name + "%"
        ));
    }

    @PostMapping("/users")
    public ApiResponse<Void> addUser(
            @RequestBody @Valid UserForm f,
            HttpServletRequest r) {
        admin(r);
        if (!List.of("ADMIN", "HEALTH_MANAGER").contains(f.role())) {
            throw new BusinessException("角色不正确");
        }
        if (f.password() == null || f.password().length() < 5) {
            throw new BusinessException("密码长度不能少于5位");
        }
        jdbc.update(
                "INSERT INTO sys_user(username, password, real_name, phone, role, status) " +
                        "VALUES(?, ?, ?, ?, ?, ?)",
                f.username(), f.password(), f.realName(),
                f.phone(), f.role(), f.status() == null ? 1 : f.status()
        );
        return ApiResponse.ok(null);
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Void> editUser(
            @PathVariable Long id,
            @RequestBody @Valid UserForm f,
            HttpServletRequest r) {
        admin(r);
        jdbc.update(
                "UPDATE sys_user SET username = ?, real_name = ?, phone = ?, " +
                        "role = ?, status = ? WHERE id = ?",
                f.username(), f.realName(), f.phone(),
                f.role(), f.status() == null ? 1 : f.status(), id
        );
        return ApiResponse.ok(null);
    }

    @PutMapping("/users/{id}/reset-password")
    public ApiResponse<Void> reset(@PathVariable Long id, HttpServletRequest r) {
        admin(r);
        String phone = jdbc.queryForObject(
                "SELECT phone FROM sys_user WHERE id = ?", String.class, id
        );
        String password = phone.length() >= 6
                ? phone.substring(phone.length() - 6) : phone;
        jdbc.update("UPDATE sys_user SET password = ? WHERE id = ?", password, id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest r) {
        admin(r);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer " +
                        "WHERE health_manager_id = ? AND deleted = 0 " +
                        "AND status <> 'CHECKED_OUT'",
                Integer.class, id
        );
        if (count != null && count > 0) {
            throw new BusinessException("该健康管家仍有在住服务对象，不能删除");
        }
        jdbc.update("UPDATE sys_user SET deleted = 1 WHERE id = ?", id);
        return ApiResponse.ok(null);
    }

    // ==================== 健康管家管理 ====================

    @GetMapping("/managers")
    public ApiResponse<List<Map<String, Object>>> managers(
            @RequestParam(defaultValue = "") String name,
            HttpServletRequest r) {
        admin(r);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT u.id, u.username, u.real_name, u.phone, u.status, " +
                        "COUNT(c.id) customer_count " +
                        "FROM sys_user u " +
                        "LEFT JOIN customer c ON c.health_manager_id = u.id " +
                        "AND c.deleted = 0 AND c.status <> 'CHECKED_OUT' " +
                        "WHERE u.deleted = 0 AND u.role = 'HEALTH_MANAGER' " +
                        "AND u.real_name LIKE ? " +
                        "GROUP BY u.id ORDER BY u.id",
                "%" + name + "%"
        ));
    }

    @GetMapping("/managers/{id}/customers")
    public ApiResponse<List<Map<String, Object>>> managerCustomers(
            @PathVariable Long id,
            HttpServletRequest r) {
        admin(r);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT id, name, gender, family_phone, status, " +
                        "CASE status " +
                        "WHEN 'IN_HOME' THEN '正常入住' " +
                        "WHEN 'OUTING' THEN '外出中' " +
                        "ELSE '状态未知' END status_label " +
                        "FROM customer " +
                        "WHERE health_manager_id = ? AND deleted = 0 " +
                        "AND status <> 'CHECKED_OUT'",
                id
        ));
    }

    // ==================== 服务对象分配 ====================

    @GetMapping("/unassigned-customers")
    public ApiResponse<List<Map<String, Object>>> unassigned(
            @RequestParam(defaultValue = "") String name,
            HttpServletRequest r) {
        admin(r);
        return ApiResponse.ok(jdbc.queryForList(
                "SELECT id, name, gender, family_phone, status, " +
                        "CASE status " +
                        "WHEN 'IN_HOME' THEN '正常入住' " +
                        "WHEN 'OUTING' THEN '外出中' " +
                        "ELSE '状态未知' END status_label " +
                        "FROM customer " +
                        "WHERE health_manager_id IS NULL AND deleted = 0 " +
                        "AND status <> 'CHECKED_OUT' " +
                        "AND name LIKE ? ORDER BY id",
                "%" + name + "%"
        ));
    }

    @PutMapping("/customers/{customerId}/manager")
    public ApiResponse<Void> assign(
            @PathVariable Long customerId,
            @RequestBody Map<String, Long> f,
            HttpServletRequest r) {
        admin(r);
        Integer active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer " +
                        "WHERE id = ? AND deleted = 0 AND status <> 'CHECKED_OUT'",
                Integer.class, customerId
        );
        if (active == null || active == 0) {
            throw new BusinessException("已退住客户不能分配健康管家");
        }
        jdbc.update(
                "UPDATE customer SET health_manager_id = ? WHERE id = ?",
                f.get("managerId"), customerId
        );
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/customers/{customerId}/manager")
    public ApiResponse<Void> unassign(
            @PathVariable Long customerId,
            HttpServletRequest r) {
        admin(r);
        jdbc.update(
                "UPDATE customer SET health_manager_id = NULL WHERE id = ?",
                customerId
        );
        return ApiResponse.ok(null);
    }

    // ==================== 健康管家我的客户 ====================

    @GetMapping("/my-customers")
    public ApiResponse<List<Map<String, Object>>> myCustomers(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String elderType,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String typeExpression =
                "CASE WHEN NOT EXISTS(SELECT 1 FROM customer_service cs " +
                        "WHERE cs.customer_id = c.id AND cs.deleted = 0) " +
                        "THEN 'SELF_CARE' ELSE 'NURSING' END";

        String sql = "SELECT c.*, rm.room_no, b.bed_no, l.name care_level_name, " +
                typeExpression + " elder_type, " +
                "CASE WHEN " + typeExpression + " = 'SELF_CARE' " +
                "THEN '自理老人' ELSE '护理老人' END elder_type_label, " +
                "CASE c.status " +
                "WHEN 'IN_HOME' THEN '正常入住' " +
                "WHEN 'OUTING' THEN '外出中' " +
                "ELSE '状态未知' END status_label " +
                "FROM customer c " +
                "LEFT JOIN room rm ON rm.id = c.room_id " +
                "LEFT JOIN bed b ON b.id = c.bed_id " +
                "LEFT JOIN care_level l ON l.id = c.care_level_id " +
                "WHERE c.deleted = 0 AND c.status <> 'CHECKED_OUT' " +
                "AND c.health_manager_id = ? AND c.name LIKE ?";

        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.add("%" + name + "%");

        if (!elderType.isBlank()) {
            sql += " AND " + typeExpression + " = ?";
            args.add(elderType);
        }
        sql += " ORDER BY c.id";

        return ApiResponse.ok(jdbc.queryForList(sql, args.toArray()));
    }

    // ==================== 私有辅助方法 ====================

    private void admin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException("仅管理员可以执行该操作");
        }
    }
}
