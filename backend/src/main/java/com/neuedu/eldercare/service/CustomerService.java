package com.neuedu.eldercare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neuedu.eldercare.common.BusinessException;
import com.neuedu.eldercare.entity.*;
import com.neuedu.eldercare.mapper.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CustomerService {

    private final CustomerMapper customers;
    private final BedMapper beds;
    private final BedUsageMapper usages;
    private final JdbcTemplate jdbc;

    public CustomerService(
            CustomerMapper c, BedMapper b,
            BedUsageMapper u, JdbcTemplate j) {
        this.customers = c;
        this.beds = b;
        this.usages = u;
        this.jdbc = j;
    }

    @Transactional
    public Customer checkIn(Customer c) {
        validateDates(c);

        Bed b = beds.selectById(c.getBedId());
        if (b == null || !"FREE".equals(b.getStatus())) {
            throw new BusinessException("所选床位当前不可用");
        }

        String building = jdbc.queryForObject(
                "SELECT building_no FROM room WHERE id = ?",
                String.class, c.getRoomId()
        );
        c.setBuildingNo(building);
        c.setStatus("IN_HOME");
        customers.insert(c);

        b.setStatus("OCCUPIED");
        beds.updateById(b);

        BedUsage u = new BedUsage();
        u.setCustomerId(c.getId());
        u.setBedId(b.getId());
        u.setStartDate(c.getCheckInDate());
        u.setEndDate(c.getContractEndDate());
        u.setActive(1);
        usages.insert(u);

        return c;
    }

    @Transactional
    public Customer update(Customer c) {
        validateDates(c);

        Customer old = customers.selectById(c.getId());
        if (old == null) {
            throw new BusinessException("客户不存在");
        }
        if (!old.getBedId().equals(c.getBedId())) {
            throw new BusinessException("修改客户资料不能直接改变床位，请使用床位调换功能");
        }

        customers.updateById(c);

        if (!java.util.Objects.equals(
                old.getContractEndDate(), c.getContractEndDate())) {
            jdbc.update(
                    "UPDATE bed_usage SET end_date = ? " +
                            "WHERE customer_id = ? AND active = 1 AND deleted = 0",
                    c.getContractEndDate(), c.getId()
            );
        }
        return customers.selectById(c.getId());
    }

    @Transactional
    public void remove(Long id) {
        Customer c = customers.selectById(id);
        if (c == null) throw new BusinessException("客户不存在");

        Bed b = beds.selectById(c.getBedId());
        if (b != null) {
            b.setStatus("FREE");
            beds.updateById(b);
        }

        BedUsage u = usages.selectOne(
                Wrappers.<BedUsage>lambdaQuery()
                        .eq(BedUsage::getCustomerId, id)
                        .eq(BedUsage::getActive, 1)
        );
        if (u != null) {
            u.setActive(0);
            u.setEndDate(LocalDate.now());
            u.setDeleted(1);
            usages.updateById(u);
        }
        customers.deleteById(id);
    }

    @Transactional
    public void changeBed(Long customerId, Long newBedId) {
        Customer c = customers.selectById(customerId);
        Bed n = beds.selectById(newBedId);

        if (c == null) throw new BusinessException("客户不存在");
        if (n == null || !"FREE".equals(n.getStatus())) {
            throw new BusinessException("目标床位不可用");
        }

        Bed old = beds.selectById(c.getBedId());
        old.setStatus("FREE");
        beds.updateById(old);

        n.setStatus("OCCUPIED");
        beds.updateById(n);

        BedUsage current = usages.selectOne(
                Wrappers.<BedUsage>lambdaQuery()
                        .eq(BedUsage::getCustomerId, customerId)
                        .eq(BedUsage::getActive, 1)
        );
        if (current != null) {
            current.setActive(0);
            current.setEndDate(LocalDate.now());
            usages.updateById(current);
        }

        BedUsage next = new BedUsage();
        next.setCustomerId(customerId);
        next.setBedId(newBedId);
        next.setStartDate(LocalDate.now());
        next.setEndDate(c.getContractEndDate());
        next.setActive(1);
        usages.insert(next);

        c.setBedId(newBedId);
        c.setRoomId(n.getRoomId());
        c.setBuildingNo(jdbc.queryForObject(
                "SELECT building_no FROM room WHERE id = ?",
                String.class, n.getRoomId()
        ));
        customers.updateById(c);
    }

    private void validateDates(Customer c) {
        if (c.getCheckInDate() != null
                && c.getContractEndDate() != null
                && c.getContractEndDate().isBefore(c.getCheckInDate())) {
            throw new BusinessException(
                    "合同到期日期不能早于入住日期"
            );
        }
    }
}
