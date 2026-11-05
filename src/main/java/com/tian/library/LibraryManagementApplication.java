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

/**
 * Spring Boot 啟動入口，並提供目前未啟用任何資料操作的命令列回呼。
 *
 * <p>{@code @SpringBootApplication} 組合設定類別、自動設定與元件掃描。
 * 從此類別所在的 com.tian.library Package 向下掃描，可找到 Controller、Service、DAO、Config 與例外處理元件。
 * Spring 建立容器時會組裝這些 Bean，並依建構子參數提供所需依賴。
 *
 * <p>實作 CommandLineRunner 後，SpringApplication 啟動流程會在容器初始化後呼叫 run。
 * 目前 run 的資料操作全部被註解，所以不會因這個回呼自動新增、查詢或修改資料。
 */
@SpringBootApplication
public class LibraryManagementApplication implements CommandLineRunner{
	private final BookService bookService;
	private final BorrowService borrowService;
	private final MemberService memberService;
	private final BookDAO bookDAO;
	private final BorrowDAO borrowDAO;
	/**
	 * 透過建構子保存 Spring 注入的依賴；目前 run 中的呼叫皆未啟用。
	 *
	 * @param bookService 書籍服務
	 * @param borrowService 借閱服務
	 * @param memberService 會員服務
	 * @param bookDAO 書籍資料存取元件
	 * @param borrowDAO 借閱資料存取元件
	 */
	public LibraryManagementApplication(BookService bookService,BorrowService borrowService, MemberService memberService, BookDAO bookDAO, BorrowDAO borrowDAO) {
		this.bookService = bookService;
		this.borrowService = borrowService;
		this.memberService = memberService;
		this.bookDAO = bookDAO;
		this.borrowDAO = borrowDAO;
	}
	// 下方是啟動回呼；其內的舊測試呼叫目前全部維持註解狀態。
	/**
	 * 在 Spring Boot 啟動流程中接收命令列參數，目前方法內沒有可執行的資料操作。
	 *
	 * <p>保留的註解程式碼是歷史手動測試紀錄，部分呼叫沿用舊方法簽章，不應視為可直接啟用的測試。
	 * 這個回呼不是 Controller API，也不會在每次 HTTP 請求時執行。
	 *
	 * @param args 傳給應用程式的命令列參數，本方法目前未使用
	 */
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
	/**
	 * 由 Java 執行程序進入 Spring Boot，建立並啟動應用程式容器。
	 *
	 * @param args 命令列參數，原樣交給 SpringApplication.run
	 * @throws RuntimeException 自動設定、Bean 初始化或其他啟動步驟失敗時可能向外傳遞
	 */
	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementApplication.class, args);



	}

}
