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
 * 處理會員查詢、註冊與登入規則，將 SQL 與資料列映射委派給 MemberDAO。
 *
 * <p>{@code @Service} 將類別註冊為 Spring Bean；Spring 透過建構子注入 DAO 與 PasswordEncoder。
 * Service 決定何時查詢、雜湊與驗證密碼，DAO 決定如何讀寫資料庫。
 * 本類別不操作 HTTP 或 Session，也不自行檢查操作者角色；對外入口的身分與權限檢查由 Controller 執行。
 *
 * <p>目前沒有宣告式或手動交易。查詢結果包含內部 Entity，對外輸出欄位仍需由 DTO 限制。
 * SQLException 原樣向上傳遞；經由現有 Controller 呼叫時，由 GlobalExceptionHandler 轉成 500 回應。
 */
@Service
public class MemberService {
	private final MemberDAO memberDAO;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 由 Spring 注入會員 DAO 與密碼工具，避免在業務流程中自行建立資料存取或雜湊實作。
	 *
	 * <p>依賴 PasswordEncoder 介面，使服務不必負責建立 BCrypt 實作；目前由 PasswordConfig 提供 Bean。
	 * final 欄位固定依賴參照，但不代表整個服務或其參照物件自動具備執行緒安全保證。
	 *
	 * @param memberDAO 執行會員 SQL 及結果映射的資料存取元件
	 * @param passwordEncoder 負責密碼雜湊與比對的工具，目前設定為 BCrypt
	 */
	public MemberService(MemberDAO memberDAO, PasswordEncoder passwordEncoder) {
		this.memberDAO = memberDAO;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 依 ID 取得會員，供登入者查詢、角色判斷及借閱前的存在性檢查使用。
	 *
	 * <p>目前 DAO 只填入 ID、姓名、Email、手機與角色，未填入密碼及建立日期；
	 * 回傳型別是 Member，不代表所有欄位都已從資料庫載入。
	 * 本方法不驗證呼叫者是否有權讀取這位會員。
	 *
	 * @param memberId 要查詢的會員 ID，本方法不另外驗證是否為正數
	 * @return 查到的會員 Entity；查無資料為 null
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public Member findById(int memberId) throws SQLException {
		return memberDAO.findById(memberId);
	}

	/**
	 * 查詢 Email 是否尚未被使用，將 DAO 的查詢結果轉成布林值。
	 *
	 * <p>雖然方法名為 findByEmail，回傳值卻不是 Member，且 true 代表查無會員。
	 * 這只是呼叫當下的檢查，不能保留該 Email，也不能防止另一個請求隨後註冊。
	 *
	 * @param email 要查詢的 Email，本方法不自行檢查格式或正規化
	 * @return 查無會員為 true；已存在為 false
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public boolean findByEmail(String email) throws SQLException {
		return memberDAO.findByEmail(email) == null;
	}

	/**
	 * 依 Email 取得包含密碼雜湊的會員資料，與回傳布林值的 findByEmail 用途不同。
	 *
	 * <p>回傳資料供內部處理，不應直接序列化為 API 回應；目前 DAO 未填入建立日期。
	 *
	 * @param email 要查詢的會員 Email
	 * @return 查到的 Member；查無資料為 null
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public Member findByMemberEmail(String email) throws SQLException {
		return memberDAO.findByEmail(email);
	}

	/**
	 * 委派 DAO 取得全部會員，供管理端進一步轉成 MemberResponse。
	 *
	 * <p>此處不執行管理者授權或分頁，呼叫入口必須自行負責權限檢查。
	 *
	 * @return 會員 Entity 清單；查無資料為空集合
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public List<Member> findAll() throws SQLException {

		return memberDAO.findAll();

	}

	/**
	 * 以姓名條件查詢會員，SQL 的 LIKE 比對及資料映射由 DAO 負責。
	 *
	 * <p>本方法不額外過濾條件或檢查角色；輸入中的 LIKE 萬用字元也不會在此被跳脫。
	 *
	 * @param name 要搜尋的姓名條件
	 * @return 符合條件的會員 Entity 清單；查無資料為空集合
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public List<Member> findByName(String name) throws SQLException {

		return memberDAO.findByName(name);

	}

	/**
	 * 委派 DAO 更新會員姓名、Email 與手機，再將影響筆數轉成業務結果。
	 *
	 * <p>目前不驗證物件、欄位或操作者權限，也不修改密碼及角色。
	 * 只有更新筆數恰為一筆才回傳 true；SQL 執行失敗會拋出例外，不會回傳 false。
	 *
	 * @param member 含目標會員 ID 與修改欄位的 Entity，不可為 null
	 * @return 影響筆數等於一為 true；其他筆數為 false
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 * @throws NullPointerException member 為 null，DAO 讀取其欄位時發生
	 */
	public boolean updateMember(Member member) throws SQLException {

		int updatedRows = memberDAO.updateMember(member);
		return updatedRows == 1;

	}

	/**
	 * 依會員 ID 委派刪除，再以影響筆數是否恰為一判斷結果。
	 *
	 * <p>本方法不檢查操作者權限或關聯借閱；若資料庫的外鍵限制拒絕刪除，例外會向上傳遞。
	 *
	 * @param memberId 要刪除的會員 ID
	 * @return 影響筆數等於一為 true；其他筆數為 false
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public boolean deleteMember(int memberId) throws SQLException {

		int updatedRows = memberDAO.deleteMember(memberId);

		return updatedRows == 1;
	}

	/**
	 * 檢查 Email 是否重複，雜湊原始密碼，再建立 Member 交由 DAO 寫入。
	 *
	 * <p>Controller 的 {@code @Valid} 才會觸發 RegisterRequest 欄位限制；
	 * 單純呼叫此 Service 方法不會自動執行同一套驗證，這裡也沒有重新檢查 DTO 的所有欄位。
	 * 目前 DAO 新增時固定寫入 USER 角色，不從註冊請求接受管理者角色。
	 *
	 * <p>先查再新增不是不可分割的操作。並行註冊可能同時通過查詢，仍需資料庫唯一約束防止重複。
	 * 若約束拒絕寫入，會拋出 SQLException，而不是自動回傳 false。
	 *
	 * @param request 包含姓名、Email、原始密碼與手機的註冊 DTO，不可為 null
	 * @return Email 已存在或新增影響筆數未大於零為 false；新增影響筆數大於零為 true
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 * @throws NullPointerException request 為 null，讀取 DTO 欄位時發生
	 * @throws IllegalArgumentException 目前的 BCrypt 工具收到 null 原始密碼時發生
	 */
	public boolean register(RegisterRequest request) throws SQLException {

		// 先提供 Email 重複的業務判斷；並行請求仍須靠資料庫唯一約束阻止重複寫入。
		if (memberDAO.findByEmail(request.getEmail()) != null) {

			return false;

		}

		// BCrypt 雜湊不可逆；資料庫保存雜湊，登入透過 matches 驗證，不保存原始密碼。
		String hashedPassword = passwordEncoder.encode(request.getPassword());

		Member member = new Member(request.getName(), request.getEmail(), hashedPassword, request.getPhone());

		return memberDAO.register(member) > 0;
	}

	/**
	 * 依 Email 查詢會員，再使用 PasswordEncoder.matches 驗證原始密碼與既有雜湊。
	 *
	 * <p>BCrypt 雜湊包含 salt，相同密碼重新 encode 後通常產生不同字串；
	 * 因此使用 matches，而不是重新雜湊後直接以字串相等判斷。
	 * 找不到會員時先回傳 null，不會繼續存取 member 的密碼欄位。
	 *
	 * <p>此方法只驗證帳密，不建立 Session，也不挑選對外輸出欄位。
	 * 登入成功的 Member 包含密碼雜湊，Controller 需轉成 UserResponse。
	 *
	 * @param request 含 Email 與原始密碼的登入 DTO，不可為 null
	 * @return 帳密驗證成功的 Member；查無會員或密碼不符為 null
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 * @throws NullPointerException request 為 null，讀取 DTO 欄位時發生
	 * @throws IllegalArgumentException 查到會員後，目前的 BCrypt 工具收到 null 原始密碼時發生
	 */
	public Member login(LoginRequest request) throws SQLException {

		// 先以 Email 取回會員與既有雜湊，密碼比對在 Java 的密碼工具內執行。
		Member member = memberDAO.findByEmail(request.getEmail());

		// 查無會員直接回傳驗證失敗，避免對 null 呼叫 getPassword。
		if (member == null) {
			return null;
		}

		// 讓密碼工具解析雜湊中的參數與 salt，驗證原始密碼，而非比較兩次 encode 的字串。
		if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			return null;
		}

		// 只回傳內部驗證結果；Session 建立與安全 DTO 轉換由 Controller 完成。
		return member;
	}

}
