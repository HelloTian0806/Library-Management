package com.tian.library.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BorrowDAO;
import com.tian.library.dto.AdminBorrowResponse;
import com.tian.library.dto.BorrowResponse;
import com.tian.library.entity.Book;
import com.tian.library.entity.Borrow;
import com.tian.library.entity.BorrowStatus;

@Service
public class BorrowService {
	private final BorrowDAO borrowDAO;
	private final BookService bookService;
	private final MemberService memberService;
	private final DataSource dataSource;

	public BorrowService(BorrowDAO borrowDAO, BookService bookService, MemberService memberService,
			DataSource dataSource) {
		this.borrowDAO = borrowDAO;
		this.bookService = bookService;
		this.memberService = memberService;
		this.dataSource = dataSource;
	}

	public boolean add(int memberId, int bookId) throws SQLException {
		// 統一bookService.decAvailableQuantity和borrowDAO.add使用同一個Connection如果其中一個失敗就回朔
		Connection conn = null;

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);

			if (memberService.findById(memberId) == null) {
				System.out.println("找不到ID");
				return false;
			}

			Book book = bookService.findById(bookId);
			if (book == null) {
				System.out.println("找不到書");
				return false;
			}

			bookService.decAvailableQuantity(conn, bookId);

			LocalDate borrowDate = LocalDate.now();

			Borrow borrow = new Borrow(memberId, bookId, borrowDate, borrowDate.plusDays(14), BorrowStatus.BORROWED);
			borrowDAO.add(conn, borrow);

			conn.commit();
			return true;
		} catch (Exception e) {

			e.printStackTrace();

			if (conn != null) {
				conn.rollback();
			}
			return false;
		} finally {

			if (conn != null) {
				conn.close();
			}
		}

	}
	//之後可以改成@Transaction的方式
	public boolean returnBook(int borrowId) throws SQLException {
		
		Connection conn = null;

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);

			Borrow borrow = findById(borrowId);
			if (borrow == null) {
				System.out.println("查無此書");
				return false;
			}

			if (borrow.getStatus() == BorrowStatus.RETURNED) {
				System.out.println("此書已經還了");
				return false;
			}

			borrowDAO.returnBook(conn, borrowId, LocalDate.now());

			bookService.increaseAvailableQuantity(conn, borrow.getBookId());
			
			conn.commit();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			if (conn != null) {
				conn.rollback();
			}
			return false;
		}finally {
			
	        if (conn != null) {
	            conn.close();
	        }
		}

	}

	public Borrow findById(int borrowId) {
		return borrowDAO.findById(borrowId);
	}
	
	public List<BorrowResponse> findMemberById(int memberId) {
		return borrowDAO.findByMemberId(memberId);
	}

	public List<AdminBorrowResponse> findAll() {
		return borrowDAO.findAll();
	}

}
