package com.tian.library.service;

import java.sql.SQLException;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BookDAO;
import com.tian.library.dao.BorrowDAO;
import com.tian.library.dao.MemberDAO;
import com.tian.library.dto.DashboardStatsResponse;


/**
 * 組合書籍、會員與借閱統計，建立供管理端使用的 DashboardStatsResponse。
 *
 * <p>{@code @Service} 使類別成為 Spring Bean，透過建構子取得三個 DAO。
 * Service 決定如何組合回應，DAO 決定 SQL 的統計條件；Controller 不需要逐一呼叫資料存取方法。
 * 本服務不自行檢查管理者角色，也沒有啟動共用交易。
 */
@Service
public class DashboardService {
	private final BookDAO bookDAO;
	private final BorrowDAO borrowDAO;
	private final MemberDAO memberDAO;
	
	/**
	 * 注入各項統計所需的 DAO，避免由 Controller 直接依賴資料庫細節。
	 *
	 * @param bookDAO 提供書目筆數的資料存取元件
	 * @param borrowDAO 提供未歸還及逾期借閱筆數的資料存取元件
	 * @param memberDAO 提供會員筆數的資料存取元件
	 */
	public DashboardService(BookDAO bookDAO, BorrowDAO borrowDAO, MemberDAO memberDAO) {
		this.bookDAO = bookDAO;
		this.borrowDAO = borrowDAO;
		this.memberDAO = memberDAO;
	}
	
	/**
	 * 依序取得四項統計，再依建構子參數順序建立回應 DTO。
	 *
	 * <p>書籍總數是 books 資料列數，不是 total_quantity 的加總；會員總數不依角色篩選。
	 * 未歸還借閱以 return_date 為 NULL 判斷；逾期另要求 due_date 早於伺服器今天，到期當天不算逾期。
	 * 逾期筆數不依賴狀態欄位是否已寫成 OVERDUE，且逾期紀錄也包含在未歸還筆數內。
	 *
	 * <p>各 DAO 自行取得連線，四次查詢不是同一交易快照；並行借還書時，結果可能來自不同時間點。
	 * 任何查詢拋出 SQLException 時會中斷後續流程，不會建立部分成功的 DTO，也不在此以零值掩蓋錯誤。
	 *
	 * @return 依序包含書目總數、會員總數、未歸還借閱數及逾期借閱數的 DTO
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public DashboardStatsResponse stats() throws SQLException{
		 
		int countBooks = bookDAO.countBooks();
		int countMembers = memberDAO.countMembers();
		int countActiveBorrows = borrowDAO.countActiveBorrows();
		int countOverdueBorrows = borrowDAO.countOverdueBorrows();
		
		return new DashboardStatsResponse(countBooks, countMembers, countActiveBorrows, countOverdueBorrows);
	}
}
