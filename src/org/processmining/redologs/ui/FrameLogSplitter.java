package org.processmining.redologs.ui;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.processmining.redologs.common.Column;
import org.processmining.redologs.common.DataModel;
import org.processmining.redologs.common.EventAttributeColumn;
import org.processmining.redologs.common.GraphNode;
import org.processmining.redologs.common.Key;
import org.processmining.redologs.common.LogTraceSplitter;
import org.processmining.redologs.common.TableInfo;
import org.processmining.redologs.oracle.OracleLogMinerExtractor;
import org.processmining.redologs.oracle.OracleRelationsExplorer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import javax.swing.BoxLayout;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.JProgressBar;

import java.awt.Component;

import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.EtchedBorder;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class FrameLogSplitter extends CustomInternalFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7078418810402851925L;
	
	//private static FrameLogSplitter _instance;
	private JTree tree;
	private DefaultTreeModel tree_model;
	private DefaultMutableTreeNode root;
	private DataModel model = null;
	private JTextField textFieldTimestamp;
	private JTextField textFieldSort;
	private JList<Object> listActivityNameFields;
	private JList<Object> listTraceIdFields;
	
	private Object timeStampSelected = null;
	private Object sortSelected = null;
	private List<Object> activityNamesSelected = new Vector<>();
	private List<Object> traceIdNamesSelected = new Vector<>();

	private List<GraphNode> listTraceIdNodes = new Vector<>();
	
//	public static FrameLogSplitter getInstance() {
//		if (_instance == null) {
//			_instance = new FrameLogSplitter();
//		}
//		return _instance;
//	}
	
	private void setDataModel(DataModel model) {
		if (model != this.model) {
			textFieldTimestamp.setText("");
			textFieldSort.setText("");
			DefaultListModel listModel = (DefaultListModel) listActivityNameFields.getModel();
			listModel.removeAllElements();
			DefaultListModel list2Model = (DefaultListModel) listTraceIdFields.getModel();
			list2Model.removeAllElements();
			timeStampSelected = null;
			sortSelected = null;
			traceIdNamesSelected = new Vector<>();
			activityNamesSelected = new Vector<>();
		}
		this.model = model;
		String title = "Log Splitter - Data Model: "+model.getName();
		root.setUserObject("Fields: "+model.getName());
		root.removeAllChildren();
		this.setTitle(title);
		
		/**/
		
		EventAttributeColumn ec = new EventAttributeColumn();
		ec.c = new Column();
		ec.c.name = OracleLogMinerExtractor.COLUMN_CHANGES;
		DefaultMutableTreeNode nodeC = new DefaultMutableTreeNode(ec);
		root.add(nodeC);
		
		for (String c: OracleLogMinerExtractor.LOG_MINER_BASIC_FIELDS) {
			EventAttributeColumn eac = new EventAttributeColumn();
			eac.c = new Column();
			eac.c.name = c;
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(eac);
			root.add(node);
		}
		
		/**/
		
		DefaultMutableTreeNode tablesFolderNode = new DefaultMutableTreeNode("Tables");
		root.add(tablesFolderNode);
		for (TableInfo t: model.getTables()) {
			DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(t);
			tablesFolderNode.add(tableNode);
			
			if (!model.getKeysPerTable(t).isEmpty()) {
				DefaultMutableTreeNode keysNode = new DefaultMutableTreeNode("Keys");
				tableNode.add(keysNode);
				for (Key k: model.getKeysPerTable(t)) {
					keysNode.add(new DefaultMutableTreeNode(k));
				}
			}
			
			for (Column c: t.columns) {
//				EventAttributeColumn ecN = new EventAttributeColumn();
//				ecN.c = new Column();
//				ecN.c.name = c;
//				ecN.c.table = t;
//				ecN.type = EventAttributeColumn.VALUE_NEW;
//				
//				EventAttributeColumn ecO = new EventAttributeColumn();
//				ecO.c = new Column();
//				ecO.c.name = c;
//				ecO.c.table = t;
//				ecO.type = EventAttributeColumn.VALUE_OLD;				
//				
//				tableNode.add(new DefaultMutableTreeNode(ecN));
//				tableNode.add(new DefaultMutableTreeNode(ecO));
				
				EventAttributeColumn ect = new EventAttributeColumn();
				ect.c = c;
				ect.type = 0;
				
				tableNode.add(new DefaultMutableTreeNode(ect));
			}
			
		}
		tree.repaint();
	}
	
//	private DataModel getSelectedDataModel() {
//		Object selected = tree.getLastSelectedPathComponent();
//		
//		if (selected != null) {
//			if (selected instanceof DefaultMutableTreeNode) {
//				Object selectedObj = ((DefaultMutableTreeNode) selected).getUserObject();
//				if (selectedObj instanceof DataModel) {
//					return (DataModel) selectedObj;
//				}
//			}
//		}
//		return null;
//	}
	
	
	
	
	private static class TransferableDataModelElement implements Transferable {

		private Object data;
		private DataFlavor dataFlavor;
		public final static DataFlavor[] flavors = new DataFlavor[] {
				new DataFlavor(Key.class, "redolog-inspector-key"),
				new DataFlavor(EventAttributeColumn.class, "redolog-inspector-EventAttributeColumn")};
		
		public TransferableDataModelElement(Object data) {
			super();
			this.data = data;
			boolean found = false;
			int i = 0;
			while (!found && i < flavors.length) {
				if (flavors[i].getDefaultRepresentationClass().isInstance(data)) {
					dataFlavor = flavors[i];
					found = true;
				}
				i++;
			}
			if (!found) {
				dataFlavor = new DataFlavor(data.getClass(), data.getClass().toString());
			}
		}
		
		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {dataFlavor};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return (flavor.getRepresentationClass().equals(dataFlavor.getRepresentationClass()));
		}

		@Override
		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			return data;
		}
		
	}
	
	private class TreeTransferHandler extends TransferHandler {
		
		@Override
		public int getSourceActions(JComponent c) {
			return TransferHandler.COPY;
		}
		
		@Override
		protected Transferable createTransferable(JComponent c) {
			JTree tree = (JTree) c;
			
			Object selected = tree.getLastSelectedPathComponent();
			
			if (selected != null) {
				if (selected instanceof DefaultMutableTreeNode) {
					Object selectedObj = ((DefaultMutableTreeNode) selected).getUserObject();
					return new TransferableDataModelElement(selectedObj);
				}
			}
			
			return null;
		}
	}
	
	private class ListTransferHandler extends TransferHandler {
		
		public static final int TRACEID_HANDLER = 1;
		public static final int ACTIVITY_HANDLER = 2;
		
		private int type = 0;
		
		public ListTransferHandler(int type) {
			this.type = type;
		}
		
		@Override
		public boolean canImport(TransferSupport info) {
			// Check for flavor
	        if (info.isDataFlavorSupported(new DataFlavor(Key.class, "")) ||
	        		info.isDataFlavorSupported(new DataFlavor(EventAttributeColumn.class, ""))) {
	            return true;
	        }
	        return false;
	   }
		
		@Override
		public boolean importData(TransferSupport info) {
			//return super.importData(support);
			if (!info.isDrop()) {
				return false;
			}
			JList<Object> list = (JList<Object>)info.getComponent();
			
			Transferable t = info.getTransferable();
	        Object data;
	        try {
	            data = t.getTransferData(new DataFlavor(Object.class,""));
	            DefaultListModel<Object> listModel = (DefaultListModel<Object>)list.getModel();
		        listModel.addElement(data);
		        if (type == TRACEID_HANDLER) {
		        	traceIdNamesSelected.add(data);
		        	if (listTraceIdNodes != null) {
		        		if (data instanceof GraphNode) {
		        			listTraceIdNodes.add((GraphNode) data);
		        		} else if (data instanceof EventAttributeColumn) {
		        			listTraceIdNodes.add(((EventAttributeColumn) data).c);
		        		}
		        	}
		        } else if (type == ACTIVITY_HANDLER) {
		        	activityNamesSelected.add(data);
		        }
		        return true;
	        } catch (Exception e) {
	        	return false;
	        }
		}
	}
	
	private class TextFieldTransferHandler extends TransferHandler {
		@Override
		public boolean canImport(TransferSupport info) {
			// Check for flavor
			if (info.isDataFlavorSupported(new DataFlavor(Key.class, "")) ||
	        		info.isDataFlavorSupported(new DataFlavor(EventAttributeColumn.class, ""))) {
	            return true;
	        }
	        return false;
	    }
		
		public void setData(Object data) {
			
		}
		
		@Override
		public boolean importData(TransferSupport info) {
			if (!info.isDrop()) {
				return false;
			}
			JTextField field = (JTextField) info.getComponent();
			
			Transferable t = info.getTransferable();
	        Object data;
	        try {
	            data = (Object)t.getTransferData(new DataFlavor(Object.class,""));
	            setData(data);
	        } 
	        catch (Exception e) { return false; }
			
	        field.setText(data.toString()); //FIXME
			return true;
		}
	}
	
	public FrameLogSplitter() {
		super("Log Splitter");
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		setBounds(350, 280, 783, 711);
		setResizable(true);
		setMaximizable(true);
		setIconifiable(true);
		
		tree = new JTree();
		root = new DefaultMutableTreeNode("Fields");
		tree_model = new DefaultTreeModel(root);
		tree.setModel(tree_model);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setTransferHandler(new TreeTransferHandler());
		tree.setDragEnabled(true);
		
		JScrollPane scrollPane = new JScrollPane(tree);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1);
		
		JButton btnLoad = new JButton("Load Data Model");
		panel_1.add(btnLoad);
		
		JButton btnSplitLog = new JButton("Split Log");
		panel_1.add(btnSplitLog);
		
		JButton btnVisualizeDataModel = new JButton("Visualize Data Model");
		btnVisualizeDataModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (model != null) {
					FrameRelationsGraph relations = new FrameRelationsGraph(true,FrameLogSplitter.this);
					RedoLogInspector.getInstance().addFrame(relations);
					relations.setVisible(true);
					relations.setFocusable(true);
					relations.reloadGraph(model);
				}
			}
		});
		panel_1.add(btnVisualizeDataModel);
		
		JPanel panel_2 = new JPanel();
		panel.add(panel_2);
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		
		final JProgressBar progressBar = new JProgressBar();
		progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel_2.add(progressBar);
		
		
		btnSplitLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String aux = null;
				
				if (timeStampSelected != null) {
					if (timeStampSelected instanceof Key) {
						
					} else if (timeStampSelected instanceof EventAttributeColumn) {
						//aux = timeStampSelected.
					} else {
						aux = timeStampSelected.toString();
					}
				}
				
				final String timestampFieldName = textFieldTimestamp.getText();
				final String sortFieldName = textFieldSort.getText();
				final List<Object> traceIdFields = traceIdNamesSelected;
				DefaultListModel<Object> listActivityModel = (DefaultListModel<Object>) listActivityNameFields.getModel();
				final String[] activityFieldNames = new String[listActivityModel.getSize()];
				
				for (int i = 0; i < listActivityModel.getSize(); i++) {
					activityFieldNames[i] = (String) listActivityModel.get(i);
				}
				
				AskNameDialog askDiag = new AskNameDialog(FrameLogSplitter.this);
				final String outputLogName = askDiag.showDialog();
				
				final File logFile = FrameLogs.getInstance().getFileFromSelector();
				final File splittedLogFile = new File(System.currentTimeMillis()+"-splitted-"+outputLogName);
				
				Thread logSplitThread = new Thread(new Runnable() {
					@Override
					public void run() {
						progressBar.setIndeterminate(true);
						progressBar.setStringPainted(true);
						progressBar.setString("Splitting...");
						//LogTraceSplitter.splitLog(logFile, model, traceIdFieldName, sortFieldName, timestampFieldName, activityFieldNames, splittedLogFile); // FIXME
						FrameLogs.getInstance().addLog(splittedLogFile.getName(), splittedLogFile);
						progressBar.setIndeterminate(false);
						progressBar.setString("Log Splitted");
					}
				});
				
				logSplitThread.start();
			}
		});
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DataModel model = FrameDataModels.getInstance().getSelectedDataModel();
				if (model != null) {
					FrameLogSplitter.this.setDataModel(model);
				}
			}
		});
		
		JPanel panel_3 = new JPanel();
		getContentPane().add(panel_3, BorderLayout.EAST);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_side = new JPanel();
		panel_3.add(panel_side);
		GridBagLayout gbl_panel_side = new GridBagLayout();
		gbl_panel_side.columnWidths = new int[] {100, 200};
		gbl_panel_side.rowHeights = new int[] {40, 40, 40, 40, 40, 0};
		gbl_panel_side.columnWeights = new double[]{1.0, 1.0};
		gbl_panel_side.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, Double.MIN_VALUE};
		panel_side.setLayout(gbl_panel_side);
		
		JLabel lblNewLabel = new JLabel("Timestamp");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel.insets = new Insets(0, 5, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		panel_side.add(lblNewLabel, gbc_lblNewLabel);
		
		textFieldTimestamp = new JTextField();
		textFieldTimestamp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				textFieldTimestamp.setText("");
				timeStampSelected = null;
			}
		});
		textFieldTimestamp.setDropMode(DropMode.INSERT);
		textFieldTimestamp.setTransferHandler(new TextFieldTransferHandler() {
			@Override
			public void setData(Object data) {
				timeStampSelected = data;
			}
		});
		textFieldTimestamp.setEditable(false);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		panel_side.add(textFieldTimestamp, gbc_textField);
		textFieldTimestamp.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Order");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_1.insets = new Insets(0, 5, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		panel_side.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		textFieldSort = new JTextField();
		textFieldSort.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				textFieldSort.setText("");
				sortSelected = null;
			}
		});
		textFieldSort.setDropMode(DropMode.INSERT);
		textFieldSort.setTransferHandler(new TextFieldTransferHandler() {
			@Override
			public void setData(Object data) {
				sortSelected = data;
			}
		});
		textFieldSort.setEditable(false);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.insets = new Insets(0, 0, 5, 0);
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 1;
		panel_side.add(textFieldSort, gbc_textField_1);
		textFieldSort.setColumns(10);
		
		JLabel lblActivityNames = new JLabel("Activity Names");
		GridBagConstraints gbc_lblActivityNames = new GridBagConstraints();
		gbc_lblActivityNames.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblActivityNames.insets = new Insets(0, 5, 5, 5);
		gbc_lblActivityNames.gridx = 0;
		gbc_lblActivityNames.gridy = 2;
		panel_side.add(lblActivityNames, gbc_lblActivityNames);
		GridBagConstraints gbc_listActivityNameFields = new GridBagConstraints();
		gbc_listActivityNameFields.insets = new Insets(0, 0, 5, 0);
		gbc_listActivityNameFields.gridx = 1;
		gbc_listActivityNameFields.gridy = 2;
		
		listActivityNameFields = new JList<>();
		
		listActivityNameFields.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_DELETE) {
					int selected = listActivityNameFields.getSelectedIndex();
					if (selected > -1) {
						DefaultListModel model = (DefaultListModel) listActivityNameFields.getModel();
						Object selectedObj = model.get(selected);
						activityNamesSelected.remove(selectedObj);
						model.remove(selected);
					}
				}
			}
		});
		listActivityNameFields.setModel(new DefaultListModel<>());
		listActivityNameFields.setDropMode(DropMode.INSERT);
		listActivityNameFields.setTransferHandler(new ListTransferHandler(ListTransferHandler.ACTIVITY_HANDLER));
		
		JScrollPane scrollPane_1 = new JScrollPane(listActivityNameFields);
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 1;
		gbc_scrollPane_1.gridy = 2;
		panel_side.add(scrollPane_1, gbc_scrollPane_1);
		
		JLabel lblNewLabel_2 = new JLabel("TraceId");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_2.insets = new Insets(0, 5, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 3;
		panel_side.add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		listTraceIdFields = new JList<>();
		listTraceIdFields.setModel(new DefaultListModel<>());
		listTraceIdFields.setDropMode(DropMode.INSERT);
		listTraceIdFields.setTransferHandler(new ListTransferHandler(ListTransferHandler.TRACEID_HANDLER));
		listTraceIdFields.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_DELETE) {
					int selected = listTraceIdFields.getSelectedIndex();
					if (selected > -1) {
						DefaultListModel model = (DefaultListModel) listTraceIdFields.getModel();
						Object selectedObj = model.get(selected);
						traceIdNamesSelected.remove(selectedObj);
						if (listTraceIdNodes != null) {
							listTraceIdNodes.remove(selectedObj);
						}
						model.remove(selected);
					}
				}
			}
		});
		
		JScrollPane scrollPane_2 = new JScrollPane(listTraceIdFields);
		GridBagConstraints gbc_textFieldTraceId = new GridBagConstraints();
		gbc_textFieldTraceId.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldTraceId.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldTraceId.gridx = 1;
		gbc_textFieldTraceId.gridy = 3;
		panel_side.add(scrollPane_2, gbc_textFieldTraceId);		
		
	}

	public void setTraceIdHierarchy(List<GraphNode> listSelectedNodes) {
		DefaultListModel model = (DefaultListModel) listTraceIdFields.getModel();
		model.removeAllElements();
		listTraceIdNodes = new Vector<>();
		for (GraphNode g: listSelectedNodes) {
			if (g instanceof Key) {
				model.addElement(g);
			} else {
				model.addElement(g);
			}
			listTraceIdNodes.add(g);
		}
	}

	public List<GraphNode> getTraceIdHierarchy() {
		return listTraceIdNodes;
	}	
}