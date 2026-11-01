package com.tian.library.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Member;
import com.tian.library.service.MemberService;

@RestController
@RequestMapping("/member")
public class MemberController {
	
	private final MemberService memberService;
	
	public MemberController (MemberService memberService) {
		
		this.memberService = memberService;
		
	}
	
	@GetMapping(params="email")
	public Member findByMemberEmail(@RequestParam String email) {
		
		return memberService.findByMemberEmail(email);
		
	}
	
	
	
}
