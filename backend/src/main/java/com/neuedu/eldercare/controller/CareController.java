package com.neuedu.eldercare.controller;

import com.neuedu.eldercare.common.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/care")
public class CareController {

    private final JdbcTemplate jdbc;

    public CareController(JdbcTemplate j) {
        this.jdbc = j;
    }

    public record ItemForm(
            @NotBlank String itemCode,
            @NotBlank String name,
            @NotNull @PositiveOrZero BigDecimal price,
            @NotBlank String executionCycle,
            @NotNull @Positive Integer executionTimes,
            String description,
            Integer status) {
    }

    public record LevelForm(
            @NotBlank String name,
            @NotBlank String levelCode,
            @NotNull @PositiveOrZero BigDecimal dailyPrice,
            Integer status,
            List<Long> itemIds) {
    }

    public record ServiceForm(
            @NotNull Long customerId,
            @NotNull Long careItemId,
            @NotNull @Positive Integer quantity,
            @NotNull LocalDate expiryDate) {
    }

    public record NursingForm(
            @NotNull Long customerServiceId,
            @NotNull @Positive Integer quantity,
            LocalDateTime nursingTime,
            String remark) {
    }

    // ==================== 护理项目管理 ====================

    @GetMapping("/items")
    public ApiResponse<List<Map<String, Object>>> items(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) Integer status) {
        String sql = "SELECT * FROM care_item WHERE deleted = 0 AND name LIKE ?" +
                (status == null ? "" : " AND status = ?") +
                " ORDER BY id DESC";
        return ApiResponse.ok(
                status == null
                        ? jdbc.queryForList(sql, "%" + name + "%")
                        : jdbc.queryForList(sql, "%" + name + "%", status)
        );
    }

    @PostMapping("/items")
    public ApiResponse<Void> addItem(@RequestBody @Valid ItemForm f, HttpServletRequest r) {
        admin(r);
        jdbc.update(
                "INSERT INTO care_item(item_code, name, price, status, execution_cycle, execution_times, description) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?)",
                f.itemCode(), f.name(), f.price(),
                f.status() == null ? 1 : f.status(),
                f.executionCycle(), f.executionTimes(), f.description()
        );
        return ApiResponse.ok(null);
    }

    @PutMapping("/items/{id}")
    @Transactional
    public ApiResponse<Void> editItem(
            @PathVariable Long id,
            @RequestBody @Valid ItemForm f,
            HttpServletRequest r) {
        admin(r);
        jdbc.update(
                "UPDATE care_item SET item_code = ?, name = ?, price = ?, status = ?, " +
                        "execution_cycle = ?, execution_times = ?, description = ? WHERE id = ?",
                f.itemCode(), f.name(), f.price(),
                f.status() == null ? 1 : f.status(),
                f.executionCycle(), f.executionTimes(), f.description(), id
        );
        if (Integer.valueOf(0).equals(f.status())) {
            jdbc.update("DELETE FROM care_level_item WHERE care_item_id = ?", id);
        }
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/items/{id}")
    @Transactional
    public ApiResponse<Void> deleteItem(@PathVariable Long id, HttpServletRequest r) {
        admin(r);
        jdbc.update("UPDATE care_item SET deleted = 1 WHERE id = ?", id);
        jdbc.update("DELETE FROM care_level_item WHERE care_item_id = ?", id);
        return ApiResponse.ok(null);
    }

    // ==================== 护理级别管理 ====================

    @GetMapping("/levels")
    public ApiResponse<List<Map<String, Object>>> levels(
            @RequestParam(required = false) Integer status) {
        String sql = "SELECT l.id, l.level_code, l.name, l.daily_price, l.description, l.status, " +
                "GROUP_CONCAT(i.name ORDER BY i.id SEPARATOR '、') item_names, " +
                "GROUP_CONCAT(i.id ORDER BY i.id) item_ids " +
                "FROM care_level l " +
                "LEFT JOIN care_level_item li ON li.care_level_id = l.id " +
                "LEFT JOIN care_item i ON i.id = li.care_item_id AND i.deleted = 0 " +
                "WHERE l.deleted = 0" +
                (status == null ? "" : " AND l.status = ?") +
                " GROUP BY l.id ORDER BY l.id";
        return ApiResponse.ok(
                status == null
                        ? jdbc.queryForList(sql)
                        : jdbc.queryForList(sql, status)
        );
    }

    @PostMapping("/levels")
    @Transactional
    public ApiResponse<Void> addLevel(@RequestBody @Valid LevelForm f, HttpServletRequest r) {
        admin(r);
        jdbc.update(
                "INSERT INTO care_level(name, level_code, daily_price, status) VALUES(?, ?, ?, ?)",
                f.name(), f.levelCode(), f.dailyPrice(),
                f.status() == null ? 1 : f.status()
        );
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        saveLevelItems(id, f.itemIds());
        return ApiResponse.ok(null);
    }

    @PutMapping("/levels/{id}")
    @Transactional
    public ApiResponse<Void> editLevel(
            @PathVariable Long id,
            @RequestBody @Valid LevelForm f,
            HttpServletRequest r) {
        admin(r);
        jdbc.update(
                "UPDATE care_level SET name = ?, level_code = ?, daily_price = ?, status = ? WHERE id = ?",
                f.name(), f.levelCode(), f.dailyPrice(),
                f.status() == null ? 1 : f.status(), id
        );
        jdbc.update("DELETE FROM care_level_item WHERE care_level_id = ?", id);
        saveLevelItems(id, f.itemIds());
        return ApiResponse.ok(null);
    }

    // ==================== 客户护理服务 ====================

    @GetMapping("/services")
    public ApiResponse<List<Map<String, Object>>> services(
            @RequestParam Long customerId,
            HttpServletRequest r) {
        own(customerId, r);
        String sql = "SELECT s.*, i.item_code, i.name item_name, i.price, " +
                "CASE WHEN s.source_type = 'LEVEL' THEN '级别基础服务' ELSE '额外加购服务' END source_label, " +
                "CASE WHEN s.paid_status = 0 THEN 'UNPAID' " +
                "WHEN s.expiry_date < CURDATE() THEN 'EXPIRED' " +
                "WHEN s.remaining_quantity <= 0 THEN 'DEPLETED' " +
                "ELSE 'NORMAL' END service_status " +
                "FROM customer_service s " +
                "JOIN care_item i ON i.id = s.care_item_id " +
                "WHERE s.customer_id = ? AND s.deleted = 0 " +
                "ORDER BY s.id DESC";
        return ApiResponse.ok(jdbc.queryForList(sql, customerId));
    }

    @PostMapping("/services")
    public ApiResponse<Void> buy(
            @RequestBody @Valid ServiceForm f,
            HttpServletRequest r) {
        admin(r);
        activeCustomer(f.customerId());
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_service " +
                        "WHERE customer_id = ? AND care_item_id = ? AND deleted = 0",
                Integer.class, f.customerId(), f.careItemId()
        );
        if (count != null && count > 0) {
            throw new BusinessException("客户已经拥有该护理项目，请使用续费功能");
        }
        jdbc.update(
                "INSERT INTO customer_service(customer_id, care_item_id, purchase_date, " +
                        "expiry_date, total_quantity, remaining_quantity, source_type, paid_status) " +
                        "VALUES(?, ?, CURDATE(), ?, ?, ?, 'ADD_ON', 1)",
                f.customerId(), f.careItemId(),
                f.expiryDate(), f.quantity(), f.quantity()
        );
        return ApiResponse.ok(null);
    }

    @PutMapping("/services/{id}/renew")
    public ApiResponse<Void> renew(
            @PathVariable Long id,
            @RequestBody Map<String, Object> f,
            HttpServletRequest r) {
        admin(r);
        int q = Integer.parseInt(String.valueOf(f.get("quantity")));
        LocalDate d = LocalDate.parse(String.valueOf(f.get("expiryDate")));
        if (q <= 0) throw new BusinessException("续费数量必须大于0");
        jdbc.update(
                "UPDATE customer_service SET total_quantity = total_quantity + ?, " +
                        "remaining_quantity = remaining_quantity + ?, " +
                        "expiry_date = ?, paid_status = 1 " +
                        "WHERE id = ? AND deleted = 0",
                q, q, d, id
        );
        return ApiResponse.ok(null);
    }

    @PutMapping("/services/{id}/payment")
    public ApiResponse<Void> payment(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> f,
            HttpServletRequest r) {
        admin(r);
        Integer paid = f.get("paidStatus");
        if (paid == null || !List.of(0, 1).contains(paid)) {
            throw new BusinessException("缴费状态不正确");
        }
        jdbc.update(
                "UPDATE customer_service SET paid_status = ? WHERE id = ? AND deleted = 0",
                paid, id
        );
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/services/{id}")
    public ApiResponse<Void> removeService(@PathVariable Long id, HttpServletRequest r) {
        admin(r);
        jdbc.update("UPDATE customer_service SET deleted = 1 WHERE id = ?", id);
        return ApiResponse.ok(null);
    }

    // ==================== 客户护理级别 ====================

    @PutMapping("/customers/{customerId}/level")
    @Transactional
    public ApiResponse<Void> setLevel(
            @PathVariable Long customerId,
            @RequestBody Map<String, Long> f,
            HttpServletRequest r) {
        admin(r);
        activeCustomer(customerId);
        Long levelId = f.get("levelId");
        Long old = jdbc.queryForObject(
                "SELECT care_level_id FROM customer WHERE id = ?", Long.class, customerId
        );
        if (old != null) {
            throw new BusinessException("客户已有护理级别，请先移除");
        }
        jdbc.update(
                "UPDATE customer SET care_level_id = ? WHERE id = ?", levelId, customerId
        );
        List<Long> ids = jdbc.queryForList(
                "SELECT care_item_id FROM care_level_item WHERE care_level_id = ?",
                Long.class, levelId
        );
        for (Long itemId : ids) {
            jdbc.update(
                    "INSERT INTO customer_service(customer_id, care_item_id, purchase_date, " +
                            "expiry_date, total_quantity, remaining_quantity, source_type, source_level_id) " +
                            "VALUES(?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 MONTH), 1, 1, 'LEVEL', ?)",
                    customerId, itemId, levelId
            );
        }
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/customers/{customerId}/level")
    @Transactional
    public ApiResponse<Void> removeLevel(
            @PathVariable Long customerId,
            HttpServletRequest r) {
        admin(r);
        Long levelId = jdbc.queryForObject(
                "SELECT care_level_id FROM customer WHERE id = ?", Long.class, customerId
        );
        jdbc.update("UPDATE customer SET care_level_id = NULL WHERE id = ?", customerId);
        jdbc.update(
                "UPDATE customer_service SET deleted = 1 " +
                        "WHERE customer_id = ? AND source_type = 'LEVEL' " +
                        "AND source_level_id = ? AND deleted = 0",
                customerId, levelId
        );
        return ApiResponse.ok(null);
    }

    // ==================== 护理记录管理 ====================

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> records(
            @RequestParam(required = false) Long customerId,
            HttpServletRequest r) {
        String role = (String) r.getAttribute("role");
        Long uid = (Long) r.getAttribute("userId");

        String sql = "SELECT n.*, c.name customer_name, i.name item_name, " +
                "u.real_name manager_name " +
                "FROM nursing_record n " +
                "JOIN customer c ON c.id = n.customer_id " +
                "JOIN customer_service s ON s.id = n.customer_service_id " +
                "JOIN care_item i ON i.id = s.care_item_id " +
                "JOIN sys_user u ON u.id = n.health_manager_id " +
                "WHERE n.deleted = 0" +
                (customerId == null ? "" : " AND n.customer_id = ?") +
                ("HEALTH_MANAGER".equals(role) ? " AND c.health_manager_id = ?" : "") +
                " ORDER BY n.nursing_time DESC";

        List<Map<String, Object>> x;
        if (customerId != null && "HEALTH_MANAGER".equals(role)) {
            x = jdbc.queryForList(sql, customerId, uid);
        } else if (customerId != null) {
            x = jdbc.queryForList(sql, customerId);
        } else if ("HEALTH_MANAGER".equals(role)) {
            x = jdbc.queryForList(sql, uid);
        } else {
            x = jdbc.queryForList(sql);
        }
        return ApiResponse.ok(x);
    }

    @PostMapping("/records")
    @Transactional
    public ApiResponse<Void> nurse(@RequestBody @Valid NursingForm f, HttpServletRequest r) {
        if (!"HEALTH_MANAGER".equals(r.getAttribute("role"))) {
            throw new BusinessException("只有健康管家可以执行日常护理");
        }
        Long uid = (Long) r.getAttribute("userId");

        Map<String, Object> x = jdbc.queryForMap(
                "SELECT s.customer_id, s.remaining_quantity, s.expiry_date, " +
                        "s.paid_status, c.health_manager_id, c.status " +
                        "FROM customer_service s " +
                        "JOIN customer c ON c.id = s.customer_id " +
                        "WHERE s.id = ? AND s.deleted = 0",
                f.customerServiceId()
        );

        if (!Objects.equals(uid, ((Number) x.get("health_manager_id")).longValue())) {
            throw new BusinessException("只能护理自己的服务对象");
        }
        if ("CHECKED_OUT".equals(x.get("status"))) {
            throw new BusinessException("客户已经退住，不能执行护理");
        }
        if (((Number) x.get("paid_status")).intValue() == 0) {
            throw new BusinessException("该护理服务尚未缴费");
        }
        if (LocalDate.parse(String.valueOf(x.get("expiry_date"))).isBefore(LocalDate.now())) {
            throw new BusinessException("该护理服务已经到期");
        }
        if (((Number) x.get("remaining_quantity")).intValue() < f.quantity()) {
            throw new BusinessException("护理服务剩余次数不足");
        }

        jdbc.update(
                "UPDATE customer_service SET remaining_quantity = remaining_quantity - ? WHERE id = ?",
                f.quantity(), f.customerServiceId()
        );
        jdbc.update(
                "INSERT INTO nursing_record(customer_id, customer_service_id, health_manager_id, " +
                        "nursing_time, quantity, remark) VALUES(?, ?, ?, ?, ?, ?)",
                x.get("customer_id"), f.customerServiceId(), uid,
                f.nursingTime() == null ? LocalDateTime.now() : f.nursingTime(),
                f.quantity(), f.remark()
        );
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id, HttpServletRequest r) {
        boolean isManager = "HEALTH_MANAGER".equals(r.getAttribute("role"));
        String sql = "UPDATE nursing_record SET deleted = 1 WHERE id = ?" +
                (isManager ? " AND health_manager_id = ?" : "");
        Object[] args = isManager ? new Object[]{id, r.getAttribute("userId")} : new Object[]{id};
        jdbc.update(sql, args);
        return ApiResponse.ok(null);
    }

    // ==================== 私有辅助方法 ====================

    private void saveLevelItems(Long levelId, List<Long> ids) {
        if (ids != null) {
            for (Long itemId : ids) {
                jdbc.update(
                        "INSERT INTO care_level_item(care_level_id, care_item_id) VALUES(?, ?)",
                        levelId, itemId
                );
            }
        }
    }

    private void admin(HttpServletRequest r) {
        if (!"ADMIN".equals(r.getAttribute("role"))) {
            throw new BusinessException("仅管理员可执行该操作");
        }
    }

    private void own(Long cid, HttpServletRequest r) {
        if ("ADMIN".equals(r.getAttribute("role"))) return;
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE id = ? AND health_manager_id = ?",
                Integer.class, cid, r.getAttribute("userId")
        );
        if (n == null || n == 0) {
            throw new BusinessException("只能查看自己的服务对象");
        }
    }

    private void activeCustomer(Long cid) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE id = ? AND deleted = 0 " +
                        "AND status <> 'CHECKED_OUT'",
                Integer.class, cid
        );
        if (n == null || n == 0) {
            throw new BusinessException("已退住客户不能再配置护理服务");
        }
    }
}
