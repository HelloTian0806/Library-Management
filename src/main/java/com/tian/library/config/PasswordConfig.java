package com.tian.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


/**
 * 提供會員服務需要的密碼工具設定，將具體雜湊實作集中在設定類別。
 *
 * <p>{@code @Configuration} 與 {@code @Bean} 讓 Spring 建立並管理 PasswordEncoder，
 * MemberService 只需依賴介面，無須在註冊或登入方法中自行建立 BCrypt 工具。
 * 這個設定只提供密碼雜湊與比對能力，不會自動建立登入端點或角色檢查流程。
 */
@Configuration
public class PasswordConfig {
	/**
	 * 以此版本 BCryptPasswordEncoder 的預設參數建立密碼工具 Bean。
	 *
	 * <p>encode 會產生含 salt 的密碼雜湊，matches 用於驗證原始密碼與既有雜湊。
	 * 雜湊不是可解密的密碼儲存方式；相同密碼多次 encode 通常產生不同字串，因此不直接比較兩次輸出。
	 * 目前未自訂 Bean 範圍，Spring 預設以 singleton 管理此 Bean，並可注入需要的服務。
	 *
	 * @return 由 Spring 管理、實作 PasswordEncoder 的 BCrypt 工具
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
