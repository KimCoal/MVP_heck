-- cad_files 테이블에 status 컬럼 추가
-- MySQL에서는 IF NOT EXISTS를 지원하지 않으므로, 컬럼이 이미 존재하면 에러가 발생합니다.
-- 에러가 발생하면 컬럼이 이미 존재하는 것이므로 무시해도 됩니다.

ALTER TABLE cad_files 
ADD COLUMN status VARCHAR(50) DEFAULT 'UPLOADING';
