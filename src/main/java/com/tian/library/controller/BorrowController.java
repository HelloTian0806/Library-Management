package com.tian.library.controller;

import java.sql.SQLException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.service.BorrowService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import com.tian.library.dto.BorrowRequest;
import com.tian.library.dto.BorrowResponse;
import com.tian.library.entity.Borrow;
@RestController
@RequestMapping("/borrows")
public class BorrowController {
	private final BorrowService borrowService;

	public BorrowController(BorrowService borrowService) {
		this.borrowService = borrowService;
	}
	

	@GetMapping("/me")
	public ResponseEntity<List<BorrowResponse>> findMemberById(HttpSession session) {
		
		Integer memberId = (Integer) session.getAttribute("memberId");
		
		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		List<BorrowResponse> borrows = borrowService.findMemberById(memberId);
		
		return ResponseEntity.ok(borrows);
	}
	// 新增借書
	@PostMapping
	public ResponseEntity<Boolean> add(@RequestBody @Valid BorrowRequest borrowRequest, HttpServletRequest httpRequest) throws SQLException{
		
		HttpSession session = httpRequest.getSession(false);
		
		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		Integer memberId = (Integer)session.getAttribute("memberId");
		
		boolean success = borrowService.add(memberId, borrowRequest.getBookId());
		
		return ResponseEntity.ok(success);
	}
	// 還書
	@PutMapping("/{borrowId}/return")
	public boolean returnBook(@PathVariable int borrowId) throws SQLException{
		return borrowService.returnBook(borrowId);
	}
}
