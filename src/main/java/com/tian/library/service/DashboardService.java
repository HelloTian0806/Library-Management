package com.tian.library.service;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BookDAO;
import com.tian.library.dao.BorrowDAO;
import com.tian.library.dao.MemberDAO;
import com.tian.library.dto.DashboardStatsResponse;


@Service
public class DashboardService {
	private final BookDAO bookDAO;
	private final BorrowDAO borrowDAO;
	private final MemberDAO memberDAO;
	
	public DashboardService(BookDAO bookDAO, BorrowDAO borrowDAO, MemberDAO memberDAO) {
		this.bookDAO = bookDAO;
		this.borrowDAO = borrowDAO;
		this.memberDAO = memberDAO;
	}
	
	public DashboardStatsResponse stats() {
		 
		int countBooks = bookDAO.countBooks();
		int countMembers = memberDAO.countMembers();
		int countActiveBorrows = borrowDAO.countActiveBorrows();
		int countOverdueBorrows = borrowDAO.countOverdueBorrows();
		
		return new DashboardStatsResponse(countBooks, countMembers, countActiveBorrows, countOverdueBorrows);
	}
}
