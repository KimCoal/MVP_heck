import { request } from "./resp";

/**
 * 부품 상세 조회
 * GET /api/parts/{id}
 */
export const getPartById = (id) => {
    return request({
        url: `/parts/${id}`,
        method: "GET",
    });
};

/**
 * 부품 메모 저장/수정
 * POST /api/parts/{id}/note
 */
export const savePartNote = (partId, note) => {
    return request({
        url: `/parts/${partId}/note`,
        method: "POST",
        data: { note },
    });
};

/**
 * 부품 메모 삭제
 * DELETE /api/parts/{id}/note
 */
export const deletePartNote = (partId) => {
    return request({
        url: `/parts/${partId}/note`,
        method: "DELETE",
    });
};

/**
 * 부품 이름 변경
 * PATCH /api/parts/{id}/display-name
 */
export const renamePart = (partId, displayName) => {
    return request({
        url: `/parts/${partId}/display-name`,
        method: "PATCH",
        data: { displayName },
    });
};
