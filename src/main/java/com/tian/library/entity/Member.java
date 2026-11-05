package com.tian.library.entity;

import java.time.LocalDate;

/**
 * 保存會員內部資料，包含帳號欄位、角色及可能載入的密碼雜湊。
 *
 * <p>這是一般 Java 資料物件，不是由 JPA 管理的持久化 Entity。
 * DAO 依查詢用途填入不同欄位，例如 findById 不載入密碼與建立日期，findByEmail 會載入密碼。
 * 建立 Member 不代表資料已完整載入，也不代表已通過登入驗證。
 *
 * <p>現有註冊與登入流程使用 BCrypt 雜湊，但本類別的建構子及 setter 只保存傳入字串。
 * 公開的 getPassword 仍可能被 JSON 序列化器讀取，對外應由 UserResponse 或 MemberResponse 限制欄位。
 * 其他 getter／setter 只讀寫物件，不執行格式驗證、授權或 SQL。
 */
public class Member {
	private int id;
	private String name;
	private String email;
	/**
	 * 現有登入查詢載入的密碼雜湊；未載入時可為 null，物件本身不會加密或雜湊傳入值。
	 */
	private String password;
	private String phone;
	/**
	 * 會員建立日期；目前映射為 LocalDate，不保留時間，部分查詢未填入此欄位。
	 */
	private LocalDate createdAt;
	/**
	 * 會員角色；Java 預設值是 null，沒有在此自動設為 USER，註冊時的 USER 由 DAO SQL 寫入。
	 */
	private Role role;
	
	public Role getRole() {
		return role;
	}
	
	/**
	 * 只設定物件角色，不授予實際 API 權限，也不更新資料庫中的角色。
	 *
	 * @param role 要保存的角色
	 */
	public void setRole(Role role) {
		this.role = role;
	}

	/**
	 * 建立待填入欄位的物件，現有 DAO 可透過 setter 逐一映射。
	 * 未設定的整數為零、參照欄位為 null，沒有自動載入或角色預設邏輯。
	 */
	public Member() {

	}

	/**
	 * 保存含 ID 與密碼欄位的會員資料，不填入角色或建立日期。
	 *
	 * @param id 會員 ID
	 * @param name 會員姓名
	 * @param email 會員 Email
	 * @param password 傳入的密碼欄位值，本建構子不會自行雜湊
	 * @param phone 手機號碼
	 */
	public Member(int id, String name, String email, String password, String phone) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.password = password;
		this.phone = phone;
	}

	/**
	 * 只保存姓名、Email 與手機；其他欄位保持預設值，不執行註冊。
	 *
	 * @param name 會員姓名
	 * @param email 會員 Email
	 * @param phone 手機號碼
	 */
	public Member(String name, String email, String phone) {
		super();
		this.name = name;
		this.email = email;
		this.phone = phone;
	}

	/**
	 * 保存待寫入的基本會員資料，現有註冊 Service 傳入已產生的密碼雜湊。
	 *
	 * @param name 會員姓名
	 * @param email 會員 Email
	 * @param password 密碼欄位值；本建構子只指派，不會驗證或轉換
	 * @param phone 手機號碼
	 */
	public Member(String name, String email, String password, String phone) {
		super();
		this.name = name;
		this.email = email;
		this.password = password;
		this.phone = phone;
	}
	
	

	/**
	 * 保存含角色的會員資料，不包含密碼或建立日期。
	 *
	 * @param id 會員 ID
	 * @param name 會員姓名
	 * @param email 會員 Email
	 * @param phone 手機號碼
	 * @param role 會員角色
	 */
	public Member(int id, String name, String email, String phone, Role role) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.role = role;
	}
	
	/**
	 * 保存會員清單需要的資料，現有 DAO 用此多載映射建立日期與角色，不填入密碼。
	 *
	 * @param id 會員 ID
	 * @param name 會員姓名
	 * @param email 會員 Email
	 * @param phone 手機號碼
	 * @param createAt 傳入的建立日期，指派給 createdAt 欄位
	 * @param role 會員角色
	 */
	public Member(int id, String name, String email, String phone, LocalDate createAt, Role role) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.createdAt = createAt;
		this.role = role;
	}
	
	/**
	 * 只保存傳入的 Email 與密碼欄位，不查詢會員或驗證登入。
	 *
	 * @param email 會員 Email
	 * @param password 傳入的密碼欄位值，本建構子不決定它是原始密碼或雜湊
	 */
	public Member(String email, String password) {
		super();
		this.email = email;
		this.password = password;
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

	/**
	 * 讀取此物件保存的密碼欄位，供內部帳密比對使用。
	 *
	 * @return 密碼欄位字串，未載入時可能為 null；不應直接包含在 API 回應
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * 只保存傳入字串，不呼叫 PasswordEncoder；雜湊須由 Service 在寫入前完成。
	 *
	 * @param password 要保存在物件中的密碼欄位值
	 */
	public void setPassword(String password) {
		this.password = password;
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
	/**
	 * 提供含 ID、姓名、Email 與手機的除錯文字，不輸出 password 欄位。
	 *
	 * <p>不在 toString 輸出密碼，不代表 JSON 序列化也會排除它，兩者是不同機制。
	 *
	 * @return 不包含密碼、角色及建立日期的會員文字表示
	 */
	@Override
	public String toString() {
		return "Member{" +
	            "id=" + id +
	            ", name='" + name + '\'' +
	            ", email='" + email + '\'' +
	            ", phone='" + phone + '\'' +
	            '}';
	}
}
