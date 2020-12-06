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
package mudownmanager;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.springframework.stereotype.Component;


/**
 * The main class of the application.
 */
@Component ("mudownmanager")
public class MuDownManagerApp extends SingleFrameApplication {

	/**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
    	MuDownManagerView downManagerView = new MuDownManagerView(this);
        show(downManagerView);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MuDownManagerApp
     */
    public static MuDownManagerApp getApplication() {
        return Application.getInstance(MuDownManagerApp.class);
    }
    
    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
    	launch(MuDownManagerApp.class, args);
    }
}
