package com.tian.library.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BorrowDAO;
import com.tian.library.entity.Book;
import com.tian.library.entity.Borrow;
import com.tian.library.entity.BorrowStatus;
@Service
public class BorrowService {
	private final BorrowDAO borrowDAO;
	private final BookService bookService;
	private final MemberService memberService;
	public BorrowService(BorrowDAO borrowDAO,BookService bookService, MemberService memberService) {
		this.borrowDAO=borrowDAO;
		this.bookService=bookService;
		this.memberService=memberService;
	}
	
	
	public boolean borrowBook(int memberId, int bookId) {
	    // 1. Check member exists
		
		if(memberService.findById(memberId)==null) {
			System.out.println("找不到ID");
			return false;
		}
	    // 2. Find book
		Book book = bookService.findById(bookId);
		if(book==null) {
			System.out.println("找不到書");
			return false;
		}
	    // 3. decrease available quantity
		if(!bookService.decAvailableQuantity(bookId)) {
			System.out.println("書本館藏不足");
			return false;
		}
	    // 4. Create Borrow object
		LocalDate borrowDate = LocalDate.now();
		Borrow borrow = new Borrow(memberId,bookId,borrowDate,calculateDueDate(),BorrowStatus.BORROWED);
	    // 5. Save borrow record
		if(borrowDAO.add(borrow)==1){
			System.out.println("成功借閱"+book.getTitle());
		}else {
			System.out.println("借閱失敗");
		}
		return true;
	}
	
	public boolean returnBook(int borrowId) {
		// find borrow record
		Borrow borrow = findById(borrowId);
		if(borrow==null) {
			System.out.println("查無此書");
			return false;
		}
	    // make sure it hasn't already been returned
		if(borrow.getStatus() == BorrowStatus.RETURNED) {
			System.out.println("此書已經還了");
			return false;
		}
	    // update returnDate
	    // update status
		boolean returnUpdate = borrowDAO.returnBook(borrowId, LocalDate.now());
		if(!returnUpdate) {
			System.out.println("更新借閱紀錄失敗");
			return false;
		}
	    // increase book available quantity
		boolean quantityUpdate = bookService.increaseAvailableQuantity(borrow.getBookId());
		if(!quantityUpdate) {
			System.out.println("更新可用庫存失敗");
			return false;
		}
		return true;
	}
	public Borrow findById(int borrowId) {
		return borrowDAO.findById(borrowId);
	}
	public List<Borrow> findAll() {
		List<Borrow> borrowList = borrowDAO.findAll();
		for (Borrow borrow : borrowList) {
			System.out.println(borrow);
		}
		return borrowDAO.findAll();
	}
	
	private LocalDate calculateDueDate() {
		return LocalDate.now().plusDays(14);
	}
}
