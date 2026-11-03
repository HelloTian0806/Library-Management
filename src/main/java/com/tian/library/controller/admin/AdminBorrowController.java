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

@RestController
@RequestMapping("/admin/borrows")
public class AdminBorrowController {
	
	private final BorrowService borrowService;
	private final MemberService memberService;

	public AdminBorrowController(BorrowService borrowService, MemberService memberService) {
		super();
		this.borrowService = borrowService;
		this.memberService = memberService;
	}
	
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
	
	

