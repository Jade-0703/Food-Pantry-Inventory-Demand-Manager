package foodpantry

import org.scalatest.funsuite.AnyFunSuite
import java.time.LocalDate

class PantryLogicSpec extends AnyFunSuite:

  test("PerishableItem calculates correct expiry status") {
    val today = LocalDate.of(2026, 7, 19)
    val item1 = PerishableItem("inv-1", "Apples", FoodCategory.Vegetables, 10.0, "kg", LocalDate.of(2026, 7, 22))
    val item2 = PerishableItem("inv-2", "Milk", FoodCategory.Dairy, 2.0, "litres", LocalDate.of(2026, 7, 19))
    val item3 = PerishableItem("inv-3", "Meat", FoodCategory.Meat, 5.0, "kg", LocalDate.of(2026, 7, 17))
    
    assert(item1.getExpiryStatus(today) == "Expires in 3 days (2026-07-22)")
    assert(item2.getExpiryStatus(today) == "Expires TODAY! (2026-07-19)")
    assert(item3.getExpiryStatus(today) == "EXPIRED (2 days ago on 2026-07-17)")
  }

  test("NonPerishableItem shows shelf stable status") {
    val today = LocalDate.of(2026, 7, 19)
    val item = NonPerishableItem("inv-4", "Rice", FoodCategory.Grains, 50.0, "kg", 24)
    assert(item.getExpiryStatus(today) == "Shelf-stable (life: 24 months)")
  }

  test("WasteMinimizingPolicy matches inventory to demand correctly and respects dietary restrictions") {
    val today = LocalDate.of(2026, 7, 19)

    // Inventory
    val perishableApples = PerishableItem("inv-1", "Organic Apples", FoodCategory.Vegetables, 5.0, "kg", LocalDate.of(2026, 7, 20))
    val shelfStableApples = NonPerishableItem("inv-2", "Canned Apples", FoodCategory.Vegetables, 10.0, "kg", 12)
    val porkChops = PerishableItem("inv-3", "Pork Chops", FoodCategory.Meat, 10.0, "kg", LocalDate.of(2026, 7, 25))
    
    val inventory = List(perishableApples, shelfStableApples, porkChops)

    // Family requests
    // Vegetarian family wants vegetables
    val vegRequest = FamilyRequest("req-1", "Veg Family", 2, DietaryRestriction.Vegetarian, FoodCategory.Vegetables, RequestStatus.Pending)
    // Halal family wants meat
    val halalRequest = FamilyRequest("req-2", "Halal Family", 3, DietaryRestriction.Halal, FoodCategory.Meat, RequestStatus.Pending)
    
    val requests = List(vegRequest, halalRequest)

    val (updatedInv, updatedReqs, allocations) = WasteMinimizingPolicy.generatePlan(inventory, requests, today)

    // 1. Veg family should get organic apples first because they expire tomorrow (waste minimized)
    val vegAllocations = allocations.filter(_.familyName == "Veg Family")
    assert(vegAllocations.nonEmpty)
    // Veg family of size 2 wants up to 2 * 2.0 = 4.0 units. 
    // They should get 4.0 units of Organic Apples (leaving 1.0 unit in inventory).
    val applesAllocation = vegAllocations.find(_.itemName == "Organic Apples")
    assert(applesAllocation.isDefined)
    assert(applesAllocation.get.allocatedQuantity == 4.0)

    // Check inventory update: inv-1 should have 1.0 left
    val updatedApples = updatedInv.find(_.id == "inv-1")
    assert(updatedApples.isDefined)
    assert(updatedApples.get.quantity == 1.0)

    // 2. Halal family should NOT get pork chops because Meat is excluded under Halal for simplicity
    val halalAllocations = allocations.filter(_.familyName == "Halal Family")
    assert(halalAllocations.isEmpty) // Excluded! porkChops are not allocated
  }
