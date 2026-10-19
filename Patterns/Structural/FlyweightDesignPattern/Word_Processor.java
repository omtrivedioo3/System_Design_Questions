/*
 * =====================================================================================
 *                    FLYWEIGHT DESIGN PATTERN (STRUCTURAL) - NOTES
 * =====================================================================================
 *
 * WHAT
 *   If you need LOTS of similar objects, don't create the same data again and again.
 *   Create it ONCE, keep it in a cache, and SHARE it.  -> saves memory.
 *
 * TWO KINDS OF DATA (most important idea)
 *   Intrinsic = common / shared / never changes   -> stored INSIDE the flyweight
 *   Extrinsic = different for every use           -> passed from OUTSIDE (method param)
 *
 *   Word processor example: the text "hello" has 5 characters but only 4 unique letters.
 *     Intrinsic : the letter 'l', font "Arial", size 12   (same everywhere)
 *     Extrinsic : row, column                             (where it is placed)
 *
 * WITHOUT vs WITH FLYWEIGHT
 *   Without : every character = new object  -> 10,000 chars = 10,000 objects
 *   With    : one object per unique letter  -> 10,000 chars = ~26 objects
 *             (position is just passed in when drawing)
 *
 * 3 PARTS OF THE PATTERN
 *   1. Flyweight interface   -> ILetter            : display(row, col)
 *   2. Concrete Flyweight    -> Letter             : holds intrinsic data (final)
 *   3. Flyweight Factory     -> LetterFactory      : Map cache, returns shared object
 *   (+ Client: main() passes extrinsic data row/col)
 *
 * UML
 *
 *        +--------------------------+
 *        |   <<interface>> ILetter  |
 *        +--------------------------+
 *        | + display(row, col)      |   <- extrinsic data comes as parameters
 *        +------------^-------------+
 *                     | implements
 *        +------------+-------------+         creates & caches
 *        |         Letter           |<---------------------------+
 *        +--------------------------+                            |
 *        | - final ch: char         |   intrinsic, shared,       |
 *        | - final font: String     |   immutable                |
 *        | - final size: int        |              +-------------+-------------+
 *        +--------------------------+              |       LetterFactory        |
 *                                                  +---------------------------+
 *                                                  | - cache: Map<Char,ILetter>|
 *                                                  +---------------------------+
 *                                                  | + getLetter(ch): ILetter  |
 *                                                  +---------------------------+
 *
 * FLOW: LetterFactory.getLetter('l')
 *
 *     'l' already in cache? -- yes --> return the SAME object
 *          | no
 *     create new Letter('l'), put in cache, return it
 *          |
 *     client calls letter.display(row, col)   <- position supplied by client
 *
 * KEY POINTS FOR INTERVIEW
 *   - Flyweight object must be IMMUTABLE (it is shared by many users).
 *   - Always get objects via the FACTORY, never 'new' directly - else no sharing.
 *   - Use only when: huge number of objects AND most of their data repeats.
 *   - Real examples: Java String pool, Integer.valueOf(-128..127) cache,
 *     game trees/bullets sharing one image, map markers sharing one icon.
 *
 * FLYWEIGHT vs SINGLETON
 *   Singleton -> exactly ONE object of a class.
 *   Flyweight -> ONE object PER UNIQUE VALUE ('a', 'b', 'c' ... each shared).
 * =====================================================================================
 */
package Patterns.Structural.FlyweightDesignPattern;

import java.util.HashMap;
import java.util.Map;

/** 1. Flyweight interface: extrinsic data (row, col) is passed in. */
interface ILetter {
  void display(int row, int col);
}

/** 2. Concrete flyweight: only intrinsic (shared) data, immutable. */
class Letter implements ILetter {
  private final char ch;
  private final String font;
  private final int size;

  Letter(char ch, String font, int size) {
    this.ch = ch;
    this.font = font;
    this.size = size;
    System.out.println("  (new object created for '" + ch + "')");
  }

  @Override
  public void display(int row, int col) {
    System.out.println("  '" + ch + "' [" + font + ", " + size + "] at row " + row + ", col " + col);
  }
}

/** 3. Factory: creates a Letter only once per character, then reuses it. */
class LetterFactory {
  private static final Map<Character, ILetter> cache = new HashMap<>();

  public static ILetter getLetter(char ch) {
    if (!cache.containsKey(ch)) {
      cache.put(ch, new Letter(ch, "Arial", 12));
    }
    return cache.get(ch);
  }

  public static int totalObjects() {
    return cache.size();
  }
}

/** Client: supplies the extrinsic data (row, col). */
public class Word_Processor {
  public static void main(String[] args) {
    String[] lines = {"hello", "hell"};

    for (int row = 0; row < lines.length; row++) {
      String line = lines[row];
      for (int col = 0; col < line.length(); col++) {
        ILetter letter = LetterFactory.getLetter(line.charAt(col)); // shared object
        letter.display(row, col); // unique position passed in
      }
    }

    System.out.println("\nCharacters typed : 9");
    System.out.println("Objects created  : " + LetterFactory.totalObjects());

    ILetter l1 = LetterFactory.getLetter('l');
    ILetter l2 = LetterFactory.getLetter('l');
    System.out.println("Same 'l' object reused? " + (l1 == l2));
  }
}
