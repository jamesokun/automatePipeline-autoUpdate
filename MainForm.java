package autoUpdate;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import modules.Form;
import modules.Mail;
import modules.Params;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * @authors jbelmont, njooma
 *
 */
@SuppressWarnings("serial")
public class MainForm extends JFrame implements Form{

	private volatile boolean _isParamSet;
	private JTextArea _statusArea;
	private JLabel _sequestLabel, _comLabel, _fileLabel;
	private JTable _queue;
	private String _appPath, _paramFile, _sep;
	private String[][] _sampleList;
	private Params _params;
	private Path _sampleListPath;
	private Vector<String> _samplesToRun;
	
	public MainForm() {
		//Basics of the GUI
		super("AutoUpdate CORE Facility Automated Pipeline");
		this.setPreferredSize(new Dimension(455, 525));
		this.setSize(this.getPreferredSize());
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLocation(new Point(50,50));
		this.setVisible(false);
		
		//Main Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setPreferredSize(new Dimension((int)this.getSize().getWidth()-40, (int)this.getSize().getHeight()-40));
		GroupLayout mainLayout = new GroupLayout(mainPanel);
		mainLayout.setAutoCreateGaps(true);
		mainLayout.setAutoCreateContainerGaps(true);
		mainPanel.setLayout(mainLayout);
		
		//Status Area GUI
		_statusArea = new JTextArea("[" + new Date().toString() + "] AutoUpdate initializing...");
		_statusArea.setFont(new java.awt.Font("MONOSPACED", java.awt.Font.PLAIN, 12));
		_statusArea.setEditable(false);
		_statusArea.setPreferredSize(new Dimension((int)mainPanel.getSize().getWidth(), (int)mainPanel.getSize().getHeight()/2-40));
		JScrollPane statusScroll = new JScrollPane(_statusArea);
		
		//Status Panel and Labels
		JPanel statusPanel = new JPanel();
		GroupLayout statusLayout = new GroupLayout(statusPanel);
		statusLayout.setAutoCreateGaps(true);
		statusLayout.setAutoCreateContainerGaps(true);
		statusPanel.setLayout(statusLayout);
		statusPanel.setBorder(BorderFactory.createLineBorder(java.awt.Color.BLACK));
		_sequestLabel = new JLabel("Sequest: Off");
		_comLabel = new JLabel("Communication: Off");
		_fileLabel = new JLabel("File Submission: Off");
		statusLayout.setHorizontalGroup(statusLayout.createSequentialGroup()
				.addComponent(_comLabel)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(_sequestLabel)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(_fileLabel)
		);
		statusLayout.setVerticalGroup(statusLayout.createParallelGroup()
				.addComponent(_comLabel)
				.addComponent(_sequestLabel)
				.addComponent(_fileLabel)
		);
		
		
		//Queue Table
		_queue = new JTable();
		JScrollPane queueScroll = new JScrollPane(_queue);
		queueScroll.setPreferredSize(new Dimension((int)mainPanel.getSize().getWidth(), (int)(mainPanel.getSize().getHeight())/2-40));
		_queue.setFillsViewportHeight(true);
		
		//Bottom Button Panel and Buttons
		JButton openButton = new JButton("Open Sample List");
		openButton.addActionListener(new OpenListener());
		JButton addButton = new JButton("Add New Sample");
		//TODO Finish the add new sample feature
//		addButton.addActionListener(new AddSampleListener());
		addButton.setEnabled(false);
		JButton rerunButton = new JButton("Rerun Sample");
		rerunButton.setToolTipText("Remember: Delete the sample from FileMaker");
		rerunButton.addActionListener(new RerunSampleListener());
		
		//Add Components to the mainPanel
		mainLayout.setHorizontalGroup(mainLayout.createParallelGroup()
				.addComponent(statusScroll)
				.addComponent(statusPanel)
				.addComponent(queueScroll)
				.addGroup(mainLayout.createSequentialGroup()
						.addComponent(openButton)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(addButton)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(rerunButton)
				)
		);
		mainLayout.setVerticalGroup(mainLayout.createSequentialGroup()
				.addComponent(statusScroll)
				.addComponent(statusPanel)
				.addComponent(queueScroll)
				.addGroup(mainLayout.createParallelGroup()
						.addComponent(openButton)
						.addComponent(addButton)
						.addComponent(rerunButton)
				)
		);
		this.add(mainPanel);
		this.pack();
		
		//Get & Set the _appPath
		_sep = System.getProperty("file.separator");
		_appPath = System.getProperty("java.class.path");
		//If .jar is launched from the command line in the autoSearch home directory,
		//relative _appPath will not contain a file separator. So check for this case.
		if(_appPath.indexOf(_sep) != -1){
			_appPath = _appPath.substring(0, _appPath.lastIndexOf(_sep));
		} else {
			_appPath = "";
		}
		//Set booleans to starting values
		_isParamSet = false;
		
		//Get everything going...
		this.loadForm();
	}
	
	//==================== ACCESSORS AND MUTATORS ====================\\
	public String getAppPath() {
		return _appPath;
	}
	public void setParamFile(String path) {
		_paramFile = path;
	}
	public String getParamFile() {
		return _paramFile;
	}
	public void setIsParamSet(boolean bool) {
		_isParamSet = bool;
	}
	public boolean isParamSet() {
		return _isParamSet;
	}
	public String getUserName() {
		return "artman";
	}
	
	//==================== METHODS ====================\\
	
	/**
	 * This method logs a message onto the status panel.
	 * It prepends the date and time to the message.
	 * 
	 * @param String message
	 */
	public void log(String message) {
		_statusArea.append("["+new Date().toString()+"] " + message + "\n");
	}
	
	/**
	 * This method loads the necessary info into the application.
	 */
	public void loadForm() {
		new SelectConfig().setConfig(this);
		while (!this.isParamSet()) {
			continue;
		}
		this.setVisible(true);
		
		//Load parameters
		_params = new Params(this);
		boolean paramsLoaded = _params.loadParam(this.getParamFile());

		_params.setCurrentClientIdentity(_params.getIdentity());
		
		//Reset title of the frame
		this.setTitle("AutoUpdate [" + _paramFile + "]");
		
		//Set up the sample list and find the finished sample list
		_samplesToRun = new Vector<String>();
		if (paramsLoaded) {
			//Find the finished sample list
			_sampleListPath = Paths.get(this.getAppPath(), _params.getClientParam("SAMPLE_LIST", "N/A"));
			if (Files.notExists(_sampleListPath)) {
				_sampleListPath = Paths.get(this.getAppPath(), "finishedSampleList.txt");
				if (Files.notExists(_sampleListPath)) {
					boolean sampleListFound = false;
					JOptionPane.showMessageDialog(this, "Cannot find the finished sample list.\nPlease place the finished sample list in the\ncurrent application directory with the filename\n'finishedSampleList.txt'.\n\nPress OK when ready.", "File Not Found", JOptionPane.WARNING_MESSAGE);
					while(!sampleListFound) {
						if (Files.exists(_sampleListPath)) {
							sampleListFound = true;
						}
					}
				}
			}
		}
		
		this.buildSampleList(); //Read finished sample list and build table
		
		_statusArea.append("done!\n");
		this.log("Sequest Host: " + _params.getGlobalParam("SEQUEST", "localhost"));
		while (true) {
			if (_samplesToRun.size() > 0) {
				this.sendToSequest();
			}
			else {
				this.checkSampleListFile();
				if (_samplesToRun.size() > 0) {
					this.sendToSequest();
				}
			}
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method reads in the finished sample list,
	 * determines which samples need to be run
	 * (either new samples or through error), and
	 * adds them to the list of samples.
	 * 
	 * It also creates the data structure underlying
	 * the visual table representation of the samples.
	 * 
	 */
	public void buildSampleList() {
		try {
			_samplesToRun.clear();
			List<String> samples = Files.readAllLines(_sampleListPath, StandardCharsets.UTF_8);
			_sampleList = new String[samples.size()][2];
			for (int i=0; i<samples.size(); i++) {
				String line = samples.get(i);
				line = line.trim();
				if (!line.equals("") && !line.equals(null)) {
					//For error samples
					if (line.startsWith("##")) {
						_sampleList[i][0] = "Waiting to run...";
						_samplesToRun.add(line.substring(2));
					}
					//For new samples
					else if (!line.startsWith("#")) {
						_sampleList[i][0] = "Waiting to run...";
						_samplesToRun.add(line);
					} else {
						_sampleList[i][0] = "Done";
					}
					_sampleList[i][1] = line.substring(line.lastIndexOf("\\")+1, line.indexOf("+"));
				}
			}
			//Reverse table list for display purposes only
			for (int i=0; i<_sampleList.length/2; i++) {
				String[] tmp = _sampleList[i];
				_sampleList[i] = _sampleList[_sampleList.length-1-i];
				_sampleList[_sampleList.length-1-i] = tmp;
			}
			String[] columnNames = {"Status", "Sample Name"};
			_queue.setModel(new DefaultTableModel(_sampleList, columnNames));
			_queue.getColumnModel().getColumn(0).setPreferredWidth(10);
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Cannot read the finished sample list.\nPlease check the file to make sure it is not corrupt.", "Unable to read file", JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}
	}
	
	/**
	 * This method sends the data to AutoSearch.
	 * It first creates a socket and connects to AutoSearch,
	 * makes sure AutoSearch is ready,
	 * and sends the files of the current sample.
	 * 
	 * The socket is a one-time connection that is recreated
	 * whenever a connection to AutoSearch is needed.
	 * 
	 * If there is an error in connection, an email is sent
	 * to make resolve the connection issue.
	 */
	private void sendToSequest() {
		Socket sock = null;
		ObjectOutputStream out = null;
		BufferedReader in = null;
		String fileToSend = _samplesToRun.get(0);
		System.out.println("File to send:" + fileToSend);
		int errorCount = 0;
		try {
			//Connect the socket to AutoSearch and open I/O
			sock = new Socket(_params.getGlobalParam("SEQUEST", "sequest.biomed.brown.edu"), Integer.parseInt(_params.getGlobalParam("SEQUEST.Port", "666")));
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			System.out.println("Sending 'Ready?' to Sequest");
			out.writeUTF("Ready?"+_params.getIdentity());
			out.flush();
			_comLabel.setText("Communication: ON");
			String message;
			System.out.println("I'm checking for a message....");
			while ((message = in.readLine()) != null) {
				System.out.println("message from autosearch:     " + message);
				//Send file if AutoSearch is ready...
				if (message.equalsIgnoreCase("ready")) {
					_sequestLabel.setText("Sequest: Off");
					_fileLabel.setText("File Submission: ON");
					this.log("Sending sample: " + fileToSend.substring(fileToSend.lastIndexOf("\\")+1, fileToSend.indexOf("+")));
					//Send files to AutoSearch
					String sequestSend = fileToSend.substring(0, fileToSend.indexOf("+"));
					sequestSend = sequestSend.substring(0, sequestSend.lastIndexOf(":"));
					while(!this.pushFiles(sequestSend, out) && errorCount<20){
						if (errorCount++ ==20) {
							throw(new IOException());
						}
						continue;
					}
					out.writeUTF(_params.getIdentity()+"+"+fileToSend);
					out.flush();
					//Set the value of the sample in the table to "Running"
					for (int i=0; i<_sampleList.length; i++) {
						if (_sampleList[i][1].equals(fileToSend.substring(fileToSend.lastIndexOf("\\")+1, fileToSend.indexOf("+")))) {
							if (_sampleList[i][0].startsWith("Waiting")) {
								_queue.setValueAt("Running", i, 0);
								break;
							}
						}
					}
				}
				//AutoSearch is currently running. Check again later.
				else if (message.equalsIgnoreCase("busy")) {
					out.close();
					in.close();
					sock.close();
					_comLabel.setText("Communication: Off");
					_sequestLabel.setText("Sequest: ON");
				}
				//AutoSearch has received the sample and will
				//now be running a search.
				else if (message.equalsIgnoreCase("received")) {
					out.close();
					in.close();
					sock.close();
					this.log("Sample received.");
					_comLabel.setText("Communication: Off");
					_sequestLabel.setText("Sequest: ON");
					_fileLabel.setText("File Submission: Off");
					for (int i=0; i<_sampleList.length; i++) {
						if (_sampleList[i][1].equals(fileToSend.substring(fileToSend.lastIndexOf("\\")+1, fileToSend.indexOf("+")))) {
							if (_sampleList[i][0].startsWith("Run")) {
								System.out.println("Found the running sample. Time to change...");
								_queue.setValueAt("Sample Received", i, 0);
								break;
							}
						}
					}
					_samplesToRun.remove(0);
					this.updateSampleListFile(fileToSend, "done");
					break;
				}	
			}
			this.checkSampleListFile();
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			//Close all I/O
			try {
				out.close();
				in.close();
				sock.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (NullPointerException e1) {
				e1.printStackTrace();
			}
			_comLabel.setText("Communication: Error");
			_fileLabel.setText("File Submission: Off");
			this.updateSampleListFile(fileToSend, "error");
			//_samplesToRun.add(_samplesToRun.remove(0));
			this.checkSampleListFile();
			//Send an email if cannot connect
			//TODO Uncomment the mail stuff when ready.
//			if (errorCount++ >= 20) {
//				new Mail(this, _params, "error:connect:sequest").send();
//			}
		}
	}
	
	/**
	 * This method pushes files over the socket connection
	 * to AutoSearch.
	 * 
	 * It first sends the directory to which the files are
	 * to be written, then the number of files.
	 * 
	 * For each file, it sends the file name and size, and
	 * finally, the file itself in packets of 4092 bytes.
	 * 
	 * Returns true upon completion.
	 * 
	 * @param file
	 * @param out
	 * @return
	 */
	private boolean pushFiles(String file, ObjectOutputStream out) {
		Path baseDir = Paths.get(_params.getClientParam("LOCAL_PATH", ""));
		try {
			Path filePath = baseDir.relativize(Paths.get(file));
			//System.out.println("File to send: "+filePath);
			String extn = null;
			//Path on AutoSearch where files should be written
			Path sequestPath = Paths.get(_params.getIdentity(), filePath.toString());
			out.writeUTF(sequestPath.toString());
			out.flush();
			//Obtain a list of all the files associated with the sample
			DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(file.substring(0, file.lastIndexOf(_sep))), file.substring(file.lastIndexOf(_sep)+1)+".*");
			Iterator<Path> fileIter = files.iterator();
			Vector<Path> fileVect = new Vector<Path>();
			//Count the number of files to be sent
			int i = 0;
			for ( ; fileIter.hasNext(); ++i) fileVect.add(fileIter.next());
			out.writeInt(i);
			out.flush();
			//For each file
			for (Path fromFile:fileVect) {
				extn = fromFile.toString().substring(fromFile.toString().lastIndexOf("."));
				//Send the file name
				out.writeUTF(fromFile.toString().substring((fromFile.toString().lastIndexOf(_sep)+1)));
				out.flush();
				//Send the file size
				out.writeLong(Files.size(fromFile));
				out.flush();
				//Send the file
				byte[] buffer = new byte[4092];
				int read;
				InputStream fileIn = Files.newInputStream(fromFile);
				while ((read = fileIn.read(buffer))>0) {
					out.write(buffer, 0, read);
					out.flush();
				}
				fileIn.close();
			}
			return true;
		} catch (AccessDeniedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "AutoUpdate does not have access to the data.", 
					"Access Denied", JOptionPane.WARNING_MESSAGE);
			return false;
		} catch (FileNotFoundException e) {
			System.err.println("File not found. Did you doublecheck the path to the data AND the encoding of the Finished Sample List (Must be UTF-8 WITHOUT BOM)?");
			return false;
		} catch (IOException e) {
			System.err.println("Error uploading file to Sequest");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Updates the finishedSampleList file to reflect either the
	 * success or error state of the sample that was just run.
	 * 
	 * Prepends a single hash if the sample was successfully sent,
	 * prepends a double hash otherwise.
	 * 
	 * @param sample - Name of the sample that was just run
	 * @param status - Success or error status
	 */
	public void updateSampleListFile(String sample, String status) {
		try {
			List<String> samples = Files.readAllLines(_sampleListPath, StandardCharsets.UTF_8);
			int i;
			if ((i = samples.lastIndexOf(sample)) >= 0) {
				if (status.equals("done")) {
					samples.set(i, "#"+samples.get(i));
				}
				else if (status.equals("error")) {
					//samples.set(i, "##"+samples.get(i));	// 130909 jmb removed double hashing
					samples.set(i, samples.get(i));
				}
			}
			else if ((i = samples.lastIndexOf("##"+sample)) >= 0) {
				if (status.equals("done")) {
					samples.set(i, samples.get(i).substring(1));
				}
			}
			Files.write(_sampleListPath, samples, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks the finishedSampleList to see if there are any
	 * new samples that need to be run. Because error samples
	 * are never removed from the queue, they are not checked
	 * for in this method.
	 */
	public void checkSampleListFile() {
		try {
			System.out.println("Checking sample list file...");
			List<String> samples = Files.readAllLines(_sampleListPath, StandardCharsets.UTF_8);
			Vector<String> toCheck = new Vector<String>();
			for (int i=0; i<samples.size(); i++) {
				if (samples.get(i).startsWith("##")) {
					toCheck.add(samples.get(i).substring(2));
				}
				else if (!samples.get(i).startsWith("#")) {
					toCheck.add(samples.get(i));
				}
			}
			if (toCheck.size() != _samplesToRun.size()) {
				this.buildSampleList();
			}
			else if (!_samplesToRun.containsAll(toCheck)) {
				this.buildSampleList();		
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This class defines the functionality for the
	 * Open Sample List button (openButton). It opens
	 * the finishedSampleList file in the operating 
	 * system's default text editing application.
	 * 
	 * @author njooma
	 *
	 */
	private class OpenListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String os = System.getProperty("os.name");
			try {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().edit(_sampleListPath.toFile());
	            }
				else if (os.startsWith("Windows")) {
					Runtime.getRuntime().exec(new String[] {"rundll32 url.dll,FileProtocolHandler", _sampleListPath.toString()});
				}
				else if (os.toLowerCase().contains("mac") || os.toLowerCase().contains("nux") || os.toLowerCase().contains("nix")) {
					Runtime.getRuntime().exec(new String[]{"/usr/bin/open", _sampleListPath.toString()});
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * This class performs the action of the Rerun Button.
	 * It gets the selected rows from the queue and adds
	 * them to the sample list so that they will be run again.
	 * 
	 * @author njooma
	 *
	 */
	private class RerunSampleListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				List<String> samples = Files.readAllLines(_sampleListPath, StandardCharsets.UTF_8);
				//Find the selected sample
				int[] rows = _queue.getSelectedRows();
				for (int i=0; i<rows.length; i++) {
					for (int j=0; j<samples.size(); j++) {
						String sample = samples.get(j);
						String toRerun = (String) _queue.getValueAt(rows[i], 1); 
						//If sample is the same as that in the finished sample list
						if (toRerun.equals(sample.substring(sample.lastIndexOf("\\")+1, sample.indexOf("+")))) {
							//And if the sample is not going to be run later...
							if (sample.startsWith("#") && !sample.startsWith("##")) {
								//Get the user to rename the sample before rerunning it
								String sampleRename = JOptionPane.showInputDialog(null, "Please rename the sample", toRerun.substring(0, toRerun.lastIndexOf(":")));
								//Copy the current sample files to reflect the new name
								Path dataDir = Paths.get(sample.substring(1, sample.split("\\+")[0].lastIndexOf(_sep)));
								DirectoryStream<Path> files = Files.newDirectoryStream(dataDir, toRerun.substring(0, toRerun.lastIndexOf(":"))+".*");
								for (Path file:files) {
									String extn = file.toString().substring(file.toString().lastIndexOf("."));
									Files.copy(Paths.get(file.toString()), Paths.get(dataDir.toString(), sampleRename+extn), StandardCopyOption.REPLACE_EXISTING);
								}
								//Add the sample to the sample list
								samples.add(samples.size(), sample.replaceAll(toRerun.substring(0, toRerun.lastIndexOf(":")), sampleRename).substring(1));
								break;
							}
						}
					}
				}
				//Write the current sample list to the finished sample list
				Files.write(_sampleListPath, samples, StandardCharsets.UTF_8);
				buildSampleList();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}