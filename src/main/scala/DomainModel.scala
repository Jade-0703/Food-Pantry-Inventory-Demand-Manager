package foodpantry

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum FoodCategory:
  case Grains, Vegetables, Dairy, Canned, Meat, Other

enum DietaryRestriction:
  case None, Vegetarian, Halal, GlutenFree

enum RequestStatus:
  case Pending, Fulfilled

// S1-7 Inheritance: sealed trait FoodItem with concrete subclasses
// S1-13 DRY: Common base trait extracts shared logic
sealed trait FoodItem:
  def id: String
  def name: String
  def category: FoodCategory
  def quantity: Double
  def unit: String
  def isPerishable: Boolean
  
  // S1-8 Subtype polymorphism: Method that subclasses override
  def getExpiryStatus(today: LocalDate): String
  
  // Helper to construct a new item with updated quantity
  def withQuantity(newQuantity: Double): FoodItem

case class PerishableItem(
  id: String,
  name: String,
  category: FoodCategory,
  quantity: Double,
  unit: String,
  expiryDate: LocalDate
) extends FoodItem:
  override val isPerishable: Boolean = true

  // S1-8 Subtype polymorphism implementation
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

case class NonPerishableItem(
  id: String,
  name: String,
  category: FoodCategory,
  quantity: Double,
  unit: String,
  shelfLifeMonths: Int
) extends FoodItem:
  override val isPerishable: Boolean = false

  // S1-8 Subtype polymorphism implementation
  override def getExpiryStatus(today: LocalDate): String =
    s"Shelf-stable (life: $shelfLifeMonths months)"

  override def withQuantity(newQuantity: Double): FoodItem =
    this.copy(quantity = newQuantity)

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

case class Allocation(
  id: String,
  familyName: String,
  itemName: String,
  category: FoodCategory,
  allocatedQuantity: Double,
  unit: String
)
