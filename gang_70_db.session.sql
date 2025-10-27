-- List each order with the employee who handled it. --
SELECT ordertest.orderID, employee.employeeName, ordertest.orderDate, ordertest.orderTotal
FROM ordertest
JOIN employee ON ordertest.employeeID = employee.employeeID
ORDER BY ordertest.orderDate DESC;

-- How many orders were placed at this location. --
SELECT locationTable.locationName, 
       COUNT(ordertest.orderID) AS numOrders
FROM ordertest
JOIN locationTable ON ordertest.orderLocation = locationTable.locationName
GROUP BY locationTable.locationName;

-- How many modifications were used for each order. --
SELECT orderItem.orderItemID, 
       menu.menuName,
       COUNT(modification.modificationID) AS totalModifications,
       SUM(modification.cost) AS extraCost
FROM orderItem
JOIN modification ON orderItem.orderItemID = modification.orderItemID
JOIN menu ON orderItem.menuID = menu.menuID
GROUP BY orderItem.orderItemID, menu.menuName
ORDER BY totalModifications DESC;

-- Show each menu item and the total quantity purchased across all orders. --
SELECT menu.menuName, SUM(orderItem.quantityPurchased) AS totalPurchased
FROM orderItem
JOIN menu ON orderItem.menuID = menu.menuID
GROUP BY menu.menuName
ORDER BY totalPurchased DESC;
