import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import { eventBus } from "shared-frontend";
import { useInactivityTimer } from "./useInactivityTimer";

interface UserResponse {
  username: string;
  inactivityTimeoutMs: number;
}

const TOAST_STORAGE_KEY = "session_expired_toast";

export const useAuthService = () => {
  const [username, setUsername] = useState<string | null>(null);
  const [inactivityTimeoutMs, setInactivityTimeoutMs] = useState<number>(0);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  useEffect(() => {
    const pendingMessage = sessionStorage.getItem(TOAST_STORAGE_KEY);
    if (pendingMessage) {
      const timerId = setTimeout(() => {
        eventBus.emit("TOAST", { message: pendingMessage, type: "error" });
        sessionStorage.removeItem(TOAST_STORAGE_KEY);
      }, 100);

      return () => clearTimeout(timerId);
    }
  }, []);

  const handleSessionExpired = useCallback((message: string) => {
    setUsername(null);
    if (window.location.pathname !== "/") {
      sessionStorage.setItem(TOAST_STORAGE_KEY, message);
      window.location.replace("/");
    }
    else {
      eventBus.emit("TOAST", { message, type: "error" });
    }
  }, []);

  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          handleSessionExpired("Сессия истекла.");
        }
        return Promise.reject(error);
      }
    );

    axios.get<UserResponse>("/api/me")
      .then((res) => {
        setUsername(res.data.username);
        setInactivityTimeoutMs(res.data.inactivityTimeoutMs ?? 0);
      })
      .catch(() => setUsername(null))
      .finally(() => setIsAuthChecking(false));

    return () => axios.interceptors.response.eject(interceptor);
  }, [handleSessionExpired]);

  const handleInactivityTimeout = useCallback(() => {
    axios.post("/api/logout").finally(() => {
      handleSessionExpired("Сессия завершена из-за неактивности.");
    });
  }, [handleSessionExpired]);

  useInactivityTimer(inactivityTimeoutMs, handleInactivityTimeout, Boolean(username));

  const login = async (name: string) => {
    const params = new URLSearchParams({ username: name, password: "" });
    await axios.post("/api/login", params);

    const { data } = await axios.get<UserResponse>("/api/me");
    setInactivityTimeoutMs(data.inactivityTimeoutMs ?? 0);
    setUsername(name);
  };

  const logout = async () => {
    try {
      await axios.post("/api/logout");
    }
    finally {
      setUsername(null);
      if (window.location.pathname !== "/") {
        window.location.replace("/");
      }
    }
  };

  return { username, isAuthChecking, login, logout };
};