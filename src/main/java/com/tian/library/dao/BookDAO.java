package com.tian.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.tian.library.entity.Book;

@Repository
public class BookDAO {
	private final DataSource dataSource;

	public BookDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public int add(Book book) {
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

	public int updateBook(Book book) {
		String sql = """
				UPDATE books
				SET
				    isbn = ?,
				    title = ?,
				    author = ?,
				    publisher = ?,
				    category = ?
				WHERE id = ?;
					""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, book.getIsbn());
			pstmt.setString(2, book.getTitle());
			pstmt.setString(3, book.getAuthor());
			pstmt.setString(4, book.getPublisher());
			pstmt.setString(5, book.getCategory());
			pstmt.setInt(6, book.getId());
			return pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;

	}

	public int deleteBook(int bookId) {
		String sql = "DELETE FROM books WHERE id = ?";

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);

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

	public List<Book> findAll() {
		String sql = "SELECT * FROM library_management.books";
		List<Book> bookList = new ArrayList<Book>();

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

	public List<Book> findByTitle(String title) {
		String sql = "SELECT * FROM books WHERE title LIKE ?";
		List<Book> bookList = new ArrayList<Book>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setString(1, "%" + title + "%");
			ResultSet rs = pstmt.executeQuery();
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

	public Book findById(int bookId) {
		String sql = "SELECT * FROM books WHERE id = ?";
		Book book = null;
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
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
			return row > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	public boolean decAvailableQuantity(Connection conn, int bookId) throws SQLException {
		String sql = """
				UPDATE books SET available_quantity = available_quantity-1 WHERE id = ? AND available_quantity > 0
				""";
		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ps.setInt(1, bookId);
			
			return ps.executeUpdate() > 0;
		}
		
	}

	public boolean increaseAvailableQuantity(int bookId) {
		String sql = """
				UPDATE books SET available_quantity = available_quantity + 1 WHERE id = ? AND available_quantity < total_quantity
				""";
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			return row > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean increaseAvailableQuantity  (Connection conn, int bookId) throws SQLException {
		String sql = """
				UPDATE books SET available_quantity = available_quantity + 1 WHERE id = ? AND available_quantity < total_quantity
				""";
		try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
			pstmt.setInt(1, bookId);
			int row = pstmt.executeUpdate();
			return row > 0;
		} 
	}
	
	public int countBooks() {
		String sql = """
				SELECT count(*) FROM books
				 """;
		try(Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				return rs.getInt("count(*)");
			}
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
}
