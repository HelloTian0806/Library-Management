package com.tian.library.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BookDAO;
import com.tian.library.entity.Book;
/**
 * 處理書籍欄位驗證、ISBN 重複判斷及庫存操作，將 SQL 執行交給 BookDAO。
 *
 * <p>{@code @Service} 使類別成為 Spring Bean，建構子注入提供所需 DAO。
 * Service 決定哪些資料可寫入、如何解讀影響筆數，DAO 負責參數綁定與資料映射。
 * 本類別不操作 HTTP 或 Session，也不自行驗證管理者身分。
 *
 * <p>庫存方法提供有、無 Connection 的多載。借還書需使用呼叫端傳入的同一連線，
 * 才能與借閱紀錄一起提交或回滾；僅有 {@code @Service} 不會自動啟動交易。
 * 目前本類別沒有使用 {@code @Transactional}，也不自行執行 commit 或 rollback。
 */
@Service
public class BookService {
	private final BookDAO bookDAO;
	/**
	 * 透過建構子注入書籍 DAO，讓資料存取實作由 Spring 組裝。
	 *
	 * @param bookDAO 執行書籍 SQL、庫存條件更新與資料映射的元件
	 */
	public BookService(BookDAO bookDAO) {
		this.bookDAO = bookDAO;
	}
	/**
	 * 依序檢查物件、必要欄位、館藏量及 ISBN，再新增書籍。
	 *
	 * <p>先檢查 null 才讀取欄位，字串則以短路運算避免在 null 上呼叫 isBlank。
	 * ISBN、書名、作者不可空白，館藏量須大於零；目前不驗證 ISBN 格式與檢查碼。
	 * 通過檢查後以館藏量覆蓋可借數量，表示新書籍尚未有借出冊數。
	 *
	 * <p>此方法會修改傳入 Book 的 availableQuantity，即使後續 DAO 寫入失敗，Java 物件也不會自動恢復。
	 * ISBN 事前查詢不能取代資料庫唯一約束，並行新增仍可能在寫入時發生衝突。
	 *
	 * @param book 欲新增的書籍 Entity，成功通過檢查後其可借數量會被重設
	 * @return 檢查未通過或新增影響筆數未大於零為 false；新增影響筆數大於零為 true
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public boolean add(Book book) throws SQLException{
		
		if (book == null) {
			return false;
		}
		
		if(book.getIsbn()==null || book.getIsbn().isBlank()) {
			return false;
		}
		if(book.getTitle()==null || book.getTitle().isBlank()) {
			return false;
		}
		if (book.getAuthor() == null || book.getAuthor().isBlank()) {
		    return false;
		}
		if(book.getTotalQuantity()<=0) {
			return false;
		}
		// 事前查詢讓重複 ISBN 能以業務結果回報，但無法阻止查詢後才發生的並行新增。
		if(bookDAO.findByIsbn(book.getIsbn())!=null) {
			return false;
		}
		// 新書尚無借出紀錄，因此忽略請求傳入的可借數量，統一以館藏量初始化。
		book.setAvailableQuantity(book.getTotalQuantity());

		return bookDAO.add(book) > 0;
	
	}
	/**
	 * 驗證更新資料、確認書籍存在，並排除與其他書籍重複的 ISBN。
	 *
	 * <p>目前只驗證正數 ID 與 ISBN、書名、作者非空白。
	 * 相同 ISBN 若屬於本書可繼續更新，若屬於其他 ID 則拒絕。
	 * DAO 只更新書目欄位，不更新 totalQuantity 或 availableQuantity，所以此處沒有數量調整流程。
	 *
	 * <p>存在性及 ISBN 查詢與最後的更新使用獨立 DAO 呼叫，並不是同一交易的鎖定檢查。
	 *
	 * @param book 含目標 ID 與修改書目資料的 Entity
	 * @return 驗證失敗、書籍不存在、ISBN 與他書重複或更新影響筆數未大於零為 false；否則為 true
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public boolean update(Book book) throws SQLException{
		
		if(book == null) {
			return false;
		}

		if(book.getId()<=0) {
			return false;
		}
		
		if (book.getIsbn() == null || book.getIsbn().isBlank()) {
		    return false;
		}
		

		if (book.getTitle() == null || book.getTitle().isBlank()) {
		    return false;
		}
		
		if (book.getAuthor() == null || book.getAuthor().isBlank()) {
		    return false;
		}
		Book foundBook = bookDAO.findById(book.getId());
		if(foundBook==null) {
			return false;
		}
		// 保留本書原有 ISBN 是合法的，只有查到另一個 ID 使用相同 ISBN 才拒絕。
		Book sameIsbn = bookDAO.findByIsbn(book.getIsbn());
		if(sameIsbn != null && sameIsbn.getId() != book.getId()) {
			return false;
		}


		return bookDAO.updateBook(book)>0;
	}
	/**
	 * 依書籍 ID 委派刪除，將影響筆數是否大於零轉成布林結果。
	 *
	 * <p>目前未先查詢借閱狀態，也未檢查操作者角色；關聯限制依資料庫設定執行。
	 *
	 * @param bookId 要刪除的書籍 ID
	 * @return 刪除影響筆數大於零為 true；否則為 false
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public boolean delete(int bookId) throws SQLException{
		return bookDAO.deleteBook(bookId) > 0;
	}
	
	/**
	 * 委派 DAO 以 LIKE 進行書名包含式搜尋，不在此加入分頁或條件清理。
	 *
	 * @param title 書名搜尋條件，LIKE 萬用字元的語意由 DAO 的 SQL 決定
	 * @return 符合條件的 Book 清單；查無資料為空集合
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public List<Book> findByTitle(String title) throws SQLException{
		return bookDAO.findByTitle(title);
	}
	
	/**
	 * 依 ISBN 查詢書籍 Entity，可用於書籍查詢或重複性判斷。
	 *
	 * @param isbn 要查詢的 ISBN，本方法不自行驗證格式
	 * @return 查到的 Book；查無資料為 null
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public Book findByIsbn(String isbn) throws SQLException{
		return bookDAO.findByIsbn(isbn);
	}
	/**
	 * 委派 DAO 取得全部書籍，回傳 Entity 而非另建 Response DTO。
	 *
	 * @return 全部 Book 的清單；查無資料為空集合
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public List<Book> findAll() throws SQLException{
		return bookDAO.findAll();
	}
	
	/**
	 * 依 ID 取得書籍，供查詢及借書前的存在性檢查使用。
	 *
	 * @param bookId 要查詢的書籍 ID
	 * @return 查到的 Book；查無資料為 null
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public Book findById(int bookId) throws SQLException{
		return bookDAO.findById(bookId);
	}
	
	/**
	 * 使用 DAO 自行取得的連線，在可借數量大於零時扣減一冊。
	 *
	 * <p>以資料庫條件 UPDATE 決定是否可扣減，避免先查庫存後以舊數值寫回的競爭。
	 * 此多載不接受交易連線，不能自動與另一個 DAO 呼叫組成同一筆借書交易。
	 *
	 * @param bookId 要扣減可借數量的書籍 ID
	 * @return 更新影響筆數大於零為 true；書籍不存在或庫存條件不符為 false
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public boolean decAvailableQuantity(int bookId) throws SQLException{
		
		return bookDAO.decAvailableQuantity(bookId) > 0;
	}
	/**
	 * 使用呼叫端的連線，在可借數量大於零時扣減一冊，參與既有交易。
	 *
	 * <p>BorrowService 使用此多載，讓庫存扣減與新增借閱共用 Connection。
	 * 本方法及對應 DAO 不提交、回滾或關閉該連線，交易生命週期由提供連線的呼叫端管理。
	 *
	 * @param conn 呼叫端提供的有效 JDBC Connection；欲參與手動交易須先關閉自動提交
	 * @param bookId 要扣減可借數量的書籍 ID
	 * @return 更新影響筆數大於零為 true；書籍不存在或庫存條件不符為 false
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public boolean decAvailableQuantity(Connection conn, int bookId) throws SQLException {
		
		return bookDAO.decAvailableQuantity(conn, bookId) > 0;
	}
	
	/**
	 * 使用 DAO 自行取得的連線，在可借數量小於館藏量時增加一冊。
	 *
	 * <p>回傳原始影響筆數，沒有轉為 boolean；條件限制可避免超過館藏量，卻不能識別重複還書。
	 * 完整還書流程仍需 BorrowService 驗證借閱狀態，並讓相關寫入共用交易連線。
	 *
	 * @param bookId 要增加可借數量的書籍 ID
	 * @return DAO 的更新影響筆數；書籍不存在或庫存已達上限時為零
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public int increaseAvailableQuantity(int bookId) throws SQLException{
		return bookDAO.increaseAvailableQuantity(bookId);
	}
	/**
	 * 使用呼叫端的連線增加一冊可借數量，讓還書紀錄更新與庫存更新可一起回滾。
	 *
	 * <p>保留影響筆數，供 BorrowService 判斷是否恰好更新一筆。
	 * 本方法不負責 commit、rollback 或關閉連線；庫存上限由 DAO 的條件 UPDATE 限制。
	 *
	 * @param conn 呼叫端管理的有效 JDBC Connection
	 * @param bookId 要增加可借數量的書籍 ID
	 * @return DAO 的更新影響筆數；書籍不存在或庫存已達上限時為零
	 * @throws SQLException 資料庫操作或資源關閉失敗時向呼叫端傳遞，不轉成查無資料或業務失敗結果
	 */
	public int increaseAvailableQuantity(Connection conn, int bookId) throws SQLException{
		return bookDAO.increaseAvailableQuantity(conn, bookId);
	}
}
