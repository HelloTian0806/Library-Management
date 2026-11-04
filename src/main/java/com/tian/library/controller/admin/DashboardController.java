package com.tian.library.controller.admin;

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
 * 提供管理者使用的儀表板統計入口。
 *
 * <p>先透過 Session 確認登入者，再查詢其角色；
 * 只有 ADMIN 可以取得統計資料。
 *
 * <p>實際路徑為 /dashboard，與 package 名稱無關。
 * 統計 SQL 與資料彙整仍由 DashboardService 及 DAO 負責。
 */
@RestController
@RequestMapping("admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final MemberService memberService;

    /**
     * 透過 constructor injection 取得所需的 Service。
     *
     * @param dashboardService 彙整儀表板統計資料
     * @param memberService 查詢登入者與角色
     */
    public DashboardController(
            DashboardService dashboardService,
            MemberService memberService) {

        this.dashboardService = dashboardService;
        this.memberService = memberService;
    }

    /**
     * 驗證管理者身分後取得統計資料。
     *
     * @param httpRequest 用於取得既有登入 Session
     * @return 未登入或查無登入會員為 401；
     *         非 ADMIN 為 403；成功為 200 與統計 DTO
     */
    @GetMapping
    public ResponseEntity<DashboardStatsResponse> dashboardStats(
            HttpServletRequest httpRequest) {

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