package com.tian.library.controller.admin;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.DashboardStatsResponse;
import com.tian.library.entity.Member;
import com.tian.library.entity.Role;
import com.tian.library.service.DashboardService;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 提供管理者使用的儀表板統計 HTTP 入口。
 *
 * <p>{@code @RestController} 將回傳值作為回應內容；{@code @RequestMapping("admin/dashboard")}
 * 定義的映射路徑為 /admin/dashboard，並非由 package 名稱推導。
 * 在進入統計查詢前，先確認既有 Session、登入會員及資料庫中的 ADMIN 角色。
 *
 * <p>DashboardService 組合各 DAO 的統計結果，Controller 只處理授權及 HTTP 回應。
 * 統計內容以 DashboardStatsResponse 封裝，避免讓前端分別呼叫多個資料存取方法。
 */
@RestController
@RequestMapping("admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final MemberService memberService;

    /**
     * 透過單一建構子取得 Spring 管理的服務，讓統計彙整與會員驗證保持各自職責。
     *
     * @param dashboardService 組合書籍、會員與借閱統計的服務
     * @param memberService 查詢登入者與目前角色的服務
     */
    public DashboardController(
            DashboardService dashboardService,
            MemberService memberService) {

        this.dashboardService = dashboardService;
        this.memberService = memberService;
    }

    /**
     * 處理 GET /admin/dashboard，通過管理者驗證後回傳統計 DTO。
     *
     * <p>書籍總數目前是書目筆數，不是館藏冊數加總；逾期統計以未歸還且到期日早於今天判斷。
     * 這些定義來自下層 SQL，Controller 不重新計算，也不將查詢失敗轉為零值回應。
     * 各統計是獨立查詢，不能因為包在同一個 DTO 就視為同一時間點的資料庫快照。
     *
     * @param httpRequest 用於取得既有登入 Session 的請求
     * @return 沒有 Session、memberId 或查無登入會員時為 401；非 ADMIN 為 403；成功為 200 與統計 DTO
     * @throws SQLException 資料庫操作失敗時向外傳遞，由 GlobalExceptionHandler 記錄例外並回應 500
     */
    @GetMapping
    public ResponseEntity<DashboardStatsResponse> dashboardStats(
            HttpServletRequest httpRequest) throws SQLException{

        // 不為尚未登入的請求建立新 Session。
        HttpSession session = httpRequest.getSession(false);

        if (session == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // Session 存在不代表已登入，仍須確認有登入者 ID。
        Integer memberId =
                (Integer) session.getAttribute("memberId");

        if (memberId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // 角色從 Database 取得，不信任前端提供的角色。
        Member member = memberService.findById(memberId);

        if (member == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // 已登入但不是管理者，回傳 403。
        if (member.getRole() != Role.ADMIN) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        // 通過授權後才執行統計查詢。
        return ResponseEntity.ok(dashboardService.stats());
    }
}