CREATE TABLE IF NOT EXISTS menu (
    menuID INT PRIMARY KEY,
    menuName VARCHAR,
    category VARCHAR,
    price DECIMAL,
    menuImage INT,
    menuDescription VARCHAR,
    seasonalStart TIMESTAMP,
    seasonalEnd TIMESTAMP
);

DROP TABLE IF EXISTS staging_menu;
--two new columns in menu for availability with their start and end date (done)
--have cashier controller check if item is available (within those days)
--cashier controller has a private variable (check currdatetime) should have date set 
--use this to make sure that the private variable is within the start and end date for the item to appear
CREATE TEMP TABLE staging_menu (
    menuID INT,
    menuName VARCHAR,
    category VARCHAR,
    price DECIMAL,
    menuImage INT,
    menuDescription VARCHAR,
    seasonalStart TIMESTAMP,
    seasonalEnd TIMESTAMP
);
\copy staging_menu FROM 'Database/DatabaseSeed/menu.csv' CSV HEADER

INSERT INTO menu (menuID, menuName, category, price, menuImage, menuDescription, seasonalStart, seasonalEnd)
SELECT menuID, menuName, category, price, menuImage, menuDescription, seasonalStart, seasonalEnd FROM staging_menu
ON CONFLICT (menuID) DO UPDATE
SET menuName = EXCLUDED.menuName,
    category = EXCLUDED.category,
    price = EXCLUDED.price,
    menuImage = EXCLUDED.menuImage,
    menuDescription = EXCLUDED.menuDescription,
    seasonalStart = EXCLUDED.seasonalStart,
    seasonalEnd = EXCLUDED.seasonalEnd;

CREATE TABLE IF NOT EXISTS employee (
    employeeID INT PRIMARY KEY,
    employeeName VARCHAR,
    employeePosition VARCHAR,
    employeePasscode VARCHAR
);

DROP TABLE IF EXISTS staging_employee;

CREATE TEMP TABLE staging_employee (
    employeeID INT,
    employeeName VARCHAR,
    employeePosition VARCHAR,
    employeePasscode VARCHAR
);

\copy staging_employee FROM 'Database/DatabaseSeed/employee.csv' CSV HEADER

INSERT INTO employee (employeeID, employeeName, employeePosition, employeePasscode)
SELECT employeeID, employeeName, employeePosition, employeePasscode FROM staging_employee
ON CONFLICT (employeeID) DO UPDATE
SET employeeName = EXCLUDED.employeeName,
    employeePosition = EXCLUDED.employeePosition,
    employeePasscode = EXCLUDED.employeePasscode;

CREATE TABLE IF NOT EXISTS ordertest (
    orderID INT PRIMARY KEY,
    employeeID INT,
    FOREIGN KEY (employeeID) REFERENCES employee(employeeID),
    orderLocation VARCHAR,
    orderDate TIMESTAMP,
    orderTotal DECIMAL
);

DROP TABLE IF EXISTS staging_order;

CREATE TEMP TABLE staging_order (
    orderID INT,
    employeeID INT,
    orderLocation VARCHAR,
    orderDate TIMESTAMP,
    orderTotal DECIMAL
);

\copy staging_order FROM 'Database/DatabaseSeed/order.csv' CSV HEADER

INSERT INTO ordertest (orderID, employeeID, orderLocation, orderDate, orderTotal)
SELECT orderID, employeeID, orderLocation, orderDate, orderTotal FROM staging_order
ON CONFLICT (orderID) DO UPDATE
SET employeeID = EXCLUDED.employeeID,
    orderLocation = EXCLUDED.orderLocation,
    orderDate = EXCLUDED.orderDate,
    orderTotal = EXCLUDED.orderTotal;

--OrderItemID,MenuID,Price,QuantityPurchased,OrderID,Size
CREATE TABLE IF NOT EXISTS orderItem (
    orderItemID INT PRIMARY KEY, 
    menuID INT,
    FOREIGN KEY (menuID) REFERENCES menu(menuID),
    priceAtPurchase DECIMAL,
    quantityPurchased DECIMAL,
    orderID INT,
    FOREIGN KEY (orderID) REFERENCES ordertest(orderID),
    orderSize INT
);

DROP TABLE IF EXISTS staging_item;

CREATE TEMP TABLE staging_item(
    orderItemID INT,
    menuID INT,
    priceAtPurchase DECIMAL,
    quantityPurchased DECIMAL,
    orderID INT,
    orderSize INT
);

\copy staging_item FROM 'Database/DatabaseSeed/orderItem.csv' CSV HEADER

INSERT INTO orderItem (orderItemID, menuID, priceAtPurchase, quantityPurchased, orderID, orderSize)
SELECT orderItemID, menuID, priceAtPurchase, quantityPurchased, orderID, orderSize FROM staging_item
ON CONFLICT (orderItemID) DO UPDATE
SET menuID = EXCLUDED.menuID,
    priceAtPurchase = EXCLUDED.priceAtPurchase,
    quantityPurchased = EXCLUDED.quantityPurchased,
    orderID = EXCLUDED.orderID,
    orderSize = EXCLUDED.orderSize;

CREATE TABLE IF NOT EXISTS inventory (
    inventoryID INT PRIMARY KEY,
    inventoryName VARCHAR,
    quantityAvailable DECIMAL,
    restockPrice DECIMAL,
    addOnPrice DECIMAL,
    restockOrdered INT,
    unit VARCHAR,
    allergy VARCHAR,
    restockMin INT
);

DROP TABLE IF EXISTS staging_inventory;

CREATE TEMP TABLE staging_inventory (
    inventoryID INT,
    inventoryName VARCHAR,
    quantityAvailable DECIMAL,
    restockPrice DECIMAL,
    addOnPrice DECIMAL,
    restockOrdered INT,
    unit VARCHAR,
    allergy VARCHAR,
    restockMin INT
);

\copy staging_inventory FROM 'Database/DatabaseSeed/inventory.csv' CSV HEADER

INSERT INTO inventory (inventoryID, inventoryName, quantityAvailable, restockPrice, addOnPrice, restockOrdered, unit, allergy, restockMin)
SELECT inventoryID, inventoryName, quantityAvailable, restockPrice, addOnPrice, restockOrdered, unit, allergy, restockMin FROM staging_inventory
ON CONFLICT (inventoryID) DO UPDATE
SET inventoryName = EXCLUDED.inventoryName,
    quantityAvailable = EXCLUDED.quantityAvailable,
    restockPrice = EXCLUDED.restockPrice,
    addOnPrice = EXCLUDED.addOnPrice,
    restockOrdered = EXCLUDED.restockOrdered,
    unit = EXCLUDED.unit,
    allergy = EXCLUDED.allergy,
    restockMin = EXCLUDED.restockMin;

CREATE TABLE IF NOT EXISTS locationTable (
    locationID INT PRIMARY KEY,
    locationName VARCHAR,
    locationAddress VARCHAR,
    locationPhoneNum VARCHAR
);

DROP TABLE IF EXISTS staging_location;

CREATE TEMP TABLE staging_location (
    locationID INT,
    locationName VARCHAR,
    locationAddress VARCHAR,
    locationPhoneNum VARCHAR
);

\copy staging_location FROM 'Database/DatabaseSeed/location.csv' CSV HEADER

INSERT INTO locationTable (locationID, locationName, locationAddress, locationPhoneNum)
SELECT locationID, locationName, locationAddress, locationPhoneNum FROM staging_location
ON CONFLICT (locationID) DO UPDATE
SET locationName = EXCLUDED.locationName,
    locationAddress = EXCLUDED.locationAddress,
    locationPhoneNum = EXCLUDED.locationPhoneNum;

CREATE TABLE IF NOT EXISTS modification (
    modificationID INT PRIMARY KEY,
    inventoryID INT,
    FOREIGN KEY (inventoryID) REFERENCES inventory(inventoryID),
    orderItemID INT,
    FOREIGN KEY (orderItemID) REFERENCES orderItem(orderItemID),
    modificationQuantity DECIMAL,
    cost DECIMAL
);

DROP TABLE IF EXISTS staging_modification;

CREATE TEMP TABLE staging_modification (
    modificationID INT,
    inventoryID INT,
    orderItemID INT,
    modificationQuantity DECIMAL,
    cost DECIMAL
);

\copy staging_modification FROM 'Database/DatabaseSeed/modifications.csv' CSV HEADER

INSERT INTO modification (modificationID, inventoryID, orderItemID, modificationQuantity, cost)
SELECT modificationID, inventoryID, orderItemID, modificationQuantity, cost FROM staging_modification
ON CONFLICT (modificationID) DO UPDATE
SET inventoryID = EXCLUDED.inventoryID,
    orderItemID = EXCLUDED.orderItemID,
    modificationQuantity = EXCLUDED.modificationQuantity,
    cost = EXCLUDED.cost;

CREATE TABLE IF NOT EXISTS menuInfo (
    menuInfoID INT PRIMARY KEY,
    inventoryID INT,
    FOREIGN KEY (inventoryID) REFERENCES inventory(inventoryID),
    menuID INT,
    FOREIGN KEY (menuID) REFERENCES menu(menuID),
    menuInfoQuantity DECIMAL
);

DROP TABLE IF EXISTS staging_menuInfo;

CREATE TEMP TABLE staging_menuInfo (
    menuInfoID INT,
    inventoryID INT,
    menuID INT,
    menuInfoQuantity DECIMAL
);

\copy staging_menuInfo FROM 'Database/DatabaseSeed/menuInfo.csv' CSV HEADER

INSERT INTO menuInfo (menuInfoID, inventoryID, menuID, menuInfoQuantity)
SELECT menuInfoID, inventoryID, menuID, menuInfoQuantity FROM staging_menuInfo
ON CONFLICT (menuInfoID) DO UPDATE
SET inventoryID = EXCLUDED.inventoryID,
    menuID = EXCLUDED.menuID,
    menuInfoQuantity = EXCLUDED.menuInfoQuantity;

DROP TABLE IF EXISTS staging_menu;
DROP TABLE IF EXISTS staging_menuInfo;
DROP TABLE IF EXISTS staging_order;
DROP TABLE IF EXISTS staging_item;
DROP TABLE IF EXISTS staging_inventory;
DROP TABLE IF EXISTS staging_location;
DROP TABLE IF EXISTS staging_modification;
DROP TABLE IF EXISTS staging_employee;