-- 圖書管理系統：目標資料庫設計，MySQL 8.0.16+ / InnoDB / utf8mb4
-- 適用全新空白資料庫；不是既有資料庫 migration。請先自行選定目標 schema。
-- 不含 DROP、測試帳號、密碼或既有資料修改；此檔尚未在 MySQL 執行。

CREATE TABLE members (
    id INT NOT NULL AUTO_INCREMENT,
    account VARCHAR(64) NOT NULL COMMENT '登入帳號；新增設計欄位',
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT '僅存應用程式產生的密碼雜湊',
    phone VARCHAR(30) NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_members_account UNIQUE (account),
    CONSTRAINT ck_members_role CHECK (role IN ('MEMBER', 'ADMIN')),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_members_account CHECK (CHAR_LENGTH(TRIM(account)) > 0),
    CONSTRAINT ck_members_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    INDEX ix_members_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE books (
    id INT NOT NULL AUTO_INCREMENT,
    isbn VARCHAR(13) NOT NULL COMMENT '去除空白與連字號後的 ISBN-10 或 ISBN-13',
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    publisher VARCHAR(255) NULL,
    category VARCHAR(100) NULL,
    total_quantity INT NOT NULL DEFAULT 0,
    available_quantity INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_books_isbn UNIQUE (isbn),
    CONSTRAINT ck_books_isbn_length CHECK (CHAR_LENGTH(isbn) IN (10, 13)),
    CONSTRAINT ck_books_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_books_author CHECK (CHAR_LENGTH(TRIM(author)) > 0),
    CONSTRAINT ck_books_quantity CHECK (
        total_quantity >= 0 AND available_quantity >= 0
        AND available_quantity <= total_quantity
    ),
    INDEX ix_books_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE borrows (
    id INT NOT NULL AUTO_INCREMENT,
    member_id INT NOT NULL,
    book_id INT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    returned_at DATETIME NULL COMMENT '實際歸還時間；新增設計欄位，與 return_date 同次更新',
    status VARCHAR(16) NOT NULL DEFAULT 'BORROWED',
    PRIMARY KEY (id),
    CONSTRAINT fk_borrows_member FOREIGN KEY (member_id) REFERENCES members(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_borrows_book FOREIGN KEY (book_id) REFERENCES books(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_borrows_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE')),
    CONSTRAINT ck_borrows_dates CHECK (due_date >= borrow_date),
    CONSTRAINT ck_borrows_return CHECK (
        (status IN ('BORROWED', 'OVERDUE') AND return_date IS NULL AND returned_at IS NULL)
        OR (status = 'RETURNED' AND return_date IS NOT NULL AND returned_at IS NOT NULL
            AND return_date >= borrow_date AND DATE(returned_at) = return_date)
    ),
    INDEX ix_borrows_member_date (member_id, borrow_date),
    INDEX ix_borrows_book_status (book_id, status),
    INDEX ix_borrows_status_due (status, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
