export class GameState {
  pieces: any[] = [];
  currentTurn = "WHITE";
  selected: string | null = null;
  availableMoves: string[] = [];

  gameOver = false;
  isStarted = false;

  resetSelection() {
    this.selected = null
    this.availableMoves = []
  }
}