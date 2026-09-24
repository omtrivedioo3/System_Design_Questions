/*
 * =====================================================================================
 *                 CHESS - LLD (FRESHER / SDE-1, 45-MIN INTERVIEW VERSION)
 * =====================================================================================
 *
 * 45-MIN PLAN
 * -------------------------------------------------------------------------------------
 *   0-5   min : Clarify requirements (below). Agree on what is OUT of scope.
 *   5-12  min : List entities + draw class diagram. Explain relationships.
 *   12-35 min : Code: enums -> Piece + subclasses -> Cell/Board -> Game.makeMove.
 *   35-45 min : Dry run a few moves, discuss extensions (checkmate, castling...).
 *
 * REQUIREMENTS (keep it small, say the rest are "extensions")
 * -------------------------------------------------------------------------------------
 *   In scope : 2 players, 8x8 board, standard setup, alternate turns (White first),
 *              each piece's move rule, no jumping (except Knight), capture,
 *              game ends when a King is captured.
 *   Extensions (only discuss): check/checkmate, castling, en passant, promotion,
 *              undo, timers, online play.
 *
 * CLASS DIAGRAM
 * -------------------------------------------------------------------------------------
 *
 *   +-----------------------------+        +-------------------+
 *   |            Game             |<>----->|      Player       |
 *   +-----------------------------+ 1    2 +-------------------+
 *   | - board: Board              |        | - name: String    |
 *   | - players: Player[2]        |        | - color: Color    |
 *   | - currentTurn: int          |        +-------------------+
 *   | - status: GameStatus        |
 *   +-----------------------------+
 *   | + makeMove(player, fr, fc,  |
 *   |            tr, tc): boolean |
 *   +--------------+--------------+
 *                  <> 1
 *                  v 1
 *   +-----------------------------+ 1   64 +-------------------+
 *   |            Board            |<>----->|       Cell        |
 *   +-----------------------------+        +-------------------+
 *   | - cells: Cell[8][8]         |        | - row, col: int   |
 *   +-----------------------------+        | - piece: Piece    |
 *   | + getCell(r, c): Cell       |        +---------+---------+
 *   | + isPathClear(from, to)     |                  | 0..1
 *   | + print()                   |                  v
 *   +-----------------------------+   +--------------------------------------+
 *                                     |        <<abstract>> Piece            |
 *                                     +--------------------------------------+
 *                                     | - color: Color                       |
 *                                     +--------------------------------------+
 *                                     | + canMove(board, from, to): boolean  |
 *                                     | # isValidMove(board, from, to) (abs) |
 *                                     +--------------------------------------+
 *                                        ^     ^     ^     ^      ^     ^
 *                                      King Queen  Rook Bishop Knight Pawn
 *
 *   Game <>-- Board <>-- Cell : composition (created & owned by parent)
 *   Cell  --> Piece           : association (piece may be empty / move away)
 *   Piece <|-- King...Pawn    : inheritance (each piece has its own move rule)
 *
 * FLOW: makeMove(player, fromRow, fromCol, toRow, toCol)
 * -------------------------------------------------------------------------------------
 *
 *     game over? ---------------------- yes --> reject
 *        | no
 *     player's turn? ------------------ no ---> reject
 *        | yes
 *     inside board? ------------------- no ---> reject
 *        | yes
 *     own piece at 'from'? ------------ no ---> reject
 *        | yes
 *     piece.canMove(board, from, to)? - no ---> reject
 *        | yes        (not same cell, not own piece at 'to',
 *        |             piece rule OK, path clear)
 *     captured piece is King? --------- yes --> status = WIN
 *        |
 *     move piece, switch turn, return true
 *
 * KEY POINTS TO SAY OUT LOUD
 * -------------------------------------------------------------------------------------
 *   - Polymorphism: Board/Game never do "if piece is Rook ...". Each Piece
 *     subclass knows its own rule -> adding a new piece = new class (Open/Closed).
 *   - Common checks written once in Piece.canMove() (Template Method pattern).
 *   - Single Responsibility: Piece = move rule, Board = cells & path, Game = turns & status.
 *
 * LIKELY FOLLOW-UPS (answer verbally)
 * -------------------------------------------------------------------------------------
 *   - Check?      After a move, see if any enemy piece canMove() onto your King.
 *                 If yes, undo the move and reject it.
 *   - Checkmate?  In check AND no move of yours gets you out of check.
 *   - Promotion?  Pawn reaches last row -> replace it with a Queen.
 *   - Undo?       Keep a stack of Move(from, to, movedPiece, capturedPiece).
 *   - Castling?   Track hasMoved on King/Rook, path must be empty.
 * =====================================================================================
 */
package ChessGame;

enum Color {
  WHITE,
  BLACK
}

enum GameStatus {
  ACTIVE,
  WHITE_WIN,
  BLACK_WIN
}

// ------------------------------------ PLAYER -----------------------------------------

class Player {
  private final String name;
  private final Color color;

  public Player(String name, Color color) {
    this.name = name;
    this.color = color;
  }

  public String getName() {
    return name;
  }

  public Color getColor() {
    return color;
  }
}

// ------------------------------------- CELL ------------------------------------------

class Cell {
  private final int row;
  private final int col;
  private Piece piece;

  public Cell(int row, int col) {
    this.row = row;
    this.col = col;
  }

  public int getRow() {
    return row;
  }

  public int getCol() {
    return col;
  }

  public Piece getPiece() {
    return piece;
  }

  public void setPiece(Piece piece) {
    this.piece = piece;
  }

  public boolean isEmpty() {
    return piece == null;
  }
}

// ------------------------------------- PIECES ----------------------------------------

abstract class Piece {
  private final Color color;

  protected Piece(Color color) {
    this.color = color;
  }

  public Color getColor() {
    return color;
  }

  /** Common checks once, then the piece-specific rule. */
  public boolean canMove(Board board, Cell from, Cell to) {
    if (from == to) {
      return false;
    }
    if (!to.isEmpty() && to.getPiece().getColor() == color) {
      return false; // can't capture own piece
    }
    return isValidMove(board, from, to);
  }

  protected abstract boolean isValidMove(Board board, Cell from, Cell to);

  /** Letter used to print the board: uppercase = White, lowercase = Black. */
  protected abstract char letter();

  public char symbol() {
    return color == Color.WHITE ? letter() : Character.toLowerCase(letter());
  }
}

class King extends Piece {
  public King(Color color) {
    super(color);
  }

  @Override
  protected boolean isValidMove(Board board, Cell from, Cell to) {
    int dr = Math.abs(from.getRow() - to.getRow());
    int dc = Math.abs(from.getCol() - to.getCol());
    return dr <= 1 && dc <= 1;
  }

  @Override
  protected char letter() {
    return 'K';
  }
}

class Queen extends Piece {
  public Queen(Color color) {
    super(color);
  }

  @Override
  protected boolean isValidMove(Board board, Cell from, Cell to) {
    int dr = Math.abs(from.getRow() - to.getRow());
    int dc = Math.abs(from.getCol() - to.getCol());
    return (dr == 0 || dc == 0 || dr == dc) && board.isPathClear(from, to);
  }

  @Override
  protected char letter() {
    return 'Q';
  }
}

class Rook extends Piece {
  public Rook(Color color) {
    super(color);
  }

  @Override
  protected boolean isValidMove(Board board, Cell from, Cell to) {
    int dr = Math.abs(from.getRow() - to.getRow());
    int dc = Math.abs(from.getCol() - to.getCol());
    return (dr == 0 || dc == 0) && board.isPathClear(from, to);
  }

  @Override
  protected char letter() {
    return 'R';
  }
}

class Bishop extends Piece {
  public Bishop(Color color) {
    super(color);
  }

  @Override
  protected boolean isValidMove(Board board, Cell from, Cell to) {
    int dr = Math.abs(from.getRow() - to.getRow());
    int dc = Math.abs(from.getCol() - to.getCol());
    return dr == dc && board.isPathClear(from, to);
  }

  @Override
  protected char letter() {
    return 'B';
  }
}

class Knight extends Piece {
  public Knight(Color color) {
    super(color);
  }

  @Override
  protected boolean isValidMove(Board board, Cell from, Cell to) {
    int dr = Math.abs(from.getRow() - to.getRow());
    int dc = Math.abs(from.getCol() - to.getCol());
    return (dr == 2 && dc == 1) || (dr == 1 && dc == 2); // jumps, no path check
  }

  @Override
  protected char letter() {
    return 'N';
  }
}

class Pawn extends Piece {
  public Pawn(Color color) {
    super(color);
  }

  @Override
  protected boolean isValidMove(Board board, Cell from, Cell to) {
    int dir = (getColor() == Color.WHITE) ? -1 : 1; // White moves up (row decreases)
    int startRow = (getColor() == Color.WHITE) ? 6 : 1;
    int dr = to.getRow() - from.getRow();
    int dc = to.getCol() - from.getCol();

    if (dc == 0 && to.isEmpty()) {
      if (dr == dir) {
        return true; // 1 step
      }
      if (dr == 2 * dir && from.getRow() == startRow) {
        return board.isPathClear(from, to); // 2 steps from start
      }
    }
    return Math.abs(dc) == 1 && dr == dir && !to.isEmpty(); // diagonal capture
  }

  @Override
  protected char letter() {
    return 'P';
  }
}

// ------------------------------------- BOARD -----------------------------------------

class Board {
  private final Cell[][] cells = new Cell[8][8];

  public Board() {
    for (int r = 0; r < 8; r++) {
      for (int c = 0; c < 8; c++) {
        cells[r][c] = new Cell(r, c);
      }
    }
    placeBackRow(0, Color.BLACK);
    placeBackRow(7, Color.WHITE);
    for (int c = 0; c < 8; c++) {
      cells[1][c].setPiece(new Pawn(Color.BLACK));
      cells[6][c].setPiece(new Pawn(Color.WHITE));
    }
  }

  private void placeBackRow(int row, Color color) {
    Piece[] pieces = {
      new Rook(color), new Knight(color), new Bishop(color), new Queen(color),
      new King(color), new Bishop(color), new Knight(color), new Rook(color)
    };
    for (int c = 0; c < 8; c++) {
      cells[row][c].setPiece(pieces[c]);
    }
  }

  public boolean isInside(int r, int c) {
    return r >= 0 && r < 8 && c >= 0 && c < 8;
  }

  public Cell getCell(int r, int c) {
    return cells[r][c];
  }

  /** All cells strictly between 'from' and 'to' must be empty. */
  public boolean isPathClear(Cell from, Cell to) {
    int stepR = Integer.signum(to.getRow() - from.getRow());
    int stepC = Integer.signum(to.getCol() - from.getCol());
    int r = from.getRow() + stepR;
    int c = from.getCol() + stepC;
    while (r != to.getRow() || c != to.getCol()) {
      if (!cells[r][c].isEmpty()) {
        return false;
      }
      r += stepR;
      c += stepC;
    }
    return true;
  }

  public void print() {
    for (int r = 0; r < 8; r++) {
      for (int c = 0; c < 8; c++) {
        System.out.print(cells[r][c].isEmpty() ? ". " : cells[r][c].getPiece().symbol() + " ");
      }
      System.out.println();
    }
    System.out.println();
  }
}

// -------------------------------------- GAME -----------------------------------------

class Game {
  private final Board board = new Board();
  private final Player[] players;
  private int currentTurn = 0; // index into players; 0 = White
  private GameStatus status = GameStatus.ACTIVE;

  public Game(Player white, Player black) {
    this.players = new Player[] {white, black};
  }

  public Board getBoard() {
    return board;
  }

  public GameStatus getStatus() {
    return status;
  }

  public boolean makeMove(Player player, int fromRow, int fromCol, int toRow, int toCol) {
    if (status != GameStatus.ACTIVE) {
      return fail("Game is over");
    }
    if (player != players[currentTurn]) {
      return fail("Not " + player.getName() + "'s turn");
    }
    if (!board.isInside(fromRow, fromCol) || !board.isInside(toRow, toCol)) {
      return fail("Outside board");
    }

    Cell from = board.getCell(fromRow, fromCol);
    Cell to = board.getCell(toRow, toCol);
    Piece piece = from.getPiece();

    if (piece == null || piece.getColor() != player.getColor()) {
      return fail("No piece of yours at the start cell");
    }
    if (!piece.canMove(board, from, to)) {
      return fail("Invalid move for " + piece.getClass().getSimpleName());
    }

    Piece captured = to.getPiece();
    to.setPiece(piece);
    from.setPiece(null);
    System.out.println(player.getName() + " moved " + piece.getClass().getSimpleName());

    if (captured instanceof King) {
      status = (player.getColor() == Color.WHITE) ? GameStatus.WHITE_WIN : GameStatus.BLACK_WIN;
      System.out.println(player.getName() + " captured the King! " + status);
    }

    currentTurn = 1 - currentTurn;
    return true;
  }

  private boolean fail(String reason) {
    System.out.println("Invalid: " + reason);
    return false;
  }
}

// -------------------------------------- DEMO -----------------------------------------

public class Chess {
  public static void main(String[] args) {
    Player alice = new Player("Alice", Color.WHITE);
    Player bob = new Player("Bob", Color.BLACK);
    Game game = new Game(alice, bob);
    game.getBoard().print();

    game.makeMove(bob, 1, 4, 3, 4);   // not Bob's turn
    game.makeMove(alice, 7, 0, 5, 0); // rook blocked by pawn
    game.makeMove(alice, 6, 4, 3, 4); // pawn can't move 3

    game.makeMove(alice, 6, 4, 5, 4); // e2-e3
    game.makeMove(bob, 1, 5, 2, 5);   // f7-f6
    game.makeMove(alice, 7, 3, 3, 7); // Qd1-h5
    game.makeMove(bob, 1, 6, 3, 6);   // g7-g5
    game.makeMove(alice, 3, 7, 0, 4); // Qh5xe8 -> captures King

    game.getBoard().print();
    System.out.println("Status: " + game.getStatus());
  }
}
