package com.tian.library.dto;

import java.time.LocalDate;

import com.tian.library.entity.Role;

/**
 * 定義管理端會員清單與搜尋結果，包含聯絡資料、建立日期及角色，不包含密碼。
 *
 * <p>由 AdminMemberController 將 Member 清單轉成此 DTO，讓對外欄位明確而不隨 Entity 新增欄位自動擴大。
 * 建立日期的來源及精度取決於 DAO 映射，目前使用 LocalDate。
 *
 * <p>此類別只包裝已取得的資料，不自行查詢資料庫或驗證授權。
 * 目前 JSON 序列化可依 getter 輸出屬性；setter 只改動 DTO，與原始 Entity 或資料庫沒有自動同步關係。
 */
public class MemberResponse {
	private int id;
	private String name;
	private String email;
	private String phone;
	private LocalDate createdAt;
	private Role role;
	
	/**
	 * 組合單筆會員清單資料，不在建構子內重新載入缺少的欄位。
	 *
	 * @param id 會員 ID
	 * @param name 會員姓名
	 * @param email 會員 Email
	 * @param phone 手機號碼
	 * @param createdAt 會員建立日期，若來源未載入則可能為 null
	 * @param role 會員角色
	 */
	public MemberResponse(int id, String name, String email, String phone, LocalDate createdAt, Role role) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.createdAt = createdAt;
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
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public LocalDate getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDate createdAt) {
		this.createdAt = createdAt;
	}
	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}
	
	
}
