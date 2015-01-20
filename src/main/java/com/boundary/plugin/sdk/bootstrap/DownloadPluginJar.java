// Copyright 2014 Boundary, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.boundary.plugin.sdk.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Utility class for downloading a plugin from a release in GitHub
 */
public class DownloadPluginJar {

	private final static String POM_PATH="pom.xml";
	private final static String JAR_DESTINATION_PATH="config/plugin.jar";
	private final static String VERSION_XPATH_EXPRESSION = "/project/version";
	private final static String BASE_URL_XPATH_EXPRESSION = "/project/properties/boundary-jar-base-url";
	private String version;
	private String baseUrl;
	
	public DownloadPluginJar() {

	}
	
	/**
	 * Reads the pom.xml file to extract information to find the jar plugin.
	 * 
	 * @param pomFile Maven POM file path
	 * @throws Exception 
	 */
	private void readPOM() throws Exception {
		DocumentBuilder parser = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		try {
			FileInputStream input = new FileInputStream(new File(POM_PATH));
			try {
				Document document = parser.parse(input);

				XPath xpath = XPathFactory.newInstance().newXPath();
				this.version = (String) xpath.evaluate(
						VERSION_XPATH_EXPRESSION, document,
						XPathConstants.STRING);
				this.baseUrl = (String) xpath.evaluate(
						BASE_URL_XPATH_EXPRESSION, document,
						XPathConstants.STRING);
				if (this.baseUrl.length() == 0) {
					throw new Exception(String.format("Base URL not found in %s%n",POM_PATH));
				}
				input.close();
			} catch (IOException e) {
				throw e;
			} finally {
				input.close();
			}
		} catch (FileNotFoundException e) {
			throw e;
		}
	}

	/**
	 * Downloads a jar file based on pom.xml information
	 * to the configuration directory <code>config</code>
	 * 
	 * @throws IOException Any kind of IO error occurs
	 */
	private void downloadJAR() throws IOException {
		OutputStream out = null;

		HttpsURLConnection connection = null;
		String sUrl = String.format("%s/%s/plugin.jar", this.baseUrl,
				this.version);
		System.err.println(sUrl);
		URL url = new URL(sUrl);
		connection = (HttpsURLConnection) url.openConnection();
//		connection.addRequestProperty("Accept","application/zip");
		// Ensure that we follow redirects
		HttpsURLConnection.setFollowRedirects(true);
		connection.connect();

		InputStream in = connection.getInputStream();
		System.err.printf("HTTP Response %d%n", connection.getResponseCode());

		out = new FileOutputStream(new File(JAR_DESTINATION_PATH));
		byte[] b = new byte[1024];

		System.err.printf("Downloading %s of %d bytes from %s...",
				JAR_DESTINATION_PATH, connection.getContentLength(),
				connection.getURL());

		while (in.read(b) != -1) {
			out.write(b);
		}

		out.close();
	}
	
	/**
	 * Downloads the plugins jar file to the configuration directory <code>config/plugin.jar</code>
	 */
	public int execute(String[] args) {
		int result = 1;
		System.err.println("Running post extract...");
		try {
			readPOM();
			downloadJAR();
			System.err.println("Download successful");
			result = 0;
		} catch (Exception e) {
			System.err.printf("%s%n",e.getMessage());
		}
		// Ensure that the standard error is flushed before exiting.
		System.err.flush();
		return result;
	}
	
	/**
	 * Main execution
	 * @param args Arguments passed on the command line
	 */
	public static void main(String [] args) {
		DownloadPluginJar postExtract = new DownloadPluginJar();
		postExtract.execute(args);
	}
}

