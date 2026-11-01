package com.tian.library.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.DashboardStatsResponse;
import com.tian.library.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
	
	private final DashboardService dashboardService;
	
	public DashboardController(DashboardService dashboardService) {
		
		this.dashboardService = dashboardService;
		
	}
	
	@GetMapping
	public DashboardStatsResponse dashboardStats() {
		
		return dashboardService.stats();
		
	}
}
