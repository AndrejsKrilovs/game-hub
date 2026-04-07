export class GameState {
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

	getPiece(coord: string) {
    return this.pieces.find(
      p => `${p.coordinates.file}${p.coordinates.rank}` === coord
    );
  }
}