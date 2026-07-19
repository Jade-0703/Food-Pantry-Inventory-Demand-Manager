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
 * Interface defining serialization logic for an arbitrary type T.
 */
trait Serializer[T]:
  def serialize(value: T): String
  def deserialize(line: String): Try[T]

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
  serializer: Serializer[T]
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
          serializer.deserialize(line).toOption
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
        writer.println(serializer.serialize(item))
      }
    finally
      writer.close()
  }

// ============================================================================
// 3. MODEL SERIALIZERS
// ============================================================================

/** CSV Serializer implementation for FoodItems. */
object FoodItemSerializer extends Serializer[FoodItem]:
  override def serialize(value: FoodItem): String = value match
    case PerishableItem(id, name, category, quantity, unit, expiryDate) =>
      s"PERISHABLE,$id,$name,${category.toString},$quantity,$unit,${expiryDate.toString}"
    case NonPerishableItem(id, name, category, quantity, unit, shelfLifeMonths) =>
      s"NONPERISHABLE,$id,$name,${category.toString},$quantity,$unit,$shelfLifeMonths"

  override def deserialize(line: String): Try[FoodItem] = Try {
    val parts = line.split(",")
    val itemType = parts(0)
    val id = parts(1)
    val name = parts(2)
    val category = FoodCategory.valueOf(parts(3))
    val quantity = parts(4).toDouble
    val unit = parts(5)
    itemType match
      case "PERISHABLE" =>
        val expiryDate = java.time.LocalDate.parse(parts(6))
        PerishableItem(id, name, category, quantity, unit, expiryDate)
      case "NONPERISHABLE" =>
        val shelfLifeMonths = parts(6).toInt
        NonPerishableItem(id, name, category, quantity, unit, shelfLifeMonths)
      case _ =>
        throw new IllegalArgumentException(s"Unknown item type: $itemType")
  }

/** CSV Serializer implementation for FamilyRequests. */
object FamilyRequestSerializer extends Serializer[FamilyRequest]:
  override def serialize(value: FamilyRequest): String =
    s"${value.id},${value.familyName},${value.householdSize},${value.dietaryRestriction.toString},${value.requestedCategory.toString},${value.status.toString}"

  override def deserialize(line: String): Try[FamilyRequest] = Try {
    val parts = line.split(",")
    val id = parts(0)
    val familyName = parts(1)
    val householdSize = parts(2).toInt
    val dietaryRestriction = DietaryRestriction.valueOf(parts(3))
    val requestedCategory = FoodCategory.valueOf(parts(4))
    val status = RequestStatus.valueOf(parts(5))
    FamilyRequest(id, familyName, householdSize, dietaryRestriction, requestedCategory, status)
  }
