import java.lang.StringBuilder
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


fun main() {
    val fromEmail = "YourEmail"
    val password = "YourPassword"
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

    val url = "jdbc:mysql://localhost:3306/MicrocenterItems?useSSL=false"
    val user = "microcenter"
    val sqlPassword = "YourPassword"
    Class.forName("com.mysql.jdbc.Driver")
    val connect: Connection = DriverManager.getConnection(url, user, sqlPassword)
    val thingsToSend = HashMap<String, ArrayList<String>>()
    val searches = connect.prepareStatement("SELECT `productName`, `category`, `store`, `minPrice`, `maxPrice`, `percentDifference`, `email`, `name` FROM `Searches`").executeQuery()
    val checkEmailsSent = connect.prepareStatement("SELECT `productName`, `openBoxPrice`, `percentDifference`, `store`, `name` FROM EmailsSent WHERE name = ?;")
    val searchesByName = HashMap<String, ArrayList<Array<Any>>>()
    while (searches.next()) {
        checkEmailsSent.setString(1, searches.getString(8))
        val results = checkEmailsSent.executeQuery()
        while (results.next()) {
            if (searchesByName[results.getString(5)] == null)
                searchesByName[results.getString(5)] = ArrayList()
            searchesByName[results.getString(5)]?.add(arrayOf(results.getString(1), results.getDouble(2), results.getDouble(3), results.getString(4)))
        }
        println("Searches by name = $searchesByName")
        val sqlStmt = arrayListOf("SELECT `category`, `productName`, `url`, `normalPrice`, `openBoxPrice`, `percentDifference`, `store` FROM `Items` WHERE ")
        if (searches.getString(1) != null) sqlStmt.add("productName LIKE '%${searches.getString(1)}%' ")
        if (searches.getString(2) != null) sqlStmt.add("category LIKE '%${searches.getString(2)}%' ")
        if (searches.getString(3) != null) sqlStmt.add("store = '%${searches.getString(3)}%' ")
        if (searches.getString(4) != null) sqlStmt.add("openBoxPrice > ${searches.getDouble(4)} ")
        if (searches.getString(4) != null) sqlStmt.add("openBoxPrice < ${searches.getDouble(5)} ")
        if (searches.getString(5) != null) sqlStmt.add("percentDifference < '${searches.getDouble(6)}%' ")
        val sb = StringBuilder()
        for (i in 0 until sqlStmt.size) {
            when (i) {
                0 -> sb.append(sqlStmt[0])
                sqlStmt.size - 1 -> sb.append(sqlStmt[i] + ";")
                else -> sb.append(" ${sqlStmt[i]} AND ")
            }
            println(sb.toString())
        }
        if (thingsToSend["${searches.getString(7)},${searches.getString(8)}"] == null)
            thingsToSend["${searches.getString(7)},${searches.getString(8)}"] = ArrayList()
        thingsToSend["${searches.getString(7)},${searches.getString(8)}"] = arrayListOf(sb.toString())
        println(thingsToSend)
    }
    for (emailAndArray in thingsToSend.entries) {
        val sb = StringBuilder()
        for (sqlStmt in emailAndArray.value) {
            val results = connect.prepareStatement(sqlStmt).executeQuery()
            while (results.next()) {
                val checkArray = arrayOf(results.getString(2), results.getDouble(5), results.getDouble(6), results.getString(7))
                var existsAlready = false
                println(emailAndArray.key.split(",")[1])
                searchesByName[emailAndArray.key.split(",")[1]]?.let {
                    it.forEachIndexed { i, oldItems ->
                        if (checkArray.contentEquals(oldItems)) {
                            existsAlready = true
                            println("Not sending item because it has already been sent")
                            searchesByName[emailAndArray.key.split(",")[1]]?.set(i, arrayOf())
                        }
                    }
                }
                if (!existsAlready) {
                    sb.append("Category = ${results.getString(1)}    ")
                    sb.append("productName = ${results.getString(2)}    ")
                    sb.append("normalPrice = ${results.getDouble(4)}    ")
                    sb.append("openBoxPrice = ${results.getDouble(5)}    ")
                    sb.append("percentDifference = ${results.getDouble(6)}    ")
                    sb.append("store = ${results.getString(7)}    ")
                    sb.append("url = ${results.getString(3)}    ")
                    sb.append("\n")
                    val insertIntoEmailsSent = connect.prepareStatement("INSERT INTO EmailsSent (`productName`, `openBoxPrice`, `percentDifference`, `store`, `name`) VALUES (?, ?, ?, ?, ?);")
                    insertIntoEmailsSent.setString(1, results.getString(2))
                    insertIntoEmailsSent.setDouble(2, results.getDouble(5))
                    insertIntoEmailsSent.setDouble(3, results.getDouble(6))
                    insertIntoEmailsSent.setString(4, results.getString(7))
                    insertIntoEmailsSent.setString(5, emailAndArray.key.split(",")[1])
                    insertIntoEmailsSent.execute()
                }
            }
        }
        for (search in searchesByName.values) {
            for (result in search) {
                if (!result.contentEquals(arrayOf())) {
                    val deleteStmt = connect.prepareStatement("DELETE FROM EmailsSent WHERE `productName` = ? AND `openBoxPrice` = ? AND `percentDifference` = ? AND `store` = ?;")
                    deleteStmt.setString(1, result[0] as String)
                    deleteStmt.setDouble(2, result[1] as Double)
                    deleteStmt.setDouble(3, result[2] as Double)
                    deleteStmt.setString(4, result[3] as String)
                    deleteStmt.execute()
                    println("Deleting line")
                }
            }
        }
        val body = sb.toString().also(::println)
        val toEmail = emailAndArray.key.split(",")[0].also(::println)
        val greeting = "Hey Caleb,\n These are the deals we found:\n\n"
        if (body != "") sendEmail(session, toEmail, "MicrocenterItems", "$greeting$body", fromEmail)
    }
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