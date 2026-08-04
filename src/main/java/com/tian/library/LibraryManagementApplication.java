package com.tian.library;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.tian.library.entity.Borrow;
import com.tian.library.service.BookService;
import com.tian.library.service.BorrowService;

@SpringBootApplication
public class LibraryManagementApplication implements CommandLineRunner{
	private final BookService bookService;
	private final BorrowService borrowService;
	public LibraryManagementApplication(BookService bookService,BorrowService borrowService) {
		this.bookService = bookService;
		this.borrowService = borrowService;
	}
	// 功能測試用
	@Override
	public void run(String... args) {
//		bookService.findAll();
//		bookService.findByIsbn("9780134685991");
//		boolean ok =bookService.findByTitle("Java");
//		if(ok) {
//			System.out.println("查詢成功");
//		}else {
//			System.out.println("查詢失敗");
//		}
		
//		bookService.add("123456", "test", "test", "test", "test", 5, 5);
//		bookService.delete("123456");
		// 借書測試
//		borrowService.borrowBook(1, 2);
//		 借書紀錄
//		borrowService.findAll();
//		// 還書
//		borrowService.returnBook(3);
//		
//		borrowService.findAll();

	}
	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementApplication.class, args);



	}

}
