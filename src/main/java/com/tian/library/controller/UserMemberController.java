package com.tian.library.controller;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.LoginRequest;
import com.tian.library.dto.RegisterRequest;
import com.tian.library.dto.UserResponse;
import com.tian.library.entity.Member;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * 提供 /auth 下的註冊、登入、登出與目前登入者查詢。
 *
 * <p>{@code @RestController} 讓 Spring 管理此 Controller，並將方法回傳內容寫入 HTTP 回應；
 * {@code @RequestMapping} 定義共同路徑。ResponseEntity 可同時指定狀態碼與回應本文，
 * 物件通常轉為 JSON，字串回應則不應一概視為 JSON 物件。
 *
 * <p>Controller 管理 Session 與 HTTP 狀態，MemberService 負責帳號規則及密碼驗證。
 * Request DTO 限定輸入欄位，UserResponse 明確挑選輸出欄位，避免直接序列化含密碼雜湊的 Member。
 * 資料庫例外向外傳遞，由 GlobalExceptionHandler 統一處理。
 */
@RestController
@RequestMapping("/auth")
public class UserMemberController {
	private final MemberService memberService;

	/**
	 * 透過單一建構子注入會員服務，Spring 可自動提供對應 Bean，不需要自行 new。
	 *
	 * @param memberService 負責會員註冊、登入驗證及查詢的服務
	 */
	public UserMemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	/**
	 * 處理 POST /auth/register，驗證輸入後交由 Service 註冊。
	 *
	 * <p>{@code @RequestBody} 將 JSON 綁定成 RegisterRequest；{@code @Valid} 依 DTO 上的
	 * 非空、長度、Email 及手機格式限制驗證。驗證失敗通常為 400，且不會進入方法本體。
	 * Service 負責檢查 Email 及雜湊密碼；這裡不自行組合 SQL，也不建立登入 Session。
	 *
	 * @param request 包含姓名、Email、原始密碼與手機的註冊 DTO
	 * @return 成功為 201 與註冊成功文字；Service 回傳 false 時為 409 與 Email 已被註冊文字
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@PostMapping("/register")
	public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest request) throws SQLException {

		// 新增帳號
		boolean success = memberService.register(request);

		if (success) {
			return ResponseEntity.status(HttpStatus.CREATED).body("註冊成功");
		}

		return ResponseEntity.status(HttpStatus.CONFLICT).body("Email已被註冊");

	}

	/**
	 * 處理 POST /auth/login，驗證成功後重建 Session 並回傳允許公開的會員資料。
	 *
	 * <p>{@code @RequestBody} 與 {@code @Valid} 先進行輸入綁定及 DTO 驗證。
	 * Service 找不到會員或密碼不符時都回傳 null，因此對外使用相同的 401 訊息。
	 * 成功後先使舊 Session 失效，再建立新 Session，避免繼續沿用登入前的 Session 識別碼。
	 *
	 * <p>Session 的 memberId 保存在伺服器端，瀏覽器通常以 Cookie 攜帶 Session 識別碼。
	 * 回應內的 role 供畫面使用，不能取代管理端 API 從資料庫查詢角色的授權檢查。
	 *
	 * @param request 包含 Email 與原始密碼的登入 DTO，密碼由 Service 比對
	 * @param httpRequest 用於取得舊 Session、使其失效並建立新 Session 的請求
	 * @return 驗證失敗為 401 與錯誤文字；成功為 200 與不含密碼的 UserResponse
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest)
			throws SQLException {

		Member member = memberService.login(request);

		if (member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤");
		}
		// 取得既有的session
		HttpSession oldSession = httpRequest.getSession(false);

		// 如果存在舊的session 就讓它失效
		if (oldSession != null) {
			oldSession.invalidate();
		}

		// true 表示需要時建立新 Session，瀏覽器後續通常透過 Session cookie 對應伺服器端資料。
		HttpSession session = httpRequest.getSession(true);

		// 只記錄登入者識別資訊；是否具備管理者角色仍需另外檢查。
		session.setAttribute("memberId", member.getId());

		// 明確挑選可公開的欄位，避免將 Member 中的密碼雜湊一併回傳。
		UserResponse user = new UserResponse(member.getId(), member.getName(), member.getEmail(), member.getRole());
		return ResponseEntity.ok(user);

	}

	/**
	 * 處理 POST /auth/logout，使既有 Session 失效以移除伺服器端登入狀態。
	 *
	 * <p>沒有 Session 時仍回傳成功，不會只為了登出建立新的 Session。
	 * 這個方法不直接修改 Pinia；前端仍需在自己的狀態管理中更新登入畫面。
	 *
	 * @param httpRequest 用於取得既有 Session 的 HTTP 請求
	 * @return 200 與登出成功文字，包含原本就沒有 Session 的情況
	 */
	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletRequest httpRequest) {
		// false 表示只取得既有 Session，沒有時回傳 null，不會為此建立新的 Session。
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		return ResponseEntity.ok("登出成功");
	}

	/**
	 * 處理 GET /auth/me，以 Session 中的 memberId 重新取得目前登入者資料。
	 *
	 * <p>依序確認 Session、memberId 與會員資料存在，任何一項不符合都回應 401。
	 * memberId 在拆箱為 int 前已有 null 檢查；本方法不建立新的 Session。
	 * 從資料庫重新查詢可取得目前會員資料，而不是直接信任前端保存的使用者物件。
	 *
	 * @param httpRequest 用於取得既有登入 Session 的請求
	 * @return 沒有 Session、memberId 或查無會員時為 401 且沒有本文；成功為 200 與 UserResponse
	 * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(HttpServletRequest httpRequest) throws SQLException {
		// false 表示只取得既有 Session，沒有時回傳 null，不會為此建立新的 Session。
		HttpSession session = httpRequest.getSession(false);

		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Integer memberId = (Integer) session.getAttribute("memberId");

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Member member = memberService.findById(memberId);

		if (member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 明確挑選可公開的欄位，避免將 Member 中的密碼雜湊一併回傳。
		UserResponse user = new UserResponse(member.getId(), member.getName(), member.getEmail(), member.getRole());
		return ResponseEntity.ok(user);
	}
}
