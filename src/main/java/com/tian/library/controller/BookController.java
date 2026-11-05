package com.tian.library.controller;

import java.sql.SQLException;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Book;
import com.tian.library.service.BookService;
/**
 * 提供書籍查詢的 HTTP 入口，將搜尋條件委派給 BookService，後續由 DAO 存取資料。
 * @RestController 讓回傳物件經由訊息轉換器序列化為 JSON；{@code @RequestMapping} 定義共同路徑。
 */

@RestController
@RequestMapping("/books")
public class BookController {
	private final BookService bookService;
	
	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param bookService 此 Controller 使用的業務 Service
	 */
	public BookController (BookService bookService) {
		this.bookService = bookService;
	}
	
	// 用書名查詢
	/**
	 * 將 query parameter 綁定為搜尋條件，實際查詢委派給 Service。
	 * @param title 要搜尋的書名條件
	 * @return 符合條件的書籍清單
	 */
	@GetMapping (params="title")
	List<Book> findByTitle(@RequestParam String title) throws SQLException{
		return bookService.findByTitle(title);
	}


}

