/* 
 * Copyright (C) 2010 hrsldn@gmail.com.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
package mudownmanager.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PostConstruct;

import mudownmanager.Properties;
import mudownmanager.PropertiesLoader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component ("PropertiesLoader")
public class PropertiesLoaderImpl implements PropertiesLoader{
	
	private static final String DEST_DIRECTORY = "mudownmanager.destdirectory";
	
	private static final String USERNAME = "mudownmanager.username";
	
	private static final String PASSWORD = "mudownmanager.password";
	
	private static final String PROXY = "mudownmanager.proxy";
	
	private static final String PROXY_URL = "mudownmanager.proxyurl";
	
	private static final String PROXY_PORT = "mudownmanager.proxyport";
	
	private static final String FIRST_LAUNCH = "mudownmanager.firstlauch";
	
	private static final String MAX_NUM_THREADS = "mudownmanager.maxnumthreads";

	private String propertiesUrl = System.getProperty("user.home")
									+ "/"
									+ ".muDownManager";
	
	private String propertiesFileUrl = System.getProperty("user.home")
									+ "/"
									+ ".muDownManager"
									+ "/"
									+ "settings.xml";
	
	@Autowired
	private HttpClient httpClient;
	
	@Autowired
	private ThreadPoolExecutor threadPoolExecutor;
	
	private Properties properties;
	
	@PostConstruct
	public void loadProperties() throws PropertiesAccessExcpetion {
		
		java.util.Properties properties = new java.util.Properties();
		try {
			this.properties = new Properties();
			
			if (new File(propertiesFileUrl).exists()) {
				properties.loadFromXML(new FileInputStream(new File(propertiesFileUrl)));
				this.properties.setDestDirectory(properties.getProperty(DEST_DIRECTORY));
				this.properties.setUsername(properties.getProperty(USERNAME));
				this.properties.setPassword(properties.getProperty(PASSWORD));
				this.properties.setProxy(Boolean.parseBoolean(properties.getProperty(PROXY)));
				this.properties.setProxyUrl(properties.getProperty(PROXY_URL));
				this.properties.setProxyPort(properties.getProperty(PROXY_PORT));
				this.properties.setFirstLaunch(Boolean.parseBoolean(properties.getProperty(FIRST_LAUNCH)));
				this.properties.setMaxNumThreads(Integer.parseInt(properties.getProperty(MAX_NUM_THREADS)));
			} else {
				this.properties.setProxy(false);
				this.properties.setFirstLaunch(true);
				this.properties.setMaxNumThreads(4);
				this.properties.setDestDirectory(System.getProperty("user.home"));
			}
			applyPropertiesChange(this.properties);
		} catch (Exception e) {
			throw new PropertiesAccessExcpetion(e);
		} 
	}
	
	public Properties getProperties() {
		return properties;
	}

	public void saveProperties(Properties properties) throws PropertiesAccessExcpetion {
		applyPropertiesChange(properties);
		java.util.Properties properties2 = new java.util.Properties();
		properties2.setProperty(DEST_DIRECTORY, properties.getDestDirectory());
		properties2.setProperty(USERNAME, properties.getUsername());
		properties2.setProperty(PASSWORD, properties.getPassword());
		properties2.setProperty(PROXY, properties.getProxy().toString());
		properties2.setProperty(PROXY_URL, properties.getProxyUrl());
		properties2.setProperty(PROXY_PORT, properties.getProxyPort());
		properties2.setProperty(FIRST_LAUNCH, "false");
		properties2.setProperty(MAX_NUM_THREADS, properties.getMaxNumThreads()+"");
		try {
			if (!new File(propertiesUrl).exists()) {
				FileUtils.forceMkdir(new File (propertiesUrl));
			}
			properties2.storeToXML(new FileOutputStream(new File(propertiesFileUrl)), null);
		} catch (Exception e) {
			throw new PropertiesAccessExcpetion(e);
		} 
	}
	
	private void applyPropertiesChange (Properties properties) {
		threadPoolExecutor.setCorePoolSize(properties.getMaxNumThreads());
		if (properties.getProxy()) {
			System.setProperty("java.net.useSystemProxies", "true");
		} else {
			System.setProperty("java.net.useSystemProxies", "false");
		}
		httpClient.getHostConfiguration().setProxy(properties.getProxyUrl(), Integer.parseInt(properties.getProxyPort()));
	}
}
