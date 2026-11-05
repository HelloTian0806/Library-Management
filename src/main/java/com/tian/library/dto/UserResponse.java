package com.tian.library.dto;

import com.tian.library.entity.Role;

/**
 * 定義登入成功與 /auth/me 回傳的會員欄位：ID、姓名、Email 與角色。
 *
 * <p>Controller 明確從 Member 選取欄位建立此 DTO，沒有 password 欄位及 getter，避免一併輸出密碼雜湊。
 * 回應中的 role 供前端呈現使用，後端管理 API 仍會自行確認登入者及角色。
 *
 * <p>此類別只包裝已取得的資料，不自行查詢資料庫或驗證授權。
 * 目前 JSON 序列化可依 getter 輸出屬性；setter 只改動 DTO，與原始 Entity 或資料庫沒有自動同步關係。
 */
public class UserResponse {
	private int id;
	private String name;
	private String email;
	private Role role;
	
	/**
	 * 以 Controller 允許公開的四個欄位建立回應，不持有 Member 的參照。
	 *
	 * @param id 會員 ID
	 * @param name 會員姓名
	 * @param email 會員 Email
	 * @param role 會員角色，不代表前端可藉修改回應來變更後端權限
	 */
	public UserResponse(int id, String name, String email, Role role) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.role = role;
	}
	
	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
}
