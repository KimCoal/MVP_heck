-- CAD 파일 정보 테이블
CREATE TABLE IF NOT EXISTS cad_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL,
    glb_file_path VARCHAR(500),
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'UPLOADING'
);

-- 부품 정보 테이블
CREATE TABLE IF NOT EXISTS parts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cad_file_id BIGINT NOT NULL,
    name VARCHAR(255),
    position_x DOUBLE,
    position_y DOUBLE,
    position_z DOUBLE,
    size_x DOUBLE,
    size_y DOUBLE,
    size_z DOUBLE,
    FOREIGN KEY (cad_file_id) REFERENCES cad_files(id) ON DELETE CASCADE
);

-- 부품별 메모 테이블
CREATE TABLE IF NOT EXISTS part_notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    part_id BIGINT NOT NULL UNIQUE,
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE CASCADE
);
