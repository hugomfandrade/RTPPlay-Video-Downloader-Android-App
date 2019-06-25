package org.hugoandrade.rtpplaydownloader.versionupdater;

import javax.swing.*;

public class VersionUpdaterLauncher {

	public static void main(String[] args)  {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new VersionUpdaterFrame();

			}
		});
    }
}
