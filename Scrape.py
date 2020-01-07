import time
from selenium import webdriver
from selenium.webdriver.support.ui import Select
import mysql.connector
import re
from multiprocessing import Process


def store(store):
    connection = mysql.connector.connect(user='microcenter',
                                         password='Wehf2k1AjVNVD6ih1IkjSuOxTZGKCahtWtsaCCYx4',
                                         host='127.0.0.1',
                                         database='MicrocenterItems')
    cursor = connection.cursor(prepared=True)
    cursor.execute('DELETE FROM Items WHERE time >= NOW() - INTERVAL 4 HOUR AND time <= NOW() - INTERVAL 3 HOUR;', [])
    connection.commit()

    profile = webdriver.FirefoxProfile()
    profile.set_preference("javascript.enabled", False)
    profile.set_preference("permissions.default.image", 2)
    driver = webdriver.Firefox(firefox_profile=profile)
    driver.get("https://www.microcenter.com/site/products/open-box.aspx")
    for catagory in driver.find_elements_by_class_name("ovalbutton"):
        scrapeCatagory(catagory, store, cursor, connection)
    driver.close()


def scrapeCatagory(catagory, store, cursor, connection):
    profile = webdriver.FirefoxProfile()
    profile.set_preference("javascript.enabled", False)
    profile.set_preference("permissions.default.image", 2)
    driver = webdriver.Firefox(firefox_profile=profile)
    driver.get(catagory.get_attribute('href'))
    changeStore(driver, store)
    while True:
        for i, item in enumerate(driver.find_elements_by_class_name("product_wrapper")):
            scrapeItem(item, i, catagory.text[5:], store, cursor, connection)
        pages = driver.find_element_by_class_name("pages").find_elements_by_tag_name("li")
        if pages[len(pages) - 1].text == '>':
            pages[len(pages) - 1].click()
        else:
            driver.close()
            return


def scrapeItem(item, elementNum, catagory, store, cursor, connection):
    try:
        dataLink = item.find_element_by_id("hypProductH2_" + str(elementNum))
        name = dataLink.get_attribute("data-name")
        url = dataLink.get_attribute("href")
        normalPrice = float(dataLink.get_attribute("data-price"))
        openBoxPrice = item.find_element_by_class_name("price-label").text
        openBoxPrice = float(int(re.sub('[^0-9]', "", openBoxPrice)[0:len(openBoxPrice)]) / 100)
    except Exception:
        return
    values = [catagory, name, url, normalPrice, openBoxPrice, float((openBoxPrice / normalPrice) * 100), store]
    print(values)
    insertIntoTable(values, cursor, connection)


def insertIntoTable(values, cursor, connection):
    # if (searchTable(values)):
    sql = 'INSERT INTO `Items` (`id`, `time`, `catagory`, `productName`, `url`, `normalPrice`, `openBoxPrice`, `percentDifference`, `store`) ' \
          'VALUES (NULL, CURRENT_TIMESTAMP, %s, %s, %s, %s, %s, %s, %s);'
    cursor.execute(sql, values)
    connection.commit()


def searchTable(values, cursor, connection):
    sql = 'SELECT * FROM Items WHERE productName = %s;'
    cursor.execute(sql, [values[1]])
    result = cursor.fetchall()
    connection.commit()
    if len(result) > 0:
        for num, row in enumerate(result, start=0):
            if values[3] == row[4] and values[4] == row[5]:
                return False
        sql = 'UPDATE Items SET normalPrice = ?, openBoxPrice = ? WHERE productName = ?'
        cursor.execute(sql, [values[3], values[4], values[1]])
        return False
    else:
        return True


def changeStore(driver, store):
    try:
        driver.find_element_by_class_name("close").click()
        driver.find_element_by_id("Change-Store").click()
        Select(driver.find_element_by_xpath(".//*[@name='storeID']")).select_by_visible_text(store)
        driver.find_element_by_xpath(".//*[@value='Change Store']").click()
    except Exception:
        changeStore(driver, store)


stores = ['MD - Rockville', 'MD - Parkville', 'VA - Fairfax']
while True:
    rock = Process(target=store, args=(stores[0],))
    rock.start()
    park = Process(target=store, args=(stores[1],))
    park.start()
    fair = Process(target=store, args=(stores[2],))
    fair.start()
    rock.join()
    park.join()
    fair.join()
    time.sleep(60 * 60 * 3)