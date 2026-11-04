package com.tian.library.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BookDAO;
import com.tian.library.entity.Book;
/**
 * 處理書籍欄位驗證、ISBN 重複檢查與庫存操作，透過 BookDAO 執行資料存取。
 * 借還書流程可傳入既有 Connection，讓庫存更新與借閱寫入共用 Transaction。
 * @Service 將此類別註冊為 Spring Bean，依賴由 constructor injection 提供。
 */
@Service
public class BookService {
	private final BookDAO bookDAO;
	/**
	 * 透過 constructor injection 取得所需依賴，便於由 Spring 組裝並在測試中替換。
	 * @param bookDAO 此 Service 協調使用的資料存取或業務元件
	 */
	public BookService(BookDAO bookDAO) {
		this.bookDAO = bookDAO;
	}
	/**
	 * 驗證必要欄位、正整數館藏量與 ISBN 重複情況，將可借數量初始化為館藏量後新增。
	 * 先查再新增不能取代 Database 的唯一約束；目前未檢查 ISBN 格式或檢查碼。
	 * @param book 欲新增或更新的書籍資料
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 */
	public boolean add(Book book) {
		
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
		// 如果有同樣的isbn就不能新增
		if(bookDAO.findByIsbn(book.getIsbn())!=null) {
			return false;
		}
		// 可借數量=館藏數量
		book.setAvailableQuantity(book.getTotalQuantity());

		return bookDAO.add(book) > 0;
	
	}
	/**
	 * 驗證書籍 ID 與必要欄位，確認目標存在，並排除與其他書籍重複的 ISBN。
	 * 此流程委派 DAO 更新書目欄位，不會因傳入數量而調整館藏或可借數量。
	 * @param book 欲新增或更新的書籍資料
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 */
	public boolean update(Book book) {
		
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
		// ISBN 不能跟其他書重複
		Book sameIsbn = bookDAO.findByIsbn(book.getIsbn());
		if(sameIsbn != null && sameIsbn.getId() != book.getId()) {
			return false;
		}


		return bookDAO.updateBook(book)>0;
	}
	/**
	 * 依書籍 ID 委派刪除，沒有另外檢查借閱狀態；關聯限制由資料庫與 DAO 處理。
	 * @param bookId 書籍 ID
	 * @return 處理結果；目前 false 未細分驗證失敗、查無資料與部分資料存取失敗
	 */
	public boolean delete(int bookId) {
		return bookDAO.deleteBook(bookId) > 0;
	}
	
	/**
	 * 將書名條件交給 DAO 進行模糊搜尋，此處不額外過濾或分頁。
	 * @param title 書名搜尋條件
	 * @return 符合條件的清單；查無結果時為空集合
	 */
	public List<Book> findByTitle(String title) {
		return bookDAO.findByTitle(title);
	}
	
	/**
	 * 依 ISBN 查詢書籍，供新增與修改時判斷重複。
	 * @param isbn 書籍 ISBN
	 * @return 找到的 Entity，查無資料時為 null
	 */
	public Book findByIsbn(String isbn) {
		return bookDAO.findByIsbn(isbn);
	}
	/**
	 * 取得全部書籍 Entity，尚未在此轉成 Response DTO。
	 * @return 符合條件的清單；查無結果時為空集合
	 */
	public List<Book> findAll() {
		return bookDAO.findAll();
	}
	
	/**
	 * 依書籍 ID 取得 Entity，供查詢及借閱流程檢查書籍是否存在。
	 * @param bookId 書籍 ID
	 * @return 找到的 Entity，查無資料時為 null
	 */
	public Book findById(int bookId) {
		return bookDAO.findById(bookId);
	}
	
	/**
	 * 委派 DAO 在可借數量大於零時扣減一冊，以條件更新避免扣成負數。
	 * 此 overload 由 DAO 自行取得及關閉連線，不能與其他獨立呼叫自動形成同一 Transaction。
	 * @param bookId 書籍 ID
	 * @return 更新至少一筆時為 true；條件不符合時為 false
	 */
	public boolean decAvailableQuantity(int bookId) {
		
		return bookDAO.decAvailableQuantity(bookId) > 0;
	}
	/**
	 * 委派 DAO 在可借數量大於零時扣減一冊，以條件更新避免扣成負數。
	 * 使用呼叫端的 Connection 參與既有 Transaction，避免另外取得連線而分開提交。
	 * @param conn 呼叫端提供的 JDBC Connection；此方法不負責 commit、rollback 或關閉
	 * @param bookId 書籍 ID
	 * @return 更新至少一筆時為 true；條件不符合時為 false
	 * @throws SQLException JDBC 操作失敗時向呼叫端傳遞
	 */
	public boolean decAvailableQuantity(Connection conn, int bookId) throws SQLException {
		
		return bookDAO.decAvailableQuantity(conn, bookId) > 0;
	}
	
	/**
	 * 委派 DAO 在可借數量小於館藏量時增加一冊，避免超過館藏量。
	 * 此 overload 由 DAO 自行取得及關閉連線，不能與其他獨立呼叫自動形成同一 Transaction。
	 * @param bookId 書籍 ID
	 * @return 更新至少一筆時為 true；條件不符合時為 false
	 */
	public int increaseAvailableQuantity(int bookId) {
		return bookDAO.increaseAvailableQuantity(bookId);
	}
	/**
	 * 委派 DAO 在可借數量小於館藏量時增加一冊，避免超過館藏量。
	 * 使用呼叫端的 Connection 參與既有 Transaction，避免另外取得連線而分開提交。
	 * @param conn 呼叫端提供的 JDBC Connection；此方法不負責 commit、rollback 或關閉
	 * @param bookId 書籍 ID
	 * @return 更新至少一筆時為 true；條件不符合時為 false
	 * @throws SQLException JDBC 操作失敗時向呼叫端傳遞
	 */
	public int increaseAvailableQuantity(Connection conn, int bookId) throws SQLException{
		return bookDAO.increaseAvailableQuantity(conn, bookId);
	}
}
