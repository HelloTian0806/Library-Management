package com.tian.library.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Borrow;
import com.tian.library.service.BorrowService;

/**
 * 提供管理端全部借閱查詢，透過 BorrowService 取得清單，不直接執行 JDBC 或 SQL。
 * 目前回傳 Borrow Entity，未轉成管理端 DTO，也未在此檢查登入或管理者角色。
 */
@RestController
@RequestMapping("/admin/borrows")
public class AdminBorrowController {
	
	private final BorrowService borrowService;

	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param borrowService 此 Controller 使用的業務 Service
	 */
	public AdminBorrowController(BorrowService borrowService) {
		super();
		this.borrowService = borrowService;
	}
	
	/**
	 * 取得全部借閱紀錄，查詢及狀態處理由下層執行，此處原樣回傳。
	 * @return Borrow Entity 清單，並非含書名與會員姓名的管理端 DTO
	 */
	@GetMapping
	public List<Borrow> findAll(){
		return borrowService.findAll();
	}
	
	
}
