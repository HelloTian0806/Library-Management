package com.tian.library.entity;

/**
 * 定義一般會員與管理者角色，供後端明確比較角色值。
 *
 * <p>資料庫以名稱儲存，DAO 使用 valueOf 轉換；列舉本身不執行登入或授權。
 * 現有管理端 Controller 會重新查詢會員角色，再以 Role.ADMIN 判斷可否存取。
 */
public enum Role {
	/**
	 * 一般會員角色；目前註冊 SQL 固定使用此值。
	 */
	USER,
	/**
	 * 管理者角色；是否能呼叫管理 API，仍由後端入口執行角色檢查。
	 */
	ADMIN
}
