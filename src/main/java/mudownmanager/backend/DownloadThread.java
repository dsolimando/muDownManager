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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import mudownmanager.Callback;
import mudownmanager.DownloadContext;
import mudownmanager.DownloadContext.Status;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class DownloadThread implements Runnable {

	private HttpClient httpClient;
	
	private Boolean stop = false;
	
	private Callback<String, Object> callback;
	
	private DownloadContext downloadContext;
	
	private Status threadStatus = Status.IDLE;
	
	@Override
	public void run() {
		GetMethod getMethod = new GetMethod(downloadContext.getUrl());
		DownloadContext contextCopy = new DownloadContext();
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("downloadContext", contextCopy);
        contextCopy.setStatus(Status.IDLE);
        callback.call(parameters);
        
        FileOutputStream fos = null;
        File file = null;
        int bufferSize = 1024;
		
        try {
        	file = new File(downloadContext.getDestDir() +"/"+downloadContext.getFilename());
			fos = new FileOutputStream(file);
			httpClient.executeMethod(getMethod);
			InputStream inputStream = getMethod.getResponseBodyAsStream();
			
			byte[] buffer = new byte [bufferSize];
			int bytesRead;
			long totalBytesRead = 0;
			long timerStart = System.currentTimeMillis();
			
			System.out.println("Starting Download...");
			
			int theoricalCurrentSize = 0;
			
			long previousTotal = 0;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				parameters = new HashMap<String, Object>();
                parameters.put("downloadContext", contextCopy);
                
				if (stop) {
					break;
				}
				
				theoricalCurrentSize+=1024;
				fos.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				
                contextCopy.setCurrentFileSize((long) (totalBytesRead/1024.));
                contextCopy.setStatus(DownloadContext.Status.RUNNING);
                threadStatus = Status.RUNNING;
                contextCopy.setTotalFileSize(downloadContext.getTotalFileSize());
                contextCopy.setUrl(downloadContext.getUrl());
                contextCopy.setFilename(downloadContext.getFilename());
                
                if (contextCopy.getBandwidth() != 0) 
                	contextCopy.setTimeRemaining((int) ((contextCopy.getTotalFileSize() - contextCopy.getCurrentFileSize()) / contextCopy.getBandwidth()));
                
                // we publish copy notification every 512 Ko
    			if ((theoricalCurrentSize % 204800) == 0 ) {
    				long currentTime = System.currentTimeMillis();
    				
    				if (currentTime != timerStart) {
    					int bandwidth = (int) (((((double)totalBytesRead - previousTotal)/1024.) / ((currentTime-timerStart)/1000.)));
    					contextCopy.setBandwidth(bandwidth);
    					previousTotal = totalBytesRead;
    					timerStart = currentTime;
    				}
    				callback.call(parameters);
    			}
			}
			
			if (stop) {
				getMethod.abort();
				contextCopy.setStatus(Status.STOPPED);
				threadStatus = Status.STOPPED;
				callback.call(parameters);
			} else {
				contextCopy.setCurrentFileSize(contextCopy.getTotalFileSize());
				contextCopy.setStatus(DownloadContext.Status.FINISHED);
				threadStatus = Status.FINISHED;
				callback.call(parameters);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			 contextCopy.setStatus(DownloadContext.Status.FAILED);
			 threadStatus = Status.FAILED;
             callback.call(parameters);
		} finally {
			getMethod.releaseConnection();
			try {
				fos.close();
				if (stop) {
					file.delete();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setHttpClient(final HttpClient httpClient) {
		this.httpClient = httpClient;
	}
	
	public void setCallback(Callback<String, Object> callback) {
		this.callback = callback;
	}
	
	public void setDownloadContext(DownloadContext downloadContext) {
		this.downloadContext = downloadContext;
	}
	
	public Status getThreadStatus() {
		return threadStatus;
	}
	
	public void stop () {
		stop = true;
	}
}

