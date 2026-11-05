package com.tian.library.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 定義借書 API 的輸入契約，只接受業務所需的書籍 ID。
 *
 * <p>現有 Controller 以 {@code @RequestBody @Valid} 綁定並驗證此 DTO，memberId 則來自 Session。
 * 驗證 Annotation 需由驗證器觸發；直接 new 或呼叫 setter 不會立即驗證。
 * 通過欄位驗證只代表 ID 非空且為正數，書籍存在、庫存與交易仍由 Service 處理。
 */
public class BorrowRequest {
	/**
	 * 使用 Integer 讓缺少欄位可表示為 null，而不是像 int 一樣只留下預設零。
	 * {@code @Positive} 要求正數但允許 null，因此另以 {@code @NotNull} 拒絕未提供的值。
	 */
	@NotNull(message = "書籍ID不可為空")
	@Positive(message = "書籍ID必須大於0")
	private Integer bookId;

	public Integer getBookId() {
		return bookId;
	}

	public void setBookId(Integer bookId) {
		this.bookId = bookId;
	}
	
	
}
