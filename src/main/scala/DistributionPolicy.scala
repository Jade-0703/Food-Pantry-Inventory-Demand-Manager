package foodpantry

import java.time.LocalDate

// S1-7 Inheritance: sealed trait with concrete subclasses/objects
sealed trait DistributionPolicy:
  def name: String
  
  // S1-8 Subtype polymorphism: Method that subclasses implement
  def generatePlan(
    inventory: List[FoodItem],
    requests: List[FamilyRequest],
    today: LocalDate
  ): (List[FoodItem], List[FamilyRequest], List[Allocation])

object WasteMinimizingPolicy extends DistributionPolicy:
  override val name: String = "Waste-Minimizing Expiry-First Policy"

  // ai-assisted: #5
  // why: Assisted with designing the immutable tail-recursive state loop to process allocations without vars.
  override def generatePlan(
    inventory: List[FoodItem],
    requests: List[FamilyRequest],
    today: LocalDate
  ): (List[FoodItem], List[FamilyRequest], List[Allocation]) =
    
    // Check if an item is compatible with dietary restrictions
    def isCompatible(item: FoodItem, diet: DietaryRestriction): Boolean =
      diet match
        case DietaryRestriction.None => true
        case DietaryRestriction.Vegetarian => item.category != FoodCategory.Meat
        case DietaryRestriction.Halal => item.category != FoodCategory.Meat // Safely avoid meat for simplicity
        case DietaryRestriction.GlutenFree => item.category != FoodCategory.Grains

    // Pure recursive helper to distribute items to requests one-by-one
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
          
          // Filter inventory: matches request category, compatible with diet, and has quantity > 0
          val candidateItems = inv.filter { item =>
            item.category == currentReq.requestedCategory && 
            isCompatible(item, currentReq.dietaryRestriction) && 
            item.quantity > 0
          }

          // Sort candidates: perishable first (by expiry date), then non-perishable
          val sortedCandidates = candidateItems.sortWith { (left, right) =>
            (left, right) match
              case (p1: PerishableItem, p2: PerishableItem) => 
                p1.expiryDate.isBefore(p2.expiryDate)
              case (_: PerishableItem, _: NonPerishableItem) => true
              case (_: NonPerishableItem, _: PerishableItem) => false
              case (np1: NonPerishableItem, np2: NonPerishableItem) => 
                np1.shelfLifeMonths < np2.shelfLifeMonths
          }

          // Recursive allocation for this single request until target quantity met or stock runs out
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
                // Create allocation record
                val newAlloc = Allocation(
                  id = s"alloc-$counter",
                  familyName = currentReq.familyName,
                  itemName = bestItem.name,
                  category = bestItem.category,
                  allocatedQuantity = taken,
                  unit = bestItem.unit
                )
                
                // Update quantity in inventory
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
          // Request is already fulfilled or invalid, pass through
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
