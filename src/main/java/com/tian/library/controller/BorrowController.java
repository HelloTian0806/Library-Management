package com.tian.library.controller;

import java.sql.SQLException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.service.BorrowService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import com.tian.library.dto.BorrowRequest;
import com.tian.library.dto.BorrowResponse;

/**
 * 提供 /borrows 下的個人借閱查詢、借書與還書 HTTP 入口。
 *
 * <p>{@code @RestController} 讓回傳值作為回應內容，{@code @RequestMapping} 定義共同路徑。
 * Controller 從既有 Session 取得 memberId，再委派 BorrowService；不接受請求自行指定操作者 ID。
 * 前端即使隱藏按鈕，也不能代替這裡的登入檢查。
 *
 * <p>BorrowRequest 是輸入契約，BorrowResponse 是輸出契約，不需要直接暴露完整 Borrow Entity。
 * 借閱規則、所有權判斷及交易由 Service 處理；Controller 不執行 commit 或 rollback。
 * 向外傳遞的 SQLException 由 GlobalExceptionHandler 統一轉成 500 回應。
 */
@RestController
@RequestMapping("/borrows")
public class BorrowController {
	private final BorrowService borrowService;

	/**
	 * 透過建構子注入取得借閱服務，讓 HTTP 處理與借閱業務分開。
	 *
	 * @param borrowService 協調借閱紀錄、書籍庫存與交易的服務
	 */
	public BorrowController(BorrowService borrowService) {
		this.borrowService = borrowService;
	}

	/**
	 * 處理 GET /borrows/me，查詢目前登入會員的借閱紀錄。
	 *
	 * <p>Spring 提供目前的 HttpServletRequest；getSession(false) 只尋找既有 Session，
	 * 不存在時不會建立新的 Session。Session 存在也不代表已登入，仍須檢查 memberId。
	 * 使用 Session 中的 ID，可避免讓網址參數決定要讀取哪一位會員的資料。
	 *
	 * @param httpRequest 用於取得既有 Session 的 HTTP 請求
	 * @return 沒有 Session 或 memberId 時為 401 且沒有本文；否則為 200 與借閱 DTO 清單
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@GetMapping("/me")
	public ResponseEntity<List<BorrowResponse>> findMemberById(HttpServletRequest httpRequest) throws SQLException{

		HttpSession session = httpRequest.getSession(false);

		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Integer memberId = (Integer) session.getAttribute("memberId");

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		List<BorrowResponse> borrows = borrowService.findMemberById(memberId);

		return ResponseEntity.ok(borrows);
	}

	// 新增借書
	/**
	 * 處理 POST /borrows，以目前登入者身分借閱指定書籍。
	 *
	 * <p>{@code @RequestBody} 透過訊息轉換器將 JSON 轉為 BorrowRequest，
	 * {@code @Valid} 觸發 bookId 的非空及正數驗證。綁定或驗證失敗時通常由 Spring 回應 400，
	 * 不會執行方法本體；所以這些步驟發生在本方法的 Session 檢查之前。
	 *
	 * <p>只有 bookId 由請求提供，memberId 來自 Session；null 檢查後才拆箱傳入 Service。
	 * Service 負責扣減庫存與新增紀錄的交易，這裡只把結果包裝成 HTTP 回應。
	 *
	 * @param borrowRequest 含欲借閱書籍 ID 的輸入 DTO，不包含操作者會員 ID
	 * @param httpRequest 用於取得既有登入 Session 的請求
	 * @return 沒有 Session 或 memberId 時為 401；通過檢查後為 200 與布林結果，false 仍是 200
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@PostMapping
	public ResponseEntity<Boolean> add(@RequestBody @Valid BorrowRequest borrowRequest, HttpServletRequest httpRequest)
			throws SQLException {

		// false 表示只取得既有 Session，沒有時回傳 null，不會為此建立新的 Session。
		HttpSession session = httpRequest.getSession(false);

		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Integer memberId = (Integer) session.getAttribute("memberId");

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		boolean success = borrowService.add(memberId, borrowRequest.getBookId());

		return ResponseEntity.ok(success);
	}

	// 還書
	/**
	 * 處理 PUT /borrows/{borrowId}/return，將指定借閱紀錄交給 Service 執行還書。
	 *
	 * <p>{@code @PathVariable} 將路徑中的 borrowId 轉為 int；不能轉換時通常由 Spring 回應 400。
	 * borrowId 只用來識別借閱紀錄，不能作為有權操作的證明。
	 * Controller 提供 Session 中的 memberId，Service 再驗證借閱是否屬於本人並處理重複還書。
	 *
	 * @param borrowId URL 中的借閱紀錄 ID，不是會員 ID 或書籍 ID
	 * @param httpRequest 用於取得既有登入 Session 的請求
	 * @return 沒有 Session 或 memberId 為 401；成功為 200 與歸還成功文字；Service 回傳 false 為 400 且沒有本文
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@PutMapping("/{borrowId}/return")
	public ResponseEntity<String> returnBook(@PathVariable int borrowId, HttpServletRequest httpRequest)
			throws SQLException {
		HttpSession session = httpRequest.getSession(false);

		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Integer memberId = (Integer) session.getAttribute("memberId");

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		boolean success = borrowService.returnBook(borrowId,memberId);

		if (success) {
			return ResponseEntity.ok("歸還成功");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}
}
