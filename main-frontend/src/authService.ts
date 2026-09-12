import {useCallback, useEffect, useState} from "react";
import axios from "axios";
import {eventBus} from "shared-frontend";

export const authService = () => {
  const [username, setUsername] = useState<string | null>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          if (username !== null) {
            setUsername(null);
            eventBus.emit("TOAST", {
              message: "Сессия истекла на сервере.",
              type: "error"
            });
          }
        }
        return Promise.reject(error);
      }
    );

    axios.get("/api/me")
      .then((res) => setUsername(res.data.username))
      .catch(() => setUsername(null))
      .finally(() => setIsAuthChecking(false));

    return () => {
      axios.interceptors.response.eject(interceptor);
    };
  }, [username]);

  const forceLocalLogout = useCallback(() => {
    setUsername(null);
    eventBus.emit("TOAST", {
      message: "Сессия завершена из-за неактивности.",
      type: "error"
    });
  }, []);

  useEffect(() => {
    if (!username) return;
    let timeoutId: ReturnType<typeof setTimeout>;
    const INACTIVITY_TIMEOUT = 10 * 1000;

    const resetTimer = () => {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        forceLocalLogout();
      }, INACTIVITY_TIMEOUT);
    };

    const events = ['mousemove', 'keydown', 'mousedown', 'scroll', 'touchstart'];
    events.forEach(event => window.addEventListener(event, resetTimer));
    resetTimer();

    return () => {
      clearTimeout(timeoutId);
      events.forEach(event => window.removeEventListener(event, resetTimer));
    };
  }, [username, forceLocalLogout]);

  const login = async (name: string) => {
    const params = new URLSearchParams();
    params.append("username", name);
    params.append("password", "");

    await axios.post("/api/login", params);
    setUsername(name);
  };

  const logout = async () => {
    try {
      await axios.post("/api/logout");
    }
    finally {
      setUsername(null);
    }
  };

  return { username, isAuthChecking, login, logout };
};