package com.tian.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.LoginRequest;
import com.tian.library.dto.RegisterRequest;
import com.tian.library.dto.UserResponse;
import com.tian.library.entity.Member;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class UserMemberController {
	private final MemberService memberService;
	
	public UserMemberController(MemberService memberService) {
		this.memberService = memberService;
	}
	
	@PostMapping("/register")
	public ResponseEntity<String> register (@RequestBody @Valid RegisterRequest request) {
			
		// 新增帳號
		boolean success = memberService.register(request);
		
		if(success) {
			return ResponseEntity.status(HttpStatus.CREATED).body("註冊成功");
		}
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body("Email已被註冊");
		
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login (@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest){
		
		Member member = memberService.login(request);
	
		if(member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤");
		}
		// 取得既有的session
		HttpSession oldSession = httpRequest.getSession(false);
		
		// 如果存在舊的session 就讓它失效
		if(oldSession != null) {
			oldSession.invalidate();
		}
		
		HttpSession session = httpRequest.getSession(true);
		
		session.setAttribute("memberId", member.getId());
		
		UserResponse user = new UserResponse(
				member.getId(),
				member.getName(),
				member.getEmail(),
				member.getRole()
				);
		return ResponseEntity.ok(user);
		
	}
	
	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletRequest httpRequest){
		HttpSession session = httpRequest.getSession(false);
		if(session != null) {
			session.invalidate();
		}
		
		return ResponseEntity.ok("登出成功");
	}
	
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me (HttpServletRequest httpRequest) {
		HttpSession session = httpRequest.getSession(false);
		
		if(session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		Integer memberId = (Integer)session.getAttribute("memberId");
		
		Member member = memberService.findById(memberId);
		
		if(member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		UserResponse user = new UserResponse(member.getId(), member.getName(), member.getEmail(), member.getRole());
		return ResponseEntity.ok(user);
	}
}
