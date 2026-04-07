export class HandleStateUseCase {
  constructor(
    private state: GameState,
    private board: BoardController,
    private ui: GameUI,
    private bus: EventBus,
    private endGame: (msg: string) => void,
    private getSymbol: (t: string, c: string) => string
  ) {}

  execute(data: any) {
    this.state.currentTurn = data.turn;
    this.state.pieces = data.pieces;
    this.board.render(data.pieces, this.getSymbol);
    if (data.lastMove) {
      const { piece, from, to, color } = data.lastMove;
      this.ui.addToHistory(
        { type: piece, color },
        from,
        to
      );
    }

    this.state.resetSelection();
    this.board.clearHighlights();

    switch (data.state) {
      case "CHECK":
        this.bus.emit("TOAST", { message: "ШАХ!", type: "info" });
        break;
      case "CHECKMATE":
        this.endGame("МАТ! Игра завершена.");
        break;
      case "STALEMATE":
        this.endGame("ПАТ! Нету доступных ходов, игра завершена.");
        break;
    }
  }
}