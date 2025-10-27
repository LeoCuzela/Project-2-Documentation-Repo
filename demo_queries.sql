--Show the employees with highest avg order value
SELECT employee.employeeName,                        
       COUNT(ordertest.orderID) AS total_orders,     
       ROUND(AVG(ordertest.orderTotal), 2) AS avg_order_value 
FROM ordertest
JOIN employee ON ordertest.employeeID = employee.employeeID 
GROUP BY employee.employeeName                       
ORDER BY avg_order_value DESC;                       

--Show highest grossing menu categories
SELECT menu.category,                                
       COUNT(orderitem.orderitemid) AS items_sold,   
       SUM(orderitem.quantitypurchased * orderitem.priceatpurchase) AS total_revenue
FROM orderitem
JOIN menu ON orderitem.menuid = menu.menuid          
GROUP BY menu.category                               
ORDER BY total_revenue DESC;                       
              

--Shows the highest grossing day
SELECT TO_CHAR(orderDate, 'Day') AS day_of_week,  
       COUNT(orderID) AS total_orders,            
       ROUND(SUM(orderTotal),2) AS total_sales             
FROM ordertest
GROUP BY day_of_week
ORDER BY total_sales DESC;


--Shows the 5 largest orders that have occured.
SELECT ordertest.orderid,
       ordertest.orderdate,
       ROUND(ordertest.ordertotal, 2) AS ordertotal
FROM ordertest
ORDER BY ordertest.ordertotal DESC
LIMIT 5;

                                   




