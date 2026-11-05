package com.tian.library.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tian.library.dao.MemberDAO;
import com.tian.library.dto.LoginRequest;
import com.tian.library.dto.RegisterRequest;
import com.tian.library.entity.Member;

/**
 * 處理會員查詢、註冊與登入規則，透過 MemberDAO 存取資料，使用 PasswordEncoder 雜湊及比對密碼。 不直接操作 HTTP 或
 * Session；登入身分的保存與 Response DTO 轉換由 Controller 處理。 {@code @Service} 將此類別註冊為
 * Spring Bean，依賴由 constructor injection 提供。
 */
@Service
public class MemberService {
	private final MemberDAO memberDAO;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 透過 constructor injection 取得所需依賴，便於由 Spring 組裝並在測試中替換。
	 * 
	 * @param memberDAO       此 Service 協調使用的資料存取或業務元件
	 * @param passwordEncoder 密碼雜湊與驗證工具
	 */
	public MemberService(MemberDAO memberDAO, PasswordEncoder passwordEncoder) {
		this.memberDAO = memberDAO;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 依會員 ID 查詢 Entity，供登入者查詢、授權判斷及借閱流程使用。
	 * 
	 * @param memberId 會員 ID
	 * @return 找到的 Entity，查無資料時為 null
	 */
	public Member findById(int memberId) throws SQLException {
		return memberDAO.findById(memberId);
	}

	/**
	 * 判斷 Email 是否尚未被使用；方法名稱雖為 find，實際回傳布林值且 true 代表查無會員。
	 * 
	 * @param email 會員 Email
	 * @return true 表示 Email 查無會員，false 表示已存在
	 */
	public boolean findByEmail(String email) throws SQLException {
		return memberDAO.findByEmail(email) == null;
	}

	/**
	 * 依 Email 取得會員 Entity，結果可能包含密碼雜湊，不應直接作為對外 Response。
	 * 
	 * @param email 會員 Email
	 * @return 找到的 Entity，查無資料時為 null
	 */
	public Member findByMemberEmail(String email) throws SQLException {
		return memberDAO.findByEmail(email);
	}

	/**
	 * 取得全部會員 Entity，允許輸出的欄位應由對外 Response DTO 決定。
	 * 
	 * @return 符合條件的清單；查無結果時為空集合
	 */
	public List<Member> findAll() throws SQLException {

		return memberDAO.findAll();

	}

	/**
	 * 依姓名搜尋會員 Entity，SQL 搜尋與欄位映射由 DAO 負責。
	 * 
	 * @param name 會員姓名搜尋條件
	 * @return 符合條件的清單；查無結果時為空集合
	 */
	public List<Member> findByName(String name) throws SQLException {

		return memberDAO.findByName(name);

	}

	/**
	 * 委派 DAO 更新會員資料，目前未在此驗證欄位或檢查操作者權限。
	 * 
	 * @param member 欲更新的會員資料
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 */
	public boolean updateMember(Member member) throws SQLException {

		int updatedRows = memberDAO.updateMember(member);
		return updatedRows == 1;

	}

	/**
	 * 依會員 ID 委派刪除，目前未在此檢查操作者權限或關聯借閱。
	 * 
	 * @param memberId 會員 ID
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 */
	public boolean deleteMember(int memberId) throws SQLException {

		int updatedRows = memberDAO.deleteMember(memberId);

		return updatedRows == 1;
	}

	/**
	 * 先查詢 Email，再以 PasswordEncoder 雜湊原始密碼，建立 Member 後交給 DAO 新增。
	 * 查詢只提供事前檢查；同時註冊的競爭仍須依靠 Database 的唯一約束。
	 * 
	 * @param request 由 Controller 傳入的登入或註冊 DTO
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 */
	public boolean register(RegisterRequest request) throws SQLException {

		// 檢查帳號是否存在
		if (memberDAO.findByEmail(request.getEmail()) != null) {

			return false;

		}

		// BCrypt 雜湊不可逆；資料庫保存雜湊，登入透過 matches 驗證，不保存原始密碼。
		String hashedPassword = passwordEncoder.encode(request.getPassword());

		Member member = new Member(request.getName(), request.getEmail(), hashedPassword, request.getPhone());

		return memberDAO.register(member) > 0;
	}

	/**
	 * 查詢會員後使用 PasswordEncoder.matches 比對原始密碼與既有雜湊。 BCrypt 含 salt，不應重新 encode
	 * 輸入後直接比較兩個字串。 回傳的 Member 含密碼雜湊，Controller 應轉成安全 DTO；此方法本身不建立 Session。
	 * 
	 * @param request 由 Controller 傳入的登入或註冊 DTO
	 * @return 驗證成功時的 Member；找不到會員或密碼不符時為 null
	 */
	public Member login(LoginRequest request) throws SQLException {

		// 查詢會員
		Member member = memberDAO.findByEmail(request.getEmail());

		// 檢查會員是否存在
		if (member == null) {
			return null;
		}

		// 驗證密碼
		if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			return null;
		}

		// 登入成功
		return member;
	}

}
