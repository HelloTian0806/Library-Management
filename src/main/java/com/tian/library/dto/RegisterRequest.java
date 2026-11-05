package com.tian.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 此 DTO 定義註冊輸入；欄位限制由 Controller 觸發驗證，而非在 setter 內執行。
/**
 * 定義註冊輸入的姓名、原始密碼、Email 與手機，不接受會員 ID 或角色。
 *
 * <p>Controller 的 {@code @Valid} 啟動 Bean Validation，驗證通過後 Service 才檢查重複 Email 並雜湊密碼。
 * 本類別的 setter 只保存值，不會 trim、查詢資料庫或自行執行驗證。
 * DTO 與 Member 分開，讓輸入契約不必包含資料庫產生的 ID、建立日期或內部角色欄位。
 */
public class RegisterRequest {
	
	/**
	 * 此欄位實際名稱是 name，現有流程將它作為會員姓名；驗證訊息沿用「帳號」用詞。
	 * {@code @Size} 要求字串長度介於四到三十，並搭配 {@code @NotBlank} 排除空白輸入。
	 */
	@NotBlank(message = "帳號不能為空")
	@Size(min = 4, max = 30, message = "帳號長度必須介於 4~30 字元")
	private String name;
	/**
	 * 原始密碼須非空白且長度介於八到七十二；這裡按 Java 字串的 UTF-16 單元計算長度，不是 UTF-8 位元組數。
	 * 雜湊由 Service 的 PasswordEncoder 完成，此欄位本身沒有雜湊或加密行為。
	 */
	@NotBlank(message = "密碼不能為空")
	@Size(min = 8, max = 72, message = "密碼長度必須介於 8~72 字元")
	private String password;
	/**
	 * 同時要求非空白及 Email 格式，不能以此保證唯一性或信箱可收信。
	 * 是否重複仍由 Service 查詢與資料庫約束處理。
	 */
	@NotBlank(message = "Email不能為空")
	@Email(message = "Email格式不正確")
	private String email;
	/**
	 * 以字串保留手機的前導零，要求非空白且符合 09 開頭、後接八個數字的格式。
	 * 正規表示式中的反斜線在 Java 字串內需再跳脫；此限制只檢查格式，不驗證門號是否存在。
	 */
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
