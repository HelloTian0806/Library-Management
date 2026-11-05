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
 * 以手寫 JDBC 存取書籍資料，負責 SQL、參數綁定與 ResultSet 映射，不處理 HTTP 回應。
 *
 * <p>{@code @Repository} 將類別註冊為 Spring Bean；它本身不會自動為這些手寫 JDBC 操作啟動交易。
 * DataSource 提供 Connection，代表與資料庫互動的連線；PreparedStatement 承載 SQL 與參數，
 * executeQuery 取得可逐列讀取的 ResultSet，executeUpdate 則回傳寫入的影響筆數。
 *
 * <p>SQL 使用 ? 綁定資料值，參數索引從一開始，順序須對應 SQL 中的佔位符。
 * 將輸入作為資料值綁定，可避免該值被當成 SQL 結構執行；參數綁定不能用來替換資料表或欄位名稱，
 * 也不會自動驗證業務規則或取消 LIKE 的萬用字元語意。
 *
 * <p>try-with-resources 在正常結束或拋出例外時，都會依宣告的反向順序嘗試關閉資源。
 * Statement 關閉時會關閉其目前的 ResultSet；使用連線池時，Connection.close 通常是將連線歸還池中。
 * SQLException 直接向上傳遞，不會被轉成零、null 或部分成功清單。
 *
 * <p>自行取得連線的多載會關閉該連線；接受 Connection 的多載只管理 Statement，
 * 不提交、回滾或關閉呼叫端連線，讓 BorrowService 將庫存與借閱寫入放在同一交易。
 * 必要欄位與 ISBN 重複檢查由 BookService 協調，資料庫約束仍可能在執行 SQL 時拒絕寫入。
 */
@Repository
public class BookDAO {
	private final DataSource dataSource;

	/**
	 * 透過建構子取得 Spring 提供的 DataSource，將連線建立與設定交給資料來源管理。
	 *
	 * <p>建構子只保存依賴，不在建立 DAO 時取得長期共用的 Connection；各方法依需要取得或接收連線。
	 *
	 * @param dataSource 提供 JDBC 連線的資料來源；若使用連線池，池的生命週期由外部設定管理
	 */
	public BookDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 新增書目、館藏量與可借數量，依 INSERT 欄位順序綁定七個參數。
	 *
	 * <p>參數一至五是書目字串，六、七是數量；DAO 不自行驗證或重設庫存，預期由 Service 先處理。
	 * executeUpdate 回傳影響筆數，本方法沒有要求或讀取自動產生的主鍵。
	 *
	 * @param book 含待寫入欄位的書籍 Entity，不可為 null
	 * @return INSERT 影響筆數，不是新增書籍 ID
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws NullPointerException book 為 null，讀取其欄位時發生
	 */
	public int add(Book book) throws SQLException{
		String sql = """
				INSERT INTO books
				(isbn, title, author, publisher, category,
				total_quantity, available_quantity)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			// 參數索引從一開始，依 SQL 中 ? 的順序綁定；輸入值不作為 SQL 語法。
			pstmt.setString(1, book.getIsbn());
			pstmt.setString(2, book.getTitle());
			pstmt.setString(3, book.getAuthor());
			pstmt.setString(4, book.getPublisher());
			pstmt.setString(5, book.getCategory());
			pstmt.setInt(6, book.getTotalQuantity());
			pstmt.setInt(7, book.getAvailableQuantity());
			// executeUpdate 回傳影響筆數，並非新增資料的自動產生 ID。
			return pstmt.executeUpdate();
		} 

	}

	/**
	 * 以 ID 更新 ISBN、書名、作者、出版社與分類，不更新兩個數量欄位。
	 *
	 * <p>前五個參數對應 SET 的值，第六個才是 WHERE 的目標 ID，不能按 Entity 欄位順序任意綁定。
	 * DAO 不自行檢查操作者權限或 ISBN 重複，這些責任由呼叫流程及資料庫約束承擔。
	 *
	 * @param book 含目標 ID 與修改書目欄位的 Entity，不可為 null
	 * @return UPDATE 回報的影響筆數；本方法不將它轉成 boolean
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws NullPointerException book 為 null，讀取其欄位時發生
	 */
	public int updateBook(Book book) throws SQLException{
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
			// 參數索引從一開始，依 SQL 中 ? 的順序綁定；輸入值不作為 SQL 語法。
			pstmt.setString(1, book.getIsbn());
			pstmt.setString(2, book.getTitle());
			pstmt.setString(3, book.getAuthor());
			pstmt.setString(4, book.getPublisher());
			pstmt.setString(5, book.getCategory());
			// 此處第六個 ? 位於 WHERE，決定修改哪一本書，而不是第六個待更新欄位。
			pstmt.setInt(6, book.getId());
			return pstmt.executeUpdate();

		} 

	}

	/**
	 * 綁定書籍 ID 後執行 DELETE，不在刪除前額外查詢借閱狀態。
	 *
	 * <p>若資料庫外鍵限制禁止刪除，會拋出 SQLException；關聯資料如何處理仍以實際 Schema 為準。
	 *
	 * @param bookId 要刪除的書籍 ID
	 * @return DELETE 影響筆數；沒有符合 ID 的資料時為零
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public int deleteBook(int bookId) throws SQLException{
		String sql = "DELETE FROM books WHERE id = ?";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);

			return pstmt.executeUpdate();

		} 

	}

	/**
	 * 以 ISBN 查詢書籍，讀取第一筆結果並建立獨立的 Book 物件。
	 *
	 * <p>ResultSet 初始游標位於第一列之前，須先呼叫 next 才能讀欄位。
	 * 此處使用 if，只映射第一筆；結果集在 Statement 關閉時一併關閉，回傳的 Book 不依賴游標繼續存在。
	 *
	 * @param isbn 要查詢的 ISBN，作為參數值綁定而非拼進 SQL
	 * @return 第一筆符合條件的 Book；查無資料為 null
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public Book findByIsbn(String isbn) throws SQLException{
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

		} 
		return null;
	}

	/**
	 * 查詢 library_management.books 的全部資料，逐列映射成 Book 清單。
	 *
	 * <p>SQL 明確指定 Schema，因此不會隨連線的預設資料庫切換。
	 * Connection、PreparedStatement、ResultSet 都列入資源宣告，依序反向關閉。
	 * 查詢沒有 ORDER BY 或分頁，不保證回傳順序。
	 *
	 * @return 全部書籍的清單；查無資料為空集合，查詢中途失敗則拋出例外
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public List<Book> findAll() throws SQLException{
		String sql = "SELECT * FROM library_management.books";
		List<Book> bookList = new ArrayList<Book>();

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery();) {

			// 每次 next 移到下一筆；逐筆建立新物件，避免整個清單重複參照同一個 Book。
			while (rs.next()) {
				Book book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"),
						rs.getString("author"), rs.getString("publisher"), rs.getString("category"),
						rs.getInt("total_quantity"), rs.getInt("available_quantity"));
				bookList.add(book);
			}
		} 
		return bookList;
	}

	/**
	 * 以 LIKE 搜尋包含條件的書名，逐筆將結果加入 Book 清單。
	 *
	 * <p>前後加 % 後再透過 setString 綁定，字串組合的是參數值，沒有改變 SQL 結構。
	 * 輸入本身含 % 或 _ 時仍有萬用字元語意；PreparedStatement 不會自動將它們當成一般文字。
	 *
	 * @param title 要搜尋的書名條件
	 * @return 符合條件的 Book 清單；查無資料為空集合
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public List<Book> findByTitle(String title) throws SQLException{
		String sql = "SELECT * FROM books WHERE title LIKE ?";
		List<Book> bookList = new ArrayList<Book>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, "%" + title + "%");
			ResultSet rs = pstmt.executeQuery();
			// 每次 next 移到下一筆；逐筆建立新物件，避免整個清單重複參照同一個 Book。
			while (rs.next()) {
				Book book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"),
						rs.getString("author"), rs.getString("publisher"), rs.getString("category"),
						rs.getInt("total_quantity"), rs.getInt("available_quantity"));
				bookList.add(book);
			}

		} 
		return bookList;
	}

	/**
	 * 依 ID 查詢第一筆書籍資料，將資料庫欄位值映射到 Book 建構子。
	 *
	 * <p>取得資料的能力不代表呼叫者有權修改或借閱，權限及業務條件由上層決定。
	 *
	 * @param bookId 要查詢的書籍 ID
	 * @return 查到的 Book；查無資料為 null
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public Book findById(int bookId) throws SQLException{
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

		} 
		return book;
	}

	/**
	 * 自行取得連線，以單一條件 UPDATE 扣減一冊可借數量。
	 *
	 * <p>資料庫在更新時檢查 available_quantity 大於零，再以現值減一，避免使用應用程式讀到的舊庫存覆寫。
	 * 本方法未顯式設定自動提交或共用交易；借書流程若要與新增紀錄一起回滾，應使用接受 Connection 的多載。
	 *
	 * @param bookId 要扣減庫存的書籍 ID
	 * @return UPDATE 影響筆數；書籍不存在或無可借數量時為零，不代表 SQL 拋出例外
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public int decAvailableQuantity(int bookId) throws SQLException{
		String sql = """
				UPDATE books SET available_quantity = available_quantity-1 WHERE id = ? AND available_quantity > 0
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			return row;
		} 	
	}
	
	/**
	 * 使用呼叫端連線，以可借數量大於零為條件扣減一冊。
	 *
	 * <p>只關閉本方法建立的 PreparedStatement，不能關閉 conn，否則會中斷呼叫端後續的借閱寫入。
	 * 影響筆數由呼叫端檢查，commit 或 rollback 也由呼叫端決定。
	 *
	 * @param conn 呼叫端提供並管理的有效 Connection
	 * @param bookId 要扣減庫存的書籍 ID
	 * @return UPDATE 影響筆數；目標不存在或庫存條件不符時為零
	 * @throws SQLException JDBC 操作或 Statement 關閉失敗時向上傳遞，供呼叫端決定回滾
	 * @throws NullPointerException conn 為 null，建立 Statement 時發生
	 */
	public int decAvailableQuantity(Connection conn, int bookId) throws SQLException {
		String sql = """
				UPDATE books SET available_quantity = available_quantity-1 WHERE id = ? AND available_quantity > 0
				""";
		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ps.setInt(1, bookId);
			
			// 只回傳影響筆數；交易連線仍由上層持有，這裡不能自行 commit。
			return ps.executeUpdate();
		}
		
	}

	/**
	 * 自行取得連線，在可借數量低於館藏量時增加一冊。
	 *
	 * <p>上限條件防止加到超過館藏量，但 SQL 沒有借閱 ID，不能單靠此方法識別重複還書。
	 * 要與還書更新形成同一交易，須使用接受 Connection 的多載。
	 *
	 * @param bookId 要增加可借數量的書籍 ID
	 * @return UPDATE 影響筆數；目標不存在或可借數量已達館藏量時為零
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public int increaseAvailableQuantity(int bookId) throws SQLException{
		String sql = """
				UPDATE books SET available_quantity = available_quantity + 1 WHERE id = ? AND available_quantity < total_quantity
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			return row;
		} 
	}
	/**
	 * 使用呼叫端連線增加可借數量，保持與還書更新相同的交易範圍。
	 *
	 * <p>SQL 的上限條件與另一多載相同，但本方法不取得、提交、回滾或關閉 Connection。
	 * 只關閉自己的 PreparedStatement，並將影響筆數交給 BorrowService 判斷。
	 *
	 * @param conn 呼叫端提供並管理的有效 Connection
	 * @param bookId 要增加可借數量的書籍 ID
	 * @return UPDATE 影響筆數；目標不存在或庫存條件不符時為零
	 * @throws SQLException JDBC 操作或 Statement 關閉失敗時向上傳遞，供呼叫端決定回滾
	 * @throws NullPointerException conn 為 null，建立 Statement 時發生
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
	 * 以 COUNT(*) 計算書目資料列數，不是 total_quantity 的加總。
	 *
	 * <p>沒有 GROUP BY 的 COUNT 通常回傳一列，即使沒有書籍，其統計值也是零。
	 * 仍須先 next 才能讀取結果；目前以運算式名稱 count(*) 讀取，沒有結果列時的備援值也是零。
	 *
	 * @return 書目筆數；若結果集沒有任何列也回傳零，但 SQLException 不會被轉成零
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public int countBooks() throws SQLException{
		String sql = """
				SELECT count(*) FROM books
				 """;
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				return rs.getInt("count(*)");
			}
			
		}
		return 0;
	}
}
