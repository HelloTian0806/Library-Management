package com.tian.library.exception;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(SQLException.class)
	public ResponseEntity<String> handleSQLException(SQLException e, HttpServletRequest httpRequest){
		
		// 完整例外留在伺服器日誌		
		log.error("資料庫操作失敗:method={}, path={}, sqlState={}, errorCode={}",
				httpRequest.getMethod(),
				httpRequest.getRequestURI(),
				e.getSQLState(),
				e.getErrorCode()
				,e);
		
		// 前端只收到一般訊息
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR	).body("系統暫時無法完成操作");
		
	}

}
