package com.neuedu.eldercare.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("bed")
public class Bed {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;
    private String bedNo;
    private String status;

    @TableLogic
    private Integer deleted;
}
