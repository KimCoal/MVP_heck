import { request } from "./resp";

/**
 * CAD 파일 업로드
 * POST /api/cad/upload
 */
export const uploadCadFile = (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return request({
        url: "/cad/upload",
        method: "POST",
        data: formData,
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
};

/**
 * 업로드된 파일 목록 조회
 * GET /api/cad/files
 */
export const getCadFiles = () => {
    return request({
        url: "/cad/files",
        method: "GET",
    });
};

/**
 * 파일 상세 조회 (부품 정보 포함)
 * GET /api/cad/files/{id}
 */
export const getCadFileById = (id) => {
    return request({
        url: `/cad/files/${id}`,
        method: "GET",
    });
};

/**
 * GLB 파일 URL 생성
 */
export const getGlbFileUrl = (id) => {
    const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
    return `${baseURL}/cad/files/${id}/glb`;
};
