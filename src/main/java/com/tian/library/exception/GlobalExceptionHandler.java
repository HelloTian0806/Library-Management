package com.tian.library.exception;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 統一處理 Spring MVC 請求流程交付的 SQLException，將內部診斷與對外回應分開。
 *
 * <p>{@code @RestControllerAdvice} 將此類別註冊為可跨 Controller 使用的例外處理元件，
 * 處理方法的回傳內容會寫入 HTTP 回應。本類別只宣告 SQLException 的處理方法，
 * 不代表它同時處理所有驗證錯誤、執行期錯誤、背景工作或啟動階段的例外。
 *
 * <p>交易回滾仍由 BorrowService 負責；這裡只記錄日誌及組成 HTTP 回應，不補做資料庫操作。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	/**
	 * 類別共用的日誌記錄器，由 SLF4J 取得；不保存每個請求的可變資料。
	 */
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	/**
	 * 接收由 MVC 例外解析流程匹配到的 SQLException，記錄診斷後回傳一般錯誤文字。
	 *
	 * <p>SQLState 與 errorCode 是資料庫／驅動程式提供的診斷資訊，不是 HTTP 狀態碼。
	 * 最後一個日誌參數為例外物件，讓 SLF4J 同時記錄堆疊；對外本文不包含這些內部細節。
	 * 目前所有匹配的 SQLException 都回應 500，沒有依唯一鍵衝突等原因另外區分狀態碼。
	 *
	 * @param e 目前被處理的 JDBC 例外
	 * @param httpRequest 目前的 HTTP 請求，用於取得方法與請求路徑
	 * @return 500 與系統暫時無法完成操作的文字，不是例外物件或資料庫錯誤訊息
	 */
	@ExceptionHandler(SQLException.class)
	public ResponseEntity<String> handleSQLException(SQLException e, HttpServletRequest httpRequest){
		
		// 記錄請求方法、路徑及資料庫診斷；末尾的 e 會另外輸出例外堆疊。
		log.error("資料庫操作失敗:method={}, path={}, sqlState={}, errorCode={}",
				httpRequest.getMethod(),
				httpRequest.getRequestURI(),
				e.getSQLState(),
				e.getErrorCode()
				,e);
		
		// 不將 SQLException 或 SQL 診斷直接序列化給前端，由固定文字表達系統錯誤。
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR	).body("系統暫時無法完成操作");
		
	}

}
