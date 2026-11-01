package com.tian.library.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Book;
import com.tian.library.service.BookService;

@RestController
@RequestMapping("/admin/books")
public class AdminBookController {

	private final BookService bookService;

	public AdminBookController(BookService bookService) {
		super();
		this.bookService = bookService;
	}
	
	// 新增
	@PostMapping
	public boolean add(@RequestBody Book book) {
		return bookService.add(book);
	}
	// 修改
	@PutMapping("/{id}")
	public boolean update(
			@PathVariable int id,
			@RequestBody Book book) {
		book.setId(id);
		return bookService.update(book);
	}
	// 刪除
	@DeleteMapping("/{id}")
	public boolean delete(@PathVariable int id) {
		return bookService.delete(id);
	}
	
	// 查詢全部
	@GetMapping
	public List<Book> findAll(){
		return bookService.findAll();
	}
	
}
