package com.tian.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.tian.library.entity.Book;

/**
 * 以 JDBC 存取書籍資料，負責 SQL、參數綁定與 ResultSet 映射，供 Service 呼叫。
 * @Repository 將 DAO 註冊為 Spring Bean，但不會自動替手寫 JDBC 開始 Transaction。
 * PreparedStatement 使用 ? 作為參數佔位符，將資料與 SQL 結構分離，避免直接串接輸入造成 SQL injection；參數索引從 1 開始。
 * 自行取得 Connection 的方法用 try-with-resources 關閉資源；關閉 Statement 也會關閉其目前的 ResultSet。
 * 部分方法攔截 SQLException 後僅印出堆疊，因此零值、null 或空集合可能同時代表失敗，並非只有查無資料。
 */
@Repository
public class BookDAO {
	private final DataSource dataSource;

	/**
	 * 透過 constructor injection 取得 DataSource，由連線池統一管理 Connection。
	 * @param dataSource Spring 設定的 JDBC 資料來源
	 */
	public BookDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 新增書籍及館藏、可借數量，資料驗證與數量初始化由 BookService 負責。
	 * @param book 要寫入的書籍 Entity
	 * @return executeUpdate 的影響筆數；SQLException 被攔截時回傳 0
	 */
	public int add(Book book) {
		String sql = """
				INSERT INTO books
				(isbn, title, author, publisher, category,
				total_quantity, available_quantity)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, book.getIsbn());
			pstmt.setString(2, book.getTitle());
			pstmt.setString(3, book.getAuthor());
			pstmt.setString(4, book.getPublisher());
			pstmt.setString(5, book.getCategory());
			pstmt.setInt(6, book.getTotalQuantity());
			pstmt.setInt(7, book.getAvailableQuantity());
			// executeUpdate 回傳影響筆數，並非新增資料的自動產生 ID。
			return pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 依 ID 更新書目資料；SQL 不更新 total_quantity 與 available_quantity。
	 * @param book 要寫入的書籍 Entity
	 * @return executeUpdate 的影響筆數；SQLException 被攔截時回傳 0
	 */
	public int updateBook(Book book) {
		String sql = """
				UPDATE books
				SET
				    isbn = ?,
				    title = ?,
				    author = ?,
				    publisher = ?,
				    category = ?
				WHERE id = ?;
					""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, book.getIsbn());
			pstmt.setString(2, book.getTitle());
			pstmt.setString(3, book.getAuthor());
			pstmt.setString(4, book.getPublisher());
			pstmt.setString(5, book.getCategory());
			pstmt.setInt(6, book.getId());
			return pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;

	}

	/**
	 * 依 ID 刪除書籍；若資料庫有借閱外鍵限制，刪除可能失敗。
	 * @param bookId 書籍 ID
	 * @return executeUpdate 的影響筆數；SQLException 被攔截時回傳 0
	 */
	public int deleteBook(int bookId) {
		String sql = "DELETE FROM books WHERE id = ?";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);

			return pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * 依 ISBN 查詢單筆書籍，將資料列映射為 Book。
	 * @param isbn 要查詢的 ISBN
	 * @return 找到的 Entity；查無資料或部分 SQL 失敗情況為 null
	 */
	public Book findByIsbn(String isbn) {
		String sql = "SELECT * FROM books WHERE isbn = ?";
		Book book = null;
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, isbn);
			// executeQuery 取得結果游標；必須先 next 才能讀第一筆，每次 next 移到下一筆。
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"), rs.getString("author"),
						rs.getString("publisher"), rs.getString("category"), rs.getInt("total_quantity"),
						rs.getInt("available_quantity"));
				return book;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 取得全部書籍；SQL 明確使用 library_management schema，不會隨 datasource 的預設 schema 改變。
	 * @return 結果清單；查無資料為空集合，SQL 失敗時也可能回傳空或已映射的部分清單
	 */
	public List<Book> findAll() {
		String sql = "SELECT * FROM library_management.books";
		List<Book> bookList = new ArrayList<Book>();

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery();) {

			while (rs.next()) {
				Book book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"),
						rs.getString("author"), rs.getString("publisher"), rs.getString("category"),
						rs.getInt("total_quantity"), rs.getInt("available_quantity"));
				bookList.add(book);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return bookList;
	}

	/**
	 * 使用 LIKE 搜尋書名；前後加上 % 代表包含條件，輸入中的 % 與 _ 仍有萬用字元意義。
	 * @param title 書名搜尋條件
	 * @return 結果清單；查無資料為空集合，SQL 失敗時也可能回傳空或已映射的部分清單
	 */
	public List<Book> findByTitle(String title) {
		String sql = "SELECT * FROM books WHERE title LIKE ?";
		List<Book> bookList = new ArrayList<Book>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, "%" + title + "%");
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Book book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"),
						rs.getString("author"), rs.getString("publisher"), rs.getString("category"),
						rs.getInt("total_quantity"), rs.getInt("available_quantity"));
				bookList.add(book);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return bookList;
	}

	/**
	 * 依主鍵查詢單筆書籍，不在此驗證借閱者權限。
	 * @param bookId 書籍 ID
	 * @return 找到的 Entity；查無資料或部分 SQL 失敗情況為 null
	 */
	public Book findById(int bookId) {
		String sql = "SELECT * FROM books WHERE id = ?";
		Book book = null;
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"), rs.getString("author"),
						rs.getString("publisher"), rs.getString("category"), rs.getInt("total_quantity"),
						rs.getInt("available_quantity"));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return book;
	}

	/**
	 * 以單一條件 UPDATE 扣減可借數量，available_quantity > 0 可避免扣成負數。
	 * 影響零筆可能是書籍不存在或已無庫存，呼叫端必須檢查結果才能決定後續借閱是否繼續。
	 * @param bookId 書籍 ID
	 * @return 影響筆數大於零時為 true，否則為 false；SQLException 被攔截時也回傳 false
	 */
	public int decAvailableQuantity(int bookId) {
		String sql = """
				UPDATE books SET available_quantity = available_quantity-1 WHERE id = ? AND available_quantity > 0
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			return row;
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
		
	}
	
	/**
	 * 以單一條件 UPDATE 扣減可借數量，available_quantity > 0 可避免扣成負數。
	 * 僅在 try-with-resources 關閉 PreparedStatement，保留呼叫端 Connection 以參與同一 Transaction。
	 * 影響零筆可能是書籍不存在或已無庫存，呼叫端必須檢查結果才能決定後續借閱是否繼續。
	 * @param conn 呼叫端持有的 Connection，本方法不提交、回滾或關閉它
	 * @param bookId 書籍 ID
	 * @throws SQLException JDBC 失敗時向外傳遞，讓呼叫端決定 rollback
	 */
	public int decAvailableQuantity(Connection conn, int bookId) throws SQLException {
		String sql = """
				UPDATE books SET available_quantity = available_quantity-1 WHERE id = ? AND available_quantity > 0
				""";
		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ps.setInt(1, bookId);
			
			return ps.executeUpdate();
		}
		
	}

	/**
	 * 以條件 UPDATE 增加可借數量，避免超過館藏量；這個條件本身無法識別重複還書。
	 * @param bookId 書籍 ID
	 * @return 影響筆數大於零時為 true，否則為 false；SQLException 被攔截時也回傳 false
	 */
	public int increaseAvailableQuantity(int bookId) {
		String sql = """
				UPDATE books SET available_quantity = available_quantity + 1 WHERE id = ? AND available_quantity < total_quantity
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			return row;
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
	/**
	 * 以條件 UPDATE 增加可借數量，避免超過館藏量；這個條件本身無法識別重複還書。
	 * 僅在 try-with-resources 關閉 PreparedStatement，保留呼叫端 Connection 以參與同一 Transaction。
	 * @param conn 呼叫端持有的 Connection，本方法不提交、回滾或關閉它
	 * @param bookId 書籍 ID
	 * @return 影響筆數大於零時為 true，否則為 false
	 * @throws SQLException JDBC 失敗時向外傳遞，讓呼叫端決定 rollback
	 */
	public int increaseAvailableQuantity  (Connection conn, int bookId) throws SQLException {
		String sql = """
				UPDATE books SET available_quantity = available_quantity + 1 WHERE id = ? AND available_quantity < total_quantity
				""";
		try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			return row;
		} 
	}
	
	/**
	 * 計算書目筆數，不是 total_quantity 加總，因此不代表實體館藏冊數。
	 * @return SQL 統計值；SQL 失敗或未取得結果時也回傳 0
	 */
	public int countBooks() {
		String sql = """
				SELECT count(*) FROM books
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
}
