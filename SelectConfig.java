package autoUpdate;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * This class allows the user to select a
 * configuration file, and also sets the 
 * mainForm to "visible" once the file
 * is selected.
 * 
 * @authors jbelmont, njooma
 *
 */
@SuppressWarnings("serial")
public class SelectConfig extends JFrame {
	
	private MainForm _mainForm;
	private JComboBox<String> _fileBox;
	
	public SelectConfig() {
		//Basics of the GUI
		super("Select a param file");
		this.setPreferredSize(new Dimension(340, 70));
		this.setSize(this.getPreferredSize());
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setLocation(new java.awt.Point(50, 50));
		this.setVisible(true);
		
		//Create the selection box
		JPanel mainPanel = new JPanel();
		_fileBox = new JComboBox<String>();
		_fileBox.setPreferredSize(new Dimension(250, 25));
		JButton loadButton = new JButton("Load");
		loadButton.addActionListener(new LoadListener(this, _fileBox));
		mainPanel.add(_fileBox);
		mainPanel.add(loadButton);
		
		//Add to the frame...
		this.add(mainPanel);
		this.pack();
		this.setVisible(true);
	}
	
	/**
	 * This method sets the param file to be used
	 * for this instance of AutoUpdate. It reads
	 * the file with available param XML files, and
	 * sets the JComboBox up so that it allows the
	 * user to select whichever param file is necessary.
	 * 
	 * @param MainForm mainForm
	 * @return boolean: true if successful, false otherwise
	 */
	public boolean setConfig(MainForm mainForm) {
		_mainForm = mainForm;
		String appPath = mainForm.getAppPath();
		Path configFile = Paths.get(appPath, "config/HTAPP_config_options.txt"); //File that contains names of available param XML files
		//If file doesn't exist, alert the user and close with error.
		if (Files.notExists(configFile)) {
			JOptionPane.showMessageDialog(this, "Cannot find the file " + configFile.toString(), "File Not Found", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
			return false;
		}
		//Read file and obtain names of available XML param files.
		try {
			List<String> configList = Files.readAllLines(configFile, StandardCharsets.UTF_8);
			for (int i=0; i<configList.size(); i++) {
				String config = configList.get(i);
				if (config.indexOf("->", 1) != -1) {
					config = config.substring(0, config.indexOf("->", 1));
				}
				//Check if file exists and add to the JComboBox selector
				Path configPath1 = Paths.get(appPath, "config/", config);
				Path configPath2 = Paths.get(appPath, config);
				if (Files.exists(configPath1) || Files.exists(configPath2)) {
					_fileBox.addItem(config);
				}
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * This private inner class is the listener for the LoadButton.
	 * It performs the action of passing the selected param file
	 * to the application.
	 * 
	 * @author njooma
	 *
	 */
	private class LoadListener implements ActionListener {

		private JFrame _frame;
		private JComboBox<String> _fileBox;
		
		public LoadListener(JFrame frame, JComboBox<String> fileBox) {
			_frame = frame;
			_fileBox = fileBox;
		}
		
		public void actionPerformed(ActionEvent e) {
			_mainForm.setParamFile((String) _fileBox.getSelectedItem());
			_mainForm.setIsParamSet(true);
			_frame.dispose();
		}
	}
}
