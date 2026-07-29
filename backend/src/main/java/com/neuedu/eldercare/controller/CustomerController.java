package com.neuedu.eldercare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.eldercare.common.ApiResponse;
import com.neuedu.eldercare.common.BusinessException;
import com.neuedu.eldercare.entity.Customer;
import com.neuedu.eldercare.mapper.CustomerMapper;
import com.neuedu.eldercare.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerMapper mapper;
    private final CustomerService service;
    private final JdbcTemplate jdbc;

    public CustomerController(CustomerMapper mapper, CustomerService service, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.service = service;
        this.jdbc = jdbc;
    }

    @GetMapping
    public ApiResponse<IPage<Customer>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String elderType,
            HttpServletRequest request) {
        requireAdmin(request);
        var query = Wrappers.<Customer>lambdaQuery()
                .like(!name.isBlank(), Customer::getName, name);

        if ("SELF_CARE".equals(elderType)) {
            query.notExists(
                    "SELECT 1 FROM customer_service cs " +
                            "WHERE cs.customer_id = customer.id AND cs.deleted = 0"
            );
        } else if ("NURSING".equals(elderType)) {
            query.exists(
                    "SELECT 1 FROM customer_service cs " +
                            "WHERE cs.customer_id = customer.id AND cs.deleted = 0"
            );
        }
        query.orderByDesc(Customer::getId);

        IPage<Customer> result = mapper.selectPage(
                Page.of(page, Math.min(Math.max(size, 1), 100)), query
        );
        result.getRecords().forEach(this::fillLabels);
        return ApiResponse.ok(result);
    }

    private void fillLabels(Customer customer) {
        if (customer.getBirthDate() != null) {
            customer.setAge(java.time.Period.between(
                    customer.getBirthDate(), LocalDate.now()
            ).getYears());
        }
        Integer services = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_service " +
                        "WHERE customer_id = ? AND deleted = 0",
                Integer.class, customer.getId()
        );
        customer.setElderTypeLabel(
                services == null || services == 0 ? "自理老人" : "护理老人"
        );

        switch (customer.getStatus() == null ? "" : customer.getStatus()) {
            case "IN_HOME" -> customer.setStatusLabel("正常入住");
            case "OUTING" -> customer.setStatusLabel("外出中");
            case "CHECKED_OUT" -> fillCheckoutLabel(customer);
            default -> customer.setStatusLabel("状态未知");
        }
    }

    private void fillCheckoutLabel(Customer customer) {
        List<Map<String, Object>> records = jdbc.queryForList(
                "SELECT checkout_type, checkout_date FROM checkout_request " +
                        "WHERE customer_id = ? AND approval_status = 'APPROVED' " +
                        "AND deleted = 0 ORDER BY approval_time DESC LIMIT 1",
                customer.getId()
        );
        if (records.isEmpty()) {
            customer.setStatusLabel("已退住");
            return;
        }
        String type = String.valueOf(records.get(0).get("checkout_type"));
        customer.setCheckoutType(type);

        if ("DEATH".equals(type)) {
            customer.setStatusLabel("死亡退住");
        } else if ("KEEP_BED".equals(type)) {
            customer.setStatusLabel("暂时离院（保留床位）");
        } else {
            LocalDate checkoutDate = LocalDate.parse(
                    String.valueOf(records.get(0).get("checkout_date"))
            );
            customer.setStatusLabel(
                    customer.getContractEndDate() != null
                            && checkoutDate.isBefore(customer.getContractEndDate())
                            ? "提前退住" : "正常退住"
            );
        }
    }

    @PostMapping
    public ApiResponse<Customer> add(
            @RequestBody @Valid Customer customer,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(service.checkIn(customer));
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(
            @PathVariable Long id,
            @RequestBody @Valid Customer customer,
            HttpServletRequest request) {
        requireAdmin(request);
        customer.setId(id);
        return ApiResponse.ok(service.update(customer));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(
            @PathVariable Long id,
            HttpServletRequest request) {
        requireAdmin(request);
        service.remove(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/change-bed")
    public ApiResponse<Void> changeBed(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            HttpServletRequest request) {
        requireAdmin(request);
        service.changeBed(id, body.get("bedId"));
        return ApiResponse.ok(null);
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException("仅管理员可以执行该操作");
        }
    }
}
