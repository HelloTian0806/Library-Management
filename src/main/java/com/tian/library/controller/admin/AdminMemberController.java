package com.tian.library.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Member;
import com.tian.library.service.MemberService;

/**
 * 提供管理端全部會員與姓名查詢，透過 MemberService 取得資料。
 * 目前直接回傳 Member Entity，沒有 Response DTO 轉換，也沒有 Session 或角色檢查。
 * admin 路徑與 package 命名不會自動提供授權保護。
 */
@RestController
@RequestMapping("/admin/member")
public class AdminMemberController {
	
	private final MemberService memberService;

	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param memberService 此 Controller 使用的業務 Service
	 */
	public AdminMemberController(MemberService memberService) {
		super();
		this.memberService = memberService;
	}
	
	/**
	 * 查詢全部會員；若請求包含 name，Spring 會優先選擇較具體的姓名查詢 mapping。
	 * @return Service 回傳的會員 Entity 清單
	 */
	@GetMapping
	public List<Member> findAll(){
		
		return memberService.findAll();
		
	}
	
	/**
	 * 以姓名 query parameter 查詢會員，params 只限制參數存在，不會驗證呼叫者權限。
	 * @param name 會員姓名搜尋條件
	 * @return 符合條件的會員 Entity 清單
	 */
	@GetMapping(params="name")
	public List<Member> findByName(@RequestParam String name) {
			
		return memberService.findByName(name);
		
	}
}
