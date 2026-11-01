package com.tian.library.dto;

public class DashboardStatsResponse {
	
	private int totalBooks;
	private int totalMembers;
	private int activeBorrows;
	private int overdueBorrows;
	
	
	
	public DashboardStatsResponse(int totalBooks, int totalMembers, int activeBorrows, int overdueBorrows) {
		super();
		this.totalBooks = totalBooks;
		this.totalMembers = totalMembers;
		this.activeBorrows = activeBorrows;
		this.overdueBorrows = overdueBorrows;
	}
	
	public int getTotalBooks() {
		return totalBooks;
	}
	public void setTotalBooks(int totalBooks) {
		this.totalBooks = totalBooks;
	}
	public int getTotalMembers() {
		return totalMembers;
	}
	public void setTotalMembers(int totalMembers) {
		this.totalMembers = totalMembers;
	}
	public int getActiveBorrows() {
		return activeBorrows;
	}
	public void setActiveBorrows(int activeBorrows) {
		this.activeBorrows = activeBorrows;
	}
	public int getOverdueBorrows() {
		return overdueBorrows;
	}
	public void setOverdueBorrows(int overdueBorrows) {
		this.overdueBorrows = overdueBorrows;
	}
	
	
}
