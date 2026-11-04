package com.tian.library.controller;

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
 * 提供註冊、登入、登出與登入者查詢，帳號驗證及密碼雜湊工作委派給 MemberService。
 * 使用 Session 保存 memberId，登入成功與身分查詢以 UserResponse 限制對外欄位。
 * {@code @RestController} 將回傳物件轉為回應內容，{@code @RequestMapping} 定義帳號 API 的共同路徑。
 */
@RestController
@RequestMapping("/auth")
public class UserMemberController {
	private final MemberService memberService;
	
	/**
	 * 透過 constructor injection 接收 Spring 提供的 Service，避免自行建立業務物件。
	 * @param memberService 此 Controller 使用的業務 Service
	 */
	public UserMemberController(MemberService memberService) {
		this.memberService = memberService;
	}
	
	/**
	 * 驗證註冊資料後交給 Service 檢查 Email、雜湊密碼並新增會員。
	 * {@code @Valid} 不通過時通常由 Spring 回應 400，不會進入方法本體。
	 * @param request 由 JSON 綁定並驗證的註冊 DTO
	 * @return 成功為 201；Service 回傳 false 時一律為 409 與 Email 重複訊息，未區分其他寫入失敗原因
	 */
	@PostMapping("/register")
	public ResponseEntity<String> register (@RequestBody @Valid RegisterRequest request) {
			
		// 新增帳號
		boolean success = memberService.register(request);
		
		if(success) {
			return ResponseEntity.status(HttpStatus.CREATED).body("註冊成功");
		}
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body("Email已被註冊");
		
	}
	
	/**
	 * 驗證登入資料，成功後使舊 Session 失效並建立新 Session，降低 Session fixation 風險。
	 * 以 UserResponse 輸出允許的欄位，不直接回傳驗證時使用的 Member。
	 * @param request 包含 Email 與原始密碼的登入 DTO，密碼驗證由 Service 處理
	 * @param httpRequest 用於取得、失效及建立 Session 的請求
	 * @return 驗證失敗為 401 與相同錯誤訊息；成功為 200 與 UserResponse
	 */
	@PostMapping("/login")
	public ResponseEntity<?> login (@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest){
		
		Member member = memberService.login(request);
	
		if(member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤");
		}
		// 取得既有的session
		HttpSession oldSession = httpRequest.getSession(false);
		
		// 如果存在舊的session 就讓它失效
		if(oldSession != null) {
			oldSession.invalidate();
		}
		
		// true 表示需要時建立新 Session，瀏覽器後續通常透過 Session cookie 對應伺服器端資料。
		HttpSession session = httpRequest.getSession(true);
		
		// 只記錄登入者識別資訊；是否具備管理者角色仍需另外檢查。
		session.setAttribute("memberId", member.getId());
		
		// 明確挑選可公開的欄位，避免將 Member 中的密碼雜湊一併回傳。
		UserResponse user = new UserResponse(
				member.getId(),
				member.getName(),
				member.getEmail(),
				member.getRole()
				);
		return ResponseEntity.ok(user);
		
	}
	
	/**
	 * 使既有 Session 失效，移除登入資訊；沒有 Session 時也回傳成功。
	 * @param httpRequest 用於取得既有 Session 的請求
	 * @return 200 與登出成功訊息
	 */
	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletRequest httpRequest){
		// false 表示只取得既有 Session，沒有時回傳 null，不會為此建立新的 Session。
		HttpSession session = httpRequest.getSession(false);
		if(session != null) {
			session.invalidate();
		}
		
		return ResponseEntity.ok("登出成功");
	}
	
	/**
	 * 以 Session 保存的會員 ID 重新查詢會員，並轉成不含密碼的 UserResponse。
	 * 目前沒有檢查既有 Session 中的 memberId 是否為 null。
	 * @param httpRequest 用於取得既有 Session 的請求
	 * @return 沒有 Session 或查無會員時為 401；成功為 200 與 UserResponse
	 * @throws NullPointerException memberId 為 null，呼叫接受 int 的 Service 方法時可能自動拆箱失敗
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me (HttpServletRequest httpRequest) {
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
		
		if(member == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		// 明確挑選可公開的欄位，避免將 Member 中的密碼雜湊一併回傳。
		UserResponse user = new UserResponse(member.getId(), member.getName(), member.getEmail(), member.getRole());
		return ResponseEntity.ok(user);
	}
}
