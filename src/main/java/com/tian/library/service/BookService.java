package com.tian.library.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.tian.library.dao.BookDAO;
import com.tian.library.entity.Book;
@Service
public class BookService {
	private final BookDAO bookDAO;
	public BookService(BookDAO bookDAO) {
		this.bookDAO = bookDAO;
	}
	public boolean add(String isbn, String title, String author, String publisher, String category, int totalQuantity,
			int availableQuantity) {
		Book book = new Book(isbn, title, author, publisher, category, totalQuantity, availableQuantity);
		return bookDAO.addBook(book) > 0;
	
	}
	public boolean update(String isbn) {
		Book book = bookDAO.findByIsbn(isbn);
		if(book != null) {
			//使用者輸入新的資料
			bookDAO.updateBook(book, isbn);
		}else {
			return false;
		}
		return true;
	}
	public boolean delete(String isbn) {
		return bookDAO.deleteBook(isbn) > 0;
	}
	
	public ArrayList<Book> findByTitle(String title) {
		ArrayList<Book> bookList = bookDAO.findByTitle(title);

		for (Book book : bookList) {
			System.out.println(book);
		}

		return bookList;
	}
	
	public boolean findByIsbn(String isbn) {
		Book book = bookDAO.findByIsbn(isbn);
		if(book == null) {
			return false;
		}
		System.out.println(book);
		return true;
	}
	public ArrayList<Book> findAll() {
		ArrayList<Book> bookList = bookDAO.findAll();
		for (Book book : bookList) {
			System.out.println(book);
		}
		return bookList;
	}
	
	public Book findById(int bookId) {
		Book book = bookDAO.findById(bookId);
		System.out.println(book.getId()+book.getAuthor()+book.getTitle());
		return book;
	}
	
	public boolean decAvailableQuantity(int bookId) {
		return bookDAO.decAvailableQuantity(bookId);
	}
	
	public boolean increaseAvailableQuantity(int bookId) {
		return bookDAO.increaseAvailableQuantity(bookId);
	}
}
