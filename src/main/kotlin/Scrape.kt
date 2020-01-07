import org.openqa.selenium.*
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.support.ui.Select
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun main() {
    while (true) {
        arrayOf("MD - Parkville", "MD - Rockville", "VA - Fairfax").forEach { store -> Thread { Store(store) }.start() }
        Thread.sleep(1000 * 60 * 60 * 3)
        "killall firefox".runCommand(null)
    }
}

class Store(private val store: String) {
    private var driver: WebDriver
    private var options: FirefoxOptions
    private val connect: Connection

    init {
        val url = "jdbc:mysql://localhost:3306/MicrocenterItems?useSSL=false"
        val user = "microcenter"
        val password = "Wehf2k1AjVNVD6ih1IkjSuOxTZGKCahtWtsaCCYx4"
        Class.forName("com.mysql.jdbc.Driver")
        connect = DriverManager.getConnection(url, user, password)
        connect.prepareStatement("DELETE FROM Items WHERE time >= NOW() - INTERVAL 4 HOUR AND time <= NOW() - INTERVAL 2 HOUR;").execute()
        try {
            options = FirefoxOptions().addPreference("permissions.default.image", 2)
            driver = FirefoxDriver(options)
            driver.get("https://www.microcenter.com/site/products/open-box.aspx")
            for (catagory in driver.findElements(By.className("ovalbutton"))) {
                scrapeCategory(catagory)
            }
        } catch (e: WebDriverException) {
            println(e.printStackTrace())
            exitProcess(1)
        }
    }

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

    private fun scrapeItem(item: WebElement, category: String, elementNum: Int) {
        try {
            val dataLink = item.findElement(By.id("hypProductH2_$elementNum"))
            val name = dataLink.getAttribute("data-name")
            val normalPrice = dataLink.getAttribute("data-price").toDouble()
            val url = dataLink.getAttribute("href")
            val openBoxPrice = item.findElement(By.className("price-label")).text.replace("[^0-9]".toRegex(), "").toDouble() / 100
            val percentDifference = (openBoxPrice / normalPrice) * 100
            arrayOf(category, name, url, normalPrice, openBoxPrice, percentDifference, store).forEach { print("$it  ") }
            println()
            insertIntoTable(category, name, url, normalPrice, openBoxPrice, percentDifference, store)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    private fun insertIntoTable(category: String, name: String, url: String, normalPrice: Double, openBoxPrice: Double, percentDifference: Double, store: String) {
//         PreparedStatements can use variables and are more efficient
        val preparedStatement = connect.prepareStatement("INSERT INTO `Items` (`id`, `time`, `catagory`, `productName`, `url`, `normalPrice`, " +
                "`openBoxPrice`, `percentDifference`, `store`) VALUES (NULL, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?);")
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

fun String.runCommand(workingDir: File?) {
    ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(60, TimeUnit.MINUTES)
}