package com.tian.library.controller.admin;

import java.sql.SQLException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.AdminBorrowResponse;
import com.tian.library.entity.Member;
import com.tian.library.entity.Role;
import com.tian.library.service.BorrowService;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 提供 /admin/borrows 下的全部借閱查詢與會員姓名搜尋。
 *
 * <p>{@code @RestController} 讓回傳內容經由訊息轉換器寫入回應，
 * {@code @RequestMapping} 定義共同路徑。兩個入口都先驗證 Session、會員與 ADMIN 角色。
 * 查詢委派 BorrowService；DAO 以 JOIN 取得書名與會員姓名，映射為 AdminBorrowResponse。
 *
 * <p>Controller 不直接執行 JDBC，也不自行重新計算借閱狀態。
 * SQLException 向外傳遞，由 GlobalExceptionHandler 記錄並回應 500。
 */
@RestController
@RequestMapping("/admin/borrows")
public class AdminBorrowController {

	private final BorrowService borrowService;
	private final MemberService memberService;

	/**
	 * 透過建構子注入借閱與會員服務，讓資料查詢與操作者驗證各自委派給對應服務。
	 *
	 * @param borrowService 查詢借閱資料並協調借還書規則的服務
	 * @param memberService 查詢登入會員與目前角色的服務
	 */
	public AdminBorrowController(BorrowService borrowService, MemberService memberService) {


		super();
		this.borrowService = borrowService;
		this.memberService = memberService;
	}

	/**
	 * 處理 GET /admin/borrows，驗證管理者後取得全部借閱 DTO。
	 *
	 * <p>Session 中的 ID 用於識別操作者；先確認會員存在，再從資料庫取得的角色判斷授權。
	 * 只有通過檢查後才讀取全部借閱資料，避免將管理資料交給一般會員。
	 * 若請求含 name 參數，Spring 會選擇條件更明確的姓名搜尋方法。
	 *
	 * @param httpRequest 用於取得既有登入 Session 的請求
	 * @return 沒有 Session、memberId 或查無登入會員時為 401；非 ADMIN 為 403；成功為 200 與管理端借閱 DTO 清單
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@GetMapping
	public ResponseEntity<List<AdminBorrowResponse>> findAll(HttpServletRequest httpRequest) throws SQLException{

		// 只取得既有 Session，避免為尚未登入的請求建立新 Session。
		HttpSession session = httpRequest.getSession(false);

		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Integer memberId = (Integer) session.getAttribute("memberId");

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 從資料庫重新確認會員與角色，不信任前端提交的角色資訊。
		Member member = memberService.findById(memberId);

		if (member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (member.getRole() != Role.ADMIN) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		return ResponseEntity.ok(borrowService.findAll());
	}

	/**
	 * 處理帶有 name 查詢參數的 GET /admin/borrows，依會員姓名搜尋借閱。
	 *
	 * <p>{@code params = "name"} 選擇對應處理方法，{@code @RequestParam} 綁定搜尋文字。
	 * name 是搜尋目標的條件，操作者身分仍來自 Session；兩者不可混為一談。
	 * 查詢參數存在不代表非空，也不代表已通過管理者授權，因此下方仍逐項檢查身分與角色。
	 *
	 * @param name 要搜尋的會員姓名條件，下層以 LIKE 比對
	 * @param httpRequest 用於取得既有登入 Session 的請求
	 * @return 沒有 Session、memberId 或查無登入會員時為 401；非 ADMIN 為 403；成功為 200 與符合條件的 DTO 清單
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@GetMapping(params = "name")
	public ResponseEntity<List<AdminBorrowResponse>> findByMemberName(@RequestParam String name,
			HttpServletRequest httpRequest) throws SQLException{

		// 只取得既有 Session，避免為尚未登入的請求建立新 Session。
		HttpSession session = httpRequest.getSession(false);

		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Integer memberId = (Integer) session.getAttribute("memberId");

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 從資料庫重新確認會員與角色，不信任前端提交的角色資訊。
		Member member = memberService.findById(memberId);

		if (member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (member.getRole() != Role.ADMIN) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		return ResponseEntity.ok(borrowService.findByMemberName(name));
	}
}
