/*
 * =====================================================================================
 *                        COMPOSITE PATTERN - FILE SYSTEM (NOTES)
 * =====================================================================================
 *
 * WHAT
 *   A structural pattern that lets you build TREE structures and treat a single
 *   object (leaf) and a group of objects (composite) the SAME way through one
 *   common interface.  "Part-whole hierarchy."
 *
 * WHEN TO USE
 *   - Data is naturally a tree: File System, Company org chart (Employee/Manager),
 *     UI components (Button inside Panel inside Window), Menu -> SubMenu -> MenuItem,
 *     arithmetic expressions (Number / Expression(+, -, *)).
 *   - Client should NOT care whether it is talking to one item or a group.
 *
 * PARTICIPANTS
 *   Component  -> FileSystemItem  (common interface: ls(), getSize())
 *   Leaf       -> File            (no children, does the real work)
 *   Composite  -> Directory       (holds List<FileSystemItem>, delegates to children)
 *   Client     -> main()          (calls ls()/getSize() on any item, no instanceof)
 *
 * UML
 *
 *                 +---------------------------+
 *                 |  <<interface>>            |
 *                 |  FileSystemItem           |<-----------------+
 *                 +---------------------------+                  |
 *                 | + ls(depth)               |                  |  0..*
 *                 | + getSize(): int          |                  |  children
 *                 +---------------------------+                  |
 *                      ^                ^                        |
 *                      | implements     | implements             |
 *            +------------------+   +-------------------------+  |
 *            |   File (Leaf)    |   |  Directory (Composite)  |<>+
 *            +------------------+   +-------------------------+
 *            | - name, size     |   | - name                  |
 *            | + ls(depth)      |   | - children: List<Item>  |
 *            | + getSize()      |   | + add(item) / remove()  |
 *            +------------------+   | + ls(depth)  -> recurse |
 *                                   | + getSize()  -> sum     |
 *                                   +-------------------------+
 *
 * KEY POINTS (say these in the interview)
 *   1. Directory holds List<FileSystemItem> (the INTERFACE), not List<File>.
 *      That is what allows a Directory to contain Files AND other Directories.
 *   2. Operations are RECURSIVE: Directory.getSize() = sum of children's getSize().
 *      Leaf is the base case of the recursion.
 *   3. Client code is uniform: item.ls() works for a file or a whole tree.
 *   4. Follows Open/Closed: add a new node type (e.g. Shortcut) by implementing
 *      the interface; Directory code does not change.
 *
 * DESIGN TRADE-OFF (common follow-up)
 *   Where to put add()/remove()?
 *   - In Composite only (done here)  -> SAFE: can't call add() on a File,
 *                                       but client must know it has a Directory.
 *   - In Component interface         -> TRANSPARENT: uniform API, but File.add()
 *                                       must throw UnsupportedOperationException.
 *   Prefer the SAFE version unless the interviewer asks otherwise.
 *
 * NOTE: named the leaf "File" - avoid importing java.io.File in this file.
 * =====================================================================================
 */
package Patterns.CompositePattern;

import java.util.ArrayList;
import java.util.List;

/** Component: common interface for leaf (File) and composite (Directory). */
interface FileSystemItem {
  void ls(int depth);

  int getSize();
}

/** Leaf: has no children. */
class File implements FileSystemItem {
  private final String name;
  private final int size; // in KB

  public File(String name, int size) {
    this.name = name;
    this.size = size;
  }

  @Override
  public void ls(int depth) {
    System.out.println("  ".repeat(depth) + "- " + name + " (" + size + " KB)");
  }

  @Override
  public int getSize() {
    return size;
  }
}

/** Composite: holds children and delegates work to them. */
class Directory implements FileSystemItem {
  private final String name;
  // Interface type -> a Directory can hold Files or other Directories
  private final List<FileSystemItem> children = new ArrayList<>();

  public Directory(String name) {
    this.name = name;
  }

  public void add(FileSystemItem item) {
    children.add(item);
  }

  public void remove(FileSystemItem item) {
    children.remove(item);
  }

  @Override
  public void ls(int depth) {
    System.out.println("  ".repeat(depth) + "+ " + name + "/");
    for (FileSystemItem child : children) {
      child.ls(depth + 1); // recursion; indentation shows the tree
    }
  }

  @Override
  public int getSize() {
    int total = 0;
    for (FileSystemItem child : children) {
      total += child.getSize(); // same call for File or Directory
    }
    return total;
  }
}

/** Client. */
public class FileSystem {
  public static void main(String[] args) {
    FileSystemItem file1 = new File("file1.txt", 10);
    FileSystemItem file2 = new File("file2.txt", 20);
    FileSystemItem file3 = new File("file3.txt", 5);

    Directory directory1 = new Directory("directory1");
    directory1.add(file1);
    directory1.add(file2);

    Directory directory2 = new Directory("directory2");
    directory2.add(file3);
    directory2.add(directory1); // directory inside directory

    directory2.ls(0);
    System.out.println("Total size of directory2: " + directory2.getSize() + " KB");
    System.out.println("Size of file1.txt: " + file1.getSize() + " KB"); // same API for a leaf
  }
}
