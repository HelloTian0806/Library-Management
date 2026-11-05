package com.tian.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 設定 Spring MVC 的跨來源資源共用規則，配合前端與後端使用不同連接埠的開發方式。
 *
 * <p>{@code @Configuration} 讓 Spring 處理此設定類別，{@code @Bean} 將回傳的 WebMvcConfigurer 納入容器。
 * 來源由通訊協定、主機與連接埠共同決定，目前只允許 http://localhost:5173。
 * 這些規則影響瀏覽器跨來源存取；登入身分與管理者權限仍由後端入口檢查。
 */
@Configuration
public class CorsConfig {
	
	/**
	 * 建立提供 CORS 設定回呼的 Bean，由 Spring MVC 呼叫其 addCorsMappings。
	 *
	 * <p>使用匿名類別實作 WebMvcConfigurer，只覆寫需要的 CORS 方法，保留其他 MVC 預設設定。
	 * 方法本身建立設定物件，不會在這裡送出 HTTP 請求。
	 *
	 * @return 包含本專案 CORS 規則的 MVC 設定元件
	 */
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			
			/**
			 * 將 CORS 規則套用至所有匹配路徑，允許指定來源、方法、請求標頭及憑證。
			 *
			 * <p>allowCredentials(true) 允許符合來源規則的跨來源請求使用 Cookie 等憑證，
			 * 但前端仍需啟用相應設定，Cookie 也仍受瀏覽器政策限制；此處不會建立登入 Session。
			 *
			 * @param registry Spring MVC 提供的 CORS 規則註冊器
			 */
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				// 以 /** 套用到所有匹配路徑，實際是否允許仍須符合下方的來源與方法規則。
				registry.addMapping("/**")
					// 指定完整來源；不同主機名稱、通訊協定或連接埠不會因此一併獲准。
					.allowedOrigins("http://localhost:5173")
					// 列出可跨來源使用的方法；OPTIONS 也是瀏覽器可能用於預檢的請求方法。
					.allowedMethods(
								"GET", "POST", "PUT", "DELETE", "OPTIONS"
							)
					// 允許請求標頭，不代表允許任意來源；來源仍受 allowedOrigins 限制。
					.allowedHeaders("*")
					// 允許符合 CORS 規則的請求使用憑證，前端的 Axios 仍需設定 withCredentials。
					.allowCredentials(true);
			}
			
		};
	}

}
