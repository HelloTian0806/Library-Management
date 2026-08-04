package com.tian.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.tian.library.entity.Book;

@Repository
public class BookDAO {
	private final DataSource dataSource;

	public BookDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public int addBook(Book book) {
		String sql = """
				INSERT INTO books
				(isbn, title, author, publisher, category,
				total_quantity, available_quantity)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, book.getIsbn());
			pstmt.setString(2, book.getTitle());
			pstmt.setString(3, book.getAuthor());
			pstmt.setString(4, book.getPublisher());
			pstmt.setString(5, book.getCategory());
			pstmt.setInt(6, book.getTotalQuantity());
			pstmt.setInt(7, book.getAvailableQuantity());
			return pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void updateBook(Book book, String isbn) {
		String sql = "UPDATE books SET total_quantity = ? WHERE isbn = ?";
		Book foundBook = findByIsbn(isbn);
	}

	public int deleteBook(String isbn) {
		String sql = "DELETE FROM books WHERE isbn = ?";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, isbn);

			return pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public Book findByIsbn(String isbn) {
		String sql = "SELECT * FROM books WHERE isbn = ?";
		Book book = null;
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, isbn);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"), rs.getString("author"),
						rs.getString("publisher"), rs.getString("category"), rs.getInt("total_quantity"),
						rs.getInt("available_quantity"));
				return book;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ArrayList<Book> findAll() {
		System.out.println("使用findAll功能");
		String sql = "SELECT * FROM library_management.books";
		ArrayList<Book> bookList = new ArrayList<Book>();

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery();) {

			while (rs.next()) {
				Book book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"),
						rs.getString("author"), rs.getString("publisher"), rs.getString("category"),
						rs.getInt("total_quantity"), rs.getInt("available_quantity"));
				bookList.add(book);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return bookList;
	}

	public ArrayList<Book> findByTitle(String title) {
		String sql = "SELECT * FROM books WHERE title LIKE ?";
		ArrayList<Book> bookList = new ArrayList<Book>();
		System.out.println("使用findByTitle功能");
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, "%" + title + "%");
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				System.out.println("有資料讀取中");
				Book book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"),
						rs.getString("author"), rs.getString("publisher"), rs.getString("category"),
						rs.getInt("total_quantity"), rs.getInt("available_quantity"));
				bookList.add(book);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return bookList;
	}

	public Book findById(int bookId) {
		String sql = "SELECT * FROM books WHERE id = ?";
		Book book = null;
		System.out.println("使用findById功能");
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				System.out.println("有資料讀取中");
				book = new Book(rs.getInt("id"), rs.getString("isbn"), rs.getString("title"), rs.getString("author"),
						rs.getString("publisher"), rs.getString("category"), rs.getInt("total_quantity"),
						rs.getInt("available_quantity"));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return book;
	}

	public boolean decAvailableQuantity(int bookId) {
		String sql = """
				UPDATE books SET available_quantity = available_quantity-1 WHERE id = ? AND available_quantity > 0
				""";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			if (row == 0) {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
	public boolean increaseAvailableQuantity(int bookId) {
		String sql="""
				UPDATE books SET available_quantity = available_quantity + 1 WHERE id = ? 
				""";
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);){
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			if(row==0) {
				return false;
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
}
