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
/**
 * @author esther
 *
 */
public class AliasCrawler {

	ArrayList<String> patternArr = null;
	ArrayList<String> absPathArr = null;


	ArrayList<String> absPathtest = null;

	public AliasCrawler(String ptFilePath) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(ptFilePath));
		File ptFile = new File(ptFilePath);

		String dir = ptFile.getParent();

		patternArr = new ArrayList<String>();
		absPathArr = new ArrayList<String>();

		String ptt = null;

		while ((ptt = br.readLine()) != null) {
			ptt = ptt.replace("<full>", "");
			ptt = ptt.replace("<alias>", "");
			patternArr.add(ptt);
			String absPath = dir + File.separatorChar + "abstract_" + ptt
					+ ".json";
			absPathArr.add(absPath);
		}
		br.close();

	}

	public static void main(String[] args) throws Exception {
		AliasCrawler ac = new AliasCrawler(
				"/Users/esther/Desktop/untitled folder 3/test_aliasMining/pt123.txt");
	
		String nameListPath = "/Users/esther/Desktop/untitled folder 3/beijing/airports_list";
		ac.findAliasesOneFile(
				nameListPath,
				"/Users/esther/Desktop/untitled folder 3/test_aliasMining/140915.txt",
				10, true);
	
	}

	public void fechAbstract(String nameListPath) throws Exception {
		for (int i = 0; i < patternArr.size(); i++) {
			fetchAbstracts(absPathArr.get(i), nameListPath, patternArr.get(i));
		}
	}

	public static void fetchAbstracts(String absPath, String entry,
			boolean isAppended) throws IOException {
		// BufferedReader inAbstract;
		BufferedWriter out = new BufferedWriter(new FileWriter(absPath,
				isAppended));
		System.out.print(entry + "...");
		int noResultCount = 0;
		while (noResultCount >= 0 && noResultCount < 30) {
			// encode the query
			String queryEncoded = URLEncoder.encode(entry, "utf-8");
			// get the result page document
			Document getdoc = getDocumentViaURL(queryEncoded);

			// get a list of string of the abstracts
			List<String> results = AliasUtil.extractAbstract(getdoc);
			System.out.println(results.size() + " results.");

			if (results.size() == 0) {
				noResultCount++;
			} else {
				noResultCount = -1;

				JSONObject jobj = new JSONObject();
				jobj.put("query", entry);
				jobj.put("results", AliasUtil.preprocess(results));

				out.write(jobj.toJSONString());
				out.newLine();
				out.flush();
			}
		}
		out.close();
	}

	public static void fetchAbstracts(String absPath, String nameListPath,
			String pt) throws IOException {
		// BufferedReader inAbstract;
		BufferedWriter out;
		// First read in the already fetched queries
		Map<String, String> fetchedQueries = new HashMap<String, String>();
		try {
			BufferedReader inAbstract = new BufferedReader(new FileReader(
					absPath));

			String line = null;
			while ((line = inAbstract.readLine()) != null) {
				JSONObject jobj = (JSONObject) JSONValue.parse(line);
				if (jobj == null)
					continue;
				JSONArray arr = (JSONArray) jobj.get("results");
				if (arr.size() == 0)
					continue;
				fetchedQueries.put((String) jobj.get("query"), line);
			}
			inAbstract.close();

			// Update the output file: remove the empty ones
			/*
			 */
			out = new BufferedWriter(new FileWriter(absPath, false));

			for (Entry<String, String> e : fetchedQueries.entrySet()) {
				out.write(e.getValue());
				out.newLine();
			}
			out.close();
		} catch (FileNotFoundException fnfex) {

		}
		// Then read the file which contains queries to fetch
		// in = new BufferedReader(new FileReader("data/poi/" + city + ".poi."
		// + typeNum + ".prefix.txt"));

		// Thus, for the output file, we can now append to it.
		/*
		 * out = new BufferedWriter(new FileWriter("data/abstract/" + city +
		 * ".abstract." + typeNum + ".json", true));
		 */
		BufferedReader inNameList = new BufferedReader(new FileReader(
				nameListPath));
		out = new BufferedWriter(new FileWriter(absPath, true));

		String query;

		while ((query = inNameList.readLine()) != null) {
			System.out.print(query + "...");
			if (fetchedQueries.containsKey(query)) {
				System.out.println("skipped.");
				continue;
			}
			int noResultCount = 0;
			while (noResultCount >= 0 && noResultCount < 30) {
				// encode the query
				String queryEncoded = URLEncoder.encode(query + pt, "utf-8");
				// get the result page document
				Document getdoc = getDocumentViaURL(queryEncoded);

				// get a list of string of the abstracts
				List<String> results = AliasUtil.extractAbstract(getdoc);
				System.out.println(results.size() + " results.");

				if (results.size() == 0) {
					noResultCount++;
				} else {
					noResultCount = -1;

					JSONObject jobj = new JSONObject();
					jobj.put("query", query);
					jobj.put("results", AliasUtil.preprocess(results));

					out.write(jobj.toJSONString());
					out.newLine();
					out.flush();
				}
			}
		}
		out.close();
		inNameList.close();
	}

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

	// http://iframe.ip138.com/ic.asp

	public static void print(JSONObject print, String filename)
			throws IOException {
		String path = "data/score/" + filename;
		File file = new File(path);
		if (!file.exists()) {
			file.createNewFile();
		}
		try {
			FileWriter ea = new FileWriter(file);
			// BufferedWriter bw = new BufferedWriter(fW);
			ea.write(print.toJSONString());
			ea.flush();
			ea.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * extract Aliases from the known patterns
	 * @param inPath
	 * @param full
	 * @param specialStr
	 *            is something like "简称xx"……
	 * @return List<String> which are the candidates of Aliases
	 */
	private List<String> extractAliases(String inPath, String full,
			String specialStr) {
		List<String> res = new ArrayList<String>();
		List<String> results;
		try {
			results = SmallTools.getResults(inPath, full);
			// System.out.println("The array size is\t" + results.size());
			for (String result : results) {
				result = SmallTools.keepChineseChar(result);// the return result
															// should be all
															// chinese(English
															// with ［］、＋、“”
															// punctuation
															// deleted
				String regex = full + "[,；;()（）的，.、?:]*"
						+ Pattern.quote(specialStr) + ".*";
				Pattern pt1 = Pattern.compile(regex);
				Matcher m = pt1.matcher(result);
				String src = null;
				while (m.find()) {
					src = m.group();
					System.out.println("sentence:" + " " + src);
					int start = src.indexOf(specialStr);
					start += specialStr.length();// start now begin the first
													// word after specialStr
					src = src.substring(start);
					src = src.trim();// if there exists spaces after
										// specialStr，we
										// delete it

					M160Splitter mst = new M160Splitter();
					String k = "[\u4e00-\u9fff]+";// match all Chinese
													// character;
					Pattern pt3 = Pattern.compile(k);
					Matcher m3 = pt3.matcher(src);

					while (m3.find()) {
						String found = m3.group();
						ArrayList<String> spL = mst.splitM160(found);
						if (spL.size() > 6) {
							spL = subList(spL, 0, 6);
							found = addString(spL);
						}
						if (!found.equals("") && !found.equals(full)) {
							res.add(found);
							break;
						}
					}
				}
			}
		} catch (NullPointerException | IOException npEx) {
			npEx.printStackTrace();
		}
		return res;
	}
	
	/**
	 * the Array-list version of Sublist
	 * @param ls
	 * @param start
	 * @param end
	 * @return
	 */
	private ArrayList<String> subList(ArrayList<String> ls, int start,
			int end) {
		ArrayList<String> subList = new ArrayList<String>();
		for (int i = start; i < end; i++) {
			subList.add(ls.get(i));
		}
		return subList;
	}

	/**
	 * @param list of string
	 * @return 
	 */
	private String addString(List<String> subList) {
		String accumulator = "";
		for (String sub : subList) {
			accumulator += sub;
		}
		return accumulator;
	}

	/**
	  
	 * @param resList
	 * @param Full
	 * @param k
	 *            threshold
	 * @param isShrank
	 *            choose absorb or not
	 * @return
	 * @throws IOException
	 */
	private Map<String, Integer> getSubstring(List<String> resList,
			String Full, int k, boolean isShrank) throws IOException {
		Map<String, Integer> stMap = new HashMap<String, Integer>();
		M160Splitter mst = new M160Splitter();
		for (String str : resList) {
			ArrayList<String> arrs = mst.splitM160(str);
			for (int i = 0; i <= arrs.size(); i++) {
				for (int j = i + 1; j <= arrs.size(); j++) {
					List<String> subList = subList(arrs, i, j);
					String sub = addString(subList);
					if (sub.length() >= 2 && !sub.equals(Full)) {
						if (stMap.get(sub) != null) {
							// System.out.println("test!!!!!!\t"+sub);
							stMap.put(sub, stMap.get(sub) + 1);
						} else {
							// System.out.println("test!!!!!!\t"+sub);
							stMap.put(sub, 1);
						}
					}
				}
			}
		}
		// return stMap;
		// open a new map,only keep those substrings which occurrences are larger than
		// integer k.
		Map<String, Integer> r1 = new HashMap<String, Integer>();
		for (Entry<String, Integer> e : stMap.entrySet()) {
			if (e.getValue() >= k) {
				r1.put(e.getKey(), e.getValue());
			}
		}

		// if we choose to absorb the substring of the substring
		if (isShrank) {
			// Set<String> subString = SmallTools.absorb(r1,
			// SmallTools.keepSub(Full, r1.keySet(), true));
			// Set<String> notsubString = SmallTools.absorb(r1,
			// SmallTools.keepSub(Full, r1.keySet(), false));
			// subString.addAll(notsubString);
			// Map<String, Integer> r = new HashMap<String, Integer>();
			// for (Entry<String, Integer> e : r1.entrySet()) {
			// if (subString.contains(e.getKey())) {
			// // System.out.println("absorb after\t" + e.getKey());
			// r.put(e.getKey(), e.getValue());
			// }
			HashMap<String, Integer> sMap = shrankFn(Full, r1);
			return sMap;
			// }
		} else {
			return r1;
		}
	}

	/**
	 * absorb 
	 * @param full
	 * @param r12
	 * @return
	 */
	private HashMap<String, Integer> shrankFn(String full,
			Map<String, Integer> r12) {
		Set<String> subString = SmallTools.absorb(r12,
				SmallTools.keepSub(full, r12.keySet(), true));
		Set<String> notsubString = SmallTools.absorb(r12,
				SmallTools.keepSub(full, r12.keySet(), false));
		subString.addAll(notsubString);
		HashMap<String, Integer> r1 = new HashMap<String, Integer>();
		for (Entry<String, Integer> e : r12.entrySet()) {
			if (subString.contains(e.getKey())) {
				// System.out.println("absorb after\t" + e.getKey());
				r1.put(e.getKey(), e.getValue());
			}
		}
		return r1;
	}

	/**
	 * @param readPath
	 *            the path where we store FULL
	 * @param outFile
	 *            where we write out of file
	 * @param k
	 *            threshold
	 * @param isShrank
	 *            choose to absorb or not
	 * @throws IOException
	 */
	void findAliasesOneFile(String readPath, String outFile, int k,
			boolean isShrank) throws IOException {
		HashSet<String> names = SmallTools.getAllLines(readPath);

		BufferedWriter bw = new BufferedWriter(new FileWriter(outFile, true));

		for (int i = 0; i < patternArr.size(); i++) {
			for (String Full : names) {
				String pattern = patternArr.get(i);

				List<String> reset = extractAliases(
						"/Users/esther/Desktop/untitled folder 3/beijing/机场简称.json",
						Full, pattern);
				int threshold = Math.max(1, reset.size() / k);
				// System.out.println(threshold);
				Map<String, Integer> rMap = getSubstring(reset, Full,
						threshold, isShrank);
				rMap = SmallTools.sortByValues(rMap);

				int top = 10;
				int count = 0;

				for (Entry<String, Integer> e : rMap.entrySet()) {
					if (count < top) {
						bw.write(Full + "\t" + e.getKey() + "\t" + e.getValue());
						bw.newLine();
						bw.flush();
						count += 1;
					} else {
						break;
					}
				}
			}
		}
		bw.close();
	}

	/**
	 * @param readPath
	 *            the path where we store all the  full-name.
	 * @param outDir
	 *            the directories 
	 * @param k
	 *            threshold
	 * @param isShrank
	 *            choose to absorb or not
	 * @param hasNum whether to include num or not in the txt files
	 * @throws IOException
	 */
	void findAliases(String readPath, String outDir, int k, boolean isShrank,
			boolean hasNum) throws IOException {
		// System.out.println("test is running");
		HashSet<String> names = SmallTools.getAllLines(readPath);
		for (int i = 0; i < patternArr.size(); i++) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outDir
					+ File.separatorChar + patternArr.get(i) + "_alias", false));

			for (String Full : names) {
				// System.out.println("Fullname is\t" + Full);
				String pattern = patternArr.get(i);
				List<String> reset = extractAliases(absPathArr.get(i), Full,
						pattern);
				int threshold = Math.max(1, reset.size() / k);
				// System.out.println(threshold);
				Map<String, Integer> rMap = getSubstring(reset, Full,
						threshold, isShrank);

				rMap = SmallTools.sortByValues(rMap);

				int top = 10;
				int count = 0;
				for (Entry<String, Integer> e : rMap.entrySet()) {
					if (count < top) {
						if (hasNum) {
							// System.out.println(Full + "\t" + e.getKey() +
							// "\t"
							// + e.getValue());
							bw.write(Full + "\t" + e.getKey() + "\t"
									+ e.getValue());
							bw.newLine();
							count += 1;
						} else {
							// System.out.println(Full + "\t" + e.getKey() +
							// "\t"
							// + e.getValue());
							bw.write(Full + "\t" + e.getKey());
							bw.newLine();
							count += 1;
						}
					} else {
						break;
					}
				}
			}
			bw.close();
		}
	}

}
