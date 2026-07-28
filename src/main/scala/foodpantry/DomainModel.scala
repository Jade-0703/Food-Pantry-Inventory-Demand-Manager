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
