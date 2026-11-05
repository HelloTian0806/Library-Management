package com.tian.library.entity;

/**
 * 定義借閱流程可使用的狀態名稱，避免在 Java 程式中散布任意狀態字串。
 *
 * <p>DAO 以 name() 寫入名稱、以 valueOf 讀回，必須與資料庫字串的拼字及大小寫一致。
 * 列舉本身不計算日期，也不限制狀態轉換；顯示狀態目前由 BorrowDAO.calculateStatus 計算。
 */
public enum BorrowStatus {
	/**
	 * 借閱中；現有借書 Service 建立新紀錄時使用此狀態。
	 */
	BORROWED,
	/**
	 * 已歸還；完整還書流程還會同步更新歸還日期與可借數量。
	 */
	RETURNED,
	/**
	 * 逾期；現有查詢可動態算出此狀態，不代表資料庫一定已寫入此名稱。
	 */
	OVERDUE
}
