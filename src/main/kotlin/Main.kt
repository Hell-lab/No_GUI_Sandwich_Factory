import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*


const val DB_DRIVER = "oracle.jdbc.driver.OracleDriver"
const val DB_URL = "jdbc:oracle:thin:@85.214.227.33:1521:cdb1?currentSchema=softeng"
const val DB_USER = "softeng"
const val DB_PASSWORD = "admin"
var dbConnection: Connection? = null
const val ORDER_TABLE = "ORDERS"
const val ORDER_ID = "ORDER_ID"
const val ORDER_ENUM = "ORDER_STATE"
const val CUSTOMER_TABLE = "CUSTOMERS"
const val CUSTOMER_ID = "CUSTOMER_ID"
const val CUSTOMER_FIRST_NAME = "FIRST_NAME"
const val CUSTOMER_LAST_NAME = "LAST_NAME"
const val CUSTOMER_ADDRESS = "ADDRESS"
const val CUSTOMER_MAIL_ADDRESS = "MAIL_ADDRESS"
const val CUSTOMER_PHONE_NUMBER = "PHONE_NUMBER"
const val PAYMENT_TABLE = "PAYMENTS"
const val PAYMENT_ID = "PAYMENT_ID"
const val PAYMENT_METHOD = "PAYMENT_METHOD"
const val DELIVERY_TABLE = "DELIVERIES"
const val DELIVERY_ID = "DELIVERY_ID"
const val DELIVERY_ADDRESS = "DELIVERY_ADDRESS"
const val DELIVERY_TYPE = "DELIVERY_TYPE"
const val DELIVERY_DESCRIPTION = "DELIVERY_DESCRIPTION"
const val PRODUCT_TABLE = "PRODUCTS"
const val PRODUCT_ID = "PRODUCT_ID"
const val PRODUCT_NAME = "PRODUCT_NAME"
const val PRODUCT_WEIGHT = "PRODUCT_WEIGHT"
const val ORDER_TO_PRODUCT_TABLE = "ORDER_TO_PRODUCT"
const val ORDER_TO_PRODUCT_ID = "ORDER_TO_PRODUCT_ID"
var customerID = 0;
var orderID = 0;
var paymentID = 0;
var deliveryID = 0;
var orderToProductID = 0;
var paymentAttempts = 1;
var paymentSuccess = false

fun main() {
	openConnectionToDatabase()
	var quit : String
	do {
		deleteTables()
		createTables()
		addProducts()
		var input: String
		var amount = 0
		val products = mutableListOf<String>()
		println("Dear Customer, welcome in our sandwich shop")
		do {
			println("Please choose a sandwich from the following list")
			products.add(askCustomerForOrder())
			println("Do you want to order more Sandwiches? (=yes)")
			input = readLine().toString()
			amount++
		} while (input == "yes")
		val ids = askCustomerForData(products)
		val actualPayment = handlePayment()
		if (paymentSuccess) {
			addPayment(actualPayment)
			val orderID = addOrder(ids.customerId, actualPayment.id, ids.deliveryId)
			connectOrderToProduct(products, orderID )
		}
		print("Do you want to quit the program? (=yes) ")
		quit = readLine().toString()
	} while (quit != "yes")

	closeConnectionToDatabase()
}

fun connectOrderToProduct(productIDs: List<String>, orderID: Int) {
	productIDs.forEach { product ->
		dbConnection!!.createStatement().use { statement ->
			statement.execute(" INSERT INTO $ORDER_TO_PRODUCT_TABLE($ORDER_TO_PRODUCT_ID, $ORDER_ID, $PRODUCT_ID) VALUES($orderToProductID, $orderID, $product)")
			orderToProductID++
		}
	}
}

fun addOrder(customerID: Int, paymentID: Int, deliveryID: Int) : Int {
	val actualOrderID = orderID
	try {
		dbConnection!!.createStatement().use { statement ->
			statement.execute(" INSERT INTO $ORDER_TABLE ($ORDER_ID, $ORDER_ENUM, $CUSTOMER_ID, $DELIVERY_ID, $PAYMENT_ID) VALUES($orderID,'CREATE', $customerID, $deliveryID, $paymentID)")
			orderID++
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}
	return actualOrderID
}

fun addPayment(actualPaymentMethod: PaymentMethod) {
	try	{
		dbConnection!!.createStatement().use { statement ->
			statement.execute(" INSERT INTO $PAYMENT_TABLE ($PAYMENT_ID, $PAYMENT_METHOD) VALUES ($paymentID, '${actualPaymentMethod.paymentMethod}')")
			paymentID++
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}

}

fun handlePayment(): PaymentMethod {
	println("Please choose one of the following payment methods: ")
	println("1: Paypal")
	println("2: Credit Card")
	println("3. Bank account")
	var paymentMethod = readLine().toString()
	when (paymentMethod) {
		"1" -> {
			println("You will be redirected to Paypal...")
			checkPayment(paymentMethod)
			paymentMethod = "PayPal"
		}
		"2" -> {
			print("Please enter your credit card number: ")
			val cardNumber = readLine().toString()
			print("Please enter the expiration date: ")
			val expirationDate = readLine().toString()
			print("Please enter your security code: ")
			val securityCode = readLine().toString()
			paymentMethod = "Credit Card"
			checkPayment(paymentMethod, cardNumber, expirationDate, securityCode)
		}
		else -> {
			print("Please enter your IBAN: ")
			val iban = readLine().toString()
			print("Please enter your BIC: ")
			val bic = readLine().toString()
			print("Please enter the name of the account holder: ")
			val accountHolder = readLine().toString()
			paymentMethod = "Bank account"
			checkPayment(paymentMethod, iban, bic, accountHolder)
		}
	}
	return PaymentMethod(paymentID, paymentMethod)
}

fun checkPayment(paymentMethod: String, vararg details: String) {
	if (paymentAttempts >= 3) {
		println("To many attempts")
	} else {
		when (paymentMethod) {
			"PayPal" -> {
				paymentSuccess = true
				paymentAttempts = 1
			}
			"Credit Card" -> {
				if (details[0].length > 16) {
					println("wrong card number")
					paymentSuccess = false
					paymentAttempts++;
					handlePayment()
				}
				val cal = Calendar.getInstance()
				val month = cal.get(Calendar.MONTH)
				val year = cal.get(Calendar.YEAR)
				if (details[1].substring(0,1).toInt() < month || details[1].substring(2,3).toInt() < year) {
					println("card is already expired")
					paymentSuccess = false
					paymentAttempts++;
					handlePayment()
				}
				if (details[2].length > 3) {
					println("security code is not valid")
					paymentSuccess = false
					paymentAttempts++;
					handlePayment()
				}
				paymentSuccess = true
				paymentAttempts = 1
			}
			else -> {
				paymentSuccess = true
				paymentAttempts = 1
			}
		}
	}
}

fun askCustomerForOrder() : String {
	var i = 1
	try {
		dbConnection!!.createStatement().use { statement ->
			val rs = statement.executeQuery(" SELECT $PRODUCT_NAME FROM  $PRODUCT_TABLE ")
			while (rs.next()) {
				println("$i: ${rs.getString("$PRODUCT_NAME")} ")
				i++
			}
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}
	return readLine().toString()
}

fun askCustomerForData(productIDs: List<String>) : ID{
	print("Please enter your first name: ")
	val firstName = readLine()
	print("Please enter your last name: ")
	val lastName = readLine()
	print("Please enter your address: ")
	val address = readLine()
	print("Pleaser enter your mail address: ")
	val mailAddress = readLine()
	print("Please enter your phone number: ")
	val phoneUmber = readLine()
	val actualCustomerID = customerID
	addUser(
		firstName!!.toString(),
		lastName!!.toString(),
		address!!.toString(),
		mailAddress!!.toString(),
		phoneUmber!!.toString()
	)
	print("Please enter your desired delivery address: ")
	val deliveryAddress = readLine()
	println("Please choose one of the following delivery types: ")
	var deliveryType = displayDeliveryTypes(productIDs)
	print("Please add further comments for delivery details: ")
	val deliveryDescription = readLine()
	deliveryType = when(deliveryType) {
		"1" -> "Drone"
		"2" -> "Wormhole"
		else -> "Car"
	}
	val actualDeliveryID = deliveryID
	try {
		dbConnection!!.createStatement().use { statement ->
			statement.execute("INSERT INTO $DELIVERY_TABLE ($DELIVERY_ID, $DELIVERY_ADDRESS, $DELIVERY_TYPE, $DELIVERY_DESCRIPTION) VALUES ( $deliveryID, '$deliveryAddress', '$deliveryType', '$deliveryDescription')")
			deliveryID++
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}
	return ID(actualCustomerID, actualDeliveryID)
}

fun displayDeliveryTypes(productIDs: List<String>) : String {
	var rs : ResultSet
	var totalWeight = 0
	productIDs.forEach { id ->
		dbConnection!!.createStatement().use { statement ->
			rs = statement.executeQuery(" SELECT $PRODUCT_WEIGHT FROM $PRODUCT_TABLE WHERE $PRODUCT_ID = $id")
			while (rs.next()) {
				totalWeight +=  rs.getString("$PRODUCT_WEIGHT").toInt()
			}
		}
	}
	var i = 1;
	if (totalWeight < 500 ) {
		println("$i: Drone")
		i++;
	}
	println("$i: Wormhole")
	i++
	println("$i: Car")
	return readLine().toString()
}

fun addProducts() {
	try {
		dbConnection!!.createStatement().use { statement ->
			statement.execute(" INSERT INTO $PRODUCT_TABLE ($PRODUCT_ID, $PRODUCT_NAME, $PRODUCT_WEIGHT) VALUES(1,'Cheese-Sandwich', 250)")
			statement.execute(" INSERT INTO $PRODUCT_TABLE ($PRODUCT_ID, $PRODUCT_NAME, $PRODUCT_WEIGHT) VALUES(2,'Ham-Sandwich', 300) ")
			statement.execute(" INSERT INTO $PRODUCT_TABLE ($PRODUCT_ID, $PRODUCT_NAME, $PRODUCT_WEIGHT) VALUES(3,'Potato-Sandwich', 250) ")
			statement.execute(" INSERT INTO $PRODUCT_TABLE ($PRODUCT_ID, $PRODUCT_NAME, $PRODUCT_WEIGHT) VALUES(4,'Veggie-Sandwich', 150) ")
			statement.execute(" INSERT INTO $PRODUCT_TABLE ($PRODUCT_ID, $PRODUCT_NAME, $PRODUCT_WEIGHT) VALUES(5,'Club-Sandwich', 400) ")
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}
}


fun addUser(firstName: String, lastName: String, address: String, mailAddress: String, phoneNumber: String) {
	try {
		dbConnection!!.createStatement().use { statement ->
			statement.execute("INSERT INTO $CUSTOMER_TABLE ($CUSTOMER_ID, $CUSTOMER_FIRST_NAME, $CUSTOMER_LAST_NAME, $CUSTOMER_ADDRESS, $CUSTOMER_MAIL_ADDRESS , $CUSTOMER_PHONE_NUMBER ) VALUES ( $customerID, '$firstName', '$lastName', '$address', '$mailAddress', '$phoneNumber')")
			customerID++
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}
}

fun openConnectionToDatabase() {
	try {
		Class.forName(DB_DRIVER)
	} catch (e: ClassNotFoundException) {
		System.err.print("ClassNotFoundException: ")
		System.err.println(e.message)
		println("\n >>> Please check your CLASSPATH variable <<<\n")
		return
	}
	try {
		dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
		println("Connected to database")
	} catch (e: SQLException) {
		e.printStackTrace()
	}
}

fun closeConnectionToDatabase() {
	if (dbConnection == null) {
		throw IllegalStateException("Connection was already closed");
	}
	try {
		dbConnection?.close()
		println("Disconnected from database")
	} catch (e: SQLException) {
		throw RuntimeException("Could not close database", e)
	}
}

fun deleteTables() {
	try {
		dbConnection!!.createStatement().use { statement ->
			statement.execute(("DROP TABLE $ORDER_TO_PRODUCT_TABLE"))
			statement.execute("DROP TABLE $ORDER_TABLE")
			statement.execute("DROP TABLE $CUSTOMER_TABLE")
			statement.execute("DROP TABLE $PAYMENT_TABLE")
			statement.execute("DROP TABLE $DELIVERY_TABLE")
			statement.execute("DROP TABLE $PRODUCT_TABLE")
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}
}

fun createTables() {
	try {
		dbConnection!!.createStatement().use { statement ->
			statement.execute(
				" CREATE TABLE $DELIVERY_TABLE ($DELIVERY_ID INTEGER not NULL, $DELIVERY_ADDRESS VARCHAR(60), $DELIVERY_TYPE VARCHAR(30), $DELIVERY_DESCRIPTION VARCHAR(60), PRIMARY KEY ( $DELIVERY_ID ))"
			)
			statement.execute(
				" CREATE TABLE $PAYMENT_TABLE ($PAYMENT_ID INTEGER not NULL, $PAYMENT_METHOD VARCHAR(30), PRIMARY KEY ( $PAYMENT_ID ))"
			)
			statement.execute(
				" CREATE TABLE $CUSTOMER_TABLE ( $CUSTOMER_ID INTEGER not NULL, $CUSTOMER_FIRST_NAME VARCHAR(30), $CUSTOMER_LAST_NAME VARCHAR(30), $CUSTOMER_ADDRESS VARCHAR(60), $CUSTOMER_MAIL_ADDRESS  VARCHAR(30), $CUSTOMER_PHONE_NUMBER  VARCHAR(15), PRIMARY KEY ( $CUSTOMER_ID ))"
			)
			statement.execute(
				" CREATE TABLE $ORDER_TABLE ( $ORDER_ID INTEGER not NULL, $ORDER_ENUM VARCHAR(30), $CUSTOMER_ID INTEGER not NULL, $DELIVERY_ID INTEGER not NULL, $PAYMENT_ID INTEGER not NULL,  PRIMARY KEY ( $ORDER_ID ), FOREIGN KEY ( $CUSTOMER_ID ) REFERENCES $CUSTOMER_TABLE($CUSTOMER_ID), FOREIGN KEY ( $DELIVERY_ID) REFERENCES $DELIVERY_TABLE($DELIVERY_ID), FOREIGN KEY ( $PAYMENT_ID ) REFERENCES $PAYMENT_TABLE($PAYMENT_ID))"
			)
			statement.execute(" CREATE TABLE $PRODUCT_TABLE($PRODUCT_ID INTEGER not NULL, $PRODUCT_NAME VARCHAR(30), $PRODUCT_WEIGHT INTEGER, PRIMARY KEY ($PRODUCT_ID))")
			statement.execute(" CREATE TABLE $ORDER_TO_PRODUCT_TABLE($ORDER_TO_PRODUCT_ID INTEGER not NULL, $ORDER_ID Integer not NULL, $PRODUCT_ID Integer not NULL, PRIMARY KEY ($ORDER_TO_PRODUCT_ID), FOREIGN KEY ($ORDER_ID) REFERENCES $ORDER_TABLE($ORDER_ID), FOREIGN KEY (PRODUCT_ID) REFERENCES $PRODUCT_TABLE($PRODUCT_ID))")
		}
	} catch (e: SQLException) {
		e.printStackTrace()
	}
}

data class PaymentMethod(val id: Int, val paymentMethod: String)

data class ID(val customerId: Int, val deliveryId: Int)
