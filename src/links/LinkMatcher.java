package links;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for finding links in an html file.
 */
public class LinkMatcher {

	public static final String REGEX = "<a[ ]+([a-z]+[ ]*=[ ]*\"[a-z0-9_]+\"[ ]*)*href[ ]*=[ ]*\"((http[s]?://[a-z0-9\\-]{2,63}(\\.[a-z0-9\\-]{2,63})+/?)*(/[a-z0-9_\\-]+/?|/[a-z0-9_\\-]+\\.[a-z0-9]+)*(\\?[a-z0-9]+=[a-z0-9+:,_\\-]+(&[a-z0-9]+=[a-z0-9+:,_\\-]+)*)?)(#[a-z0-9+:,_\\-]+(=[a-z0-9+:,_\\-\\.]+)*)*\"[ ]*([a-z]+[ ]*=[ ]*\"[a-z0-9_]+\"[ ]*)*[ ]*>";
	public static final String DOMAINREGEX = "http[s]*://([a-z0-9\\-]{2,63}(\\.[a-z0-9\\-]{2,63})+)(.*)?";
	public static int PORT = 80;

	/**
	 * Take an html file and return a list of hyperlinks in that html that
	 * satisfy the following requirements: 1. The list should not contain
	 * duplicates. For the purpose of this assignment, duplicates are either
	 * (a) the links that are the same, except for the fragment.
	 * 
	 * @param filename
	 *            The name of the HTML file.
	 * @return List
	 */
	public static List<String> findLinks(String filename) {
		List<String> links = new ArrayList<>();

		try {
			// read from html file
			BufferedReader in = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}

			Pattern p = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(sb.toString());
			while (m.find()) {
				// group 2 is the url without fragment, not adding duplicate
				if (m.group(2) != null && m.group(2).length() > 0 && !links.contains(m.group(2))) {
					links.add(m.group(2));
				}
			}

			in.close();
		}
		catch (IOException e) {
			System.out.println("IOException happened during findLinks: " + e);
		}

		return links;
	}

	/**
	 * Take a URL, fetch an html page at this URL (using sockets), and find all
	 * unique hyperlinks on that webpage. The list should not contain
	 * "duplicates" (see the previous comment) or links that take you to the
	 * same page. The difference with the previous method is that it should
	 * fetch the HTML from the server first.
	 * 
	 * @param url
	 * @return List
	 */
	public static List<String> fetchAndFindLinks(String url) {
		List<String> links = new ArrayList<>();
		String domain = "";
		String path = "";

		try {
			// get domain name and path
			Pattern p = Pattern.compile(DOMAINREGEX, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(url);
			if (m.find()) {
				if (m.group(1) != null && m.group(1).length() > 0) {
					domain = m.group(1);
				}
				if (m.group(3) != null && m.group(3).length() > 0) {
					path = m.group(3);
				}
			}

			// create socket and input/output stream
			Socket socket = new Socket(domain, PORT);
			OutputStream outStream = socket.getOutputStream();
			InputStream inStream = socket.getInputStream();

			// create and send request
			String request = getRequest(domain, path);
			outStream.write(request.getBytes());
			outStream.flush();

			// wrap the input stream to make it easier to read from
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
			StringBuilder sb = new StringBuilder();

			// use input stream to read server's response
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}

			// remove header
			String result = sb.toString().substring(sb.toString().indexOf("<!DOCTYPE html>"));
			System.out.println(result);
			System.out.println();

			// load links into list
			p = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
			m = p.matcher(result);
			while (m.find()) {
				// group 2 is the url without fragment
				if (m.group(2) != null && m.group(2) != "") {
					// remove '/' if it appears in the end
					String tempUrl = (m.group(2).charAt(m.group(2).length() - 1) == '/')
							? m.group(2).substring(0, m.group(2).length() - 1) : m.group(2);

					// not adding duplicate
					if (!links.contains(tempUrl)) {
						links.add(tempUrl);
					}
				}
			}
		}
		catch (IOException e) {
			System.out.println("IOException happened during fetchAndFindLinks: " + e);
		}

		return links;
	}

	/**
	 * A method that creates a GET request for the given host and resource
	 * @param host
	 * @param pathResourceQuery
	 * @return String
	 *          - HTTP GET request returned as a string
	 */
	private static String getRequest(String host, String pathResourceQuery) {
		String request = "GET " + pathResourceQuery + " HTTP/1.1" + System.lineSeparator() // GET
				// request
				+ "Host: " + host + System.lineSeparator() // Host header required for HTTP/1.1
				+ "Connection: close" + System.lineSeparator() // make sure the server closes the
				// connection after we fetch one page
				+ System.lineSeparator();
		return request;
	}
}
