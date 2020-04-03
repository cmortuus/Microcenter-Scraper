import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

fun main() {
    val jsonArray = JSONArray();
    val emailBody = StringBuilder()
    val fromEmail = "YOUR EMAIL REPLACE THIS"
    val password = "YOUR PASSWORD REPLACE THIS"
    val props = Properties()

    props["mail.smtp.host"] = "smtp.gmail.com"
    props["mail.smtp.port"] = "465"
    props["mail.smtp.ssl.enable"] = "true"
    props["mail.smtp.auth"] = "true"

    //create Authenticator object to pass in Session.getInstance argument
    // Get the Session object.// and pass username and password
    val session = Session.getInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(fromEmail, password)
        }
    }).also { it.debug = true }


    val url = "jdbc:mysql://localhost:3306/MicrocenterItems?characterEncoding=latin1&useConfigs=maxPerformance"
    val user = "microcenter"
    val sqlPassword = "YOUR MYSQL PASSWORD REPLACE THIS"
    Class.forName("com.mysql.jdbc.Driver")
    val connect: Connection = DriverManager.getConnection(url, user, sqlPassword)
//  Items in sent emails key == the cols and value == if it was in the latest scrape
    val emailedItems = HashMap<String, Boolean>()
//  The items that have been grabbed sorted by the name of the search and then the vals of the cols
    val items = HashMap<String, Array<Any>>()
//  Searches
    val searchesSQL = ArrayList<String>()

    val sent = connect.prepareStatement("SELECT `productName`, `openBoxPrice`, `percentDifference`, `store` FROM EmailsSent").executeQuery()
    while (sent.next()) {
        emailedItems["${sent.getString("productName")}, ${sent.getDouble("openBoxPrice")}, ${sent.getDouble("percentDifference")}, ${sent.getString("store")}"] = false
    }
    val searches = connect.prepareStatement("SELECT `productName`, `category`, `store`, `minPrice`, `maxPrice`, `percentDifference` FROM `Searches`").executeQuery()
    while (searches.next()) {
//      Create the sql stmts to get the items
        val sqlStmt = arrayListOf("SELECT `category`, `productName`, `url`, `normalPrice`, `openBoxPrice`, `percentDifference`, `store` FROM `Items` WHERE ")
        if (searches.getString(1) != null) sqlStmt.add("productName LIKE '%${searches.getString(1)}%' ")
        if (searches.getString(2) != null) sqlStmt.add("category LIKE '%${searches.getString(2)}%' ")
        if (searches.getString(3) != null) sqlStmt.add("store = '%${searches.getString(3)}%' ")
        if (searches.getString(4) != null) sqlStmt.add("openBoxPrice > ${searches.getDouble(4)} ")
        if (searches.getString(5) != null) sqlStmt.add("openBoxPrice < ${searches.getDouble(5)} ")
        if (searches.getString(6) != null) sqlStmt.add("percentDifference < '${searches.getDouble(6)}%' ")
        val sb = StringBuilder()
        for (i in 0 until sqlStmt.size) {
            when (i) {
                0 -> sb.append(sqlStmt[0])
                sqlStmt.size - 1 -> sb.append(sqlStmt[i] + ";")
                else -> sb.append(" ${sqlStmt[i]} AND ")
            }
        }
        searchesSQL.add(sb.toString())
    }
    for (search in searchesSQL) {
        val results = connect.prepareStatement(search).executeQuery()
        val sb = StringBuilder()
        while (results.next()) {
            val resultsVals = "${results.getString("productName")}, ${results.getDouble("openBoxPrice")}, ${results.getDouble("percentDifference")}, ${results.getString("store")}"
            val jsonObject = JSONObject()
//            jsonObject["img"] = mapOf("img" to "https://90a1c75758623581b3f8-5c119c3de181c9857fcb2784776b17ef.ssl.cf2.rackcdn.com/laptop-cat.jpg")
            jsonObject["name"] = results.getString(2)
            jsonObject["price"] = "$ ${results.getString(5)}"
            jsonObject["percent"] = "% ${results.getString(6).substring(0, 5)}"
            jsonObject["store"] = results.getString(7)
            jsonObject["url"] = results.getString(3)
            jsonArray.add(jsonObject)
            if (emailedItems[resultsVals] == null) {
                sb.append("  ${results.getString(1)}    ")
                sb.append("  ${results.getString(2)}    ")
                sb.append("  ${results.getDouble(4)}    ")
                sb.append("  ${results.getDouble(5)}    ")
                sb.append("  ${results.getDouble(6)}    ")
                sb.append("  ${results.getString(7)}    ")
                sb.append("  ${results.getString(3)}    ")
                sb.append("\n\n")
                val insertIntoEmailsSent = connect.prepareStatement("INSERT INTO EmailsSent (`productName`, `openBoxPrice`, `percentDifference`, `store`) VALUES (?, ?, ?, ?);")
                insertIntoEmailsSent.setString(1, results.getString(2))
                insertIntoEmailsSent.setDouble(2, results.getDouble(5))
                insertIntoEmailsSent.setDouble(3, results.getDouble(6))
                insertIntoEmailsSent.setString(4, results.getString(7))
                insertIntoEmailsSent.execute()
                emailBody.append(sb.toString())
            } else {
                println("Not sending item because it has already been sent")
                emailedItems[resultsVals] = true
            }
        }
    }
    for (itemAndBool in emailedItems.entries) if (!itemAndBool.value) deleteItem(itemAndBool.key, connect)
    if (emailBody.toString() != "") sendEmail(session, "calebmorton98@gmail.com", "Microcenter Items", emailBody.toString(), fromEmail)
    val finalJson = JSONObject()
    finalJson["project"] = "microcenter"
    finalJson["data"] = jsonArray
    if (jsonArray.size > 0) apiRequest(finalJson.toJSONString().also(::println))
}

fun deleteItem(sqlString: String, connect: Connection) {
    val sqlArray = sqlString.split(", ")
    val deleteStmt = connect.prepareStatement("DELETE FROM EmailsSent WHERE `productName` = ? AND `openBoxPrice` = ? AND `percentDifference` = ? AND `store` = ?;")
    deleteStmt.setString(1, sqlArray[0])
    deleteStmt.setDouble(2, sqlArray[1].toDouble())
    deleteStmt.setDouble(3, sqlArray[2].toDouble())
    deleteStmt.setString(4, sqlArray[3])
    deleteStmt.execute()
    println("Deleting line")
}

/**
 * Utility method to send simple HTML email
 * @param session
 * @param toEmail
 * @param subject
 * @param body
 * @param from
 */
fun sendEmail(session: Session?, toEmail: String?, subject: String?, body: String?, from: String) {
    try {
        val msg = MimeMessage(session)
        //set message headers
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
        msg.addHeader("format", "flowed")
        msg.addHeader("Content-Transfer-Encoding", "8bit")
        msg.setFrom(InternetAddress(from, "NoReply-JD"))
        msg.replyTo = InternetAddress.parse(from, false)
        msg.setSubject(subject, "UTF-8")
        msg.setText(body, "UTF-8")
        msg.sentDate = Date()
        msg.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail, false)
        )
        println("Message is ready")
        Transport.send(msg)
        println("EMail Sent Successfully!!")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun apiRequest(jsonString: String) {
    val url = URL("YOUR URL REPLACE THIS")
    val con: HttpURLConnection = url.openConnection() as HttpURLConnection
    con.doOutput = true
    con.requestMethod = "POST"
    con.setRequestProperty("Content-Type", "application/json; utf-8")
    con.setRequestProperty("Accept", "application/json")
    con.outputStream.use { os ->
        val input = jsonString.toByteArray()
        os.write(input, 0, input.size)
    }
    BufferedReader(InputStreamReader(con.inputStream, "utf-8")).use { br ->
        val response = StringBuilder()
        var responseLine: String?
        while (br.readLine().also { responseLine = it } != null) {
            response.append(responseLine!!.trim { it <= ' ' })
        }
        println(response.toString())
    }
}