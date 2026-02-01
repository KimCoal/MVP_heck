import axios from "axios";

const axiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
    withCredentials: false,
});

// 요청 인터셉터
axiosInstance.interceptors.request.use((config) => {
    return config;
});

// 응답 인터셉터: 에러 로깅
axiosInstance.interceptors.response.use(
    (res) => res,
    (err) => {
        console.error(
            "[API ERROR]",
            err?.response?.status,
            err?.response?.data || err?.message,
        );
        return Promise.reject(err);
    },
);

export default axiosInstance;
