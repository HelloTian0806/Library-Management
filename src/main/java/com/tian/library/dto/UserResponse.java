package com.tian.library.dto;

import com.tian.library.entity.Role;

public class UserResponse {
	private int id;
	private String name;
	private String email;
	private Role role;
	
	public UserResponse(int id, String name, String email, Role role) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.role = role;
	}
	
	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
}
