/**
 * ============================================================================
 *                          FOOD PANTRY DOMAIN MODEL
 * ============================================================================
 * This file defines the core algebraic data structures (ADTs) representing the 
 * inventory items, recipient requests, and distribution allocations.
 *
 * Implements:
 *   - S1-7 (Inheritance & Sealed Traits)
 *   - S1-8 (Subtype Polymorphism via overrides)
 *   - S1-13 (DRY: Common base extraction)
 * ============================================================================
 */
package foodpantry

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ============================================================================
// 1. CORE ENUMS
// ============================================================================

/** Represent food groupings managed inside the inventory system. */
enum FoodCategory:
  case Grains, Vegetables, Dairy, Canned, Meat, Other

/** Represent dietary guidelines requested by recipient families. */
enum DietaryRestriction:
  case Standard, Vegetarian, Halal, GlutenFree
  def displayName: String = this match
    case Standard => "Standard"
    case GlutenFree => "Gluten-Free"
    case other => other.toString

object DietaryRestriction:
  def fromString(str: String): DietaryRestriction = str match
    case "None" | "Standard" => Standard
    case "GlutenFree" | "Gluten-Free" => GlutenFree
    case "Vegetarian" => Vegetarian
    case "Halal" => Halal
    case other => scala.util.Try(DietaryRestriction.valueOf(other)).getOrElse(Standard)

/** Represent the processing status of a logged family request. */
enum RequestStatus:
  case Pending, Fulfilled

// ============================================================================
// 2. CORE TRAITS & CLASSES
// ============================================================================

/**
 * Base abstraction for any food item logged in the pantry system.
 * Extracts shared properties and abstract interfaces.
 */
sealed trait FoodItem:
  def id: String
  def name: String
  def category: FoodCategory
  def quantity: Double
  def unit: String
  def isPerishable: Boolean
  
  /**
   * S1-8 Subtype Polymorphism.
   * Computes the dynamic textual description representing the item's safety.
   */
  def getExpiryStatus(today: LocalDate): String
  
  /** Constructs a copy of this food item with an updated numeric quantity. */
  def withQuantity(newQuantity: Double): FoodItem

/**
 * Represents perishable food items that carry explicit expiration bounds.
 */
case class PerishableItem(
  id: String,
  name: String,
  category: FoodCategory,
  quantity: Double,
  unit: String,
  expiryDate: LocalDate
) extends FoodItem:
  override val isPerishable: Boolean = true

  /** Returns days remaining or expired relative to today. */
  override def getExpiryStatus(today: LocalDate): String =
    val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)
    if daysUntilExpiry < 0 then
      s"EXPIRED (${Math.abs(daysUntilExpiry)} days ago)"
    else if daysUntilExpiry == 0 then
      "Expires TODAY!"
    else
      s"Expires in $daysUntilExpiry days"

  override def withQuantity(newQuantity: Double): FoodItem =
    this.copy(quantity = newQuantity)

/**
 * Represents shelf-stable items that possess nominal shelf lives.
 */
case class NonPerishableItem(
  id: String,
  name: String,
  category: FoodCategory,
  quantity: Double,
  unit: String,
  shelfLifeMonths: Int
) extends FoodItem:
  override val isPerishable: Boolean = false

  /** Returns shelf life duration details. */
  override def getExpiryStatus(today: LocalDate): String =
    s"Shelf-stable (life: $shelfLifeMonths months)"

  override def withQuantity(newQuantity: Double): FoodItem =
    this.copy(quantity = newQuantity)

// ============================================================================
// 3. TRANSACTION DATA MODELS
// ============================================================================

/**
 * Represents a demand record logged by a coordinator on behalf of a family.
 */
case class FamilyRequest(
  id: String,
  familyName: String,
  householdSize: Int,
  dietaryRestriction: DietaryRestriction,
  requestedCategory: FoodCategory,
  status: RequestStatus
):
  def withStatus(newStatus: RequestStatus): FamilyRequest =
    this.copy(status = newStatus)

/**
 * Represents a unit of food allocated from inventory to a specific family.
 */
case class Allocation(
  id: String,
  familyName: String,
  itemName: String,
  category: FoodCategory,
  allocatedQuantity: Double,
  unit: String
)

// ============================================================================
// 4. DOMAIN SERIALIZERS (COMPANION OBJECTS)
// ============================================================================

object FoodItem:
  import scala.util.Try

  val createTableSql: String =
    """CREATE TABLE IF NOT EXISTS inventory (
      |  id TEXT PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  category TEXT NOT NULL,
      |  quantity REAL NOT NULL,
      |  unit TEXT NOT NULL,
      |  isPerishable INTEGER NOT NULL,
      |  expiryDate TEXT,
      |  shelfLifeMonths INTEGER
      |)""".stripMargin

  def sqlRowToFoodItem(rs: java.sql.ResultSet): FoodItem =
    val id = rs.getString("id")
    val name = rs.getString("name")
    val category = FoodCategory.valueOf(rs.getString("category"))
    val quantity = rs.getDouble("quantity")
    val unit = rs.getString("unit")
    val isPerishable = rs.getInt("isPerishable") == 1
    if isPerishable then
      val expiryDate = java.time.LocalDate.parse(rs.getString("expiryDate"))
      PerishableItem(id, name, category, quantity, unit, expiryDate)
    else
      val shelfLifeMonths = rs.getInt("shelfLifeMonths")
      NonPerishableItem(id, name, category, quantity, unit, shelfLifeMonths)

  def foodItemToSqlParams(item: FoodItem): Seq[AnyRef] = item match
    case PerishableItem(id, name, category, quantity, unit, expiryDate) =>
      Seq(id, name, category.toString, java.lang.Double.valueOf(quantity), unit, java.lang.Integer.valueOf(1), expiryDate.toString, java.lang.Integer.valueOf(0))
    case NonPerishableItem(id, name, category, quantity, unit, shelfLifeMonths) =>
      Seq(id, name, category.toString, java.lang.Double.valueOf(quantity), unit, java.lang.Integer.valueOf(0), "", java.lang.Integer.valueOf(shelfLifeMonths))

  def serialize(value: FoodItem): String = value match
    case PerishableItem(id, name, category, quantity, unit, expiryDate) =>
      s"PERISHABLE,$id,$name,${category.toString},$quantity,$unit,${expiryDate.toString}"
    case NonPerishableItem(id, name, category, quantity, unit, shelfLifeMonths) =>
      s"NONPERISHABLE,$id,$name,${category.toString},$quantity,$unit,$shelfLifeMonths"

  def deserialize(line: String): Try[FoodItem] = Try {
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

object FamilyRequest:
  import scala.util.Try

  val createTableSql: String =
    """CREATE TABLE IF NOT EXISTS requests (
      |  id TEXT PRIMARY KEY,
      |  familyName TEXT NOT NULL,
      |  householdSize INTEGER NOT NULL,
      |  dietaryRestriction TEXT NOT NULL,
      |  requestedCategory TEXT NOT NULL,
      |  status TEXT NOT NULL
      |)""".stripMargin

  def sqlRowToFamilyRequest(rs: java.sql.ResultSet): FamilyRequest =
    val id = rs.getString("id")
    val familyName = rs.getString("familyName")
    val householdSize = rs.getInt("householdSize")
    val dietaryRestriction = DietaryRestriction.fromString(rs.getString("dietaryRestriction"))
    val requestedCategory = FoodCategory.valueOf(rs.getString("requestedCategory"))
    val status = RequestStatus.valueOf(rs.getString("status"))
    FamilyRequest(id, familyName, householdSize, dietaryRestriction, requestedCategory, status)

  def familyRequestToSqlParams(req: FamilyRequest): Seq[AnyRef] =
    Seq(req.id, req.familyName, java.lang.Integer.valueOf(req.householdSize), req.dietaryRestriction.toString, req.requestedCategory.toString, req.status.toString)

  def serialize(value: FamilyRequest): String =
    s"${value.id},${value.familyName},${value.householdSize},${value.dietaryRestriction.toString},${value.requestedCategory.toString},${value.status.toString}"

  def deserialize(line: String): Try[FamilyRequest] = Try {
    val parts = line.split(",")
    val id = parts(0)
    val familyName = parts(1)
    val householdSize = parts(2).toInt
    val dietaryRestriction = DietaryRestriction.fromString(parts(3))
    val requestedCategory = FoodCategory.valueOf(parts(4))
    val status = RequestStatus.valueOf(parts(5))
    FamilyRequest(id, familyName, householdSize, dietaryRestriction, requestedCategory, status)
  }


