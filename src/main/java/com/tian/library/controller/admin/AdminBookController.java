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

/**
 * 提供管理端書籍新增、修改、刪除與查詢入口，透過 BookService 執行業務操作。
 * admin 路徑只是路由分類，這個類別目前沒有 Session 或管理者權限檢查。
 */
@RestController
@RequestMapping("/admin/books")
public class AdminBookController {

	private final BookService bookService;

	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param bookService 此 Controller 使用的業務 Service
	 */
	public AdminBookController(BookService bookService) {
		super();
		this.bookService = bookService;
	}
	
	// 新增
	/**
	 * 將 JSON 綁定成 Book 後交給 Service 驗證及新增；此處沒有使用 {@code @Valid}。
	 * @param book 請求中的書籍資料，可借數量由 Service 在新增流程決定
	 * @return Service 的新增結果，false 仍以一般成功 HTTP 回應輸出
	 */
	@PostMapping
	public boolean add(@RequestBody Book book) {
		return bookService.add(book);
	}
	// 修改
	/**
	 * 以 URL 的資源 ID 決定修改目標，覆蓋 request body 可能提供的不同 ID。
	 * @param id 路徑指定的書籍 ID
	 * @param book 要交給 Service 的修改資料
	 * @return Service 的修改結果
	 */
	@PutMapping("/{id}")
	public boolean update(
			@PathVariable int id,
			@RequestBody Book book) {
		book.setId(id);
		return bookService.update(book);
	}
	// 刪除
	/**
	 * 將書籍刪除交給 Service，此處不額外轉換資料關聯限制等失敗原因。
	 * @param id 要刪除的書籍 ID
	 * @return Service 的刪除結果
	 */
	@DeleteMapping("/{id}")
	public boolean delete(@PathVariable int id) {
		return bookService.delete(id);
	}
	
	// 查詢全部
	/**
	 * 取得全部書籍，由 Spring 將回傳的 Entity 清單序列化為 JSON。
	 * @return 全部書籍清單
	 */
	@GetMapping
	public List<Book> findAll(){
		return bookService.findAll();
	}
	
}
