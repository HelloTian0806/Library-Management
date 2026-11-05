package com.tian.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.tian.library.entity.Member;
import com.tian.library.entity.Role;
/**
 * 以手寫 JDBC 存取會員資料，負責 SQL、參數綁定與 ResultSet 映射，不處理 HTTP 回應。
 *
 * <p>{@code @Repository} 將類別註冊為 Spring Bean；它本身不會自動為這些手寫 JDBC 操作啟動交易。
 * DataSource 提供 Connection，代表與資料庫互動的連線；PreparedStatement 承載 SQL 與參數，
 * executeQuery 取得可逐列讀取的 ResultSet，executeUpdate 則回傳寫入的影響筆數。
 *
 * <p>SQL 使用 ? 綁定資料值，參數索引從一開始，順序須對應 SQL 中的佔位符。
 * 將輸入作為資料值綁定，可避免該值被當成 SQL 結構執行；參數綁定不能用來替換資料表或欄位名稱，
 * 也不會自動驗證業務規則或取消 LIKE 的萬用字元語意。
 *
 * <p>try-with-resources 在正常結束或拋出例外時，都會依宣告的反向順序嘗試關閉資源。
 * Statement 關閉時會關閉其目前的 ResultSet；使用連線池時，Connection.close 通常是將連線歸還池中。
 * SQLException 直接向上傳遞，不會被轉成零、null 或部分成功清單。
 *
 * <p>所有方法均自行取得並關閉 Connection，沒有接受外部交易連線的多載，也不在此建立跨方法交易。
 * 各查詢只映射明確讀取的欄位；SELECT * 不代表 Member 的所有欄位都會自動填入。
 * 角色使用 Role.valueOf 轉換，資料庫字串須與列舉名稱及大小寫相符。
 */
@Repository
public class MemberDAO {
	private final DataSource dataSource;

	/**
	 * 透過建構子取得 Spring 提供的 DataSource，將連線建立與設定交給資料來源管理。
	 *
	 * <p>建構子只保存依賴，不在建立 DAO 時取得長期共用的 Connection；各方法依需要取得或接收連線。
	 *
	 * @param dataSource 提供 JDBC 連線的資料來源；若使用連線池，池的生命週期由外部設定管理
	 */
	public MemberDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 新增會員，依順序綁定姓名、Email、密碼與手機，角色由 SQL 固定為 USER。
	 *
	 * <p>DAO 直接寫入 member.password，不會自動雜湊，現有 MemberService 會先產生 BCrypt 雜湊。
	 * 唯一約束或其他資料庫限制若拒絕新增，會拋出 SQLException，不會以零值隱藏失敗。
	 *
	 * @param member 含待寫入欄位的會員 Entity，不可為 null
	 * @return INSERT 影響筆數，不是自動產生的會員 ID
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws NullPointerException member 為 null，讀取欄位時發生
	 */
	public int register(Member member) throws SQLException{

		String sql = """
				INSERT INTO members(name,email,password,phone,role)
				VALUES(?,?,?,?,'USER')
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, member.getName());
			pstmt.setString(2, member.getEmail());
			// 此處直接寫入既有值，雜湊必須在進入 DAO 前完成，DAO 不會自動將原始密碼轉成雜湊。
			pstmt.setString(3, member.getPassword());
			pstmt.setString(4, member.getPhone());
			
			return pstmt.executeUpdate();
		} 
	}
	
	/**
	 * 以 ID 查詢第一筆會員，填入 ID、姓名、Email、手機與角色。
	 *
	 * <p>雖然 SQL 使用 SELECT *，映射程式沒有讀取密碼及建立日期，因此這兩個欄位保持預設值。
	 * ResultSet 僅負責讀資料，建立後的 Member 可在 JDBC 資源關閉後使用。
	 *
	 * @param memberId 要查詢的會員 ID
	 * @return 查到的 Member；查無資料為 null
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的角色字串無法對應 Role 時發生
	 * @throws NullPointerException 角色欄位為 SQL NULL，傳入 Role.valueOf 時發生
	 */
	public Member findById(int memberId) throws SQLException{


		String sql = """
				SELECT * FROM members WHERE id = ?
				""";

		Member member = null;

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {

			pstmt.setInt(1, memberId);

			// executeQuery 取得結果游標；必須先 next 才能讀第一筆，每次 next 移到下一筆。
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				member = new Member();
				member.setId(rs.getInt("id"));
				member.setName(rs.getString("name"));
				member.setEmail(rs.getString("email"));
				member.setPhone(rs.getString("phone"));
				member.setRole(Role.valueOf(rs.getString("role")));
			}
		} 

		return member;

	}
	
	/**
	 * 以 Email 查詢第一筆會員，包含登入比對所需的密碼雜湊。
	 *
	 * <p>SQL 參數使用 setString 綁定；映射包含 ID、姓名、Email、密碼、手機與角色，未填入建立日期。
	 * 回傳 Entity 包含敏感欄位，不能直接當成公開 API 的輸出契約。
	 * 正常路徑會顯式關閉 ResultSet；若映射中途失敗，外層資源區塊仍會嘗試關閉 Statement 及連線。
	 *
	 * @param email 要查詢的 Email
	 * @return 查到的會員；查無資料為 null
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的角色字串無法對應 Role 時發生
	 * @throws NullPointerException 角色欄位為 SQL NULL，傳入 Role.valueOf 時發生
	 */
	public Member findByEmail(String email) throws SQLException{
		String sql = """
				SELECT * FROM members WHERE email = ?
				""";
		Member member = null;
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			pstmt.setString(1, email);
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				member = new Member();
				
				member.setId(rs.getInt("id"));
				member.setName(rs.getString("name"));
				member.setEmail(rs.getString("email"));
				// 登入需要雜湊進行 matches；資料取得權限不等於允許將此欄位輸出給前端。
				member.setPassword(rs.getString("password"));
				member.setPhone(rs.getString("phone"));
				member.setRole(Role.valueOf(rs.getString("role")));
			}
			
			// 正常路徑先顯式關閉結果集；若前面拋例外，外層 Statement 關閉時也會關閉目前結果集。
			rs.close();
			
		}

		return member;
				
	}
	
	/**
	 * 以姓名 LIKE 條件搜尋會員，映射包含建立日期但不填入密碼。
	 *
	 * <p>在參數值前後加 % 代表包含式比對；輸入的 % 與 _ 仍是萬用字元，不會因綁定而自動跳脫。
	 * while(next()) 逐列移動游標，建立各自的 Member 後加入集合，不是把 ResultSet 直接交給上層。
	 * getDate 將 created_at 讀成日期，轉為 LocalDate 後不保留時分秒。
	 *
	 * @param name 姓名搜尋條件
	 * @return 符合條件的 Member 清單；查無資料為空集合，處理中途失敗則拋出例外
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的角色字串無法對應 Role 時發生
	 * @throws NullPointerException created_at 或角色為 SQL NULL，轉換日期或列舉時發生
	 */
	public List <Member> findByName(String name) throws SQLException{


		String sql = """
				SELECT * FROM members WHERE name LIKE ?
				""";

			
		
		List<Member> memberList = new ArrayList<>();
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {

			pstmt.setString(1, "%" + name + "%");

			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				Member member = new Member(
						rs.getInt("id"),
						rs.getString("name"),
						rs.getString("email"),
						rs.getString("phone"),
						// getDate 僅保留日期，因此這個映射不保留 timestamp 的時分秒。
						rs.getDate("created_at").toLocalDate(),
						Role.valueOf(rs.getString("role")));

				memberList.add(member);
			}
			
			// 正常路徑先顯式關閉結果集；若前面拋例外，外層 Statement 關閉時也會關閉目前結果集。
			rs.close();
			
		} 
		
		return memberList;

	}

	/**
	 * 查詢全部會員，映射 ID、姓名、Email、手機、建立日期與角色，不填入密碼。
	 *
	 * <p>三個 JDBC 資源都列入 try-with-resources，會先關閉 ResultSet，再關閉 Statement 與連線。
	 * SQL 沒有 ORDER BY 或分頁；建立日期以 LocalDate 表示，不保留時間資訊。
	 *
	 * @return Member 清單；查無資料為空集合，SQL 失敗不會回傳已累積的部分清單
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的角色字串無法對應 Role 時發生
	 * @throws NullPointerException created_at 或角色為 SQL NULL，轉換日期或列舉時發生
	 */
	public List<Member> findAll() throws SQLException{


		String sql = """
				SELECT * FROM members
				 """;

		List<Member> memberList = new ArrayList<>();

		try(Connection conn = dataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery()){
			
			while(rs.next()) {
				
				Member member = new Member(
						rs.getInt("id"),
						rs.getString("name"),
						rs.getString("email"),
						rs.getString("phone"),
						// getDate 僅保留日期，因此這個映射不保留 timestamp 的時分秒。
						rs.getDate("created_at").toLocalDate(),
						Role.valueOf(rs.getString("role")));
				memberList.add(member);				
			}
		}
		
		return memberList;
	}

	/**
	 * 以會員 ID 更新姓名、Email 與手機，不更新密碼、角色或建立日期。
	 *
	 * <p>前三個參數對應 SET 欄位，第四個是 WHERE ID。DAO 不自行執行欄位驗證或授權。
	 * 回傳 JDBC 回報的筆數，由 Service 判斷是否符合預期；資料庫錯誤不會在此轉成 false。
	 *
	 * @param member 含目標會員 ID 與修改欄位的 Entity，不可為 null
	 * @return UPDATE 影響筆數，本方法不轉成 boolean
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws NullPointerException member 為 null，讀取欄位時發生
	 */
	public int updateMember(Member member) throws SQLException{

		String sql = """
				UPDATE members
				SET name = ?, email = ? , phone = ?
				WHERE id = ?
				""";
		
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			pstmt.setString(1, member.getName());
			pstmt.setString(2, member.getEmail());
			pstmt.setString(3, member.getPhone());
			pstmt.setInt(4, member.getId());
			
			int rows = pstmt.executeUpdate();
			
			return rows;
		}
		
	}

	/**
	 * 綁定會員 ID 後刪除對應資料，不自行檢查操作者權限或借閱狀態。
	 *
	 * <p>如果 Schema 的外鍵限制拒絕刪除，會拋出 SQLException；不在此額外刪除關聯借閱。
	 *
	 * @param memberId 要刪除的會員 ID
	 * @return DELETE 影響筆數；沒有符合 ID 的資料時為零
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public int deleteMember(int memberId) throws SQLException{
		
		String sql = """
				DELETE FROM members WHERE id = ?
				""";

		try(Connection conn = dataSource.getConnection(); PreparedStatement ptsmt = conn.prepareStatement(sql)){
			
			ptsmt.setInt(1, memberId);
			
			int rows = ptsmt.executeUpdate();
			
			return rows;
			
		}


	}
	
	/**
	 * 計算所有會員筆數，不依角色篩選，並以 SQL 別名 total 讀取結果。
	 *
	 * <p>COUNT(*) 沒有 GROUP BY，空資料表通常仍有一列結果，值為零。
	 * 若連結果列都沒有，這個方法會主動拋出 SQLException，與其他回傳零作備援的統計方法不同。
	 *
	 * @return 會員資料列數，包含各種角色
	 * @throws SQLException JDBC 操作或資源關閉失敗，或查詢未回傳預期的統計結果列
	 */
	public int countMembers() throws SQLException {
	    String sql = "SELECT COUNT(*) AS total FROM members";

	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql);
	         ResultSet rs = pstmt.executeQuery()) {

	        if (rs.next()) {
	            return rs.getInt("total");
	        }

	        // COUNT 應有統計結果列；缺少整列結果與真正的零筆會員不同，不能混為一談。
	        throw new SQLException("會員數統計未回傳結果列");
	    }
	}
}
