package Tic_Tac_Toe;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

/**
 * ============================================================================================================
 *                                  TIC-TAC-TOE LLD - UML CLASS DIAGRAM & FLOW
 * ============================================================================================================
 *
 * 1. UML CLASS DIAGRAM:
 * ---------------------
 *   +------------------------------------+          +------------------------------------+
 *   |              Game                  |          |               Board                |
 *   +------------------------------------+          +------------------------------------+
 *   | - players : Deque<Player>          |          | ~ matrix : Symbol[][]              |
 *   | - board   : Board                  | *------> | ~ size   : int                     |
 *   +------------------------------------+  (1..1)  +------------------------------------+
 *   | + Game(p1: Player, p2: Player)     |          | + Board(size: int)                 |
 *   | + startGame() : void               |          | + printBoard() : void              |
 *   | ~ validateMove(r: int, c: int):bool|          | + isFull() : boolean               |
 *   | ~ isWinner(s: Symbol) : boolean    |          +------------------------------------+
 *   +------------------------------------+                            |
 *                     o                                               | uses
 *                     | (1..2)                                        v
 *                     v                                     +-------------------+
 *   +------------------------------------+                  |   <<enumeration>> |
 *   |              Player                |                  |      Symbol       |
 *   +------------------------------------+                  +-------------------+
 *   | - name   : String                  |                  | X                 |
 *   | - age    : int                     | -------------->  | O                 |
 *   | - symbol : Symbol                  |     uses         +-------------------+
 *   +------------------------------------+
 *   | + Player(name, age, symbol)        |
 *   | + getName() : String               |
 *   | + getSymbol() : Symbol             |
 *   +------------------------------------+
 *
 *   UML Legend:
 *   `*-->` : Composition (Game owns Board lifecycle)
 *   `o-->` : Aggregation (Game holds Players in Deque)
 *   `-`    : private   |   `+` : public   |   `~` : package-private
 *
 *
 * 2. GAME LOOP EXECUTION FLOW (Inside Game.startGame()):
 * ------------------------------------------------------
 *   [START GAME] ---> Initialize Deque with [Player 1 (X), Player 2 (O)] & Empty 3x3 [Board]
 *        |
 *        v
 *   +---> WHILE (true)
 *   |       |
 *   |       +---> 1. PEEK current player from front of Deque: `Player currentPlayer = players.peek()`
 *   |       |        *(Why peek instead of poll? So if they enter an invalid move, they don't lose their turn!)*
 *   |       |
 *   |       +---> 2. INPUT: Read (row, col) from Scanner
 *   |       |
 *   |       +---> 3. VALIDATE MOVE: `validateMove(row, col)`
 *   |       |          |---> Out of bounds (<0 or >=size)?  ---YES---> Print Error & CONTINUE loop (retry turn)
 *   |       |          +---> Cell already occupied (!=null)? ---YES---> Print Error & CONTINUE loop (retry turn)
 *   |       |
 *   |       +---> 4. APPLY MOVE: `board.matrix[row][col] = currentPlayer.getSymbol()` & `board.printBoard()`
 *   |       |
 *   |       +---> 5. CHECK WIN CONDITION: `isWinner(currentPlayer.getSymbol())`
 *   |       |          +---> Checks Row, Column, Diagonal, Anti-Diagonal
 *   |       |          +---> If TRUE ---> Print "🎉 Winner is [Player]!" ---> BREAK (Game Over)
 *   |       |
 *   |       +---> 6. CHECK DRAW CONDITION: `board.isFull()`
 *   |       |          +---> All 9 cells filled with no winner?
 *   |       |          +---> If TRUE ---> Print "🤝 Game ended in a Draw!" ---> BREAK (Game Over)
 *   |       |
 *   |       +---> 7. ROTATE TURN (Queue Rotation):
 *   |                `players.poll()` (remove from front) ---> `players.add(currentPlayer)` (push to back)
 *   |                Next iteration automatically picks the other player!
 *   +-------+
 * ============================================================================================================
 */

enum Symbol {
  X,
  O
}

class Player {
  private final String name;
  private final int age;
  private final Symbol symbol;

  Player(String name, int age, Symbol symbol) {
    this.name = name;
    this.age = age;
    this.symbol = symbol;
  }

  public String getName() {
    return name;
  }

  public Symbol getSymbol() {
    return symbol;
  }
}

class Board {
  final Symbol[][] matrix;
  final int size;

  Board(int size) {
    this.size = size;
    this.matrix = new Symbol[size][size];
  }

  public void printBoard() {
    System.out.println("\n-------------");
    for (int i = 0; i < size; i++) {
      System.out.print("| ");
      for (int j = 0; j < size; j++) {
        String cell = (matrix[i][j] == null) ? " " : matrix[i][j].name();
        System.out.print(cell + " | ");
      }
      System.out.println("\n-------------");
    }
  }

  public boolean isFull() {
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        if (matrix[i][j] == null) {
          return false;
        }
      }
    }
    return true;
  }
}

class Game {
  private final Deque<Player> players;
  private final Board board;

  Game(Player player1, Player player2) {
    // Fix #1: Initialize the Deque before calling .add()!
    this.players = new ArrayDeque<>();
    this.players.add(player1);
    this.players.add(player2);
    this.board = new Board(3);
  }

  void startGame() {
    Scanner sc = new Scanner(System.in);
    board.printBoard();

    while (true) {
      // Peek (don't remove yet) so if the move is invalid, the same player retries!
      Player currentPlayer = players.peek();

      System.out.print("Enter row (0-2) and col (0-2) for " + currentPlayer.getName()
          + " (" + currentPlayer.getSymbol() + "): ");
      int row = sc.nextInt();
      int col = sc.nextInt();

      // Validate move: if invalid, continue loop so currentPlayer retries
      if (!validateMove(row, col)) {
        continue;
      }

      // Valid move: place symbol and print board
      board.matrix[row][col] = currentPlayer.getSymbol();
      board.printBoard();

      // Fix #2: Check winner immediately AFTER currentPlayer makes their move!
      if (isWinner(currentPlayer.getSymbol())) {
        System.out.println("🎉 Winner is " + currentPlayer.getName() + " (" + currentPlayer.getSymbol() + ")!");
        break;
      }

      // Fix #3: Check for Draw / Tie if board is full
      if (board.isFull()) {
        System.out.println("🤝 Game ended in a Draw!");
        break;
      }

      // Rotate turn: remove current player from front and add to back of queue
      players.poll();
      players.add(currentPlayer);
    }
  }

  boolean validateMove(int row, int col) {
    if (row < 0 || row >= board.size || col < 0 || col >= board.size) {
      System.out.println("❌ Invalid move: Out of bounds! Please enter values between 0 and " + (board.size - 1));
      return false;
    }

    if (board.matrix[row][col] != null) {
      System.out.println("❌ Invalid move: Spot (" + row + ", " + col + ") is already occupied!");
      return false;
    }
    return true;
  }

  boolean isWinner(Symbol symbol) {
    // Check rows
    for (int i = 0; i < board.size; i++) {
      if (board.matrix[i][0] == symbol && board.matrix[i][1] == symbol && board.matrix[i][2] == symbol) {
        return true;
      }
    }

    // Check columns
    for (int j = 0; j < board.size; j++) {
      if (board.matrix[0][j] == symbol && board.matrix[1][j] == symbol && board.matrix[2][j] == symbol) {
        return true;
      }
    }

    // Check main diagonal
    if (board.matrix[0][0] == symbol && board.matrix[1][1] == symbol && board.matrix[2][2] == symbol) {
      return true;
    }

    // Check anti-diagonal
    if (board.matrix[0][2] == symbol && board.matrix[1][1] == symbol && board.matrix[2][0] == symbol) {
      return true;
    }

    return false;
  }
}

public class Tic_Tac_Toe {
  public static void main(String[] args) {
    Player player1 = new Player("Om", 21, Symbol.X);
    Player player2 = new Player("Gaurav", 22, Symbol.O);
    Game game = new Game(player1, player2);
    game.startGame();
  }
}
