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
  case None, Vegetarian, Halal, GlutenFree

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

  def serialize(value: FamilyRequest): String =
    s"${value.id},${value.familyName},${value.householdSize},${value.dietaryRestriction.toString},${value.requestedCategory.toString},${value.status.toString}"

  def deserialize(line: String): Try[FamilyRequest] = Try {
    val parts = line.split(",")
    val id = parts(0)
    val familyName = parts(1)
    val householdSize = parts(2).toInt
    val dietaryRestriction = DietaryRestriction.valueOf(parts(3))
    val requestedCategory = FoodCategory.valueOf(parts(4))
    val status = RequestStatus.valueOf(parts(5))
    FamilyRequest(id, familyName, householdSize, dietaryRestriction, requestedCategory, status)
  }

