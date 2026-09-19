package com.tian.library.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BookDAO;
import com.tian.library.entity.Book;
@Service
public class BookService {
	private final BookDAO bookDAO;
	public BookService(BookDAO bookDAO) {
		this.bookDAO = bookDAO;
	}
	public boolean add(Book book) {
		
		if (book == null) {
			return false;
		}
		
		if(book.getIsbn()==null || book.getIsbn().isBlank()) {
			return false;
		}
		if(book.getTitle()==null || book.getTitle().isBlank()) {
			return false;
		}
		if (book.getAuthor() == null || book.getAuthor().isBlank()) {
		    return false;
		}
		if(book.getTotalQuantity()<=0) {
			return false;
		}
		// 如果有同樣的isbn就不能新增
		if(bookDAO.findByIsbn(book.getIsbn())!=null) {
			return false;
		}
		// 可借數量=館藏數量
		book.setAvailableQuantity(book.getTotalQuantity());

		return bookDAO.add(book) > 0;
	
	}
	public boolean update(Book book) {
		
		if(book == null) {
			return false;
		}

		if(book.getId()<=0) {
			return false;
		}
		
		if (book.getIsbn() == null || book.getIsbn().isBlank()) {
		    return false;
		}
		

		if (book.getTitle() == null || book.getTitle().isBlank()) {
		    return false;
		}
		
		if (book.getAuthor() == null || book.getAuthor().isBlank()) {
		    return false;
		}
		Book foundBook = bookDAO.findById(book.getId());
		if(foundBook==null) {
			return false;
		}
		// ISBN 不能跟其他書重複
		Book sameIsbn = bookDAO.findByIsbn(book.getIsbn());
		if(sameIsbn != null && sameIsbn.getId() != book.getId()) {
			return false;
		}


		return bookDAO.updateBook(book)>0;
	}
	public boolean delete(int bookId) {
		return bookDAO.deleteBook(bookId) > 0;
	}
	
	public List<Book> findByTitle(String title) {
		return bookDAO.findByTitle(title);
	}
	
	public Book findByIsbn(String isbn) {
		return bookDAO.findByIsbn(isbn);
	}
	public List<Book> findAll() {
		return bookDAO.findAll();
	}
	
	public Book findById(int bookId) {
		return bookDAO.findById(bookId);
	}
	
	public boolean decAvailableQuantity(int bookId) {
		
		return bookDAO.decAvailableQuantity(bookId);
	}
	public boolean decAvailableQuantity(Connection conn, int bookId) throws SQLException {
		
		return bookDAO.decAvailableQuantity(conn, bookId);
	}
	
	public boolean increaseAvailableQuantity(int bookId) {
		return bookDAO.increaseAvailableQuantity(bookId);
	}
	public boolean increaseAvailableQuantity(Connection conn, int bookId) throws SQLException{
		return bookDAO.increaseAvailableQuantity(conn, bookId);
	}
}
