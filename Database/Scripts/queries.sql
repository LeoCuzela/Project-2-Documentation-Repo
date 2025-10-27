-- Weekly Sales History -- ***
SELECT
    EXTRACT(WEEK FROM orderDate) AS week_number,
    COUNT(*) AS total_orders
FROM
    ordertest
GROUP BY
    week_number
ORDER BY
    week_number
;

--Realistic Sales History -- 
SELECT
    EXTRACT(HOUR FROM orderDate) AS hour_of_day,
    COUNT(*) AS total_orders,
    SUM(orderTotal) AS total_sales
FROM
    ordertest
GROUP BY
    hour_of_day
ORDER BY
    hour_of_day
;

-- Peak Sales Day -- ***
SELECT
    CAST(orderDate AS DATE) AS sales_day,
    SUM(orderTotal) AS total_sales
FROM
    ordertest
GROUP BY
    sales_day
ORDER BY
    total_sales DESC
LIMIT 10
;

-- Menu Item Inventory 
SELECT
    m.menuName,
    COUNT(mi.inventoryID) AS inventory_items_used
FROM
    menuInfo mi
JOIN
    menu m ON mi.menuID = m.menuID
GROUP BY
    m.menuName
ORDER BY
    inventory_items_used DESC
;

-- Best of the Worst
WITH daily_sales AS (
    SELECT
        DATE(orderDate) AS sale_day,
        SUM(orderTotal) AS total_sales
    FROM ordertest
    WHERE EXTRACT(WEEK FROM orderDate) = 26
    GROUP BY DATE(orderDate)
),
worst_day AS (
    SELECT sale_day, total_sales
    FROM daily_sales
    ORDER BY total_sales ASC
    LIMIT 1
),
top_seller AS (
    SELECT
        m.menuName,
        SUM(oi.quantityPurchased) AS total_quantity
    FROM orderItem oi
    JOIN ordertest o ON oi.orderID = o.orderID
    JOIN menu m ON oi.menuID = m.menuID
    JOIN worst_day wd ON DATE(o.orderDate) = DATE(wd.sale_day)
    GROUP BY m.menuName
    ORDER BY total_quantity DESC
    LIMIT 1
)

SELECT
    TO_CHAR(wd.sale_day, 'DD Month') AS lowest_sales_day,
    wd.total_sales,
    ts.menuName AS top_seller,
    ts.total_quantity
FROM worst_day wd
JOIN top_seller ts ON TRUE;

-- List each order with the employee who handled it. -- ***
SELECT
	ordertest.orderID,
	employee.employeeName,
	ordertest.orderDate,
	ordertest.orderTotal
FROM
	ordertest
JOIN
	employee ON ordertest.employeeID = employee.employeeID
ORDER BY
	ordertest.orderDate DESC
;

-- How many orders were placed at this location. -- ***
SELECT
	locationTable.locationName,
	COUNT(ordertest.orderID) AS numOrders
FROM
	ordertest
JOIN
	locationTable ON ordertest.orderLocation = locationTable.locationName
GROUP BY
	locationTable.locationName
;

-- How many modifications were used for each order. --
SELECT
	orderItem.orderItemID,
	menu.menuName,
	COUNT(modification.modificationID) AS totalModifications,
	SUM(modification.cost) AS extraCost
FROM
	orderItem
JOIN
	modification ON orderItem.orderItemID = modification.orderItemID
JOIN
	menu ON orderItem.menuID = menu.menuID
GROUP BY
	orderItem.orderItemID,
	menu.menuName
ORDER BY
	totalModifications DESC
;

-- Show each menu item and the total quantity purchased across all orders. --
SELECT
	menu.menuName,
	SUM(orderItem.quantityPurchased) AS totalPurchased
FROM
	orderItem
JOIN
	menu ON orderItem.menuID = menu.menuID
GROUP BY
	menu.menuName
ORDER BY
	totalPurchased DESC
;

-- Show the employees with highest avg order value -- ***
SELECT
	employee.employeeName,
	COUNT(ordertest.orderID) AS total_orders,
	ROUND(AVG(ordertest.orderTotal), 2) AS avg_order_value
FROM
	ordertest
JOIN
	employee ON ordertest.employeeID = employee.employeeID
GROUP BY
	employee.employeeName
ORDER BY
	avg_order_value DESC
;

-- Show highest grossing menu categories --
SELECT
	menu.category,
	COUNT(orderitem.orderitemid) AS items_sold,
	SUM(orderitem.quantitypurchased * orderitem.priceatpurchase) AS total_revenue
FROM
	orderitem
JOIN
	menu ON orderitem.menuid = menu.menuid
GROUP BY
	menu.category
ORDER BY
	total_revenue DESC
;

-- Shows the highest grossing day -- ***
SELECT
	TO_CHAR(orderDate, 'Day') AS day_of_week,
	COUNT(orderID) AS total_orders,
	ROUND(SUM(orderTotal), 2) AS total_sales
FROM
	ordertest
GROUP BY
	day_of_week
ORDER BY
	total_sales DESC
;

-- Shows the 10 largest orders that have occurred. --
SELECT
	ordertest.orderid,
	ordertest.orderdate,
	ROUND(ordertest.ordertotal, 2) AS ordertotal
FROM
	ordertest
ORDER BY
	ordertest.ordertotal DESC
LIMIT
	10
;

-- Show the revenue contribution by each inventory add-on. --
SELECT
    inventory.inventoryName,
    SUM(modification.cost * modification.modificationQuantity) AS total_revenue_from_addon
FROM
    modification
JOIN
    inventory ON modification.inventoryID = inventory.inventoryID
GROUP BY
    inventory.inventoryName
ORDER BY
    total_revenue_from_addon DESC
;

-- Show the employee who generated the most total revenue. --
SELECT
    employee.employeeName,
    SUM(ordertest.orderTotal) AS total_revenue
FROM
    employee
JOIN
    ordertest ON employee.employeeID = ordertest.employeeID
GROUP BY
    employee.employeeName
ORDER BY
    total_revenue DESC
;

-- Show the highest grossing orders with breakdwon of order items. --
SELECT
    ordertest.orderID,
    ordertest.orderDate,
    SUM(orderItem.quantityPurchased * orderItem.priceAtPurchase) AS order_revenue,
    COUNT(orderItem.orderItemID) AS num_items
FROM
    ordertest
JOIN
    orderItem ON ordertest.orderID = orderItem.orderID
GROUP BY
    ordertest.orderID, ordertest.orderDate
ORDER BY
    order_revenue DESC
LIMIT 5
;

-- Show the employees and the average size of orders they each handled. --
SELECT
    employee.employeeName,
    ROUND(AVG(item_counts.total_items), 2) AS avg_items_per_order
FROM
    employee
JOIN
    ordertest ON employee.employeeID = ordertest.employeeID
JOIN (
    SELECT
        orderItem.orderID,
        SUM(orderItem.quantityPurchased) AS total_items
    FROM
        orderItem
    GROUP BY
        orderItem.orderID
) AS item_counts ON ordertest.orderID = item_counts.orderID
GROUP BY
    employee.employeeName
ORDER BY
    avg_items_per_order DESC
;

-- Show the different menu items that generated the most modification revenue. --
SELECT
    menu.menuName,
    SUM(modification.cost) AS total_modification_revenue
FROM
    orderItem
JOIN
    menu ON orderItem.menuID = menu.menuID
JOIN
    modification ON orderItem.orderItemID = modification.orderItemID
GROUP BY
    menu.menuName
ORDER BY
    total_modification_revenue DESC
;