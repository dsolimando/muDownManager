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
 */
package mudownmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import mudownmanager.backend.MUClient;

public class MuClientMockup implements MUClient {
	
	public void configure(Properties properties) {
		// TODO Auto-generated method stub
		
	}

    public boolean login(String username, String password) {
        return true;
    }

    public DownloadContext getDownloadInfos(String url) {
        DownloadContext context = new DownloadContext();
        context.setBandwidth(-1);
        context.setStatus(DownloadContext.Status.IDLE);
        context.setCurrentFileSize(0);
        context.setTotalFileSize((int) (Math.random() * 1000000));
        context.setTimeRemaining(-1);
        context.setFilename(String.format("file_%s.exe", (int) (Math.random() * 10)));
        context.setUrl(url);
        return context;
    }

    public void startCopy(final DownloadContext downloadContext, final Callback callback) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                DownloadContext contextCopy = new DownloadContext();
                Map<String, Object> parameters = new HashMap<String, Object>();
                while (true) {
                    try {
                        parameters = new HashMap<String, Object>();
                        parameters.put("downloadContext", contextCopy);
                        contextCopy.setCurrentFileSize(contextCopy.getCurrentFileSize() + 10000);
                        contextCopy.setStatus(DownloadContext.Status.RUNNING);
                        contextCopy.setTotalFileSize(downloadContext.getTotalFileSize());

                        if (contextCopy.getCurrentFileSize() >= contextCopy.getTotalFileSize()) {
                            contextCopy.setCurrentFileSize(contextCopy.getTotalFileSize());
                            contextCopy.setStatus(DownloadContext.Status.FINISHED);
                        }

                        if (contextCopy.getCurrentFileSize() >= 100000)
                            throw new CopyException();

                        contextCopy.setBandwidth(10000);
                        contextCopy.setUrl(downloadContext.getUrl());
                        contextCopy.setFilename(downloadContext.getFilename());
                        contextCopy.setTimeRemaining((int) ((contextCopy.getTotalFileSize() - contextCopy.getCurrentFileSize()) / contextCopy.getBandwidth()));
                        callback.call(parameters);

                        if (contextCopy.getCurrentFileSize() >= contextCopy.getTotalFileSize()) {
                            break;
                        }

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MuClientMockup.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (CopyException e) {
                        contextCopy.setStatus(DownloadContext.Status.FAILED);
                        callback.call(parameters);
                        break;
                    }
                }
            }
        });
        thread.start();
    }

	@Override
	public void stop(DownloadContext downloadContext) {
		// TODO Auto-generated method stub
		
	}
	
	public void setMaxDownloads(int maxDownloads) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String resolveLinkbuck(String url) {
		// TODO Auto-generated method stub
		return null;
	}
}
