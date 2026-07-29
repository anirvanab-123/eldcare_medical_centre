package com.neuedu.eldercare.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("bed_usage")
public class BedUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;
    private Long bedId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer active;

    @TableLogic
    private Integer deleted;
}
