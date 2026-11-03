package com.tian.library.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Member;
import com.tian.library.service.MemberService;

/**
 * 提供依 Email 查詢會員的 HTTP 入口，透過 MemberService 取得 Member Entity。
 * 目前直接回傳 Entity，未透過 Response DTO 限制輸出，也未在此檢查 Session 或角色。
 */
@RestController
@RequestMapping("/member")
public class MemberController {
	
	private final MemberService memberService;
	
	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param memberService 此 Controller 使用的業務 Service
	 */
	public MemberController (MemberService memberService) {
		
		this.memberService = memberService;
		
	}
	
	/**
	 * 依 Email 查詢會員，params 條件要求請求中有 email 參數。
	 * 目前下層查詢會填入密碼雜湊，直接回傳 Entity 可能使該欄位被序列化至 JSON。
	 * @param email 要查詢的會員 Email
	 * @return 找到的 Member；查無資料時為 null，此處沒有明確轉成 404
	 */
	@GetMapping(params="email")
	public Member findByMemberEmail(@RequestParam String email) {
		
		return memberService.findByMemberEmail(email);
		
	}
	
	
	
}
