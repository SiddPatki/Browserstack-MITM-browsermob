// Sample test in Java to run Automate session.

import com.browserstack.local.Local;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.net.Inet4Address;
import java.net.URL;
import java.util.HashMap;

public class TestMITM {
    // replace <BROWSERSTACK_USERNAME-BROWSERSTACK_ACCESS_KEY> with your Username and Key. You can also set an environment variable.

    public static final String AUTOMATE_USERNAME = <"BROWSERSTACK_USERNAME">;
    public static final String AUTOMATE_ACCESS_KEY = <"BROWSERSTACK_ACCESS_KEY">;

    public static final String URL = "https://" + AUTOMATE_USERNAME + ":" + AUTOMATE_ACCESS_KEY + "@hub-cloud.browserstack.com/wd/hub";
    public static BrowserMobProxyServer proxy = null;
    public static Local bsLocal = null;

    public static void main(String[] args) throws Exception {

        // BROWSERMOB PROXY SETUP
        proxy = new BrowserMobProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start(0);
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        String hostIp = Inet4Address.getLocalHost().getHostAddress();
        String portIp = String.valueOf(proxy.getPort());
        seleniumProxy.setHttpProxy(hostIp + ":" + proxy.getPort());
        seleniumProxy.setSslProxy(hostIp + ":" + proxy.getPort());


        proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
        System.out.println("BrowserMobProxy PORT = " + proxy.getPort() + "\n" + "Selenium HTTP Proxy = " + seleniumProxy.getHttpProxy() + "\n" + "Selenium SSL Proxy = " + seleniumProxy.getSslProxy() + "\n" + "Selenium Proxy Type = " + seleniumProxy.getProxyType() + "\n" + "HostIP passed in Selenium proxy = " + hostIp);

        //SLEEP ADDED TO ENSURE SUFFICIENT TIME FOR PROXY TO BE SETUP BEFORE CONNECTING IT TO BINAEY
        Thread.sleep(6000);


        bsLocal = new Local();

        // replace <browserstack-accesskey> with your key. You can also set an environment variable - "BROWSERSTACK_ACCESS_KEY".
        HashMap<String, String> bsLocalArgs = new HashMap<String, String>();
        bsLocalArgs.put("key", AUTOMATE_ACCESS_KEY);
        bsLocalArgs.put("localProxyHost", hostIp);
        bsLocalArgs.put("localProxyPort", portIp);
        bsLocalArgs.put("force", "true");

        bsLocalArgs.put("forcelocal", "true");
        bsLocalArgs.put("forceproxy", "true");
        bsLocalArgs.put("localIdentifier", "langBindLocal");
        bsLocalArgs.put("v", "true");
        bsLocalArgs.put("logFile", "./logs.txt");


        //starts the Local instance with the required arguments
        bsLocal.start(bsLocalArgs);

        //SLEEP ADDED TO ENSURE SUFFICIENT TIME FOR LOCAL BINARY TO BE STARTED BEFORE CONNECTING TO IT
        Thread.sleep(6000);

        //check if BrowserStack local instance is running
        System.out.println(bsLocal.isRunning());

        ChromeOptions options = new ChromeOptions();
        options.addArguments("incognito");  // ChromeOptions for starting chrome in incognito mode
        options.addArguments("--disable-web-security");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--ignore-urlfetcher-cert-requests");


        DesiredCapabilities caps = new DesiredCapabilities();

        caps.setCapability("os", "Windows");
        caps.setCapability("os_version", "10");
        caps.setCapability("browser", "Chrome");
        caps.setCapability("browser_version", "97.0");
        caps.setCapability("browserstack.local", "true");
        caps.setCapability("browserstack.localIdentifier", "langBindLocal");
        caps.setCapability("browserstack.selenium_version", "3.14.0");
        caps.setCapability("name", "Google HAR capture"); // test name
        caps.setCapability("build", "BrowserMob MITM"); // CI/CD job or build name
        caps.setCapability(ChromeOptions.CAPABILITY, options);
        caps.setCapability(CapabilityType.PROXY, seleniumProxy);
        caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        caps.setCapability("acceptSslCerts", "true");

        final WebDriver driver = new RemoteWebDriver(new URL(URL), caps);
        try {
            proxy.newHar("google.com");
            driver.get("https://www.google.com");
            Thread.sleep(2000);
            Har har = proxy.getHar();
            File harFile = new File("googleBrowserstackLocal.har");
            har.writeTo(harFile);

            Thread.sleep(5000);

            if (harFile.exists() && !harFile.isDirectory()) {
                markTestStatus("passed", "BrowserMob MITM proxy works!", driver);
            }
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
        proxy.stop();
        driver.quit();

        // stop the Local instance
        bsLocal.stop();

    }

    // This method accepts the status, reason and WebDriver instance and marks the test on BrowserStack
    public static void markTestStatus(String status, String reason, WebDriver driver) {
        final JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"" + status + "\", \"reason\": \"" + reason + "\"}}");
    }
} 