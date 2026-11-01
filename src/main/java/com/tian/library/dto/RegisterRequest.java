package com.tian.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 用來限制前端送來的資料
public class RegisterRequest {
	
	@NotBlank(message = "帳號不能為空")
	@Size(min = 4, max = 30, message = "帳號長度必須介於 4~30 字元")
	private String name;
	@NotBlank(message = "密碼不能為空")
	@Size(min = 8, max = 72, message = "密碼長度必須介於 8~72 字元")
	private String password;
	@NotBlank(message = "Email不能為空")
	@Email(message = "Email格式不正確")
	private String email;
	@NotBlank
	@Pattern(regexp = "09\\d{8}$", message = "手機號碼必須為 09 為開頭的 10 位數字")
	private String phone;
	
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	

}
