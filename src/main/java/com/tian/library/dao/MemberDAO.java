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
@Repository
public class MemberDAO {
	private final DataSource dataSource;

	public MemberDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public int addMember(Member member) {
		// 1. 撰寫 INSERT SQL
		String sql = """
				INSERT INTO members(name,email,password,phone)
				VALUES(?,?,?,?)
				""";
		// 2. 取得資料庫連線(Connection)
		// 3. 建立 PreparedStatement
		// 4. 設定 SQL 參數
		// 5. 執行 executeUpdate()
		// 6. 判斷是否新增成功
		// 7. 發生 SQLException 時處理例外
		// 8. 回傳新增結果
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

	public Member findById(int memberId) {

		// 1. 撰寫 SELECT SQL
		String sql = """
				SELECT * FROM members WHERE id = ?
				""";
		// 2. 建立 Member 物件(初始為 null)
		Member member = null;
		// 3. 取得資料庫連線(Connection)
		// 4. 建立 PreparedStatement
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			// 5. 設定查詢參數
			pstmt.setInt(1, memberId);
			// 6. 執行 executeQuery()
			ResultSet rs = pstmt.executeQuery();
			// 7. 如果有資料，將 ResultSet 轉成 Member 物件
			if (rs.next()) {
				member = new Member();
				member.setId(rs.getInt("id"));
				member.setName(rs.getString("name"));
				member.setEmail(rs.getString("email"));
				member.setPhone(rs.getString("phone"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// 8. 回傳 Member
		System.out.println(member);
		return member;

	}

	public List<Member> findAll() {

		// 1. 撰寫 SELECT SQL
		String sql = """
				SELECT * FROM members
				 """;
		// 2. 建立 List<Member>
		List<Member> memberList = new ArrayList<>();
		// 3. 取得資料庫連線(Connection)
		// 4. 建立 PreparedStatement
		// 5. 執行 executeQuery()
		// 6. 逐筆讀取 ResultSet
		// 7. 將每筆資料轉成 Member 並加入 List
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			ResultSet rs = pstmt.executeQuery();
			if(rs.next()) {
				Member member = new Member(rs.getInt("id"),rs.getString("name"),rs.getString("email"),rs.getString("phone"));
				memberList.add(member);				
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
		// 8. 回傳 List<Member>
		return memberList;
	}

	public boolean updateMember(Member member) {

		// 1. 撰寫 UPDATE SQL

		// 2. 取得資料庫連線(Connection)

		// 3. 建立 PreparedStatement

		// 4. 設定更新資料

		// 5. 執行 executeUpdate()

		// 6. 判斷是否更新成功

		// 7. 回傳更新結果
		return true;
	}

	public boolean deleteMember(int memberId) {

		// 1. 撰寫 DELETE SQL

		// 2. 取得資料庫連線(Connection)

		// 3. 建立 PreparedStatement

		// 4. 設定刪除條件

		// 5. 執行 executeUpdate()

		// 6. 判斷是否刪除成功

		// 7. 回傳刪除結果
		return true;
	}
}
