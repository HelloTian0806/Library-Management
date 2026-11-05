package com.tian.library.dto;

/**
 * 定義管理端儀表板四項統計的輸出，由 DashboardService 組合 DAO 結果。
 *
 * <p>DTO 只保存各數值，不執行統計，也不提供四個查詢屬於同一時間快照的保證。
 * 沒有在建構子或 setter 檢查非負數、加總或各數值之間的關係。
 *
 * <p>此類別只包裝已取得的資料，不自行查詢資料庫或驗證授權。
 * 目前 JSON 序列化可依 getter 輸出屬性；setter 只改動 DTO，與原始 Entity 或資料庫沒有自動同步關係。
 */
public class DashboardStatsResponse {
	
	/**
	 * 書目資料列數，不是每筆 total_quantity 的館藏冊數加總。
	 */
	private int totalBooks;
	/**
	 * 會員資料列數，現有 SQL 不依角色篩選。
	 */
	private int totalMembers;
	/**
	 * 歸還日期為 SQL NULL 的借閱筆數，包含尚未歸還的逾期紀錄。
	 */
	private int activeBorrows;
	/**
	 * 尚未歸還且到期日早於伺服器今天的筆數，到期當天不計入。
	 */
	private int overdueBorrows;
	
	
	
	/**
	 * 按欄位順序保存四項統計，呼叫端需傳入語意正確的數值。
	 *
	 * @param totalBooks 書目總筆數
	 * @param totalMembers 會員總筆數
	 * @param activeBorrows 未歸還借閱筆數
	 * @param overdueBorrows 逾期且未歸還借閱筆數
	 */
	public DashboardStatsResponse(int totalBooks, int totalMembers, int activeBorrows, int overdueBorrows) {
		super();
		this.totalBooks = totalBooks;
		this.totalMembers = totalMembers;
		this.activeBorrows = activeBorrows;
		this.overdueBorrows = overdueBorrows;
	}
	
	public int getTotalBooks() {
		return totalBooks;
	}
	public void setTotalBooks(int totalBooks) {
		this.totalBooks = totalBooks;
	}
	public int getTotalMembers() {
		return totalMembers;
	}
	public void setTotalMembers(int totalMembers) {
		this.totalMembers = totalMembers;
	}
	public int getActiveBorrows() {
		return activeBorrows;
	}
	public void setActiveBorrows(int activeBorrows) {
		this.activeBorrows = activeBorrows;
	}
	public int getOverdueBorrows() {
		return overdueBorrows;
	}
	public void setOverdueBorrows(int overdueBorrows) {
		this.overdueBorrows = overdueBorrows;
	}
	
	
}
