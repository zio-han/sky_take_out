package com.sky.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 自动生成get、set、toString、equals、hashCode方法，简化实体代码
@Data
// 开启建造者模式，支持链式赋值创建对象
@Builder
// 生成无参构造方法，框架反序列化、MyBatis映射必须使用
@NoArgsConstructor
// 生成全参构造方法，配合Builder模式保证对象创建完整兼容
@AllArgsConstructor
// Swagger注解：描述当前实体类作用，用于生成接口文档
@ApiModel(description = "员工登录返回的数据格式")
public class EmployeeLoginVO implements Serializable {

    @ApiModelProperty("主键值")
    private Long id;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("jwt令牌")
    private String token;

}
