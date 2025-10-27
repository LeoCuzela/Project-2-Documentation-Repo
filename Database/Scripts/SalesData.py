from datetime import *
import random
import math
curDate = date(2024, 6, 30)
#DATE = now.strftime("%b-%d-%Y %H:%M:%S")
FILE_NAME = "save.txt"
#june 30th 2024
# Define variables
peakDay = [date(2024, 8, 12), date(2025, 6, 5), date(2025, 4, 30), 
           date(2024, 12, 4), date(2025, 7, 6)]

peakDayAvgs = [325, 2, 0.3]
normalDayAvgs = [250, 1.5, 0.3]
stDevs = [50, 0.25, 0.25]

menuItems = {
    1 : 5.8,
    2 : 6,
    3 : 6.5,
    4 : 6.25,
    5 : 6.25,
    6 : 6.25,
    7 : 6.5,
    8 : 6.75,
    9 : 6.75,
    10 : 4.65,
    11 : 4.85,
    12 : 5.8,
    13 : 6.25,
    14 : 6.25,
    15 : 6.25,
    16 : 6.25,
    17 : 5.2,
    18 : 6.5,
    19 : 6.5,
    20 : 6.5,
    21 : 6.95,
    22 : 6.95,
    23 : 5.8,
    24 : 6.5,
    25 : 5.2,
    26 : 6.5,
    27 : 6.25,
    28 : 6.5,
    29 : 6.5,
    30 : 6.5,
    31 : 6.75,
    32 : 6.75,
    33 : 6.75
    }

ORDER_TABLE = "OrderID,EmployeeID,Location,Date,OrderTotal"
ORDER_ITEMS_TABLE = "OrderItemID,MenuID,Price,QuantityPurchased,OrderID,Size"
MOD_TABLE = "ModificationID,InventoryID,OrderItemID,Quantity,Cost"



orderID = 0
orderItemID = 0
modID = 0
numEmployees = 3

modQuantity = 1
totalSales = 0 # Not in final product, used for testing
def updateMenuPrice():
    
    # This SHOULD iterate from 1 through the number of menu items since
    # the last parameter of range is exclusive
    for key in range(1, len(menuItems) + 1):
        menuItems[key] += 0.25

def swapAvgs():
    if curDate in peakDay:
        return peakDayAvgs
    else:
        return normalDayAvgs
        
        
# Loop per day
for i in range(65*7):
    
    # Changes Avgs used in normal distribution if curDate is a peak day.
    curDayAvgs = swapAvgs()
    
    # Increase price every 5th
    if i % 91 == 0:
        updateMenuPrice()    
    
    # Number of orders in a day
    numOrders = abs(int( math.ceil(random.normalvariate(curDayAvgs[0], stDevs[0])) ))
    for j in range(numOrders):
        orderID += 1
        
        # Order Table format: 
            # "OrderID,EmployeeID,Location,Date,OrderTotal"
        ORDER_TABLE += "\n" + str(orderID) + "," + str(i%numEmployees + 1) 
        # orderTime for added time of day
        orderTime = datetime.combine(curDate, time(random.randint(7, 21), random.randint(0, 59)))
        ORDER_TABLE += ",College Station," + orderTime.strftime("%Y-%m-%d %H:%M:%S")

        
        # Order total initialized to 0 at the start of the order
        orderTotal = 0
        
        # Number of items in an order
        numOrderItems = abs(int(random.normalvariate(curDayAvgs[1], stDevs[1])))
        
        # Rerolls is less than 1
        while numOrderItems < 1:
            numOrderItems = abs(int(random.normalvariate(curDayAvgs[1], stDevs[1])))
        print("Number of order items:", numOrderItems)
        for x in range(numOrderItems):
            orderItemID += 1
            
            # Randomly select a menu item
            menuID = random.randint(1, len(menuItems))
            addOnPrice = 0
            
            # Choose size
            size = random.randint(-1, 1)
            
            # MODIFICATIONS
            # mod table format: 
                # "ModificationID,InventoryID,OrderItemID,Quantity,Cost"
            
            # 1/4 add ice
            # add to mod string
            if random.randint(1, 4) == 1:
                addOnPrice += 0.25
                modID += 1
                MOD_TABLE += "\n" + str(modID) + "," + str(menuID) + ","
                MOD_TABLE += str(orderItemID) + "," + str(modQuantity) + "," + str(0.25)
                
            # 1/4 add bobs
            # add to mod string
            if random.randint(1, 4) == 1:
                addOnPrice += 1
                modID += 1
                MOD_TABLE += "\n" + str(modID) +"," + str(menuID) + ","
                MOD_TABLE += str(orderItemID) + "," + str(modQuantity) + "," + str(1)
            

            # The fix was adding ceil to this.
            quantityOfItem = abs(int( math.ceil(random.normalvariate(1, 0.07)) ))
            print("Quantity of Item:", quantityOfItem)
            
            # Changed addOnPrice to be modified by quantityOfItem, can be changed
            itemPrice = (menuItems[menuID] + addOnPrice + (0.5 * size) ) * quantityOfItem
            
            # Rounds to 2 decimal points
            itemPrice = int(itemPrice * 100) / 100.0
            orderTotal += itemPrice
            
            # Order items table format:
                # "OrderItemID,MenuID,Price,QuantityPurchased,OrderID,Size"
            ORDER_ITEMS_TABLE += "\n" + str(orderItemID) + ","
            ORDER_ITEMS_TABLE += str(menuID) + "," + str(itemPrice) + ","
            ORDER_ITEMS_TABLE += str(quantityOfItem) + "," + str(orderID) + "," + str(size)
            print(str(size))
        
        # Add order total to order table and total sales
        ORDER_TABLE += "," + str(orderTotal)
        totalSales += orderTotal
        print("Order total: {:.2f}".format(orderTotal))
        print("Current total Sales {:.2f}".format(totalSales), "\n\n")
            
            
            
    # End of loop
    curDate = curDate + timedelta(days = 1)
print(totalSales)
outFile = open("orderItem.csv", "w")
outFile.write(ORDER_ITEMS_TABLE)
outFile.close()

outFile = open("order.csv", "w")
outFile.write(ORDER_TABLE)
outFile.close()

outFile = open("modifications.csv", "w")
outFile.write(MOD_TABLE)
outFile.close()
   # print(curDate)