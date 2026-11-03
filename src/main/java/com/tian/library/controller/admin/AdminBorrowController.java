package com.tian.library.controller.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.AdminBorrowResponse;
import com.tian.library.dto.BorrowResponse;
import com.tian.library.entity.Borrow;
import com.tian.library.entity.Member;
import com.tian.library.entity.Role;
import com.tian.library.service.BorrowService;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 提供管理端全部借閱查詢，透過 BorrowService 取得清單，不直接執行 JDBC 或 SQL。
 * 目前回傳 Borrow Entity，未轉成管理端 DTO，也未在此檢查登入或管理者角色。
 */
@RestController
@RequestMapping("/admin/borrows")
public class AdminBorrowController {
	
	private final BorrowService borrowService;
	private final MemberService memberService;


	public AdminBorrowController(BorrowService borrowService, MemberService memberService) {

	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param borrowService 此 Controller 使用的業務 Service
	 */
		super();
		this.borrowService = borrowService;
		this.memberService = memberService;
	}
	/**
	 * 取得全部借閱紀錄，查詢及狀態處理由下層執行，此處原樣回傳。
	 * @return Borrow Entity 清單，並非含書名與會員姓名的管理端 DTO
	 */
	@GetMapping
	public ResponseEntity<List<AdminBorrowResponse>> findAll(HttpServletRequest httpRequest){

		HttpSession session = httpRequest.getSession(false);
		
		Integer memberId;
		
		if(session == null) {
			memberId = null;
		}else {
			memberId = (Integer)session.getAttribute("memberId");
		}
		
		if(memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		Member member = memberService.findById(memberId);
		
		if(member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		if(member.getRole() != Role.ADMIN) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		return ResponseEntity.ok(borrowService.findAll());
		}
	}
	
	

