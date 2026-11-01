package com.tian.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.tian.library.dto.BorrowResponse;
import com.tian.library.entity.Borrow;
import com.tian.library.entity.BorrowStatus;

@Repository
public class BorrowDAO {
	private final DataSource dataSource;

	public BorrowDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public int add(Borrow borrow) {
		String sql = """
				INSERT INTO borrows (member_id, book_id, borrow_date, due_date,status) VALUES(?,?,?,?,?);
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, borrow.getMemberId());
			pstmt.setInt(2, borrow.getBookId());
			pstmt.setDate(3, java.sql.Date.valueOf(borrow.getBorrowDate()));
			pstmt.setDate(4, java.sql.Date.valueOf(borrow.getDueDate()));
			pstmt.setString(5, borrow.getStatus().name());
			return pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int add(Connection conn, Borrow borrow) throws SQLException {
		String sql ="""
				INSERT INTO borrows (member_id, book_id, borrow_date, due_date, status) VALUES(?,?,?,?,?);
				""";
		
		try (PreparedStatement ps = conn.prepareStatement(sql)){
			ps.setInt(1, borrow.getMemberId());
			ps.setInt(2, borrow.getBookId());
			ps.setDate(3, java.sql.Date.valueOf(borrow.getBorrowDate()));
			ps.setDate(4, java.sql.Date.valueOf(borrow.getDueDate()));
			ps.setString(5, borrow.getStatus().name());
			
			return ps.executeUpdate();
		}
		
		
	}

	public Borrow findById(int borrowId) {
		String sql = """
				SELECT * FROM borrows WHERE id = ?
				 """;
		Borrow borrow = null;
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, borrowId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				java.sql.Date returnDate = rs.getDate("return_date");
				borrow = new Borrow(rs.getInt("id"), rs.getInt("member_id"), rs.getInt("book_id"),
						rs.getDate("borrow_date").toLocalDate(), rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				borrow.setStatus(calculateStatus(borrow));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return borrow;
	}
	
	public List<BorrowResponse> findByMemberId(int memberId) {
		String sql = """
				SELECT br.id, b.title as book_title, br.borrow_date, br.due_date, br.return_date, br.status
				FROM borrows br JOIN books b ON br.book_id = b.id
				WHERE br.member_id = ?
				""";
		List<BorrowResponse> borrowList = new ArrayList<BorrowResponse>();
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, memberId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				java.sql.Date returnDate = rs.getDate("return_date");
				
				BorrowResponse borrowResponse = new BorrowResponse(
						rs.getInt("id"),
						rs.getString("book_title"), 
						rs.getDate("borrow_date").toLocalDate(),
						rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				borrowResponse.setStatus(calculateStatus(borrowResponse));
				
				borrowList.add(borrowResponse);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return borrowList;
	}

	public List<Borrow> findAll() {
		String sql = """
				SELECT * FROM borrows
				""";
		List<Borrow> borrowList = new ArrayList<>();
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				java.sql.Date returnDate = rs.getDate("return_date");
				Borrow borrow = new Borrow(rs.getInt("id"), rs.getInt("member_id"), rs.getInt("book_id"),
						rs.getDate("borrow_date").toLocalDate(), rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				borrow.setStatus(calculateStatus(borrow));
				
				borrowList.add(borrow);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return borrowList;
	}

	public boolean updateStatus(int borrowId, BorrowStatus status) {
		String sql = """
				UPDATE borrows SET status = ? WHERE id = ?
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, status.name());
			pstmt.setInt(2, borrowId);
			int row = pstmt.executeUpdate();
			return row >0;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	public boolean updateReturnDate(int borrowId, LocalDate returnDate) {
		String sql = """
				UPDATE borrows SET return_date = ? WHERE id = ?
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setDate(1, java.sql.Date.valueOf(returnDate));
			pstmt.setInt(2, borrowId);
			int row = pstmt.executeUpdate();
			return row > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	public boolean returnBook(Connection conn, int borrowId, LocalDate returnDate) throws SQLException{
		String sql = """
				UPDATE borrows SET status = ? , return_date = ? WHERE id = ?
				""";
		try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, BorrowStatus.RETURNED.name());
			pstmt.setDate(2, java.sql.Date.valueOf(returnDate));
			pstmt.setInt(3, borrowId);
			int row = pstmt.executeUpdate();
			return row > 0;
		}
	}
	
	public int countActiveBorrows() {
		String sql = """
				SELECT count(*) FROM borrows WHERE return_date IS NULL
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
	
	public int countOverdueBorrows() {
		String sql = """
				SELECT count(*) FROM borrows WHERE status = "OVERDUED"
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
	
	public BorrowStatus calculateStatus(BorrowResponse borrowResponse) {
		
		if (borrowResponse.getStatus() == BorrowStatus.RETURNED) {
			return BorrowStatus.RETURNED;
		}
		
		if (LocalDate.now().isAfter(borrowResponse.getDueDate())) {
			return BorrowStatus.OVERDUE;
		}
		
		return BorrowStatus.BORROWED;
	}
	
	public BorrowStatus calculateStatus(Borrow borrow) {
		
		if (borrow.getStatus() == BorrowStatus.RETURNED) {
			return BorrowStatus.RETURNED;
		}
		
		if (LocalDate.now().isAfter(borrow.getDueDate())) {
			return BorrowStatus.OVERDUE;
		}
		
		return BorrowStatus.BORROWED;
	}
}
