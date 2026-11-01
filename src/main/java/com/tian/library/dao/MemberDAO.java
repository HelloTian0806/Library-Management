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
@Repository
public class MemberDAO {
	private final DataSource dataSource;

	public MemberDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

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
	
	public Member findById(int memberId) {


		String sql = """
				SELECT * FROM members WHERE id = ?
				""";

		Member member = null;

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {

			pstmt.setInt(1, memberId);

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
