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
 * 以 JDBC 存取借閱資料，負責 SQL、參數綁定與 ResultSet 映射，供 Service 呼叫。
 * @Repository 將 DAO 註冊為 Spring Bean，但不會自動替手寫 JDBC 開始 Transaction。
 * PreparedStatement 的 ? 將資料與 SQL 結構分離，避免直接串接輸入造成 SQL injection；參數索引從 1 開始。
 * 自行取得 Connection 的方法用 try-with-resources 關閉資源；關閉 Statement 也會關閉其目前的 ResultSet。
 * 部分方法攔截 SQLException 後僅印出堆疊，因此零值、null 或空集合可能同時代表失敗，並非只有查無資料。
 */
@Repository
public class BorrowDAO {
	private final DataSource dataSource;

	/**
	 * 透過 constructor injection 取得 DataSource，由連線池統一管理 Connection。
	 * @param dataSource Spring 設定的 JDBC 資料來源
	 */
	public BorrowDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 新增借閱紀錄，借閱日、到期日與狀態由呼叫端提供，DAO 不自行訂定借閱期限。
	 * @param borrow 借閱 Entity
	 * @return executeUpdate 的影響筆數；SQLException 被攔截時回傳 0
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
	 * 新增借閱紀錄，借閱日、到期日與狀態由呼叫端提供，DAO 不自行訂定借閱期限。
	 * 僅在 try-with-resources 關閉 PreparedStatement，保留呼叫端 Connection 以參與同一 Transaction。
	 * @param conn 呼叫端持有的 Connection，本方法不提交、回滾或關閉它
	 * @param borrow 借閱 Entity
	 * @return executeUpdate 的影響筆數
	 * @throws SQLException JDBC 失敗時向外傳遞，讓呼叫端決定 rollback
	 */
	public int add(Connection conn, Borrow borrow) throws SQLException {
		String sql ="""
				INSERT INTO borrows (member_id, book_id, borrow_date, due_date, status) VALUES(?,?,?,?,?);
				""";
		
		try (PreparedStatement ps = conn.prepareStatement(sql)){
			ps.setInt(1, borrow.getMemberId());
			ps.setInt(2, borrow.getBookId());
			ps.setDate(3, java.sql.Date.valueOf(borrow.getBorrowDate()));
			ps.setDate(4, java.sql.Date.valueOf(borrow.getDueDate()));
			ps.setString(5, borrow.getStatus().name());
			
			return ps.executeUpdate();
		}
		
		
	}

	/**
	 * 依借閱 ID 讀取 Entity，再依今天日期計算回傳狀態；setStatus 只修改 Java 物件，不會更新資料庫。
	 * @param borrowId 借閱紀錄 ID
	 * @return 找到的 Entity；查無資料或部分 SQL 失敗情況為 null
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
				
				borrow.setStatus(calculateStatus(borrow));
			}
		} 
		return borrow;
	}
	
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
				
				borrow.setStatus(calculateStatus(borrow));
			}
		}
		
		return borrow;
	}
	
	
	
	
	/**
	 * 以會員 ID 篩選借閱並 JOIN books 取得書名，直接映射會員使用的 BorrowResponse。
	 * 單次 JOIN 避免查出清單後再為每筆書籍各查一次的 N+1 查詢；呼叫者權限由上層判斷。
	 * @param memberId 會員 ID
	 * @return 結果清單；查無資料為空集合，SQL 失敗時也可能回傳空或已映射的部分清單
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
	 * JOIN members 與 books，一次取得管理端 DTO 所需的會員姓名與書名。
	 * INNER JOIN 只會回傳能匹配兩側關聯資料的紀錄，且查詢沒有 ORDER BY 或分頁。
	 * @return 結果清單；查無資料為空集合，SQL 失敗時也可能回傳空或已映射的部分清單
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
				
				borrow.setStatus(calculateStatus(borrow));
				
				borrowList.add(borrow);
			}
		} 
		
		return borrowList;
	}
	
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
				
				borrow.setStatus(calculateStatus(borrow));
				
				borrowList.add(borrow);
			}
		}
		
		return borrowList;
	}

	/**
	 * 依借閱 ID 將指定狀態寫入資料庫，不同於查詢時只修改 Java 物件的 calculateStatus。
	 * 此 SQL 不會同步更新歸還日期，也未限制前一個狀態。
	 * @param borrowId 借閱紀錄 ID
	 * @param status 要寫入的借閱狀態
	 * @return 影響筆數大於零時為 true，否則為 false；SQLException 被攔截時也回傳 false
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
	 * 單獨更新歸還日期，不會一併更新狀態或庫存，不能視為完整還書流程。
	 * @param borrowId 借閱紀錄 ID
	 * @param returnDate 要寫入的歸還日期
	 * @return 影響筆數大於零時為 true，否則為 false；SQLException 被攔截時也回傳 false
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
	 * 使用呼叫端 Connection，同時更新 RETURNED 狀態與歸還日期，庫存由 Service 另外協調。
	 * 僅在 try-with-resources 關閉 PreparedStatement，保留呼叫端 Connection 以參與同一 Transaction。
	 * WHERE 只有借閱 ID，沒有舊狀態條件，因此不能單靠此更新防止同時重複還書。
	 * @param conn 呼叫端持有的 Connection，本方法不提交、回滾或關閉它
	 * @param borrowId 借閱紀錄 ID
	 * @param returnDate 要寫入的歸還日期
	 * @return 影響筆數大於零時為 true，否則為 false
	 * @throws SQLException JDBC 失敗時向外傳遞，讓呼叫端決定 rollback
	 */
	public int returnBook(Connection conn, int borrowId, LocalDate returnDate) throws SQLException{
		String sql = """
				UPDATE borrows SET status = ? , return_date = ? WHERE id = ?
				""";
		try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, BorrowStatus.RETURNED.name());
			pstmt.setDate(2, java.sql.Date.valueOf(returnDate));
			pstmt.setInt(3, borrowId);
			int row = pstmt.executeUpdate();
			return row;
		}
	}
	
	/**
	 * 以 return_date IS NULL 計算尚未歸還筆數，不限於資料庫的 BORROWED 狀態。
	 * @return SQL 統計值；SQL 失敗或未取得結果時也回傳 0
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
	 * 此查詢沒有依 due_date 計算，結果可能與查詢時動態算出的逾期清單不同。
	 * @return SQL 統計值；SQL 失敗或未取得結果時也回傳 0
	 */
	public int countOverdueBorrows() throws SQLException{
		String sql = """
				SELECT count(*) FROM borrows WHERE return_date IS NULL AND due_date < ?
				 """;
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			pstmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				return rs.getInt("count(*)");
			}
			
		}
		return 0;
	}
	
	/**
	 * 保留已歸還狀態，否則依今天是否晚於到期日判斷 OVERDUE 或 BORROWED。
	 * 到期日當天不算逾期；LocalDate.now 使用執行環境預設時區，此方法不寫回資料庫。
	 * 這是目前放在 DAO 的業務判斷，非 SQL 資料映射所必須的操作。
	 * @param borrowResponse 包含狀態及到期日的借閱 DTO
	 * @return RETURNED、OVERDUE 或 BORROWED
	 * @throws NullPointerException 傳入物件為 null，或未歸還且到期日為 null 時可能發生
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
	 * 保留已歸還狀態，否則依今天是否晚於到期日判斷 OVERDUE 或 BORROWED。
	 * 到期日當天不算逾期；LocalDate.now 使用執行環境預設時區，此方法不寫回資料庫。
	 * 這是目前放在 DAO 的業務判斷，非 SQL 資料映射所必須的操作。
	 * @param borrowResponse 包含狀態及到期日的借閱 DTO
	 * @return RETURNED、OVERDUE 或 BORROWED
	 * @throws NullPointerException 傳入物件為 null，或未歸還且到期日為 null 時可能發生
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
	 * 保留已歸還狀態，否則依今天是否晚於到期日判斷 OVERDUE 或 BORROWED。
	 * 到期日當天不算逾期；LocalDate.now 使用執行環境預設時區，此方法不寫回資料庫。
	 * 這是目前放在 DAO 的業務判斷，非 SQL 資料映射所必須的操作。
	 * @param borrow 包含狀態及到期日的借閱 Entity
	 * @return RETURNED、OVERDUE 或 BORROWED
	 * @throws NullPointerException 傳入物件為 null，或未歸還且到期日為 null 時可能發生
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
