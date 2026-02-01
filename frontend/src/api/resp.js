import axiosInstance from "./axiosInstance";

/**
 * 백엔드 공통 응답(ApiRespDto)을 표준 처리
 * - status !== "success"면 에러 throw
 * - 성공이면 data 필드만 반환
 */
export const request = async (config) => {
    try {
        const res = await axiosInstance(config);
        const body = res?.data;

        if (!body) {
            throw new Error("서버 응답이 비어있습니다.");
        }

        // 백엔드: { status, message, data }
        const { status, message, data } = body;

        if (status !== "success") {
            throw new Error(message || "요청 처리에 실패했습니다.");
        }

        // 성공 시 'data' 알맹이만 반환
        return data;
    } catch (error) {
        console.error("[API REQUEST ERROR]", error);
        throw error;
    }
};
