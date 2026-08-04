/**
 * ============================================================================
 *                          PERSISTENCE & REPOSITORIES
 * ============================================================================
 * Defines generic data access traits and file-backed CSV serialization layers 
 * for storing data safely in CSV formats on local storage.
 *
 * Implements:
 *   - S1-9 (Parametric Polymorphism via generics [T])
 *   - S1-10 (Encapsulation via private fields)
 *   - S1-12 (Exception Handling via scala.util.Try)
 * ============================================================================
 */
package foodpantry

import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Try

// ============================================================================
// 1. GENERIC ABSTRACTIONS
// ============================================================================

/**
 * S1-9 Parametric Polymorphism.
 * Generic boundary interface for storing and retrieving lists of records.
 */
trait Repository[T]:
  def loadAll(): Try[List[T]]
  def saveAll(items: List[T]): Try[Unit]

// ============================================================================
// 2. CONCRETE PERSISTENCE LAYER
// ============================================================================

// ai-assisted: #3
// why: Assisted with drafting the generic persistence repository interface pattern using serializers.
/**
 * Generic repository implementation backing persistence onto a plain text file.
 */
class FileRepository[T](
  filePathString: String,
  serialize: T => String,
  deserialize: String => Try[T]
) extends Repository[T]:

  // S1-10 Encapsulation: Private field with adjacent rationale comment
  // Rationale: Keep database filepath hidden to prevent raw file modification bypass
  private val dataFilePath: String = filePathString

  /**
   * S1-12 Exception Handling.
   * Loads and deserializes all items from the encapsulated file source.
   */
  override def loadAll(): Try[List[T]] = Try {
    val file = new File(dataFilePath)
    if !file.exists() then
      List.empty[T]
    else
      val source = Source.fromFile(file)
      try
        val lines = source.getLines().toList
        // Convert lines to items, ignoring empty or invalid ones safely
        lines.filter(_.trim.nonEmpty).flatMap { line =>
          deserialize(line).toOption
        }
      finally
        source.close()
  }

  /**
   * S1-12 Exception Handling.
   * Writes all items to the encapsulated file.
   */
  override def saveAll(items: List[T]): Try[Unit] = Try {
    val file = new File(dataFilePath)
    val writer = new PrintWriter(file)
    try
      items.foreach { item =>
        writer.println(serialize(item))
      }
    finally
      writer.close()
  }
