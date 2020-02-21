import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.support.ui.Select
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun main() {
    AutoUpdateApp("Scraper")
    arrayOf("MD - Parkville", "MD - Rockville", "VA - Fairfax").forEach { store -> Thread { Store(store) }.start() }
}

class Store(private val store: String) {
    private var driver: WebDriver
    private var options: FirefoxOptions
    private val connect: Connection

    init {
        Thread.sleep(30 * 1000)
        val url = "jdbc:mysql://localhost:3306/MicrocenterItems?characterEncoding=latin1&useConfigs=maxPerformance"
        val user = "microcenter"
        val password = ""
        Class.forName("com.mysql.jdbc.Driver")
        connect = DriverManager.getConnection(url, user, password)
        connect.prepareStatement("DELETE FROM Items WHERE time <= NOW() - INTERVAL 10 MINUTE;").execute()
        try {
            options = FirefoxOptions().addPreference("permissions.default.image", 1)
            driver = FirefoxDriver(options)
            driver.get("https://www.microcenter.com/site/products/open-box.aspx")
            changeStore(driver)
            for (category in driver.findElements(By.className("ovalbutton"))) {
                scrapeCategory(category)
            }
        } catch (e: WebDriverException) {
            println(e.printStackTrace())
            exitProcess(1)
        }
    }

    /**
     * On the open box site on microcenter there are 8 categoriesThis scrapes each of them
     * @param category    A Selenium WebElement containing to a single category
     */
    private fun scrapeCategory(category: WebElement) {
        try {
            val driver: WebDriver = FirefoxDriver(options)
            driver.get(category.getAttribute("href"))
            changeStore(driver)
            while (true) {
                driver.findElement(By.className("pages")).findElements(By.tagName("li")).last().also {
                    if (it.text != ">") {
                        driver.close()
                        return
                    } else {
                        driver.findElements(By.className("product_wrapper")).forEachIndexed { i, item -> scrapeItem(item, category.text.substring(5), i) }
                    }
                }.click()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Scrapes a single item
     * @param item          A Selenium WebElement containing just an item
     * @param category      A string containing the name of the catagory
     * @param elementNum    The iterator showing what element on the page it is
     */
    private fun scrapeItem(item: WebElement, category: String, elementNum: Int) {
        try {
            val dataLink = item.findElement(By.id("hypProductH2_$elementNum"))
            val name = dataLink.getAttribute("data-name")
            val normalPrice = dataLink.getAttribute("data-price").toDouble()
            val url = dataLink.getAttribute("href")
            val openBoxPrice =
                item.findElement(By.className("price-label")).text.replace("[^0-9]".toRegex(), "").toDouble() / 100
            val percentDifference = (openBoxPrice / normalPrice) * 100
            arrayOf(category, name, url, normalPrice, openBoxPrice, percentDifference, store).forEach { print("$it  ") }
            println()
            insertIntoTable(category, name, url, normalPrice, openBoxPrice, percentDifference, store)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * By default it is going to be on the wrong store. This corrects that.
     * @param driver    A selenium web driver
     */
    private fun changeStore(driver: WebDriver) {
        try {
            driver.findElement(By.className("close")).click()
            driver.findElement(By.id("Change-Store")).click()
            Select(driver.findElement(By.xpath(".//*[@name='storeID']"))).selectByVisibleText(store)
            driver.findElement(By.xpath(".//*[@value='Change Store']")).click()
        } catch (e: Exception) {
            changeStore(driver)
        }
    }

    /**
     * @param allofthem All the items that are passed in to send to the database
     */
    private fun insertIntoTable(
        category: String,
        name: String,
        url: String,
        normalPrice: Double,
        openBoxPrice: Double,
        percentDifference: Double,
        store: String
    ) {
//         PreparedStatements can use variables and are more efficient
        val preparedStatement = connect.prepareStatement(
            "INSERT INTO `Items` (`id`, `time`, `category`, `productName`, `url`, `normalPrice`, " +
                    "`openBoxPrice`, `percentDifference`, `store`) VALUES (NULL, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?);"
        )
        // Parameters start with 1
        preparedStatement.setString(1, category)
        preparedStatement.setString(2, name)
        preparedStatement.setString(3, url)
        preparedStatement.setDouble(4, normalPrice)
        preparedStatement.setDouble(5, openBoxPrice)
        preparedStatement.setDouble(6, percentDifference)
        preparedStatement.setString(7, store)
        preparedStatement.executeUpdate()
    }
}

/**
 * Modifies the string class to add a run command func
 */
fun String.runCommand(workingDir: File?) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}

class AutoUpdateApp(private val programName: String) {
    init {
        Thread {
            val jsonString = "{\"programCheckIn\" : \"$programName\"}"
            while (true) {
                println("sending")
                apiRequest(jsonString)
                Thread.sleep(6 * 1000)
            }
        }.start()
    }

    private fun apiRequest(jsonString: String) {
        val url = URL("http://youcantblock.me/")
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
}