package com.tian.library.controller;

import java.sql.SQLException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.service.BorrowService;

import com.tian.library.entity.Borrow;
@RestController
@RequestMapping("/borrows")
public class BorrowController {
	private final BorrowService borrowService;

	public BorrowController(BorrowService borrowService) {
		this.borrowService = borrowService;
	}
	
	//查詢
	@GetMapping
	public List<Borrow> findAll(){
		return borrowService.findAll();
	}
	// 先用id查詢，之後可以新增以會員姓名、書名、借閱狀態等的條件搜尋
	@GetMapping("/{id}")
	public Borrow findById(@PathVariable int id) {
		return borrowService.findById(id);
	}
	// 新增借書
	@PostMapping
	public boolean add(@RequestBody Borrow borrow) throws SQLException{
		return borrowService.add(borrow.getMemberId(), borrow.getBookId());
	}
	// 還書
	@PutMapping("/{id}/return")
	public boolean returnBook(@PathVariable int borrowId) throws SQLException{
		return borrowService.returnBook(borrowId);
	}
}
