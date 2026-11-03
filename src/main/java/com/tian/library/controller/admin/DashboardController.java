package com.tian.library.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.DashboardStatsResponse;
import com.tian.library.service.DashboardService;

/**
 * 提供儀表板統計入口，由 DashboardService 彙整各 DAO 的統計結果。
 * 雖位於 admin package，實際路徑由 {@code @RequestMapping} 決定，為 /dashboard。
 * 目前此類別沒有檢查 Session 或管理者角色。
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {
	
	private final DashboardService dashboardService;
	
	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param dashboardService 此 Controller 使用的業務 Service
	 */
	public DashboardController(DashboardService dashboardService) {
		
		this.dashboardService = dashboardService;
		
	}
	
	/**
	 * 取得統計 DTO，統計 SQL 與彙整工作由下層負責，Controller 不直接操作資料庫。
	 * @return 書籍、會員、未歸還及逾期借閱的統計結果，計算定義由下層實作決定
	 */
	@GetMapping
	public DashboardStatsResponse dashboardStats() {
		
		return dashboardService.stats();
		
	}
}
