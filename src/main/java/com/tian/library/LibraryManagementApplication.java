package com.tian.library;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.tian.library.dao.BookDAO;
import com.tian.library.dao.BorrowDAO;
import com.tian.library.dto.RegisterRequest;
import com.tian.library.entity.Borrow;
import com.tian.library.service.BookService;
import com.tian.library.service.BorrowService;
import com.tian.library.service.MemberService;

@SpringBootApplication
public class LibraryManagementApplication implements CommandLineRunner{
	private final BookService bookService;
	private final BorrowService borrowService;
	private final MemberService memberService;
	private final BookDAO bookDAO;
	private final BorrowDAO borrowDAO;
	public LibraryManagementApplication(BookService bookService,BorrowService borrowService, MemberService memberService, BookDAO bookDAO, BorrowDAO borrowDAO) {
		this.bookService = bookService;
		this.borrowService = borrowService;
		this.memberService = memberService;
		this.bookDAO = bookDAO;
		this.borrowDAO = borrowDAO;
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
//		borrowService.returnBook(4);
//		
//		borrowService.findAll();
		
		//註冊 測試
//		RegisterRequest req = new RegisterRequest();
//		req.setEmail("A12345@gamil.com");
//		req.setName("test");
//		req.setPassword("12345678");
//		req.setPhone("0912345678");
//		boolean success = memberService.register(req);
//		if(success) {
//			System.out.println("成功");
//		}else {
//			System.out.println("失敗");
//		}
		
//		Borrow borrow = borrowDAO.findById(5);
//		System.out.println(borrow.getStatus());
		

	}
	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementApplication.class, args);



	}

}
