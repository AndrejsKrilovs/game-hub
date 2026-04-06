export class GameState {
	pendingMove: { piece: any; from: string; to: string } | null = null;
  pieces: any[] = [];
  currentTurn = "WHITE";
  selected: string | null = null;
  availableMoves: string[] = [];

  gameOver = false;
  isStarted = false;

  resetSelection() {
    this.selected = null;
    this.availableMoves = [];
  }
}