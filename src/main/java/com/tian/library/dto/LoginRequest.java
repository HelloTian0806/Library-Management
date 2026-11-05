package com.tian.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 定義登入 JSON 所需的 Email 與原始密碼，不包含角色或登入後會員資料。
 *
 * <p>Controller 的 {@code @Valid} 觸發欄位限制；帳號是否存在與密碼是否正確由 MemberService 判斷。
 * getter／setter 僅供綁定及讀取，不自動驗證、建立 Session 或雜湊密碼。
 * 此 DTO 含原始密碼，不應直接當成登入成功的回應。
 */
public class LoginRequest {
	/**
	 * {@code @NotBlank} 拒絕 null、空字串及全空白；{@code @Email} 檢查 Email 格式。
	 * 格式驗證不會確認信箱存在，也不會自動 trim 或轉換大小寫。
	 */
	@NotBlank(message = "Email不可為空")
	@Email(message = "Email格式錯誤")
	private String email;
	/**
	 * 原始密碼必須非空白；此處沒有設定登入密碼長度限制，也不會將它轉成雜湊。
	 */
	@NotBlank
	private String password;
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
