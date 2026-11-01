package com.tian.library.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tian.library.dao.MemberDAO;
import com.tian.library.dto.LoginRequest;
import com.tian.library.dto.RegisterRequest;
import com.tian.library.entity.Member;

@Service
public class MemberService {
	private final MemberDAO memberDAO;
	private final PasswordEncoder passwordEncoder;
	
	public MemberService(MemberDAO memberDAO, PasswordEncoder passwordEncoder) {
		this.memberDAO = memberDAO;
		this.passwordEncoder = passwordEncoder;
	}

	public Member findById(int memberId) {
		return memberDAO.findById(memberId);
	}
	
	public boolean findByEmail(String email) {
		return memberDAO.findByEmail(email) == null;
	}
	
	public Member findByMemberEmail(String email) {
		return memberDAO.findByEmail(email);
	}
	
	public List<Member> findAll(){
		
		return memberDAO.findAll();
		
	}
	
	public List<Member> findByName(String name) {
		
		return memberDAO.findByName(name);
		
	}
	
	public boolean updateMember(Member member) {
		
		return memberDAO.updateMember(member);
		
	}
	
	public boolean deleteMember(int memberId) {
		
		return memberDAO.deleteMember(memberId);
		
	}
	
	public boolean register(RegisterRequest request) {
		
		// 檢查帳號是否存在
		if (memberDAO.findByEmail(request.getEmail()) != null) {
			
			return false;
		
		}
		
		// 密碼雜湊
		String hashedPassword = passwordEncoder.encode(request.getPassword());

		
		Member member = new Member(request.getName(),request.getEmail(),hashedPassword,request.getPhone());
		
		return memberDAO.register(member) > 0;
	}
	
	public Member login(LoginRequest request) {
		
		// 查詢會員
		Member member = memberDAO.findByEmail(request.getEmail());
		
		// 檢查會員是否存在
		if(member == null) {
			return null;
		}
		
		// 驗證密碼
		if(!passwordEncoder.matches(request.getPassword(),member.getPassword() )) {
			return null;
		}
		
		// 登入成功
		return member;
	}
	
}
