package com.tian.library.entity;

import java.time.LocalDate;

public class Borrow {
	private int id;
	private int memberId;
	private int bookId;
	private LocalDate borrowDate;
	private LocalDate dueDate;
	private LocalDate returnDate;
	private BorrowStatus status;
	public Borrow() {
		
	}
	public Borrow(int memberId, int bookId, LocalDate borrowDate,LocalDate dueDate, BorrowStatus status) {
		super();
		this.memberId = memberId;
		this.bookId = bookId;
		this.borrowDate = borrowDate;
		this.dueDate = dueDate;
		this.status = status;
	}
	
	
	public Borrow(int id, int memberId, int bookId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
			BorrowStatus status) {
		super();
		this.id = id;
		this.memberId = memberId;
		this.bookId = bookId;
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
	public int getMemberId() {
		return memberId;
	}
	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}
	public int getBookId() {
		return bookId;
	}
	public void setBookId(int bookId) {
		this.bookId = bookId;
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
	
	@Override
	public String toString() {
		return "Borrow{"+"Id="+id+", MemberId="+memberId+
				", BookId="+bookId+
				", BorrowDate="+borrowDate+
				", DueDate="+dueDate+
				", returnDate="+returnDate+
				", Status="+status+"}";
	}
	
}
