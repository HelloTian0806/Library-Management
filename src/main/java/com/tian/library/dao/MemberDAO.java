package com.tian.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.tian.library.entity.Role;
import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.tian.library.entity.Member;
/**
 * 以 JDBC 存取會員資料，負責 SQL、參數綁定與 ResultSet 映射，供 Service 呼叫。
 * {@code @Repository} 將 DAO 註冊為 Spring Bean，但不會自動替手寫 JDBC 開始 Transaction。
 * PreparedStatement 的 ? 將資料與 SQL 結構分離，避免直接串接輸入造成 SQL injection；參數索引從 1 開始。
 * 自行取得 Connection 的方法用 try-with-resources 關閉資源；關閉 Statement 也會關閉其目前的 ResultSet。
 * 部分方法攔截 SQLException 後僅印出堆疊，因此零值、null 或空集合可能同時代表失敗，並非只有查無資料。
 */
@Repository
public class MemberDAO {
	private final DataSource dataSource;

	/**
	 * 透過 constructor injection 取得 DataSource，由連線池統一管理 Connection。
	 * @param dataSource Spring 設定的 JDBC 資料來源
	 */
	public MemberDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 新增會員，固定以 USER 角色寫入；密碼欄位預期已由 Service 產生雜湊。
	 * 不在 DAO 雜湊原始密碼，也不在這裡決定 HTTP 回應；Email 重複仍須由資料庫唯一約束防護。
	 * @param member 包含待寫入欄位的會員 Entity
	 * @return executeUpdate 的影響筆數；SQLException 被攔截時回傳 0
	 */
	public int register(Member member) {

		String sql = """
				INSERT INTO members(name,email,password,phone,role)
				VALUES(?,?,?,?,'USER')
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, member.getName());
			pstmt.setString(2, member.getEmail());
			pstmt.setString(3, member.getPassword());
			pstmt.setString(4, member.getPhone());
			int result = pstmt.executeUpdate();
			if (result > 0) {
				return result;
			} else {
				System.out.println("無法新增");
				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;

	}
	
	/**
	 * 依會員 ID 查詢；目前映射 id、name、email、phone、role，不填入 password 與 createdAt。
	 * @param memberId 會員 ID
	 * @return 找到的 Entity；查無資料或部分 SQL 失敗情況為 null
	 */
	public Member findById(int memberId) {


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
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return member;

	}
	
	/**
	 * 依 Email 查詢會員，包含登入驗證所需的密碼雜湊；不可直接將結果當作公開 JSON 回應。
	 * SQL 雖使用 SELECT *，目前沒有將 created_at 映射至 Entity。
	 * @param email 會員 Email
	 * @return 找到的會員，查無資料為 null
	 * @throws IllegalStateException JDBC 查詢失敗時包裝 SQLException 並保留原因
	 */
	public Member findByEmail(String email) {
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
			
			rs.close();
			
		}catch (SQLException e) {
			throw new IllegalStateException("查詢會員Email失敗", e);
		}
		
		return member;
				
	}
	
	/**
	 * 使用 LIKE 搜尋姓名並映射 Member，包含加入日期但不填入密碼雜湊。
	 * 輸入中的 % 與 _ 仍會作為 LIKE 萬用字元；參數綁定不會自動取消其搜尋語意。
	 * @param name 姓名搜尋條件
	 * @return 結果清單；查無資料為空集合，SQL 失敗時也可能回傳空或已映射的部分清單
	 * @throws NullPointerException created_at 為 SQL NULL 時，直接呼叫 toLocalDate 會失敗
	 */
	public List <Member> findByName(String name) {


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
			
			rs.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return memberList;

	}

	/**
	 * 查詢全部會員並映射姓名、Email、電話、加入日期與角色，不填入密碼雜湊。
	 * @return 結果清單；查無資料為空集合，SQL 失敗時也可能回傳空或已映射的部分清單
	 * @throws NullPointerException created_at 為 SQL NULL 時，直接呼叫 toLocalDate 會失敗
	 */
	public List<Member> findAll() {


		String sql = """
				SELECT * FROM members
				 """;

		List<Member> memberList = new ArrayList<>();

		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			ResultSet rs = pstmt.executeQuery();
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
			
			rs.close();
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return memberList;
	}

	/**
	 * 依會員 ID 更新姓名、Email 與電話，不更新密碼、角色或加入日期。
	 * @param member 包含待寫入欄位的會員 Entity
	 * @return 影響筆數大於零時為 true，否則為 false；SQLException 被攔截時也回傳 false
	 */
	public boolean updateMember(Member member) {

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
			
			return rows > 0;
		}catch (SQLException e) {
			
			e.printStackTrace();
			
		}
		
		return false;
	}

	/**
	 * 依會員 ID 刪除；資料庫若限制刪除仍有借閱關聯的會員，操作可能失敗。
	 * @param memberId 會員 ID
	 * @return 影響筆數大於零時為 true，否則為 false；SQLException 被攔截時也回傳 false
	 */
	public boolean deleteMember(int memberId) {
		
		String sql = """
				DELETE FROM members WHERE id = ?
				""";

		try(Connection conn = dataSource.getConnection(); PreparedStatement ptsmt = conn.prepareStatement(sql)){
			
			ptsmt.setInt(1, memberId);
			
			int rows = ptsmt.executeUpdate();
			
			return rows > 0;
			
		}catch (SQLException e) {
			
			e.printStackTrace();
			
		}
		
		return false;
	}
	
	/**
	 * 計算所有會員筆數，SQL 沒有依角色或其他狀態篩選。
	 * @return SQL 統計值；SQL 失敗或未取得結果時也回傳 0
	 */
	public int countMembers() {
		String sql = """
				SELECT count(*) FROM members
				 """;
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				return rs.getInt("count(*)");
			}
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
}
