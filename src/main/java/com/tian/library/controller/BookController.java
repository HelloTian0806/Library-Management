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
 * 提供公開的書名搜尋入口，負責接收 HTTP 查詢條件，再委派 BookService 查詢。
 *
 * <p>{@code @RestController} 將類別註冊為 Spring 管理的 Controller，並讓方法回傳值成為回應內容。
 * 在目前的 Web 設定下，書籍清單通常由 Jackson 序列化為 JSON。
 * {@code @RequestMapping("/books")} 定義類別共用路徑；此類別內沒有 Session 或角色檢查。
 *
 * <p>Controller 處理 HTTP 契約，Service 處理業務規則，DAO 才執行 SQL。
 * 這樣可避免將資料庫操作與網頁請求處理混在同一個方法。
 */

@RestController
@RequestMapping("/books")
public class BookController {
	private final BookService bookService;
	
	/**
	 * 透過建構子注入取得 BookService，依賴由 Spring 組裝，不在 Controller 自行建立。
	 * 單一建構子可省略 {@code @Autowired}；final 欄位讓依賴在建構後保持同一個參照。
	 *
	 * @param bookService 處理書籍查詢與驗證的服務
	 */
	public BookController (BookService bookService) {
		this.bookService = bookService;
	}
	
	// 用書名查詢
	/**
	 * 處理帶有 title 參數的 GET /books 請求，例如 {@code /books?title=Java}。
	 *
	 * <p>{@code params="title"} 是選擇此處理方法的條件，不是 SQL 篩選條件。
	 * {@code @RequestParam} 將查詢參數綁定到 title；空字串仍算有提供參數，並不會自動驗證內容。
	 * 搜尋會經由 Service 交給 DAO，以 LIKE 進行包含式比對；本方法不加入分頁。
	 *
	 * @param title 要搜尋的書名條件，由網址查詢參數提供
	 * @return 正常完成時為 200 與書籍清單；查無資料為 JSON 空陣列
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@GetMapping (params="title")
	List<Book> findByTitle(@RequestParam String title) throws SQLException{
		return bookService.findByTitle(title);
	}


}

