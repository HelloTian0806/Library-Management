package com.tian.library.service;

import org.springframework.stereotype.Service;

import com.tian.library.dao.MemberDAO;
import com.tian.library.entity.Member;

@Service
public class MemberService {
	private final MemberDAO memberDAO;

	public MemberService(MemberDAO memberDAO) {
		this.memberDAO = memberDAO;
	}

	public Member findById(int memberId) {
		return memberDAO.findById(memberId);
	}
}
