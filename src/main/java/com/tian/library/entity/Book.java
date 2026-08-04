package com.tian.library.entity;

public class Book {
	private int id;
	private String isbn;
	private String title;
	private String author;
	private String publisher;
	private String category;
	private int totalQuantity;
	private int availableQuantity;

	public Book(String isbn, String title, String author, String publisher, String category, int totalQuantity,
			int availableQuantity) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publisher = publisher;
		this.category = category;
		this.totalQuantity = totalQuantity;
		this.availableQuantity = availableQuantity;
	}
	
	public Book(int id, String isbn, String title, String author, String publisher, String category, int totalQuantity,
			int availableQuantity) {
		this.id= id;
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publisher = publisher;
		this.category = category;
		this.totalQuantity = totalQuantity;
		this.availableQuantity = availableQuantity;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getIsbn() {
		return isbn;
	}
	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public int getTotalQuantity() {
		return totalQuantity;
	}
	public void setTotalQuantity(int totalQuantity) {
		this.totalQuantity = totalQuantity;
	}
	public int getAvailableQuantity() {
		return availableQuantity;
	}
	public void setAvailableQuantity(int availableQuantity) {
		this.availableQuantity = availableQuantity;
	}
	
	@Override
	public String toString() {
		return "Book{" +
	            "id=" + id +
	            ", name='" + isbn + '\'' +
	            ", title='" + title + '\'' +
	            ", author='" + author + '\'' +
	            ", publisher='" + publisher + '\'' +
	            ", category='" + category + '\'' +
	            ", totalQuantity=" + totalQuantity +
	            ", availableQuantity=" + availableQuantity +
	            '}';
	}
	
}
