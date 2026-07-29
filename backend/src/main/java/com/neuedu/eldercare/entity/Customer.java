package com.neuedu.eldercare.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "客户姓名不能为空")
    private String name;

    @NotBlank(message = "性别不能为空")
    private String gender;

    @NotNull(message = "出生日期不能为空")
    private LocalDate birthDate;

    @Pattern(
            regexp = "(^$)|(^[0-9Xx]{18}$)",
            message = "身份证号格式不正确"
    )
    private String idCard;

    private String bloodType;

    @NotBlank(message = "家属姓名不能为空")
    private String familyName;

    @NotBlank(message = "家属电话不能为空")
    private String familyPhone;

    private String buildingNo;

    @NotNull(message = "房间不能为空")
    private Long roomId;

    @NotNull(message = "床位不能为空")
    private Long bedId;

    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;

    @NotNull(message = "合同到期日期不能为空")
    private LocalDate contractEndDate;

    private Long careLevelId;
    private Long healthManagerId;
    private String status;

    @TableField(exist = false)
    private String statusLabel;

    @TableField(exist = false)
    private String checkoutType;

    @TableField(exist = false)
    private String elderTypeLabel;

    @TableField(exist = false)
    private Integer age;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
