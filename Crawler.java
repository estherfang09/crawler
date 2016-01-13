import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

//2014.May.23
//The program get the result from Baidu search engine according to the queries input 
//and list the frequency of each em term (include tf-idf score)

/**
 * @author esther
 * 
 */

/**
 * @author esther
 *
 */



	private static Document getDocumentViaURL(String question)
			throws IOException {

		int time = (int) (Math.random() * 10000) + 1000;
		// the store address of server
		String serverAddr = "http://www.baidu.com/s?wd="
				+ question
				+ "&rsv_spt=1&issp=1&rsv_bp=0&ie=utf-8&tn=baiduhome_pg&pn=0&rn=100&inputT="
				+ time;

		// start a new doc to store it
		Document doc = null;
		StringBuilder builder = null;
		boolean dataGet = false;// set the initial data get or not to be false

		Proxy proxy = null;// set the initial proxy
		HttpURLConnection conn = null;
		BufferedReader reader = null;
		String line = null;
		URL url = null;

		do {
			// first try to get a proxy
			proxy = getProxy();

			// System.out.println("Using proxy: " + proxy);

			try {
				url = new URL(serverAddr);
				// connect the Url via proxy
				if (proxy == null)
					conn = (HttpURLConnection) url.openConnection();
				else
					conn = (HttpURLConnection) url.openConnection(proxy);
				conn.setReadTimeout(5 * 1000);// Set the time out time to be 5
												// seconds

				// Baidu Baike's not found page is in 302
				conn.setInstanceFollowRedirects(false);

				conn.connect();

				int code = conn.getResponseCode();
				// if the ip-address unavailable anymore
				if (code == HttpURLConnection.HTTP_MOVED_PERM
						|| code == HttpURLConnection.HTTP_MOVED_TEMP) {
					String newLocation = conn.getHeaderField("Location");

					if (newLocation
							.equalsIgnoreCase("http://www.baidu.com/search/error.html")) {
						System.out.println("Baidu error page: " + proxy);
						// proxyList.remove(proxy);
						// numOfProxy = proxyList.size();
						continue;
						// Page not found

						// break;

					}
				}

				line = null;
				builder = new StringBuilder();
				reader = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));

				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}

				dataGet = true;
				doc = Jsoup.parse(builder.toString());

			} catch (FileNotFoundException fnfEx) {
				// 404
				System.out.println("404 not found");
				// continue;
			} catch (IOException ioEx) {
				dataGet = false;
			} finally {
				if (reader != null) {
					try {
						reader.close();
						reader = null;
					} catch (IOException ioeEx) {
					}
				}
				if (conn != null) {
					conn.disconnect();
					conn = null;
				}
			}
		} while (dataGet == false);

		return doc;
	}

	private static String[] IPPortArray = {
			
			// "122.96.59.106","80","58.20.127.26","3128",
			"211.138.121.36", "80",
			"111.1.36.12",
			"80",
			"111.1.36.133",
			"80",
			"211.138.121.37",
			"81",
			"211.138.121.38",
			"80",
			"211.138.121.37",
			"80",
			"183.129.198.242 ",
			"80",
			"211.138.121.38",
			"80",
			"211.138.121.38",
			"82",
			"183.129.198.247",
			"80",
			"120.198.230.31",
			"80",

			// from cnproxy 5.26
			"61.58.90.215", "8088", "68.91.163.19", "3128", "145.255.4.150",
			"8080", "171.36.107.240", "11498", "171.101.128.123", "3128",
			"175.139.214.123", "8080", "176.110.173.130", "8080",
			"183.129.137.174", "3128", "183.129.198.228", "80",
			"183.207.224.17", "80", "183.207.224.17", "82", "183.207.224.17",
			"83", "218.108.170.166", "80", "218.194.58.249", "80",
			"219.229.36.145", "18186", "221.7.11.22", "80",
			"211.138.121.36",
			"80",
			"218.240.156.82",
			"80",
			"182.98.163.166",
			"3128",
			"210.73.220.18",
			"8088",
			"218.240.156.82",
			"80",
			// from cnproxy 7.26
			"111.1.36.27", "81", "115.236.59.194", "3128", "111.1.36.25", "83",
			"111.1.36.26", "85", "111.1.36.26", "81", "111.1.36.26", "80",
			"111.1.36.25", "83", "111.1.36.25", "80", "111.1.36.163", "80",
			"111.1.36.165", "80", "111.1.36.22", "80", "111.1.36.23", "80",
			"111.1.36.27", "82", "111.1.36.27", "83", "111.1.36.27", "80",
			"111.1.36.27", "84", "111.1.36.27", "81", "111.1.36.27", "85",
			"210.14.138.102", "8080", "119.188.46.42", "8080",
			"117.25.129.238", "8888", "210.73.220.18", "8088", "61.174.9.96",
			"8080", "211.151.13.22", "81", "115.29.184.17", "82",
			"42.121.105.155", "8888", };

	private static Random random = new Random();
	private static int numOfProxy;

	// get proxy from the IPPortArray randomly
	private static Proxy getProxy() {
		int index = random.nextInt(numOfProxy);
		return proxyList.get(index);
	}

	// open a new list of Proxies
	private static List<Proxy> proxyList = new ArrayList<Proxy>();

	static {
		for (int i = 0; i < IPPortArray.length / 2; ++i) {
			proxyList.add(new Proxy(Proxy.Type.HTTP,
					new InetSocketAddress(IPPortArray[2 * i], Integer
							.valueOf(IPPortArray[2 * i + 1]))));
		}
		numOfProxy = proxyList.size();
	};



}
