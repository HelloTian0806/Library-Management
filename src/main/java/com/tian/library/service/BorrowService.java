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

/**
 * 協調 BorrowDAO、BookService 與 MemberService，完成借書、還書及借閱查詢。
 * 目前直接透過 DataSource 管理 JDBC Transaction，尚未使用 Spring 宣告式交易。
 * {@code @Service} 將此類別註冊為 Spring Bean，依賴由 constructor injection 提供。
 */
@Service
public class BorrowService {
	private final BorrowDAO borrowDAO;
	private final BookService bookService;
	private final MemberService memberService;
	private final DataSource dataSource;

	/**
	 * 透過 constructor injection 取得所需依賴，便於由 Spring 組裝並在測試中替換。
	 * @param borrowDAO 此 Service 協調使用的資料存取或業務元件
	 * @param bookService 此 Service 協調使用的資料存取或業務元件
	 * @param memberService 此 Service 協調使用的資料存取或業務元件
	 * @param dataSource 提供 JDBC Connection 的資料來源
	 */
	public BorrowService(BorrowDAO borrowDAO, BookService bookService, MemberService memberService,
			DataSource dataSource) {
		this.borrowDAO = borrowDAO;
		this.bookService = bookService;
		this.memberService = memberService;
		this.dataSource = dataSource;
	}

	/**
	 * 確認會員與書籍存在，使用同一 Connection 扣庫存並新增借閱，將到期日設為借閱日後 14 天。
	 * 目前忽略扣庫存的布林結果與新增筆數，因此沒有拋出例外不等於所有業務步驟都成功。
	 * 會員與書籍查詢使用各自的連線，不屬於這裡手動開啟的 Transaction。
	 * @param memberId 會員 ID
	 * @param bookId 書籍 ID
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 * @throws SQLException rollback 或 close 失敗時仍可能向外傳遞
	 */
	public boolean add(int memberId, int bookId) throws SQLException {
		// 寫入共用 Connection 才能一起提交或回滾；回傳 false 並不會自動觸發 rollback。
		Connection conn = null;

		try {
			// 向連線池借用 Connection；finally 中 close 通常是歸還連線池。
			conn = dataSource.getConnection();
			// 停用每個 SQL 自動提交，讓後續寫入由同一 Transaction 控制。
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

			// 提交目前 Connection 的寫入；commit 本身不會驗證前面忽略的更新結果。
			conn.commit();
			return true;
			// 借書攔截所有 Exception，印出堆疊並嘗試回滾，正常回滾後以 false 表示失敗。
		} catch (Exception e) {

			e.printStackTrace();

			if (conn != null) {
				// 撤銷目前 Connection 尚未提交的寫入，不影響其他獨立連線的操作。
				conn.rollback();
			}
			return false;
		} finally {

			if (conn != null) {
				// 即使提早 return 或發生例外，finally 仍會執行以釋放 Connection。
				conn.close();
			}
		}

	}
	// 若未來改用 Spring 的 @Transactional，DAO 也必須透過能參與 Spring Transaction 的方式取得連線。
	/**
	 * 查詢借閱並排除已歸還狀態，使用同一 Connection 更新還書日期與可借數量。
	 * 目前查詢使用另一條連線，且忽略更新結果；此流程沒有鎖定或以條件更新防止同時重複還書。
	 * @param borrowId 借閱紀錄 ID，不是書籍 ID
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 * @throws SQLException rollback 或 close 失敗時仍可能向外傳遞
	 */
	public boolean returnBook(int borrowId) throws SQLException {
		
		Connection conn = null;

		try {
			// 向連線池借用 Connection；finally 中 close 通常是歸還連線池。
			conn = dataSource.getConnection();
			// 停用每個 SQL 自動提交，讓後續寫入由同一 Transaction 控制。
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
			
			// 提交目前 Connection 的寫入；commit 本身不會驗證前面忽略的更新結果。
			conn.commit();
			return true;
			// 還書只攔截 SQLException；其他執行期例外不會進入這個 rollback 區塊。
		} catch (SQLException e) {
			e.printStackTrace();
			if (conn != null) {
				// 撤銷目前 Connection 尚未提交的寫入，不影響其他獨立連線的操作。
				conn.rollback();
			}
			return false;
		}finally {
			
	        if (conn != null) {
	            // 即使提早 return 或發生例外，finally 仍會執行以釋放 Connection。
	            conn.close();
	        }
		}

	}

	/**
	 * 依借閱紀錄 ID 查詢，狀態計算目前由 DAO 執行，不會在此寫回逾期狀態。
	 * @param borrowId 借閱紀錄 ID，不是書籍 ID
	 * @return 找到的 Entity，查無資料時為 null
	 */
	public Borrow findById(int borrowId) {
		return borrowDAO.findById(borrowId);
	}
	
	/**
	 * 查詢特定會員的借閱 DTO，DAO 透過 JOIN 取得書名並映射 BorrowResponse。
	 * @param memberId 會員 ID
	 * @return 符合條件的清單；查無結果時為空集合
	 */
	public List<BorrowResponse> findMemberById(int memberId) {
		return borrowDAO.findByMemberId(memberId);
	}

	/**
	 * 取得管理端借閱 DTO，書名與會員資料由 DAO 的 JOIN 查詢及映射提供。
	 * @return 符合條件的清單；查無結果時為空集合
	 */
	public List<AdminBorrowResponse> findAll() {
		return borrowDAO.findAll();
	}

}
