-- CAD 파일 정보 테이블
CREATE TABLE IF NOT EXISTS cad_files (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         original_filename VARCHAR(255) NOT NULL,

    -- 로컬 경로(현재 MVP 유지) / 나중에 S3로 전환 시 key로 교체 가능
    glb_file_path VARCHAR(500),

    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    file_size BIGINT,

    -- 상태 확장 (UPLOADING/PROCESSING/COMPLETED/FAILED 등)
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADING',

    -- 운영용 필드(권장)
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    error_message VARCHAR(1000) NULL
    );

-- 부품 정보 테이블 (B안용 식별자/계층/매핑 강화)
CREATE TABLE IF NOT EXISTS parts (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     cad_file_id BIGINT NOT NULL,

    -- 원본에서 온 이름(중복 가능)
                                     name VARCHAR(255),

    -- ✅ 추가: 사용자 표시용 이름(중복 가능, 선택)
    display_name VARCHAR(255) NULL,

    -- ✅ 핵심: 파일 내부에서 부품을 안정적으로 식별하는 키
    -- 예: glTF node index(문자열로 저장) 또는 node uuid, 혹은 nodePath 해시
    part_key VARCHAR(255) NOT NULL,

    -- (가능하면) glTF node 인덱스, 없으면 NULL
    node_index INT NULL,

    -- (권장) 계층 경로: Root/AssemblyA/Bolt_12
    node_path VARCHAR(1000) NULL,

    -- 계층 구조를 DB로 표현하고 싶으면(선택)
    parent_part_id BIGINT NULL,

    position_x DOUBLE,
    position_y DOUBLE,
    position_z DOUBLE,
    size_x DOUBLE,
    size_y DOUBLE,
    size_z DOUBLE,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_parts_cad_file
    FOREIGN KEY (cad_file_id) REFERENCES cad_files(id) ON DELETE CASCADE,

    CONSTRAINT fk_parts_parent
    FOREIGN KEY (parent_part_id) REFERENCES parts(id) ON DELETE SET NULL,

    -- ✅ 파일 내 part_key는 유일해야 매핑이 안 깨짐
    UNIQUE KEY uq_parts_file_partkey (cad_file_id, part_key),

    -- (권장) 조회 최적화 인덱스
    KEY idx_parts_cadfile (cad_file_id),
    KEY idx_parts_node_index (node_index),
    KEY idx_parts_parent (parent_part_id)
    );

-- 부품별 메모 테이블 (부품당 1개 메모 유지)
CREATE TABLE IF NOT EXISTS part_notes (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          part_id BIGINT NOT NULL UNIQUE,
                                          note TEXT,
                                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                          CONSTRAINT fk_part_notes_part
                                          FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE CASCADE
    );
