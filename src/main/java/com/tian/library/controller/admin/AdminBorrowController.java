package com.tian.library.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Borrow;
import com.tian.library.service.BorrowService;

@RestController
@RequestMapping("/admin/borrows")
public class AdminBorrowController {
	
	private final BorrowService borrowService;

	public AdminBorrowController(BorrowService borrowService) {
		super();
		this.borrowService = borrowService;
	}
	
	@GetMapping
	public List<Borrow> findAll(){
		return borrowService.findAll();
	}
	
	
}
