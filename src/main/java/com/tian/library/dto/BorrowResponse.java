package com.tian.library.dto;

import java.time.LocalDate;

import com.tian.library.entity.BorrowStatus;

/**
 * 定義會員端借閱查詢的輸出，包含書名、日期與狀態，不是 Borrow Entity 的逐欄複製。
 *
 * <p>DAO 透過 JOIN 取得 bookTitle 後直接建立此 DTO；id 代表借閱紀錄 ID，而不是書籍 ID。
 * 目前查詢後由 DAO 計算並設定顯示狀態，DTO 不自行判斷逾期。
 *
 * <p>此類別只包裝已取得的資料，不自行查詢資料庫或驗證授權。
 * 目前 JSON 序列化可依 getter 輸出屬性；setter 只改動 DTO，與原始 Entity 或資料庫沒有自動同步關係。
 */
public class BorrowResponse {
	/**
	 * 借閱紀錄識別值，還書 API 使用的是此種 ID，而非書目 ID。
	 */
	private int id;
	private String bookTitle;
	private LocalDate borrowDate;
	private LocalDate dueDate;
	/**
	 * 歸還日期；資料庫沒有記錄日期時映射為 null，前端須依回應值呈現。
	 */
	private LocalDate returnDate;
	
	/**
	 * 保存會員端借閱顯示所需欄位，不執行日期計算或資料庫寫入。
	 *
	 * @param id 借閱紀錄 ID
	 * @param bookTitle JOIN 查詢取得的書名
	 * @param borrowDate 借閱日
	 * @param dueDate 到期日
	 * @param returnDate 歸還日期，可為 null
	 * @param status 初始傳入的狀態，DAO 查詢後可能再以計算結果設定
	 */
	public BorrowResponse(int id, String bookTitle, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
			BorrowStatus status) {
		super();
		this.id = id;
		this.bookTitle = bookTitle;
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
	public String getBookTitle() {
		return bookTitle;
	}
	public void setBookTitle(String bookTitle) {
		this.bookTitle = bookTitle;
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
	 * 保存上層計算的顯示狀態，不執行 UPDATE，也不重新計算日期。
	 *
	 * @param status 要呈現在回應中的狀態
	 */
	public void setStatus(BorrowStatus status) {
		this.status = status;
	}
	/**
	 * 目前保存的狀態值；欄位放在方法後面不影響 getter 所提供的 JSON 屬性。
	 */
	private BorrowStatus status;
}
