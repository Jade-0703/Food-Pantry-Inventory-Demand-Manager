/**
 * ============================================================================
 *                          DISTRIBUTION POLICIES
 * ============================================================================
 * Defines matching policies to distribute inventory resources to pending 
 * requests. Implements a specialized tail-recursive matching loop prioritizing 
 * perishables to prevent food waste.
 *
 * Implements:
 *   - S1-7 (Inheritance & Sealed Traits)
 *   - S1-8 (Subtype Polymorphism via overrides)
 *   - S1-11 (Immutability: 0 vars, 0 mutable collections in algorithm)
 * ============================================================================
 */
package foodpantry

import java.time.LocalDate

// ============================================================================
// 1. BASE POLICY TRAIT
// ============================================================================

/**
 * Base interface for matching inventory to family requests.
 */
sealed trait DistributionPolicy:
  def name: String
  
  /**
   * Generates a daily distribution allocation layout.
   * 
   * @param inventory The list of current pantry stock items.
   * @param requests The list of family request profiles.
   * @param today The reference date representing today (used for expiry math).
   * @return A tuple of (Updated Inventory, Updated Requests, Allocations list).
   */
  def generatePlan(
    inventory: List[FoodItem],
    requests: List[FamilyRequest],
    today: LocalDate
  ): (List[FoodItem], List[FamilyRequest], List[Allocation])

// ============================================================================
// 2. CONCRETE OPTIMIZING POLICY
// ============================================================================

object WasteMinimizingPolicy extends DistributionPolicy:
  override val name: String = "Waste-Minimizing Expiry-First Policy"

  // ai-assisted: #5
  // why: Assisted with designing the immutable tail-recursive state loop to process allocations without vars.
  /**
   * Matches compatible stock prioritizing perishable items close to expiration.
   */
  override def generatePlan(
    inventory: List[FoodItem],
    requests: List[FamilyRequest],
    today: LocalDate
  ): (List[FoodItem], List[FamilyRequest], List[Allocation]) =
    
    /** Checks whether an item category is compatible with a dietary restriction. */
    def isCompatible(item: FoodItem, diet: DietaryRestriction): Boolean =
      diet match
        case DietaryRestriction.None => true
        case DietaryRestriction.Vegetarian => 
          item.category != FoodCategory.Meat
        case DietaryRestriction.Halal => 
          item.category != FoodCategory.Meat || !item.name.toLowerCase.contains("pork")
        case DietaryRestriction.GlutenFree => 
          item.category != FoodCategory.Grains || 
          item.name.toLowerCase.contains("gluten-free") || 
          item.name.toLowerCase.contains("gf") || 
          item.name.toLowerCase.contains("rice")

    /**
     * Tail-recursive matching loop over family request objects.
     */
    @scala.annotation.tailrec
    def distributeRecursive(
      inv: List[FoodItem],
      reqs: List[FamilyRequest],
      processedReqs: List[FamilyRequest],
      allocations: List[Allocation],
      allocIdCounter: Int
    ): (List[FoodItem], List[FamilyRequest], List[Allocation]) =
      reqs match
        case Nil => 
          (inv, processedReqs.reverse, allocations.reverse)
          
        case currentReq :: tailReqs if currentReq.status == RequestStatus.Pending =>
          // Target quantity is 2.0 units per household member
          val targetQuantity = currentReq.householdSize * 2.0
          
          // Filter stock: category matches, complies with diet, and has remaining quantity
          val candidateItems = inv.filter { item =>
            item.category == currentReq.requestedCategory && 
            isCompatible(item, currentReq.dietaryRestriction) && 
            item.quantity > 0
          }

          // Sort candidates: perishable first (by expiry date ascending), then shelf-life duration
          val sortedCandidates = candidateItems.sortWith { (left, right) =>
            (left, right) match
              case (p1: PerishableItem, p2: PerishableItem) => 
                p1.expiryDate.isBefore(p2.expiryDate)
              case (_: PerishableItem, _: NonPerishableItem) => true
              case (_: NonPerishableItem, _: PerishableItem) => false
              case (np1: NonPerishableItem, np2: NonPerishableItem) => 
                np1.shelfLifeMonths < np2.shelfLifeMonths
          }

          /** Recursive worker allocating portions from sorted candidates to a single family. */
          def allocateForRequest(
            currentInv: List[FoodItem],
            candidates: List[FoodItem],
            allocatedQty: Double,
            localAllocations: List[Allocation],
            counter: Int
          ): (List[FoodItem], List[Allocation], Int) =
            if allocatedQty >= targetQuantity || candidates.isEmpty then
              (currentInv, localAllocations, counter)
            else
              val bestItem = candidates.head
              val needed = targetQuantity - allocatedQty
              val taken = Math.min(bestItem.quantity, needed)
              
              if taken <= 0 then
                allocateForRequest(currentInv, candidates.tail, allocatedQty, localAllocations, counter)
              else
                // Construct new allocation transaction
                val newAlloc = Allocation(
                  id = s"alloc-$counter",
                  familyName = currentReq.familyName,
                  itemName = bestItem.name,
                  category = bestItem.category,
                  allocatedQuantity = taken,
                  unit = bestItem.unit
                )
                
                // Copy-update inventory item state
                val updatedInv = currentInv.map { item =>
                  if item.id == bestItem.id then
                    item.withQuantity(item.quantity - taken)
                  else
                    item
                }
                
                allocateForRequest(
                  currentInv = updatedInv,
                  candidates = candidates.tail,
                  allocatedQty = allocatedQty + taken,
                  localAllocations = newAlloc :: localAllocations,
                  counter = counter + 1
                )

          val (updatedInventory, newAllocations, nextCounter) = allocateForRequest(
            currentInv = inv,
            candidates = sortedCandidates,
            allocatedQty = 0.0,
            localAllocations = Nil,
            counter = allocIdCounter
          )

          // If allocations were made, mark request as fulfilled
          val finalizedReq = if newAllocations.nonEmpty then
            currentReq.withStatus(RequestStatus.Fulfilled)
          else
            currentReq

          distributeRecursive(
            inv = updatedInventory,
            reqs = tailReqs,
            processedReqs = finalizedReq :: processedReqs,
            allocations = newAllocations ++ allocations,
            allocIdCounter = nextCounter
          )
          
        case currentReq :: tailReqs =>
          // Pass-through already resolved requests
          distributeRecursive(
            inv = inv,
            reqs = tailReqs,
            processedReqs = currentReq :: processedReqs,
            allocations = allocations,
            allocIdCounter = allocIdCounter
          )

    distributeRecursive(
      inv = inventory,
      reqs = requests,
      processedReqs = Nil,
      allocations = Nil,
      allocIdCounter = 1
    )
