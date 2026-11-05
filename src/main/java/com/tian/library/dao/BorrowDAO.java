package com.tian.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.tian.library.dto.AdminBorrowResponse;
import com.tian.library.dto.BorrowResponse;
import com.tian.library.entity.Borrow;
import com.tian.library.entity.BorrowStatus;

/**
 * 以手寫 JDBC 存取借閱資料，負責 SQL、參數綁定與 ResultSet 映射，不處理 HTTP 回應。
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
 * <p>接受 Connection 的方法保留呼叫端連線，不提交、回滾或關閉它；其餘方法自行取得及關閉連線。
 * 借閱期限、所有權與完整交易由 BorrowService 協調；本 DAO 另包含動態狀態計算，
 * 目前這些計算只影響回傳物件，不會自行寫回資料庫。
 * 資料庫狀態使用 BorrowStatus.valueOf 轉換，須與列舉名稱及大小寫相符。
 */
@Repository
public class BorrowDAO {
	private final DataSource dataSource;

	/**
	 * 透過建構子取得 Spring 提供的 DataSource，將連線建立與設定交給資料來源管理。
	 *
	 * <p>建構子只保存依賴，不在建立 DAO 時取得長期共用的 Connection；各方法依需要取得或接收連線。
	 *
	 * @param dataSource 提供 JDBC 連線的資料來源；若使用連線池，池的生命週期由外部設定管理
	 */
	public BorrowDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 自行取得連線新增借閱，依序綁定會員、書籍、借閱日、到期日及狀態。
	 *
	 * <p>LocalDate 先轉成 java.sql.Date 再綁定 SQL DATE，狀態以 enum.name() 的名稱寫入。
	 * DAO 不自行訂定到期日或扣減庫存；要與庫存操作共用交易，應使用接受 Connection 的多載。
	 *
	 * @param borrow 由上層準備日期及狀態的借閱 Entity，不可為 null
	 * @return INSERT 影響筆數，不是自動產生的借閱 ID
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws NullPointerException borrow、借閱日、到期日或狀態為 null，讀取或轉換時發生
	 */
	public int add(Borrow borrow) throws SQLException{
		String sql = """
				INSERT INTO borrows (member_id, book_id, borrow_date, due_date,status) VALUES(?,?,?,?,?);
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, borrow.getMemberId());
			pstmt.setInt(2, borrow.getBookId());
			pstmt.setDate(3, java.sql.Date.valueOf(borrow.getBorrowDate()));
			pstmt.setDate(4, java.sql.Date.valueOf(borrow.getDueDate()));
			pstmt.setString(5, borrow.getStatus().name());
			// executeUpdate 回傳影響筆數，並非新增資料的自動產生 ID。
			return pstmt.executeUpdate();
		} 
	}
	
	/**
	 * 使用呼叫端 Connection 新增借閱，讓此寫入可與庫存扣減一起提交或回滾。
	 *
	 * <p>參數順序與 INSERT 欄位一致；只關閉此處建立的 PreparedStatement，不關閉 conn。
	 * executeUpdate 的零筆結果與 SQLException 是不同情況，呼叫端須檢查筆數並處理例外。
	 *
	 * @param conn 呼叫端提供並管理的有效 Connection
	 * @param borrow 含會員、書籍、日期及狀態的 Entity，不可為 null
	 * @return INSERT 影響筆數
	 * @throws SQLException JDBC 操作或 Statement 關閉失敗時向上傳遞，供呼叫端決定回滾
	 * @throws NullPointerException conn、borrow、借閱日、到期日或狀態為 null，使用或轉換時發生
	 */
	public int add(Connection conn, Borrow borrow) throws SQLException {
		String sql ="""
				INSERT INTO borrows (member_id, book_id, borrow_date, due_date, status) VALUES(?,?,?,?,?);
				""";
		
		try (PreparedStatement ps = conn.prepareStatement(sql)){
			ps.setInt(1, borrow.getMemberId());
			ps.setInt(2, borrow.getBookId());
			// LocalDate 先轉為 JDBC 接受的 SQL 日期型別，不含時間與時區欄位。
			ps.setDate(3, java.sql.Date.valueOf(borrow.getBorrowDate()));
			ps.setDate(4, java.sql.Date.valueOf(borrow.getDueDate()));
			ps.setString(5, borrow.getStatus().name());
			
			return ps.executeUpdate();
		}
		
		
	}

	/**
	 * 以 ID 讀取第一筆借閱 Entity，再計算回傳物件的目前狀態。
	 *
	 * <p>return_date 允許讀到 SQL NULL，因此先判斷再轉 LocalDate；借閱日及到期日則直接轉換。
	 * BorrowStatus.valueOf 先將儲存字串轉為列舉，calculateStatus 才根據狀態及日期計算顯示結果。
	 * setStatus 只修改 Java 物件，不會執行 UPDATE；此查詢也沒有 FOR UPDATE 的鎖定語意。
	 *
	 * @param borrowId 要查詢的借閱紀錄 ID
	 * @return 查到並計算狀態的 Borrow；查無資料為 null
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的狀態字串無法對應 BorrowStatus 時發生
	 * @throws NullPointerException 借閱日、到期日或狀態為 SQL NULL，轉換或計算時發生
	 */
	public Borrow findById(int borrowId) throws SQLException{
		String sql = """
				SELECT * FROM borrows WHERE id = ?
				 """;
		Borrow borrow = null;
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, borrowId);
			// executeQuery 取得結果游標；必須先 next 才能讀第一筆，每次 next 移到下一筆。
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				// SQL NULL 表示尚未歸還，轉換 LocalDate 前必須先判斷 null。
				java.sql.Date returnDate = rs.getDate("return_date");
				borrow = new Borrow(rs.getInt("id"), rs.getInt("member_id"), rs.getInt("book_id"),
						rs.getDate("borrow_date").toLocalDate(), rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				// 只改動已建立的 Java 物件；JDBC 不會追蹤 Entity 變動並自動產生 UPDATE。
				borrow.setStatus(calculateStatus(borrow));
			}
		} 
		return borrow;
	}
	
	/**
	 * 使用呼叫端連線，以 SELECT ... FOR UPDATE 讀取借閱並取得交易鎖。
	 *
	 * <p>在支援交易與列鎖的 InnoDB 中，呼叫端須先關閉自動提交，並讓後續檢查及寫入沿用 conn，
	 * 鎖才能保護整個還書流程；只關閉 Statement 不會結束該交易，鎖於 commit 或 rollback 後釋放。
	 * 此方法不驗證所有權或是否已歸還，也不更新庫存，這些由 BorrowService 接續處理。
	 * 回傳前仍會動態計算物件狀態；查詢失敗直接拋出例外，不偽裝成查無資料。
	 *
	 * @param conn 呼叫端提供並管理的有效交易連線，本方法不提交、回滾或關閉它
	 * @param borrowId 要鎖定並查詢的借閱紀錄 ID
	 * @return 查到並計算狀態的 Borrow；查無資料為 null
	 * @throws SQLException JDBC 操作或 Statement 關閉失敗時向上傳遞，供呼叫端決定回滾
	 * @throws IllegalArgumentException 資料庫中的狀態字串無法對應 BorrowStatus 時發生
	 * @throws NullPointerException conn 為 null，或借閱日、到期日、狀態為 SQL NULL，使用或轉換時發生
	 */
	public Borrow findByIdForUpdate(Connection conn ,int borrowId) throws SQLException{
		String sql = """
				SELECT * FROM borrows WHERE id = ? FOR UPDATE
				 """;
		Borrow borrow = null;
		try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, borrowId);
			// executeQuery 取得結果游標；必須先 next 才能讀第一筆，每次 next 移到下一筆。
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				// SQL NULL 表示尚未歸還，轉換 LocalDate 前必須先判斷 null。
				java.sql.Date returnDate = rs.getDate("return_date");
				borrow = new Borrow(rs.getInt("id"), rs.getInt("member_id"), rs.getInt("book_id"),
						rs.getDate("borrow_date").toLocalDate(), rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				// 只改動已建立的 Java 物件；JDBC 不會追蹤 Entity 變動並自動產生 UPDATE。
				borrow.setStatus(calculateStatus(borrow));
			}
		}
		
		return borrow;
	}
	
	
	
	
	/**
	 * 以會員 ID 篩選借閱，JOIN books 取得書名，直接映射成會員端 BorrowResponse。
	 *
	 * <p>br、b 是資料表別名；book_title 是查詢欄位別名，供 getString 依名稱讀取。
	 * 一次 JOIN 取回每筆借閱所需書名，避免上層再為每筆結果另查書籍。
	 * JOIN 是 INNER JOIN，只保留有匹配書籍的紀錄；未設定排序或分頁。
	 * 每筆狀態會動態計算，但不寫回資料庫；呼叫者身分檢查由上層執行。
	 *
	 * @param memberId 要查詢借閱的會員 ID，綁定到 WHERE 的第一個參數
	 * @return 借閱 DTO 清單；查無資料為空集合
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的狀態字串無法對應 BorrowStatus 時發生
	 * @throws NullPointerException 借閱日、到期日或狀態為 SQL NULL，轉換或計算時發生
	 */
	public List<BorrowResponse> findByMemberId(int memberId) throws SQLException{
		String sql = """
				SELECT br.id, b.title as book_title, br.borrow_date, br.due_date, br.return_date, br.status
				FROM borrows br JOIN books b ON br.book_id = b.id
				WHERE br.member_id = ?
				""";
		List<BorrowResponse> borrowList = new ArrayList<BorrowResponse>();
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, memberId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				// SQL NULL 表示尚未歸還，轉換 LocalDate 前必須先判斷 null。
				java.sql.Date returnDate = rs.getDate("return_date");
				
				BorrowResponse borrowResponse = new BorrowResponse(
						rs.getInt("id"),
						rs.getString("book_title"), 
						rs.getDate("borrow_date").toLocalDate(),
						rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				borrowResponse.setStatus(calculateStatus(borrowResponse));
				
				borrowList.add(borrowResponse);
			}
		} 
		
		return borrowList;
	}

	/**
	 * JOIN members 與 books，取得管理端 DTO 所需的會員姓名、書名及借閱欄位。
	 *
	 * <p>以 member_name、book_title 別名區分資料來源，再逐列建立 AdminBorrowResponse。
	 * INNER JOIN 僅保留能匹配會員及書籍的紀錄；查詢沒有 ORDER BY、分頁或角色驗證。
	 * calculateStatus 計算回傳狀態，不更新資料庫狀態欄位。
	 *
	 * @return 管理端借閱 DTO 清單；查無資料為空集合，SQL 失敗不回傳部分清單
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的狀態字串無法對應 BorrowStatus 時發生
	 * @throws NullPointerException 借閱日、到期日或狀態為 SQL NULL，轉換或計算時發生
	 */
	public List<AdminBorrowResponse> findAll() throws SQLException{
		String sql = """
				SELECT br.id, br.member_id, m.name AS member_name,
				b.title AS book_title,
				br.borrow_date, br.due_date,
				br.return_date, br.status
				FROM borrows br
				JOIN members m ON br.member_id = m.id
				JOIN books b ON br.book_id = b.id
				""";
		List<AdminBorrowResponse> borrowList = new ArrayList<>();
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				// SQL NULL 表示尚未歸還，轉換 LocalDate 前必須先判斷 null。
				java.sql.Date returnDate = rs.getDate("return_date");
				AdminBorrowResponse borrow = new AdminBorrowResponse(
						rs.getInt("id"),
						rs.getInt("member_id"),
						rs.getString("member_name"),
						rs.getString("book_title"),
						rs.getDate("borrow_date").toLocalDate(),
						rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				// 只改動已建立的 Java 物件；JDBC 不會追蹤 Entity 變動並自動產生 UPDATE。
				borrow.setStatus(calculateStatus(borrow));
				
				borrowList.add(borrow);
			}
		} 
		
		return borrowList;
	}
	
	/**
	 * 以會員姓名 LIKE 條件搜尋借閱，JOIN 會員與書籍並映射管理端 DTO。
	 *
	 * <p>搜尋文字前後加 %，再綁定至 WHERE m.name LIKE 的第一個參數，不直接拼接 SQL。
	 * 輸入中的 % 與 _ 仍具有萬用字元意義；會員姓名不是操作者身分，DAO 不在此驗證管理權限。
	 * 查詢未指定排序或分頁，每筆結果另依日期計算顯示狀態，不會寫回資料庫。
	 *
	 * @param name 要搜尋的會員姓名條件
	 * @return 符合條件的 AdminBorrowResponse 清單；查無資料為空集合
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws IllegalArgumentException 資料庫中的狀態字串無法對應 BorrowStatus 時發生
	 * @throws NullPointerException 借閱日、到期日或狀態為 SQL NULL，轉換或計算時發生
	 */
	public List<AdminBorrowResponse> findByMemberName (String name) throws SQLException {
		String sql = """
				SELECT br.id, br.member_id, m.name AS member_name,
				b.title AS book_title,
				br.borrow_date, br.due_date,
				br.return_date, br.status
				FROM borrows br
				JOIN members m ON br.member_id = m.id
				JOIN books b ON br.book_id = b.id
				WHERE m.name LIKE ?
				""";
		List<AdminBorrowResponse> borrowList = new ArrayList<>();
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, "%"+name+"%");
			
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				// SQL NULL 表示尚未歸還，轉換 LocalDate 前必須先判斷 null。
				java.sql.Date returnDate = rs.getDate("return_date");
				AdminBorrowResponse borrow = new AdminBorrowResponse(
						rs.getInt("id"),
						rs.getInt("member_id"),
						rs.getString("member_name"),
						rs.getString("book_title"),
						rs.getDate("borrow_date").toLocalDate(),
						rs.getDate("due_date").toLocalDate(),
						returnDate == null ? null : returnDate.toLocalDate(),
						BorrowStatus.valueOf(rs.getString("status")));
				
				// 只改動已建立的 Java 物件；JDBC 不會追蹤 Entity 變動並自動產生 UPDATE。
				borrow.setStatus(calculateStatus(borrow));
				
				borrowList.add(borrow);
			}
		}
		
		return borrowList;
	}

	/**
	 * 依借閱 ID 將指定狀態寫入資料庫，與只計算 Java 物件狀態的方法不同。
	 *
	 * <p>status.name() 寫入列舉名稱；WHERE 僅有 ID，沒有驗證狀態轉換或前一狀態。
	 * 本方法不一併更新歸還日期或庫存，不能單獨視為完整借還書流程。
	 *
	 * @param borrowId 要更新的借閱紀錄 ID
	 * @param status 要寫入的狀態，不可為 null
	 * @return UPDATE 影響筆數，不是 boolean
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws NullPointerException status 為 null，呼叫 name() 時發生
	 */
	public int updateStatus(int borrowId, BorrowStatus status) throws SQLException{
		String sql = """
				UPDATE borrows SET status = ? WHERE id = ?
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, status.name());
			pstmt.setInt(2, borrowId);
			int row = pstmt.executeUpdate();
			return row ;
		} 
		
	}
	/**
	 * 單獨更新指定借閱的歸還日期，不同步修改狀態與庫存。
	 *
	 * <p>先將 LocalDate 轉成 SQL DATE，再以第二個參數綁定借閱 ID。
	 * 此方法自行取得連線，不能與其他獨立呼叫自動形成完整還書交易。
	 *
	 * @param borrowId 要更新的借閱紀錄 ID
	 * @param returnDate 要寫入的歸還日期，不可為 null；此寫法不能用 null 清除日期
	 * @return UPDATE 影響筆數
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 * @throws NullPointerException returnDate 為 null，轉換 java.sql.Date 時發生
	 */
	public int updateReturnDate(int borrowId, LocalDate returnDate) throws SQLException{
		String sql = """
				UPDATE borrows SET return_date = ? WHERE id = ?
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setDate(1, java.sql.Date.valueOf(returnDate));
			pstmt.setInt(2, borrowId);
			int row = pstmt.executeUpdate();
			return row;
		} 
		
	}
	/**
	 * 使用呼叫端連線，同時將狀態改為 RETURNED 並寫入歸還日期。
	 *
	 * <p>參數一是固定的狀態名稱，二是日期，三是借閱 ID；本方法不更新書籍庫存。
	 * WHERE 只有 ID，沒有所有權或舊狀態條件，不能單靠此 SQL 防止重複還書。
	 * 現有 BorrowService 會先以同一交易連線鎖定紀錄並檢查本人及狀態，再呼叫此方法。
	 * 本方法只關閉 Statement，讓 Service 繼續更新庫存及決定提交或回滾。
	 *
	 * @param conn 呼叫端提供並管理的有效 Connection
	 * @param borrowId 要歸還的借閱紀錄 ID
	 * @param returnDate 要寫入的歸還日期，不可為 null
	 * @return UPDATE 影響筆數，由 Service 檢查是否恰為一筆
	 * @throws SQLException JDBC 操作或 Statement 關閉失敗時向上傳遞，供呼叫端決定回滾
	 * @throws NullPointerException conn 或 returnDate 為 null，使用連線或轉換日期時發生
	 */
	public int returnBook(Connection conn, int borrowId, LocalDate returnDate) throws SQLException{
		String sql = """
				UPDATE borrows SET status = ? , return_date = ? WHERE id = ?
				""";
		try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
			// name() 寫入列舉常數名稱；更新成功與否仍須由呼叫端檢查影響筆數。
			pstmt.setString(1, BorrowStatus.RETURNED.name());
			pstmt.setDate(2, java.sql.Date.valueOf(returnDate));
			pstmt.setInt(3, borrowId);
			int row = pstmt.executeUpdate();
			return row;
		}
	}
	
	/**
	 * 以 return_date IS NULL 統計尚未歸還的紀錄，包含已逾期但尚未歸還的紀錄。
	 *
	 * <p>SQL NULL 須使用 IS NULL 判斷，不能以等號比較。
	 * 此處不依 BORROWED 狀態篩選，也不檢查到期日；COUNT 沒有 GROUP BY，通常回傳一列統計值。
	 *
	 * @return 未歸還筆數；若結果集沒有列則回傳零，但 JDBC 例外會向上傳遞
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public int countActiveBorrows() throws SQLException{
		String sql = """
				SELECT count(*) FROM borrows WHERE return_date IS NULL
				 """;
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				return rs.getInt("count(*)");
			}
			
		}
		
		return 0;
	}
	
	/**
	 * 統計尚未歸還，且到期日早於伺服器今天的借閱紀錄。
	 *
	 * <p>以 setDate 將 LocalDate.now() 轉為 SQL DATE，綁定到 due_date 比較條件。
	 * 小於今天才算逾期，所以到期當天不計入；日期採用 Java 執行環境的預設時區。
	 * 查詢不依賴資料庫狀態是否已更新成 OVERDUE，也不會為了統計而修改狀態。
	 *
	 * @return 符合日期及未歸還條件的筆數；若結果集沒有列則回傳零
	 * @throws SQLException JDBC 操作或資源關閉失敗時向上傳遞，不轉成預設回傳值
	 */
	public int countOverdueBorrows() throws SQLException{
		String sql = """
				SELECT count(*) FROM borrows WHERE return_date IS NULL AND due_date < ?
				 """;
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			// 今天來自 Java 執行環境，不是資料庫的 CURDATE()；以參數綁定保持 SQL 結構固定。
			pstmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				return rs.getInt("count(*)");
			}
			
		}
		return 0;
	}
	
	/**
	 * 計算管理端 DTO 的顯示狀態，依序判斷已歸還、逾期與借閱中。
	 *
	 * <p>狀態已是 RETURNED 就直接回傳，不讀取到期日；否則比較今天是否嚴格晚於到期日。
	 * 只回傳計算結果，不修改 DTO 或資料庫；呼叫處的 setStatus 才會修改回傳物件。
	 * 判斷不讀取 returnDate，因此不能用這個方法修正日期與狀態欄位不一致的資料。
	 *
	 * @param borrowResponse 要計算狀態的管理端借閱 DTO，不可為 null
	 * @return 已標示歸還為 RETURNED；其他狀態且今天晚於到期日為 OVERDUE；其餘為 BORROWED
	 * @throws NullPointerException DTO 為 null，或狀態不是 RETURNED 且到期日為 null 時發生
	 */
	public BorrowStatus calculateStatus(AdminBorrowResponse borrowResponse) {
		
		if (borrowResponse.getStatus() == BorrowStatus.RETURNED) {
			return BorrowStatus.RETURNED;
		}
		
		if (LocalDate.now().isAfter(borrowResponse.getDueDate())) {
			return BorrowStatus.OVERDUE;
		}
		
		return BorrowStatus.BORROWED;
	}
	
	/**
	 * 計算會員端 DTO 的顯示狀態，規則與管理端 DTO 的多載一致。
	 *
	 * <p>先保留 RETURNED，否則以伺服器預設時區的今天判斷是否晚於到期日。
	 * 到期當天仍回傳 BORROWED；不讀取歸還日期，也不更新 DTO 或資料庫。
	 *
	 * @param borrowResponse 要計算狀態的會員端借閱 DTO，不可為 null
	 * @return RETURNED、OVERDUE 或 BORROWED
	 * @throws NullPointerException DTO 為 null，或狀態不是 RETURNED 且到期日為 null 時發生
	 */
	public BorrowStatus calculateStatus(BorrowResponse borrowResponse) {
		
		if (borrowResponse.getStatus() == BorrowStatus.RETURNED) {
			return BorrowStatus.RETURNED;
		}
		
		if (LocalDate.now().isAfter(borrowResponse.getDueDate())) {
			return BorrowStatus.OVERDUE;
		}
		
		return BorrowStatus.BORROWED;
	}
	
	/**
	 * 計算 Borrow Entity 的顯示狀態，供一般查詢與鎖定查詢使用。
	 *
	 * <p>先保留 RETURNED，再以伺服器今天是否晚於到期日判斷逾期。
	 * 僅回傳列舉值，不修改 Entity 或資料庫；這是目前放在 DAO 的日期判斷規則。
	 * 本方法不讀取歸還日期，也不因回傳 OVERDUE 而自動持久化該狀態。
	 *
	 * @param borrow 要計算狀態的借閱 Entity，不可為 null
	 * @return RETURNED、OVERDUE 或 BORROWED
	 * @throws NullPointerException borrow 為 null，或狀態不是 RETURNED 且到期日為 null 時發生
	 */
	public BorrowStatus calculateStatus(Borrow borrow) {
		
		if (borrow.getStatus() == BorrowStatus.RETURNED) {
			return BorrowStatus.RETURNED;
		}
		
		if (LocalDate.now().isAfter(borrow.getDueDate())) {
			return BorrowStatus.OVERDUE;
		}
		
		return BorrowStatus.BORROWED;
	}
}
