package com.tian.library.entity;

import java.time.LocalDate;

/**
 * 保存一筆借閱的會員 ID、書籍 ID、日期與狀態，由 DAO 手動映射。
 *
 * <p>會員與書籍以整數 ID 關聯，這裡沒有自動載入 Member 或 Book 的物件關聯。
 * LocalDate 只表示日期，不包含時分秒或時區；日期的取得與十四天借期由 BorrowService 決定。
 *
 * <p>本類別沒有 JPA 變更追蹤，setter 不會自動更新資料庫。
 * 狀態可能由 DAO 查詢後動態計算，因此物件中的狀態不一定等於資料庫原本儲存的字串。
 * 對外借閱查詢使用 BorrowResponse 或 AdminBorrowResponse，以便另外包含書名及會員姓名。
 */
public class Borrow {
	/**
	 * 借閱紀錄 ID，與會員 ID 或書籍 ID 是不同的識別值。
	 */
	private int id;
	/**
	 * 此筆借閱所屬的會員 ID；還書所有權由 Service 以此欄位比對操作者。
	 */
	private int memberId;
	private int bookId;
	private LocalDate borrowDate;
	/**
	 * 到期日期；本物件不自行計算，也不保證晚於借閱日。
	 */
	private LocalDate dueDate;
	/**
	 * 歸還日期；DAO 允許將 SQL NULL 映射為 null，代表沒有記錄歸還日期。
	 */
	private LocalDate returnDate;
	/**
	 * 借閱狀態，可能是資料庫讀值或 DAO 計算後的顯示狀態；設定它不會連帶更新日期或庫存。
	 */
	private BorrowStatus status;
	/**
	 * 建立欄位尚未填入的物件；整數欄位預設為零，日期與狀態預設為 null。
	 * 此建構子不查詢資料庫，也不自動產生借閱日期。
	 */
	public Borrow() {
		
	}
	/**
	 * 建立不含借閱 ID 與歸還日期的借閱物件，現有 Service 用於準備新增資料。
	 *
	 * @param memberId 借閱會員 ID
	 * @param bookId 借閱書籍 ID
	 * @param borrowDate 上層決定的借閱日
	 * @param dueDate 上層決定的到期日
	 * @param status 上層決定的初始狀態
	 */
	public Borrow(int memberId, int bookId, LocalDate borrowDate,LocalDate dueDate, BorrowStatus status) {
		super();
		this.memberId = memberId;
		this.bookId = bookId;
		this.borrowDate = borrowDate;
		this.dueDate = dueDate;
		this.status = status;
	}
	
	
	/**
	 * 依傳入欄位建立完整借閱物件，供 DAO 映射查詢結果，不在此檢查欄位一致性。
	 *
	 * @param id 借閱紀錄 ID
	 * @param memberId 所屬會員 ID
	 * @param bookId 書籍 ID
	 * @param borrowDate 借閱日
	 * @param dueDate 到期日
	 * @param returnDate 歸還日期，可為 null
	 * @param status 傳入的借閱狀態
	 */
	public Borrow(int id, int memberId, int bookId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
			BorrowStatus status) {
		super();
		this.id = id;
		this.memberId = memberId;
		this.bookId = bookId;
		this.borrowDate = borrowDate;
		this.dueDate = dueDate;
		this.returnDate = returnDate;
		this.status = status;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getMemberId() {
		return memberId;
	}
	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}
	public int getBookId() {
		return bookId;
	}
	public void setBookId(int bookId) {
		this.bookId = bookId;
	}
	public LocalDate getBorrowDate() {
		return borrowDate;
	}
	public void setBorrowDate(LocalDate borrowDate) {
		this.borrowDate = borrowDate;
	}
	public LocalDate getDueDate() {
		return dueDate;
	}
	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}
	public LocalDate getReturnDate() {
		return returnDate;
	}
	public void setReturnDate(LocalDate returnDate) {
		this.returnDate = returnDate;
	}
	public BorrowStatus getStatus() {
		return status;
	}
	/**
	 * 只改變記憶體中的狀態，不更新歸還日期、庫存或資料庫。
	 *
	 * @param status 要保存在此物件的狀態
	 */
	public void setStatus(BorrowStatus status) {
		this.status = status;
	}
	
	/**
	 * 提供含各識別值、日期與狀態的除錯文字，不會觸發關聯查詢。
	 *
	 * @return 此借閱物件的文字表示
	 */
	@Override
	public String toString() {
		return "Borrow{"+"Id="+id+", MemberId="+memberId+
				", BookId="+bookId+
				", BorrowDate="+borrowDate+
				", DueDate="+dueDate+
				", returnDate="+returnDate+
				", Status="+status+"}";
	}
	
}
