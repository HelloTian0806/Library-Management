package com.tian.library.service;

import java.sql.SQLException;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BookDAO;
import com.tian.library.dao.BorrowDAO;
import com.tian.library.dao.MemberDAO;
import com.tian.library.dto.DashboardStatsResponse;


/**
 * 彙整 BookDAO、MemberDAO 與 BorrowDAO 的查詢結果，建立儀表板統計 DTO。
 * Service 負責組合資料，SQL 的統計條件由各 DAO 定義。
 * {@code @Service} 將此類別註冊為 Spring Bean，依賴由 constructor injection 提供。
 */
@Service
public class DashboardService {
	private final BookDAO bookDAO;
	private final BorrowDAO borrowDAO;
	private final MemberDAO memberDAO;
	
	/**
	 * 透過 constructor injection 取得所需依賴，便於由 Spring 組裝並在測試中替換。
	 * @param bookDAO 此 Service 協調使用的資料存取或業務元件
	 * @param borrowDAO 此 Service 協調使用的資料存取或業務元件
	 * @param memberDAO 此 Service 協調使用的資料存取或業務元件
	 */
	public DashboardService(BookDAO bookDAO, BorrowDAO borrowDAO, MemberDAO memberDAO) {
		this.bookDAO = bookDAO;
		this.borrowDAO = borrowDAO;
		this.memberDAO = memberDAO;
	}
	
	/**
	 * 依序取得四項統計並建立 Response DTO，避免由 Controller 分別呼叫多個 DAO。
	 * 各統計使用獨立查詢，並非同一 Transaction 的一致快照；資料變動時數值可能對應不同時間點。
	 * DAO 某些查詢失敗時會回傳零，此處無法區分真正的零與查詢失敗。
	 * @return 彙整四項數量的 DashboardStatsResponse
	 */
	public DashboardStatsResponse stats() throws SQLException{
		 
		int countBooks = bookDAO.countBooks();
		int countMembers = memberDAO.countMembers();
		int countActiveBorrows = borrowDAO.countActiveBorrows();
		int countOverdueBorrows = borrowDAO.countOverdueBorrows();
		
		return new DashboardStatsResponse(countBooks, countMembers, countActiveBorrows, countOverdueBorrows);
	}
}
