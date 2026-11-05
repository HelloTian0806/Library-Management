package com.tian.library.dto;

import java.time.LocalDate;

import com.tian.library.entity.BorrowStatus;

/**
 * 定義管理端借閱清單與姓名搜尋的輸出，額外包含會員 ID 與會員姓名。
 *
 * <p>由 DAO 的借閱、會員及書籍 JOIN 結果建立，欄位是畫面需求的組合，不對應單一資料表。
 * id 是借閱紀錄 ID；memberId 是紀錄所屬會員，不能當成目前操作者的登入證明。
 *
 * <p>此類別只包裝已取得的資料，不自行查詢資料庫或驗證授權。
 * 目前 JSON 序列化可依 getter 輸出屬性；setter 只改動 DTO，與原始 Entity 或資料庫沒有自動同步關係。
 */
public class AdminBorrowResponse {
    private int id; 
    private int memberId;
    private String memberName;
    private String bookTitle;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    /**
     * 沒有歸還日期時可為 null；本 DTO 不會因日期改變而自動更新 status。
     */
    private LocalDate returnDate;
    private BorrowStatus status;
    
    
	/**
	 * 以 JOIN 查詢已取得的欄位建立管理端回應，不在這裡額外查詢會員或書籍。
	 *
	 * @param id 借閱紀錄 ID
	 * @param memberId 借閱所屬會員 ID
	 * @param memberName 會員姓名
	 * @param bookTitle 書名
	 * @param borrowDate 借閱日
	 * @param dueDate 到期日
	 * @param returnDate 歸還日期，可為 null
	 * @param status 傳入的狀態，DAO 可在建立後再設定動態計算的結果
	 */
	public AdminBorrowResponse(int id, int memberId, String memberName, String bookTitle, LocalDate borrowDate,
			LocalDate dueDate, LocalDate returnDate, BorrowStatus status) {
		super();
		this.id = id;
		this.memberId = memberId;
		this.memberName = memberName;
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
	public int getMemberId() {
		return memberId;
	}
	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}
	public String getMemberName() {
		return memberName;
	}
	public void setMemberName(String memberName) {
		this.memberName = memberName;
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
	public void setStatus(BorrowStatus status) {
		this.status = status;
	}
    
    
    
}
