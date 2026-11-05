package com.tian.library.entity;

/**
 * 保存書目及館藏數量的內部資料模型，由 BookDAO 明確讀取資料列後建立。
 *
 * <p>雖然放在 entity Package，目前沒有 JPA 的 {@code @Entity} 映射或變更追蹤。
 * 建構子與 setter 只修改記憶體資料，不會自動執行 SQL；必要欄位及 ISBN 檢查由 BookService 處理。
 * 目前書籍 Controller 也直接使用此類別作為輸入或輸出，與已有獨立 Request／Response DTO 的會員流程不同。
 *
 * <p>getter／setter 提供屬性讀寫，也供目前 JSON 訊息轉換器存取屬性。
 * 本類別沒有無參數建構子，JSON 如何建立物件仍取決於 Jackson 的建構子辨識設定，不能僅因有 setter 就保證可綁定。
 */
public class Book {
	/**
	 * 書籍資料列的識別值；沒有傳入 ID 的建構子會讓此欄位保持 int 預設值零。
	 */
	private int id;
	/**
	 * 書籍 ISBN，使用字串保留格式；本欄位本身沒有驗證格式或保證唯一性的能力。
	 */
	private String isbn;
	private String title;
	private String author;
	private String publisher;
	private String category;
	/**
	 * 館藏總冊數，與書目資料列筆數不同；目前書籍新增時由 Service 檢查須大於零。
	 */
	private int totalQuantity;
	/**
	 * 目前可借冊數；借還書透過資料庫條件更新維持數量，直接設定此欄位不會更新資料庫。
	 */
	private int availableQuantity;

	/**
	 * 以不含資料列 ID 的欄位建立書籍物件，不在此驗證數量或執行新增。
	 *
	 * @param isbn 書籍 ISBN
	 * @param title 書名
	 * @param author 作者
	 * @param publisher 出版社
	 * @param category 分類
	 * @param totalQuantity 傳入的館藏冊數
	 * @param availableQuantity 傳入的可借冊數；BookService.add 會另以館藏量重設
	 */
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
	
	/**
	 * 以包含 ID 的欄位建立書籍，供 DAO 將查詢結果映射成物件。
	 *
	 * @param id 書籍識別值
	 * @param isbn 書籍 ISBN
	 * @param title 書名
	 * @param author 作者
	 * @param publisher 出版社
	 * @param category 分類
	 * @param totalQuantity 館藏冊數
	 * @param availableQuantity 可借冊數
	 */
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
	/**
	 * 只改變此物件的可借冊數，不檢查上下限、不執行 SQL，也不取得交易鎖。
	 *
	 * @param availableQuantity 要存入物件的可借冊數
	 */
	public void setAvailableQuantity(int availableQuantity) {
		this.availableQuantity = availableQuantity;
	}
	
	/**
	 * 提供除錯用的文字表示，並非 API 的 JSON 序列化規則。
	 *
	 * <p>目前輸出中的 name 標籤實際放入 isbn，僅描述既有行為，沒有在此修正標籤。
	 *
	 * @return 包含書目與數量的文字
	 */
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
