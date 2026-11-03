package com.tian.library.controller.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.MemberResponse;
import com.tian.library.entity.Member;
import com.tian.library.entity.Role;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/admin/member")
public class AdminMemberController {

	private final MemberService memberService;

	public AdminMemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping
	public ResponseEntity<List<MemberResponse>> findAll(HttpServletRequest httpRequest){
		
		HttpSession session = httpRequest.getSession(false);
		
		Integer memberId;
		if(session == null) {
			memberId = null;
		} else {
			memberId = (Integer)session.getAttribute("memberId");
		}
		
		if(memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		Member member = memberService.findById(memberId);
		
		if(member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		if (member.getRole() != Role.ADMIN) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		List<Member> memberList = memberService.findAll();
		
		List<MemberResponse> response = new ArrayList<>();
		
		for(Member memberItem : memberList) {
			response.add(new MemberResponse(
							memberItem.getId(),
							memberItem.getName(),
							memberItem.getEmail(),
							memberItem.getPhone(),
							memberItem.getCreatedAt(),
							memberItem.getRole())
					);
		}
		
		return ResponseEntity.ok(response);
		
	}

	@GetMapping(params = "name")
	public ResponseEntity<List<MemberResponse>> findByName(@RequestParam String name, HttpServletRequest httpRequest) {

		HttpSession session = httpRequest.getSession(false);

		Integer memberId;

		if (session == null) {
			memberId = null;
		} else {
			memberId = (Integer) session.getAttribute("memberId");
		}

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Member member = memberService.findById(memberId);

		if (member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (member.getRole() != Role.ADMIN) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		List<Member> memberList = memberService.findByName(name);

		List<MemberResponse> response = new ArrayList<>();

		for (Member memberItem : memberList) {
			response.add(new MemberResponse(
					memberItem.getId(),
					memberItem.getName(),
					memberItem.getEmail(),
					memberItem.getPhone(),
					memberItem.getCreatedAt(),
					memberItem.getRole()));
		}

		return ResponseEntity.ok(response);

	}
}
