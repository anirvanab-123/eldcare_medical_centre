package com.neuedu.eldercare.controller;

import com.neuedu.eldercare.common.ApiResponse;
import com.neuedu.eldercare.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final JdbcTemplate jdbc;

    public RequestController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record OutingForm(
            @NotNull Long customerId,
            @NotBlank String reason,
            @NotNull LocalDateTime outingTime,
            @NotNull LocalDateTime expectedReturnTime) {
    }

    public record CheckoutForm(
            @NotNull Long customerId,
            @NotBlank String checkoutType,
            @NotBlank String reason,
            @NotNull LocalDate checkoutDate) {
    }

    public record ApprovalForm(
            @NotBlank String result,
            String remark) {
    }

    // ==================== 外出管理 ====================

    @GetMapping("/outing")
    public ApiResponse<Map<String, Object>> outings(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest req) {

        String role = (String) req.getAttribute("role");
        Long uid = (Long) req.getAttribute("userId");
        size = limit(size);
        page = Math.max(page, 1);

        String from = " FROM outing_request o " +
                "JOIN customer c ON c.id = o.customer_id " +
                "JOIN sys_user u ON u.id = o.applicant_id " +
                "LEFT JOIN sys_user a ON a.id = o.approver_id " +
                "WHERE o.deleted = 0 AND c.name LIKE ?" +
                ("HEALTH_MANAGER".equals(role) ? " AND o.applicant_id = ?" : "");

        Object[] base = "HEALTH_MANAGER".equals(role)
                ? new Object[]{"%" + name + "%", uid}
                : new Object[]{"%" + name + "%"};

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*)" + from, Long.class, base
        );

        List<Object> args = new ArrayList<>(Arrays.asList(base));
        args.add(size);
        args.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(
                "SELECT o.*, c.name customer_name, " +
                        "u.real_name applicant_name, a.real_name approver_name" +
                        from + " ORDER BY o.id DESC LIMIT ? OFFSET ?",
                args.toArray()
        );
        return ApiResponse.ok(Map.of(
                "records", records,
                "total", total == null ? 0 : total
        ));
    }

    @PostMapping("/outing")
    public ApiResponse<Void> addOuting(
            @RequestBody @Valid OutingForm f,
            HttpServletRequest req) {
        requireManager(req);
        assertOwnCustomer(f.customerId(), req);
        jdbc.update(
                "INSERT INTO outing_request(customer_id, applicant_id, reason, " +
                        "outing_time, expected_return_time) VALUES(?, ?, ?, ?, ?)",
                f.customerId(), req.getAttribute("userId"),
                f.reason(), f.outingTime(), f.expectedReturnTime()
        );
        return ApiResponse.ok(null);
    }

    @Transactional
    @PutMapping("/outing/{id}/approve")
    public ApiResponse<Void> approveOuting(
            @PathVariable Long id,
            @RequestBody @Valid ApprovalForm f,
            HttpServletRequest req) {
        requireAdmin(req);
        requireSubmitted("outing_request", id);
        String status = approvalStatus(f.result());

        jdbc.update(
                "UPDATE outing_request SET approval_status = ?, approver_id = ?, " +
                        "approval_time = NOW(), approval_remark = ? WHERE id = ?",
                status, req.getAttribute("userId"), f.remark(), id
        );

        if ("APPROVED".equals(status)) {
            Long customerId = jdbc.queryForObject(
                    "SELECT customer_id FROM outing_request WHERE id = ?",
                    Long.class, id
            );
            jdbc.update(
                    "UPDATE customer SET status = 'OUTING' WHERE id = ?",
                    customerId
            );
            jdbc.update(
                    "UPDATE bed b JOIN customer c ON c.bed_id = b.id " +
                            "SET b.status = 'OUTING' WHERE c.id = ?",
                    customerId
            );
        }
        return ApiResponse.ok(null);
    }

    @Transactional
    @PutMapping("/outing/{id}/return")
    public ApiResponse<Void> registerReturn(
            @PathVariable Long id,
            HttpServletRequest req) {
        requireManager(req);
        Long customerId = jdbc.queryForObject(
                "SELECT customer_id FROM outing_request " +
                        "WHERE id = ? AND deleted = 0",
                Long.class, id
        );
        assertOwnCustomer(customerId, req);

        jdbc.update(
                "UPDATE outing_request SET actual_return_time = NOW() " +
                        "WHERE id = ? AND approval_status = 'APPROVED'",
                id
        );
        jdbc.update(
                "UPDATE customer SET status = 'IN_HOME' WHERE id = ?",
                customerId
        );
        jdbc.update(
                "UPDATE bed b JOIN customer c ON c.bed_id = b.id " +
                        "SET b.status = 'OCCUPIED' WHERE c.id = ?",
                customerId
        );
        return ApiResponse.ok(null);
    }

    // ==================== 退住管理 ====================

    @GetMapping("/checkout")
    public ApiResponse<Map<String, Object>> checkouts(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest req) {

        String role = (String) req.getAttribute("role");
        Long uid = (Long) req.getAttribute("userId");
        size = limit(size);
        page = Math.max(page, 1);

        String from = " FROM checkout_request o " +
                "JOIN customer c ON c.id = o.customer_id " +
                "JOIN sys_user u ON u.id = o.applicant_id " +
                "LEFT JOIN sys_user a ON a.id = o.approver_id " +
                "WHERE o.deleted = 0 AND c.name LIKE ?" +
                ("HEALTH_MANAGER".equals(role) ? " AND o.applicant_id = ?" : "");

        Object[] base = "HEALTH_MANAGER".equals(role)
                ? new Object[]{"%" + name + "%", uid}
                : new Object[]{"%" + name + "%"};

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*)" + from, Long.class, base
        );

        List<Object> args = new ArrayList<>(Arrays.asList(base));
        args.add(size);
        args.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(
                "SELECT o.*, c.name customer_name, " +
                        "u.real_name applicant_name, a.real_name approver_name" +
                        from + " ORDER BY o.id DESC LIMIT ? OFFSET ?",
                args.toArray()
        );
        return ApiResponse.ok(Map.of(
                "records", records,
                "total", total == null ? 0 : total
        ));
    }

    @PostMapping("/checkout")
    public ApiResponse<Void> addCheckout(
            @RequestBody @Valid CheckoutForm f,
            HttpServletRequest req) {
        requireManager(req);
        assertOwnCustomer(f.customerId(), req);

        if (!List.of("NORMAL", "DEATH", "KEEP_BED")
                .contains(f.checkoutType())) {
            throw new BusinessException("退住类型不正确");
        }
        jdbc.update(
                "INSERT INTO checkout_request(customer_id, applicant_id, " +
                        "checkout_type, reason, checkout_date) " +
                        "VALUES(?, ?, ?, ?, ?)",
                f.customerId(), req.getAttribute("userId"),
                f.checkoutType(), f.reason(), f.checkoutDate()
        );
        return ApiResponse.ok(null);
    }

    @Transactional
    @PutMapping("/checkout/{id}/approve")
    public ApiResponse<Void> approveCheckout(
            @PathVariable Long id,
            @RequestBody @Valid ApprovalForm f,
            HttpServletRequest req) {
        requireAdmin(req);
        requireSubmitted("checkout_request", id);
        String status = approvalStatus(f.result());

        jdbc.update(
                "UPDATE checkout_request SET approval_status = ?, " +
                        "approver_id = ?, approval_time = NOW(), " +
                        "approval_remark = ? WHERE id = ?",
                status, req.getAttribute("userId"), f.remark(), id
        );

        if ("APPROVED".equals(status)) {
            Map<String, Object> x = jdbc.queryForMap(
                    "SELECT customer_id, checkout_type " +
                            "FROM checkout_request WHERE id = ?", id
            );
            if (!"KEEP_BED".equals(x.get("checkout_type"))) {
                Long cid = ((Number) x.get("customer_id")).longValue();
                jdbc.update(
                        "UPDATE bed b JOIN customer c ON c.bed_id = b.id " +
                                "SET b.status = 'FREE' WHERE c.id = ?",
                        cid
                );
                jdbc.update(
                        "UPDATE bed_usage SET active = 0, end_date = CURDATE() " +
                                "WHERE customer_id = ? AND active = 1",
                        cid
                );
                jdbc.update(
                        "UPDATE customer_service SET deleted = 1 " +
                                "WHERE customer_id = ? AND deleted = 0",
                        cid
                );
                jdbc.update(
                        "UPDATE customer SET status = 'CHECKED_OUT', " +
                                "room_id = NULL, bed_id = NULL, " +
                                "health_manager_id = NULL, " +
                                "care_level_id = NULL WHERE id = ?",
                        cid
                );
            }
        }
        return ApiResponse.ok(null);
    }

    // ==================== 私有辅助方法 ====================

    private void assertOwnCustomer(Long customerId, HttpServletRequest req) {
        if ("ADMIN".equals(req.getAttribute("role"))) return;
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer " +
                        "WHERE id = ? AND health_manager_id = ? " +
                        "AND deleted = 0",
                Integer.class, customerId, req.getAttribute("userId")
        );
        if (n == null || n == 0) {
            throw new BusinessException("只能操作自己的服务对象");
        }
    }

    private void requireAdmin(HttpServletRequest req) {
        if (!"ADMIN".equals(req.getAttribute("role"))) {
            throw new BusinessException("仅管理员可执行审批");
        }
    }

    private void requireSubmitted(String table, Long id) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table +
                        " WHERE id = ? AND approval_status = 'SUBMITTED' " +
                        "AND deleted = 0",
                Integer.class, id
        );
        if (n == null || n == 0) {
            throw new BusinessException("申请不存在或已审批");
        }
    }

    private String approvalStatus(String result) {
        return switch (result.toUpperCase()) {
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            default -> throw new BusinessException("审批结果不正确");
        };
    }

    private void requireManager(HttpServletRequest req) {
        if (!"HEALTH_MANAGER".equals(req.getAttribute("role"))) {
            throw new BusinessException("申请只能由健康管家发起");
        }
    }

    private int limit(int size) {
        return Math.max(1, Math.min(size, 100));
    }
}
