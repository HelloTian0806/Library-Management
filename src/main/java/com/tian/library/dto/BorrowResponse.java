package com.tian.library.dto;

import java.time.LocalDate;

import com.tian.library.entity.BorrowStatus;

public class BorrowResponse {
	private int id;
	private String bookTitle;
	private LocalDate borrowDate;
	private LocalDate dueDate;
	private LocalDate returnDate;
	
	public BorrowResponse(int id, String bookTitle, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
			BorrowStatus status) {
		super();
		this.id = id;
		this.bookTitle = bookTitle;
		this.borrowDate = borrowDate;
		this.dueDate = dueDate;
		this.returnDate = returnDate;
		this.status = status;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getBookTitle() {
		return bookTitle;
	}
	public void setBookTitle(String bookTitle) {
		this.bookTitle = bookTitle;
	}
	public LocalDate getBorrowDate() {
		return borrowDate;
	}
	public void setBorrowDate(LocalDate borrowDate) {
		this.borrowDate = borrowDate;
	}
	public LocalDate getDueDate() {
		return dueDate;
	}
	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}
	public LocalDate getReturnDate() {
		return returnDate;
	}
	public void setReturnDate(LocalDate returnDate) {
		this.returnDate = returnDate;
	}
	public BorrowStatus getStatus() {
		return status;
	}
	public void setStatus(BorrowStatus status) {
		this.status = status;
	}
	private BorrowStatus status;
}
