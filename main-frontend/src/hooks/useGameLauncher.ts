import { useCallback, useEffect, useRef, useState } from "react";
import { Subject, from, EMPTY } from "rxjs";
import { exhaustMap, catchError, tap } from "rxjs/operators";
import axios from "axios";
import { eventBus } from "shared-frontend";
import { Game } from "../types";

export const useGameLauncher = () => {
  const [loadingGameId, setLoadingGameId] = useState<string | null>(null);
  const startGameSubject$ = useRef(new Subject<Game>());

  useEffect(() => {
    const subscription = startGameSubject$.current
      .pipe(
        exhaustMap((game) => {
          if (!game.active || !game.startEndpoint || !game.targetUrl) {
            return EMPTY;
          }

          setLoadingGameId(game.id);
          return from(axios.post(game.startEndpoint)).pipe(
            tap(() => {
              window.location.replace(game.targetUrl!);
            }),
            catchError((err) => {
              let message = "Не удалось запустить игру. Попробуйте снова.";
              if (axios.isAxiosError(err) && err.response?.data?.message) {
                message = err.response.data.message;
              }

              eventBus.emit("TOAST", { message, type: "error" });
              setLoadingGameId(null);
              return EMPTY;
            })
          );
        })
      )
      .subscribe();

    return () => subscription.unsubscribe();
  }, []);

  const startGame = useCallback((game: Game) => {
    startGameSubject$.current.next(game);
  }, []);

  return { loadingGameId, startGame };
};