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
	 * 確認會員與書籍存在後，扣減庫存並新增借閱紀錄。
	 *
	 * <p>兩個寫入使用同一 Connection，只有全部成功才 commit；
	 * 任一步驟失敗時，撤銷本次 Transaction 的寫入。
	 *
	 * @param memberId 後端驗證過的登入會員 ID
	 * @param bookId 要借閱的書籍 ID
	 * @return 成功為 true；會員不存在、書籍不存在、庫存不足
	 *         或新增筆數不符合預期時為 false
	 * @throws SQLException JDBC 操作、提交、回滾或關閉資源失敗
	 * @throws RuntimeException 執行期錯誤；重新拋出前會嘗試 rollback
	 */
	public boolean add(int memberId, int bookId) throws SQLException {

	    // try-with-resources 會在離開區塊時關閉 Connection。
	    // 使用連線池時，close 通常表示將連線歸還連線池。
	    try (Connection conn = dataSource.getConnection()) {

	        conn.setAutoCommit(false);

	        try {
	            // 這兩個查詢目前各自使用其他 Connection，
	            // 只是事前檢查，不能保證資料之後不會被其他 Transaction 改動。
	            if (memberService.findById(memberId) == null) {
	                conn.rollback();
	                return false;
	            }

	            Book book = bookService.findById(bookId);

	            if (book == null) {
	                conn.rollback();
	                return false;
	            }

	            // 必須使用同一 Connection，才能與新增借閱一起回滾。
	            // 條件 UPDATE 影響零筆時不一定拋出例外，因此要檢查結果。
	            boolean stockUpdated =
	                    bookService.decAvailableQuantity(conn, bookId);

	            if (!stockUpdated) {
	                conn.rollback();
	                return false;
	            }

	            LocalDate borrowDate = LocalDate.now();

	            Borrow borrow = new Borrow(
	                    memberId,
	                    bookId,
	                    borrowDate,
	                    borrowDate.plusDays(14),
	                    BorrowStatus.BORROWED
	            );

	            int insertedRows = borrowDAO.add(conn, borrow);

	            if (insertedRows != 1) {
	                // 新增失敗時，也必須撤銷前面的庫存扣減。
	                conn.rollback();
	                return false;
	            }

	            conn.commit();
	            return true;

	        } catch (SQLException | RuntimeException e) {
	            // 保留真正的失敗原因，交由上層統一處理。
	            rollbackPreservingException(conn, e);
	            throw e;
	        }
	    }
	}

	/**
	 * 驗證借閱所有權後，更新還書狀態並增加可借數量。
	 *
	 * <p>必須由 DAO 使用同一 Connection 執行 SELECT ... FOR UPDATE。
	 * 在支援列鎖的 InnoDB Transaction 中，鎖會持續至 commit 或 rollback，
	 * 讓同一筆借閱的其他還書請求等待，避免同時通過狀態檢查。
	 *
	 * @param borrowId 要歸還的借閱紀錄 ID
	 * @param memberId 後端驗證過的登入會員 ID，不接受前端自行指定
	 * @return 成功為 true；查無紀錄、非本人、已歸還
	 *         或更新筆數不符合預期時為 false
	 * @throws SQLException JDBC 操作、提交、回滾或關閉資源失敗
	 * @throws RuntimeException 執行期錯誤；重新拋出前會嘗試 rollback
	 */
	public boolean returnBook(int borrowId, int memberId)
	        throws SQLException {

	    try (Connection conn = dataSource.getConnection()) {

	        conn.setAutoCommit(false);

	        try {
	            // DAO 必須使用傳入的 conn，不可自行另開 Connection，
	            // 也不可在方法結束時關閉這個 Connection。
	            Borrow borrow =
	                    borrowDAO.findByIdForUpdate(conn, borrowId);

	            if (borrow == null) {
	                conn.rollback();
	                return false;
	            }

	            // 保留目前政策：只有借閱者本人可以還書。
	            // 此判斷必須在任何寫入之前執行。
	            if (borrow.getMemberId() != memberId) {
	                conn.rollback();
	                return false;
	            }

	            // 鎖定後才檢查狀態。
	            // 若前一個請求已完成還書，後一個請求應讀到 RETURNED。
	            if (borrow.getStatus() == BorrowStatus.RETURNED) {
	                conn.rollback();
	                return false;
	            }

	            int updatedRows = borrowDAO.returnBook(
	                    conn,
	                    borrowId,
	                    LocalDate.now()
	            );

	            if (updatedRows != 1) {
	                conn.rollback();
	                return false;
	            }

	            int bookUpdated =
	                    bookService.increaseAvailableQuantity(
	                            conn,
	                            borrow.getBookId()
	                    );

	            if (bookUpdated != 1) {
	                // 庫存沒有成功增加，就撤銷前面的還書更新。
	                conn.rollback();
	                return false;
	            }

	            conn.commit();
	            return true;

	        } catch (SQLException | RuntimeException e) {
	            rollbackPreservingException(conn, e);
	            throw e;
	        }
	    }
	}

	/**
	 * 發生例外後嘗試回滾，同時保留原始錯誤。
	 *
	 * <p>如果 rollback 也失敗，將第二個錯誤附加到原始例外，
	 * 避免只看見回滾錯誤，卻遺失最初的 SQL 或程式錯誤。
	 *
	 * @param conn 本次 Transaction 使用的 Connection
	 * @param originalException 最初發生、稍後將重新拋出的例外
	 */
	private void rollbackPreservingException(
	        Connection conn,
	        Exception originalException) {

	    try {
	        conn.rollback();
	    } catch (SQLException rollbackException) {
	        originalException.addSuppressed(rollbackException);
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
	
	public List<AdminBorrowResponse> findByMemberName(String name) {
		return borrowDAO.findByMemberName(name);
	}

}
