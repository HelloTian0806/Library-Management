package com.tian.library.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Member;
import com.tian.library.service.MemberService;

@RestController
@RequestMapping("/admin/member")
public class AdminMemberController {
	
	private final MemberService memberService;

	public AdminMemberController(MemberService memberService) {
		super();
		this.memberService = memberService;
	}
	
	@GetMapping
	public List<Member> findAll(){
		
		return memberService.findAll();
		
	}
	
	@GetMapping(params="name")
	public List<Member> findByName(@RequestParam String name) {
			
		return memberService.findByName(name);
		
	}
}
