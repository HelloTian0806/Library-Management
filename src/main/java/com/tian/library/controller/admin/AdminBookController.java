package com.tian.library.controller.admin;

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
 * 提供管理端書籍操作。
 * 每個入口都先驗證登入身分與 ADMIN 角色，再呼叫 BookService。
 */
@RestController
@RequestMapping("/admin/books")
public class AdminBookController {

    private final BookService bookService;
    private final MemberService memberService;

    /**
     * 由 Spring 注入書籍業務服務與會員查詢服務。
     *
     * @param bookService 處理書籍操作
     * @param memberService 查詢登入者與角色
     */
    public AdminBookController(
            BookService bookService,
            MemberService memberService) {
        this.bookService = bookService;
        this.memberService = memberService;
    }

    /**
     * 管理者新增書籍。
     *
     * @param book 要新增的書籍資料
     * @param request 用於取得登入 Session
     * @return 未登入為 401、權限不足為 403；通過檢查後回傳新增結果
     */
    @PostMapping
    public ResponseEntity<Boolean> add(
            @RequestBody Book book,
            HttpServletRequest request) {

        HttpStatus errorStatus = checkAdmin(request);

        if (errorStatus != null) {
            return ResponseEntity.status(errorStatus).build();
        }

        return ResponseEntity.ok(bookService.add(book));
    }

    /**
     * 管理者修改指定書籍，以 URL 中的 ID 決定目標。
     *
     * @param id 要修改的書籍 ID
     * @param book 修改資料
     * @param request 用於取得登入 Session
     * @return 未登入為 401、權限不足為 403；通過檢查後回傳修改結果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Boolean> update(
            @PathVariable int id,
            @RequestBody Book book,
            HttpServletRequest request) {

        HttpStatus errorStatus = checkAdmin(request);

        if (errorStatus != null) {
            return ResponseEntity.status(errorStatus).build();
        }

        book.setId(id);
        return ResponseEntity.ok(bookService.update(book));
    }

    /**
     * 管理者刪除指定書籍。
     *
     * @param id 要刪除的書籍 ID
     * @param request 用於取得登入 Session
     * @return 未登入為 401、權限不足為 403；通過檢查後回傳刪除結果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(
            @PathVariable int id,
            HttpServletRequest request) {

        HttpStatus errorStatus = checkAdmin(request);

        if (errorStatus != null) {
            return ResponseEntity.status(errorStatus).build();
        }

        return ResponseEntity.ok(bookService.delete(id));
    }

    /**
     * 管理者查詢全部書籍。
     *
     * @param request 用於取得登入 Session
     * @return 未登入為 401、權限不足為 403；通過檢查後回傳書籍清單
     */
    @GetMapping
    public ResponseEntity<List<Book>> findAll(
            HttpServletRequest request) {

        HttpStatus errorStatus = checkAdmin(request);

        if (errorStatus != null) {
            return ResponseEntity.status(errorStatus).build();
        }

        return ResponseEntity.ok(bookService.findAll());
    }

    /**
     * 驗證目前請求是否來自管理者。
     * 角色從 Database 取得，不信任前端傳入的角色。
     *
     * @param request 目前的 HTTP 請求
     * @return 未登入為 401、非管理者為 403；通過檢查為 null
     */
    private HttpStatus checkAdmin(HttpServletRequest request) {

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