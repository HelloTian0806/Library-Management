package com.tian.library.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BorrowRequest {
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
