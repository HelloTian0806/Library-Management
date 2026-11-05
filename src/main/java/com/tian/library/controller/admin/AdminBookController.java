package com.tian.library.controller.admin;

import java.sql.SQLException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.entity.Book;
import com.tian.library.entity.Member;
import com.tian.library.entity.Role;
import com.tian.library.service.BookService;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 提供 /admin/books 下的書籍新增、修改、刪除及全部查詢。
 *
 * <p>{@code @RestController} 將回傳值作為回應內容，{@code @RequestMapping} 設定共同路徑。
 * 每個入口都先呼叫 checkAdmin，確認登入者存在且角色為 ADMIN，再呼叫 BookService。
 * URL 中的 admin 只是命名，真正的授權由後端程式執行。
 *
 * <p>目前直接以 Book Entity 接收與輸出書籍資料，沒有獨立的書籍 DTO。
 * 必要欄位及 ISBN 檢查由 Service 執行；本類別未使用 {@code @Valid} 觸發欄位驗證。
 * SQLException 向外傳遞，由 GlobalExceptionHandler 回應 500。
 */
@RestController
@RequestMapping("/admin/books")
public class AdminBookController {

	private final BookService bookService;
	private final MemberService memberService;

	/**
	 * 由 Spring 注入書籍與會員服務，分別處理書籍規則及登入者角色查詢。
	 *
	 * @param bookService 處理書籍驗證與資料操作的服務
	 * @param memberService 查詢登入者是否存在及其目前角色的服務
	 */
	public AdminBookController(BookService bookService, MemberService memberService) {
		this.bookService = bookService;
		this.memberService = memberService;
	}

	/**
	 * 處理 POST /admin/books，驗證管理者後新增書籍。
	 *
	 * <p>{@code @RequestBody} 將 JSON 綁定為 Book；綁定本身不等於完成業務驗證。
	 * BookService 才檢查必要欄位、數量與 ISBN，並初始化可借數量。
	 *
	 * @param book 請求本文提供的書籍 Entity
	 * @param request 用於取得既有 Session 的請求
	 * @return 未登入或登入會員無效為 401；非管理者為 403；通過授權後為 200 與新增結果，false 仍是 200
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@PostMapping
	public ResponseEntity<Boolean> add(@RequestBody Book book, HttpServletRequest request) throws SQLException {

		HttpStatus errorStatus = checkAdmin(request);

		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}

		return ResponseEntity.ok(bookService.add(book));
	}

	/**
	 * 處理 PUT /admin/books/{id}，以 URL 中的 ID 決定修改目標。
	 *
	 * <p>{@code @PathVariable} 綁定目標 ID，{@code @RequestBody} 綁定修改資料。
	 * 通過授權後以路徑 ID 覆蓋本文中的 ID，避免兩個來源不一致。
	 * BookService 及 DAO 更新書目欄位，目前不藉此修改館藏或可借數量。
	 *
	 * @param id URL 中要修改的書籍 ID
	 * @param book 包含待修改書目資料的 Entity
	 * @param request 用於取得既有 Session 的請求
	 * @return 未登入或登入會員無效為 401；非管理者為 403；通過授權後為 200 與修改結果
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@PutMapping("/{id}")
	public ResponseEntity<Boolean> update(@PathVariable int id, @RequestBody Book book, HttpServletRequest request)
			throws SQLException {

		HttpStatus errorStatus = checkAdmin(request);

		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}

		book.setId(id);
		return ResponseEntity.ok(bookService.update(book));
	}

	/**
	 * 處理 DELETE /admin/books/{id}，通過管理者驗證後委派刪除。
	 *
	 * <p>{@code @DeleteMapping} 選擇 DELETE 請求，不會自動刪除資料；真正的刪除由下層 SQL 執行。
	 * 資料庫若有外鍵限制，操作可能拋出 SQLException，而不是回傳成功。
	 *
	 * @param id URL 中要刪除的書籍 ID
	 * @param request 用於取得既有 Session 的請求
	 * @return 未登入或登入會員無效為 401；非管理者為 403；通過授權後為 200 與刪除結果
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Boolean> delete(@PathVariable int id, HttpServletRequest request) throws SQLException {

		HttpStatus errorStatus = checkAdmin(request);

		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}

		return ResponseEntity.ok(bookService.delete(id));
	}

	/**
	 * 處理 GET /admin/books，通過管理者驗證後查詢全部書籍。
	 *
	 * @param request 用於取得既有 Session 的請求
	 * @return 未登入或登入會員無效為 401；非管理者為 403；成功為 200 與 Book 清單，查無資料為空陣列
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@GetMapping
	public ResponseEntity<List<Book>> findAll(HttpServletRequest request) throws SQLException {

		HttpStatus errorStatus = checkAdmin(request);

		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}

		return ResponseEntity.ok(bookService.findAll());
	}

	/**
	 * 共用管理者驗證流程，將失敗原因交給各入口組成 HTTP 回應。
	 *
	 * <p>先尋找既有 Session，再檢查 memberId 確實為 Integer，避免對不合型別的屬性直接轉型。
	 * 接著向資料庫重新取得會員及角色，讓刪除會員或角色變更能反映在後續請求。
	 * 401 表示沒有有效登入身分；403 表示身分有效但不是管理者。
	 *
	 * @param request 目前的 HTTP 請求
	 * @return 驗證失敗時的 401 或 403；null 是此方法約定的通過標記，不是 HTTP 回應
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	private HttpStatus checkAdmin(HttpServletRequest request) throws SQLException {

		// 不為未登入的請求建立新 Session。
		HttpSession session = request.getSession(false);

		if (session == null) {
			return HttpStatus.UNAUTHORIZED;
		}

		// Session 存在，不代表其中一定有有效的登入者 ID。
		Object memberIdAttribute = session.getAttribute("memberId");

		if (!(memberIdAttribute instanceof Integer)) {
			return HttpStatus.UNAUTHORIZED;
		}

		Integer memberId = (Integer) memberIdAttribute;

		// 重新取得會員與角色，使會員刪除或角色變更能反映。
		Member member = memberService.findById(memberId);

		if (member == null) {
			return HttpStatus.UNAUTHORIZED;
		}

		if (member.getRole() != Role.ADMIN) {
			return HttpStatus.FORBIDDEN;
		}

		return null;
	}
}