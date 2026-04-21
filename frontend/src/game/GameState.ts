export class GameState {
  pieces: any[] = [];
  currentTurn = "WHITE";
  selected: string | null = null;
  availableMoves: string[] = [];

  gameOver = false;
  isStarted = false;

  promotion: {
    active: boolean;
    options: any[];
  } = {
    active: false,
    options: []
  };

  resetSelection() {
    this.selected = null;
    this.availableMoves = [];
  }
}