package com.tian.library.controller.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tian.library.dto.MemberResponse;
import com.tian.library.entity.Member;
import com.tian.library.entity.Role;
import com.tian.library.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 提供管理端的全部會員查詢與姓名搜尋。
 *
 * <p>每個 API 都先從 Session 取得登入者 ID，
 * 再透過 MemberService 查詢會員，確認其角色為 ADMIN。
 *
 * <p>查詢結果轉成 MemberResponse，明確限制對外欄位，
 * 避免將 Entity 中的 password 等內部資料直接序列化。
 *
 * <p>admin 路徑只是 API 分類，真正的授權由方法內的檢查執行。
 */
@RestController
@RequestMapping("/admin/member")
public class AdminMemberController {

    private final MemberService memberService;

    /**
     * 透過 constructor injection 接收 Spring 管理的 Service。
     *
     * @param memberService 負責會員查詢的業務服務
     */
    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 管理者查詢全部會員。
     *
     * <p>若請求包含 name 參數，Spring 會優先選擇
     * {@code @GetMapping(params = "name")} 的姓名查詢方法。
     *
     * @param httpRequest 用於取得既有登入 Session
     * @return 未登入或查無登入會員時為 401；非 ADMIN 為 403；
     *         通過檢查後為 200 與會員 DTO 清單
     */
    @GetMapping
    public ResponseEntity<List<MemberResponse>> findAll(
            HttpServletRequest httpRequest) {

        // false 表示不為未登入的請求建立新 Session。
        HttpSession session = httpRequest.getSession(false);

        if (session == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // Session 存在不代表已登入，還必須確認有登入者 ID。
        Integer memberId =
                (Integer) session.getAttribute("memberId");

        if (memberId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // 從 Database 重新取得會員與角色，不信任前端提供的角色。
        Member member = memberService.findById(memberId);

        if (member == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // 已登入但沒有管理者權限，使用 403，而不是 401。
        if (member.getRole() != Role.ADMIN) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        // 只有通過授權後，才查詢全部會員資料。
        List<Member> memberList = memberService.findAll();

        // 建立真正的空集合，查無資料時會回傳 JSON 空陣列 []。
        List<MemberResponse> response = new ArrayList<>();

        for (Member memberItem : memberList) {
            // 只複製 API 允許輸出的欄位，不修改原本的 Entity。
            response.add(new MemberResponse(
                    memberItem.getId(),
                    memberItem.getName(),
                    memberItem.getEmail(),
                    memberItem.getPhone(),
                    memberItem.getCreatedAt(),
                    memberItem.getRole()
            ));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 管理者依姓名搜尋會員。
     *
     * <p>{@code params = "name"} 要求請求具有 name 參數；
     * {@code @RequestParam} 將該參數綁定至方法的 name。
     * 這些 Annotation 本身不會檢查登入身分或角色。
     *
     * @param name 交給 Service 搜尋的姓名條件
     * @param httpRequest 用於取得既有登入 Session
     * @return 未登入或查無登入會員時為 401；非 ADMIN 為 403；
     *         通過檢查後為 200 與符合條件的會員 DTO 清單
     */
    @GetMapping(params = "name")
    public ResponseEntity<List<MemberResponse>> findByName(
            @RequestParam String name,
            HttpServletRequest httpRequest) {

        HttpSession session = httpRequest.getSession(false);

        if (session == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Integer memberId =
                (Integer) session.getAttribute("memberId");

        if (memberId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Member member = memberService.findById(memberId);

        if (member == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        if (member.getRole() != Role.ADMIN) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        // 搜尋條件不決定操作者身分；身分始終來自上方的 Session。
        List<Member> memberList = memberService.findByName(name);

        List<MemberResponse> response = new ArrayList<>();

        for (Member memberItem : memberList) {
            response.add(new MemberResponse(
                    memberItem.getId(),
                    memberItem.getName(),
                    memberItem.getEmail(),
                    memberItem.getPhone(),
                    memberItem.getCreatedAt(),
                    memberItem.getRole()
            ));
        }

        return ResponseEntity.ok(response);
    }
}