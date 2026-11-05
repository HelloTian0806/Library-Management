package com.tian.library.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * 協調 BorrowDAO、BookService 與 MemberService，處理借還書規則及借閱查詢。
 *
 * <p>{@code @Service} 將類別註冊為 Spring Bean，依賴由建構子注入。
 * 跨多個資料寫入的借還書流程放在 Service，DAO 各自負責 SQL 與映射，避免 Controller 操作交易細節。
 *
 * <p>本類別目前使用 DataSource 取得 JDBC Connection，手動關閉自動提交並決定 commit 或 rollback，
 * 未使用 Spring 的 {@code @Transactional}。參與同一交易的 DAO 寫入必須使用同一個 Connection；
 * 僅在同一個 Java 方法依序呼叫多個 DAO，不代表這些操作就屬於同一交易。
 *
 * <p>memberId 由現有 Controller 從 Session 取得，本服務本身不讀取 Session。
 * SQLException 向上傳遞，不偽裝成查無資料；目前查詢用的逾期狀態由 DAO 動態計算。
 */
@Service
public class BorrowService {
	private final BorrowDAO borrowDAO;
	private final BookService bookService;
	private final MemberService memberService;
	private final DataSource dataSource;

	/**
	 * 由 Spring 注入借閱 DAO、書籍與會員服務，以及提供交易連線的 DataSource。
	 *
	 * @param borrowDAO 執行借閱 SQL、鎖定查詢及結果映射的元件
	 * @param bookService 查詢書籍並更新可借數量的服務
	 * @param memberService 借書前確認會員存在的服務
	 * @param dataSource 提供 JDBC Connection 的資料來源，與 DAO 使用的資料來源由 Spring 組裝
	 */
	public BorrowService(BorrowDAO borrowDAO, BookService bookService, MemberService memberService,
			DataSource dataSource) {
		this.borrowDAO = borrowDAO;
		this.bookService = bookService;
		this.memberService = memberService;
		this.dataSource = dataSource;
	}

	/**
	 * 確認會員及書籍存在後，扣減可借數量並新增借閱紀錄。
	 *
	 * <p>關閉自動提交後，兩個寫入共用 conn；只有影響筆數符合預期才 commit。
	 * 庫存已扣減但新增失敗時，以 rollback 撤銷尚未提交的庫存變更，避免只有一半資料成功。
	 * 業務條件不符且回滾成功時回傳 false；內層處理發生 SQLException 或 RuntimeException 時，
	 * 先嘗試回滾再重新拋出原始例外。
	 *
	 * <p>會員與書籍存在性查詢各自使用其他連線，不屬於 conn 的交易快照或鎖定範圍。
	 * 借閱日採用伺服器預設時區的今天，到期日為借閱日加十四個日曆日，初始狀態為 BORROWED。
	 * 本方法沒有檢查會員借閱上限或是否已借同一本書。
	 *
	 * @param memberId 呼叫端提供的會員 ID；現有 Controller 從登入 Session 取得
	 * @param bookId 要借閱的書籍 ID
	 * @return 成功提交且連線關閉未失敗為 true；會員或書籍不存在、庫存未扣減或新增筆數不是一為 false
	 * @throws SQLException 取得連線、設定交易、SQL、提交、回滾或關閉連線失敗；提交後的關閉失敗也可能向外傳遞
	 * @throws RuntimeException 執行期錯誤；若發生於內層 try，重新拋出前會嘗試 rollback
	 */
	public boolean add(int memberId, int bookId) throws SQLException {

	    // try-with-resources 會在離開區塊時關閉 Connection。
	    // 使用連線池時，close 通常表示將連線歸還連線池。
	    try (Connection conn = dataSource.getConnection()) {

	        // 關閉自動提交，讓後續使用此連線的寫入先保留在交易中，由下方統一決定結果。
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

	            // 使用同一次取得的日期計算借閱日與到期日，避免兩次取今天剛好跨日。
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

	            // 所有寫入均符合預期，才提交整筆交易；提交也會釋放此交易持有的鎖。
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
	 * 驗證借閱所有權及未歸還狀態，再更新還書資訊與可借數量。
	 *
	 * <p>DAO 使用同一交易連線執行 SELECT ... FOR UPDATE。
	 * 在支援交易與列鎖的 InnoDB 中，符合條件的紀錄會被鎖定至交易結束，
	 * 同一筆借閱的其他還書請求須等待，以免兩個請求同時通過狀態檢查而各加一次庫存。
	 * 鎖定後仍需檢查會員 ID 與狀態；列鎖本身不負責授權，也不代表可以跳過業務規則。
	 *
	 * <p>歸還日期使用伺服器預設時區的今天；借閱更新與庫存更新都須恰為一筆才提交。
	 * 庫存更新不符預期時，回滾前面的借閱更新；逾期但尚未歸還的紀錄仍可進行此流程。
	 * 目前只有借閱者本人能還書，本方法沒有管理者代還的分支。
	 *
	 * @param borrowId 要歸還的借閱紀錄 ID
	 * @param memberId 呼叫端提供的操作者會員 ID；現有 Controller 從 Session 取得
	 * @return 成功提交且連線關閉未失敗為 true；查無紀錄、非本人、已歸還或任一更新筆數不是一為 false
	 * @throws SQLException 取得連線、設定交易、SQL、提交、回滾或關閉連線失敗；提交後的關閉失敗也可能向外傳遞
	 * @throws RuntimeException 執行期錯誤；若發生於內層 try，重新拋出前會嘗試 rollback
	 */
	public boolean returnBook(int borrowId, int memberId)
	        throws SQLException {

	    try (Connection conn = dataSource.getConnection()) {

	        // 關閉自動提交，讓後續使用此連線的寫入先保留在交易中，由下方統一決定結果。
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

	            // 權限與狀態檢查都通過後，才以同一連線更新狀態及歸還日期。
	            int updatedRows = borrowDAO.returnBook(
	                    conn,
	                    borrowId,
	                    LocalDate.now()
	            );

	            if (updatedRows != 1) {
	                conn.rollback();
	                return false;
	            }

	            // 保留影響筆數，零筆不一定是 SQL 例外，卻代表本次還書尚未完整成功。
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

	            // 所有寫入均符合預期，才提交整筆交易；提交也會釋放此交易持有的鎖。
	            conn.commit();
	            return true;

	        } catch (SQLException | RuntimeException e) {
	            rollbackPreservingException(conn, e);
	            throw e;
	        }
	    }
	}

	/**
	 * 在錯誤處理路徑嘗試回滾，保留原始例外作為主要失敗原因。
	 *
	 * <p>若 rollback 又拋出 SQLException，以 addSuppressed 附加到原始例外。
	 * 呼叫端隨後重新拋出原始例外，因此日誌仍能看見原始 SQL 或程式錯誤，以及附加的回滾失敗。
	 * 嘗試回滾不代表回滾一定成功，也不能撤銷已經成功提交的交易。
	 *
	 * @param conn 本次交易使用且尚由呼叫端管理的連線
	 * @param originalException 最初的失敗原因，必要時會附加回滾例外，但此方法本身不重新拋出它
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
	 * 依借閱 ID 委派查詢 Entity，不在此檢查操作者所有權。
	 *
	 * <p>DAO 根據日期動態計算回傳物件的狀態，不會因此更新資料庫中的狀態欄位。
	 *
	 * @param borrowId 要查詢的借閱紀錄 ID，不是書籍 ID
	 * @return 查到的 Borrow；查無資料為 null
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public Borrow findById(int borrowId) throws SQLException{
		return borrowDAO.findById(borrowId);
	}
	
	/**
	 * 取得特定會員的借閱 DTO，書名由 DAO 的 JOIN 查詢一併提供。
	 *
	 * <p>目前 Controller 從 Session 傳入 memberId；本方法本身不驗證呼叫者身分。
	 * 回傳欄位由 BorrowResponse 定義，無須在 Controller 再逐筆查詢書名。
	 *
	 * @param memberId 要查詢借閱紀錄的會員 ID
	 * @return 該會員的借閱 DTO 清單；查無資料為空集合
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public List<BorrowResponse> findMemberById(int memberId) throws SQLException{
		return borrowDAO.findByMemberId(memberId);
	}

	/**
	 * 取得管理端借閱 DTO 清單，書名與會員姓名由 DAO 的 JOIN 查詢提供。
	 *
	 * <p>本方法不判斷操作者角色，現有管理端 Controller 必須先完成管理者授權再呼叫。
	 *
	 * @return 管理端借閱 DTO 清單；查無資料為空集合
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public List<AdminBorrowResponse> findAll() throws SQLException{
		return borrowDAO.findAll();
	}
	
	/**
	 * 依會員姓名條件委派查詢管理端借閱 DTO。
	 *
	 * <p>搜尋姓名是查詢條件，不是操作者身分；管理者驗證由現有 Controller 執行。
	 * DAO 負責 LIKE 搜尋、JOIN 與狀態計算，本方法不另作分頁或資料轉換。
	 *
	 * @param name 要搜尋的會員姓名條件
	 * @return 符合條件的管理端借閱 DTO 清單；查無資料為空集合
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public List<AdminBorrowResponse> findByMemberName(String name) throws SQLException{
		return borrowDAO.findByMemberName(name);
	}

}
