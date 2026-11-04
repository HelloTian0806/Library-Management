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
 * 處理借閱查詢、借書及還書請求，透過 BorrowService 協調借閱紀錄與庫存。 Controller 負責 HTTP 與 Session
 * 資料；Transaction 的 commit、rollback 由 Service 處理。 {@code @RequestMapping}
 * 定義共同路徑，{@code @RestController} 使回傳值成為 HTTP 回應內容。
 */
@RestController
@RequestMapping("/borrows")
public class BorrowController {
	private final BorrowService borrowService;

	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * 
	 * @param borrowService 此 Controller 使用的業務 Service
	 */
	public BorrowController(BorrowService borrowService) {
		this.borrowService = borrowService;
	}

	/**
	 * 使用 Session 中的會員 ID 查詢個人借閱，避免由請求自行指定其他會員。 Spring 注入 HttpSession 時可能建立
	 * 
	 * @param session 登入成功後保存會員識別資訊的 Session
	 * @return 缺少 memberId 時為 401；否則為 200 與 BorrowResponse 清單
	 */
	@GetMapping("/me")
	public ResponseEntity<List<BorrowResponse>> findMemberById(HttpServletRequest httpRequest) {

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
	 * 接收借書 DTO，使用 Session 的會員 ID 呼叫 Service。 {@code @RequestBody} 將 JSON 轉成
	 * DTO；{@code @Valid} 觸發欄位驗證，失敗時通常由 Spring 回應 400。
	 * 
	 * @param borrowRequest 包含欲借閱書籍 ID 的請求 DTO
	 * @param httpRequest   用於取得既有 Session 的 HTTP 請求
	 * @return 沒有 Session 時為 401；否則以 200 回傳 Service 的布林結果
	 * @throws SQLException         Service 向外傳遞的 JDBC 例外
	 * @throws NullPointerException memberId 為 null，自動拆箱為 int 時可能發生
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
	 * 依借閱紀錄 ID 呼叫 Service 還書
	 * 
	 * @param borrowId 借閱紀錄 ID，不是會員 ID 或書籍 ID
	 * @return Service 的還書結果，false 不會在此被轉成失敗 HTTP 狀態碼
	 * @throws SQLException Service 向外傳遞的 JDBC 例外
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
