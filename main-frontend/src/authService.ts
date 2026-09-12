import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import { eventBus } from "shared-frontend";

interface UserResponse {
  username: string;
  inactivityTimeoutMs: number;
}

const TOAST_STORAGE_KEY = "session_expired_toast";

export const authService = () => {
  const [username, setUsername] = useState<string | null>(null);
  const [inactivityTimeoutMs, setInactivityTimeoutMs] = useState<number>(0);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  useEffect(() => {
    const pendingMessage = sessionStorage.getItem(TOAST_STORAGE_KEY);
    if (pendingMessage) {
      eventBus.emit("TOAST", { message: pendingMessage, type: "error" });
      sessionStorage.removeItem(TOAST_STORAGE_KEY);
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
          if (username !== null) {
            handleSessionExpired("Сессия истекла.");
          }
        }
        return Promise.reject(error);
      }
    );

    axios.get<UserResponse>("/api/me")
      .then((res) => {
        setUsername(res.data.username);
        if (res.data.inactivityTimeoutMs) {
          setInactivityTimeoutMs(res.data.inactivityTimeoutMs);
        }
      })
      .catch(() => setUsername(null))
      .finally(() => setIsAuthChecking(false));

    return () => {
      axios.interceptors.response.eject(interceptor);
    };
  }, [username, handleSessionExpired]);

  const forceLocalLogout = useCallback(async () => {
    try {
      await axios.post("/api/logout");
    }
    finally {
      handleSessionExpired("Сессия завершена из-за неактивности.");
    }
  }, [handleSessionExpired]);

  useEffect(() => {
    if (!username || !inactivityTimeoutMs) return;
    let timeoutId: ReturnType<typeof setTimeout>;

    const resetTimer = () => {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        void forceLocalLogout();
      }, inactivityTimeoutMs);
    };

    const events = ["mousemove", "keydown", "mousedown", "scroll", "touchstart"];
    events.forEach((event) => window.addEventListener(event, resetTimer));
    resetTimer();

    return () => {
      clearTimeout(timeoutId);
      events.forEach((event) => window.removeEventListener(event, resetTimer));
    };
  }, [username, inactivityTimeoutMs, forceLocalLogout]);

  const login = async (name: string) => {
    const params = new URLSearchParams();
    params.append("username", name);
    params.append("password", "");

    await axios.post("/api/login", params);
    const meRes = await axios.get<UserResponse>("/api/me");
    if (meRes.data.inactivityTimeoutMs) {
      setInactivityTimeoutMs(meRes.data.inactivityTimeoutMs);
    }
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