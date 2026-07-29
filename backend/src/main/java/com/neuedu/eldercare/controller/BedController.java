package com.neuedu.eldercare.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neuedu.eldercare.common.ApiResponse;
import com.neuedu.eldercare.common.BusinessException;
import com.neuedu.eldercare.entity.Bed;
import com.neuedu.eldercare.mapper.BedMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/beds")
public class BedController {

    private final BedMapper beds;
    private final JdbcTemplate jdbc;

    public BedController(BedMapper b, JdbcTemplate j) {
        this.beds = b;
        this.jdbc = j;
    }

    @GetMapping
    public ApiResponse<List<Bed>> list(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(
                beds.selectList(
                        Wrappers.<Bed>lambdaQuery()
                                .eq(roomId != null, Bed::getRoomId, roomId)
                                .eq(status != null && !status.isBlank(), Bed::getStatus, status)
                )
        );
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("total", beds.selectCount(null));
        for (String s : List.of("FREE", "OCCUPIED", "OUTING")) {
            m.put(s.toLowerCase(), beds.selectCount(
                    Wrappers.<Bed>lambdaQuery().eq(Bed::getStatus, s)
            ));
        }
        return ApiResponse.ok(m);
    }

    @GetMapping("/overview")
    public ApiResponse<List<Map<String, Object>>> overview(
            @RequestParam(required = false) String building,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) Long roomId) {

        String sql = "SELECT r.id room_id, r.building_no, r.floor_no, r.room_no, " +
                "r.room_type, r.area, r.capacity, b.id bed_id, b.bed_no, " +
                "CASE WHEN c.id IS NULL THEN 'FREE' " +
                "WHEN c.status='OUTING' THEN 'OUTING' " +
                "ELSE 'OCCUPIED' END AS status, " +
                "c.id customer_id, c.name customer_name, c.status customer_status " +
                "FROM room r JOIN bed b ON b.room_id = r.id AND b.deleted = 0 " +
                "LEFT JOIN customer c ON c.bed_id = b.id AND c.deleted = 0 " +
                "AND c.status <> 'CHECKED_OUT' " +
                "WHERE r.deleted = 0" +
                (building == null || building.isBlank() ? "" : " AND r.building_no = ?") +
                (floor == null ? "" : " AND r.floor_no = ?") +
                (roomId == null ? "" : " AND r.id = ?") +
                " ORDER BY r.building_no, r.floor_no, r.room_no, b.bed_no";

        List<Object> args = new ArrayList<>();
        if (building != null && !building.isBlank()) args.add(building);
        if (floor != null) args.add(floor);
        if (roomId != null) args.add(roomId);

        return ApiResponse.ok(jdbc.queryForList(sql, args.toArray()));
    }

    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> usage(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "ACTIVE") String state,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.max(1, Math.min(size, 100));
        page = Math.max(page, 1);

        String where = " FROM bed_usage u " +
                "JOIN customer c ON c.id = u.customer_id " +
                "JOIN bed b ON b.id = u.bed_id " +
                "JOIN room r ON r.id = b.room_id " +
                "WHERE u.deleted = 0 AND c.name LIKE ?" +
                ("ALL".equals(state) ? "" : " AND u.active = " + ("ACTIVE".equals(state) ? 1 : 0)) +
                (startDate == null ? "" : " AND u.start_date = ?");

        List<Object> base = new ArrayList<>();
        base.add("%" + name + "%");
        if (startDate != null) base.add(startDate);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*)" + where, Long.class, base.toArray()
        );

        List<Object> args = new ArrayList<>(base);
        args.add(size);
        args.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(
                "SELECT u.*, c.name customer_name, r.building_no, r.room_no, b.bed_no" +
                        where + " ORDER BY u.id DESC LIMIT ? OFFSET ?",
                args.toArray()
        );

        return ApiResponse.ok(Map.of(
                "records", records,
                "total", total == null ? 0 : total,
                "page", page,
                "size", size
        ));
    }

    @PutMapping("/usage/{id}/end-date")
    public ApiResponse<Void> updateEndDate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException("仅管理员可以修改床位使用详情");
        }

        LocalDate end = LocalDate.parse(body.get("endDate"));
        Map<String, Object> usage = jdbc.queryForMap(
                "SELECT start_date FROM bed_usage WHERE id = ? AND deleted = 0", id
        );
        LocalDate start = LocalDate.parse(String.valueOf(usage.get("start_date")));

        if (end.isBefore(start)) {
            throw new BusinessException("结束日期不能早于入住日期");
        }

        jdbc.update("UPDATE bed_usage SET end_date = ? WHERE id = ?", end, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/rooms")
    public ApiResponse<List<Map<String, Object>>> rooms(
            @RequestParam(required = false) String building,
            @RequestParam(required = false) Integer floor) {

        String sql = "SELECT * FROM room WHERE deleted = 0" +
                (building == null || building.isBlank() ? "" : " AND building_no = ?") +
                (floor == null ? "" : " AND floor_no = ?") +
                " ORDER BY building_no, floor_no, room_no";

        List<Object> args = new ArrayList<>();
        if (building != null && !building.isBlank()) args.add(building);
        if (floor != null) args.add(floor);

        return ApiResponse.ok(jdbc.queryForList(sql, args.toArray()));
    }

    @GetMapping("/floors")
    public ApiResponse<List<Map<String, Object>>> floors(
            @RequestParam(required = false) String building) {

        String sql = "SELECT DISTINCT floor_no FROM room WHERE deleted = 0" +
                (building == null || building.isBlank() ? "" : " AND building_no = ?") +
                " ORDER BY floor_no";

        List<Object> args = new ArrayList<>();
        if (building != null && !building.isBlank()) args.add(building);

        List<Map<String, Object>> result = new ArrayList<>();
        jdbc.queryForList(sql, args.toArray()).forEach(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", row.get("floor_no"));
            m.put("label", row.get("floor_no") + "楼");
            result.add(m);
        });

        return ApiResponse.ok(result);
    }
}
