package com.tian.library.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Book;
import com.tian.library.service.BookService;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/books")
public class BookController {
	private final BookService bookService;
	
	public BookController (BookService bookService) {
		this.bookService = bookService;
	}
	
	@GetMapping
	public List<Book> findAll(){
		return bookService.findAll();
	}
	@GetMapping (params="title")
	List<Book> findByTitle(String title){
		return bookService.findByTitle(title);
	}
}
