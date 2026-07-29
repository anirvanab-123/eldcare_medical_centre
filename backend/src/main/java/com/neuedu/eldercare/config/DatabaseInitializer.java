package com.neuedu.eldercare.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DatabaseInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        addColumnIfMissing(
                "customer_service", "source_type",
                "VARCHAR(20) NOT NULL DEFAULT 'ADD_ON' COMMENT 'LEVEL/ADD_ON'"
        );
        addColumnIfMissing(
                "customer_service", "source_level_id", "BIGINT NULL"
        );
        addColumnIfMissing(
                "customer_service", "paid_status",
                "TINYINT NOT NULL DEFAULT 1 COMMENT '1已缴费/0欠费'"
        );
        jdbc.update(
                "UPDATE customer_service " +
                        "SET source_type = 'ADD_ON' " +
                        "WHERE source_type IS NULL OR source_type = ''"
        );

        retireBuilding607();

        jdbc.update(
                "UPDATE customer_service s " +
                        "JOIN customer c ON c.id = s.customer_id " +
                        "SET s.deleted = 1 " +
                        "WHERE c.status = 'CHECKED_OUT' " +
                        "AND s.deleted = 0"
        );
        jdbc.update(
                "UPDATE customer " +
                        "SET health_manager_id = NULL, care_level_id = NULL " +
                        "WHERE status = 'CHECKED_OUT'"
        );
    }

    private void addColumnIfMissing(
            String table, String column, String definition) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column
        );
        if (count == null || count == 0) {
            jdbc.execute(
                    "ALTER TABLE " + table +
                            " ADD COLUMN " + column + " " + definition
            );
        }
    }

    private void addRoomWithBeds(
            String building, int floor, String roomNo) {
        jdbc.update(
                "INSERT IGNORE INTO room(building_no, floor_no, room_no) " +
                        "VALUES(?, ?, ?)",
                building, floor, roomNo
        );
        Long roomId = jdbc.queryForObject(
                "SELECT id FROM room " +
                        "WHERE building_no = ? AND room_no = ? " +
                        "AND deleted = 0",
                Long.class, building, roomNo
        );
        jdbc.update(
                "INSERT IGNORE INTO bed(room_id, bed_no, status) " +
                        "VALUES(?, 'A', 'FREE')",
                roomId
        );
        jdbc.update(
                "INSERT IGNORE INTO bed(room_id, bed_no, status) " +
                        "VALUES(?, 'B', 'FREE')",
                roomId
        );
    }

    private void retireBuilding607() {
        var customers = jdbc.queryForList(
                "SELECT c.id, c.bed_id, c.status " +
                        "FROM customer c " +
                        "JOIN room r ON r.id = c.room_id " +
                        "WHERE r.building_no = '607' " +
                        "AND c.deleted = 0 " +
                        "AND c.status <> 'CHECKED_OUT'"
        );
        for (var customer : customers) {
            var free = jdbc.queryForList(
                    "SELECT b.id, r.id room_id " +
                            "FROM bed b " +
                            "JOIN room r ON r.id = b.room_id " +
                            "WHERE r.building_no = '606' " +
                            "AND r.deleted = 0 " +
                            "AND b.deleted = 0 " +
                            "AND b.status = 'FREE' " +
                            "ORDER BY r.floor_no, r.room_no, b.bed_no " +
                            "LIMIT 1"
            );
            if (free.isEmpty()) {
                Long firstRoom = jdbc.queryForObject(
                        "SELECT id FROM room " +
                                "WHERE building_no = '606' " +
                                "AND deleted = 0 " +
                                "ORDER BY floor_no, room_no LIMIT 1",
                        Long.class
                );
                jdbc.update(
                        "INSERT INTO bed(room_id, bed_no, status) " +
                                "VALUES(?, ?, 'FREE')",
                        firstRoom,
                        "扩展-" + customer.get("id")
                );
                free = jdbc.queryForList(
                        "SELECT b.id, r.id room_id " +
                                "FROM bed b " +
                                "JOIN room r ON r.id = b.room_id " +
                                "WHERE r.building_no = '606' " +
                                "AND b.status = 'FREE' " +
                                "AND b.deleted = 0 " +
                                "ORDER BY b.id DESC LIMIT 1"
                );
            }
            Long customerId = ((Number) customer.get("id")).longValue();
            Long oldBed = ((Number) customer.get("bed_id")).longValue();
            Long newBed = ((Number) free.get(0).get("id")).longValue();
            Long newRoom = ((Number) free.get(0).get("room_id")).longValue();

            jdbc.update(
                    "UPDATE bed SET status = 'FREE' WHERE id = ?",
                    oldBed
            );
            jdbc.update(
                    "UPDATE bed SET status = ? WHERE id = ?",
                    "OUTING".equals(customer.get("status"))
                            ? "OUTING" : "OCCUPIED",
                    newBed
            );
            jdbc.update(
                    "UPDATE bed_usage SET active = 0, end_date = CURDATE() " +
                            "WHERE customer_id = ? AND active = 1",
                    customerId
            );
            jdbc.update(
                    "INSERT INTO bed_usage(" +
                            "customer_id, bed_id, start_date, end_date, active" +
                            ") SELECT id, ?, CURDATE(), contract_end_date, 1 " +
                            "FROM customer WHERE id = ?",
                    newBed, customerId
            );
            jdbc.update(
                    "UPDATE customer " +
                            "SET building_no = '606', " +
                            "room_id = ?, bed_id = ? " +
                            "WHERE id = ?",
                    newRoom, newBed, customerId
            );
        }
        jdbc.update(
                "UPDATE bed b JOIN room r ON r.id = b.room_id " +
                        "SET b.deleted = 1 " +
                        "WHERE r.building_no = '607'"
        );
        jdbc.update(
                "UPDATE room SET deleted = 1 " +
                        "WHERE building_no = '607'"
        );
    }
}
