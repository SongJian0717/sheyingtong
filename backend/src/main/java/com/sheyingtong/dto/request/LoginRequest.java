package com.sheyingtong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度为3-50个字符")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 30, message = "密码长度为4-30个字符")
    private String password;
}
