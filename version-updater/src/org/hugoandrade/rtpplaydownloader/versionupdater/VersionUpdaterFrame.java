package org.hugoandrade.rtpplaydownloader.versionupdater;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class VersionUpdaterFrame extends JFrame {

	private JPanel container = new JPanel();
	private JLabel userLabel = new JLabel("Username");
	private JTextField userTextField = new JTextField(15);
	private JLabel passLabel = new JLabel("Password");
	private JPasswordField passTextField = new JPasswordField(15);
	private JLabel currentVersionLabel = new JLabel();
	private JTextField newVersionField = new JTextField(15);
	private JButton button = new JButton("");

	public VersionUpdaterFrame() {
		super("Azure Version Updater");

		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.add(userLabel);
		container.add(userTextField);
		container.add(passLabel);
		container.add(passTextField);
		container.add(currentVersionLabel);
		container.add(newVersionField);
		container.add(button);

		setSize(300,200);
		setLocation(500,280);

		userLabel.setBounds(70,30,150,20);
		userTextField.setBounds(70,30,150,20);
		passLabel.setBounds(70,30,150,20);
		passTextField.setBounds(70,65,150,20);
		currentVersionLabel.setBounds(70,30,150,20);
		newVersionField.setBounds(70,65,150,20);
		button.setBounds(110,100,80,20);

		getContentPane().add(container);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		setLoginUI();
	}

	private void setLoginUI() {

		userTextField.setText("");
		userTextField.setVisible(true);
		userLabel.setVisible(true);
		passTextField.setText("");
		passTextField.setVisible(true);
		passLabel.setVisible(true);
		currentVersionLabel.setVisible(false);
		newVersionField.setVisible(false);

		for(ActionListener l : button.getActionListeners()) button.removeActionListener(l);
		button.setText("Login");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String user = userTextField.getText();
				String pass = passTextField.getText();

				tryLogin(user, pass);
			}
		});
	}

	private void setUpdateVersionUI(Connection connection, String id, String currentVersion) {

		userTextField.setVisible(false);
		userLabel.setVisible(false);
		passTextField.setVisible(false);
		passLabel.setVisible(false);
		currentVersionLabel.setText("Current App Version:\n" + currentVersion);
		currentVersionLabel.setVisible(true);
		newVersionField.setText("");
		newVersionField.setVisible(true);

		for(ActionListener l : button.getActionListeners()) button.removeActionListener(l);
		button.setText("Update");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String newVersion = newVersionField.getText();

				tryUpdateVersion(connection, id, newVersion);
			}
		});
	}

	private void tryUpdateVersion(Connection connection, String id, String newVersion) {

		String updateSql = String.format("UPDATE dbo.RTPPlayAppVersion\n" +
				"SET CurrentVersion = '%s'\n" +
				"WHERE id = '%s'", newVersion, id);

		try (Statement statement = connection.createStatement()) {

			int i = statement.executeUpdate(updateSql);

			// Print results from select statement
			if (i == 1)
			{
				System.out.println("Ok");
				setUpdateVersionUI(connection, id, newVersion);
				return;
			}
			connection.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		setLoginUI();
	}

	private void tryLogin(String user, String password) {

		String hostName = PropertiesAdapter.getInstance().getHostName();
		String dbName = PropertiesAdapter.getInstance().getDatabase();
		String userSuffix = PropertiesAdapter.getInstance().getUserSuffix();

		String url = String.format("jdbc:sqlserver://%s:1433;"
				+ "database=%s;"
				+ "user=%s@%s;"
				+ "password=%s;"
				+ "encrypt=true;"
				+ "trustServerCertificate=false;"
				+ "hostNameInCertificate=*.database.windows.net;"
				+ "loginTimeout=30;", hostName, dbName, user, userSuffix, password);

		Connection connection = null;

		try {
			connection = DriverManager.getConnection(url);
			String schema = connection.getSchema();
			System.out.println("Successful connection: " + schema);

			String selectSql = "SELECT * FROM dbo.RTPPlayAppVersion";

			try (Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery(selectSql)) {

				// Print results from select statement
				while (resultSet.next())
				{
					System.out.println(resultSet.getString("id") + " -> " + resultSet.getString("CurrentVersion"));
					setUpdateVersionUI(connection, resultSet.getString("id"), resultSet.getString("CurrentVersion"));
					return;
				}
				connection.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		setLoginUI();
	}
}
