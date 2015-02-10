package org.processmining.redologs.ui;

import javax.swing.JScrollPane;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.processmining.openslex.SLEXEventCollection;
import org.processmining.openslex.SLEXPerspective;
import org.processmining.openslex.SLEXStorage;
import org.processmining.openslex.SLEXStorageCollection;
import org.processmining.openslex.SLEXStorageImpl;
import org.processmining.redologs.common.Column;
import org.processmining.redologs.common.DataModel;
import org.processmining.redologs.common.EventAttributeColumn;
import org.processmining.redologs.common.GraphNode;
import org.processmining.redologs.common.Key;
import org.processmining.redologs.common.LogTraceSplitter;
import org.processmining.redologs.common.ProgressHandler;
import org.processmining.redologs.common.SLEXAttributeMapper;
import org.processmining.redologs.common.TableInfo;
import org.processmining.redologs.common.TraceIDPattern;
import org.processmining.redologs.oracle.OracleLogMinerExtractor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.JProgressBar;

import java.awt.Component;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.ListSelectionModel;

public class FrameLogSplitter extends CustomInternalFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7078418810402851925L;
	
	private JTree tree;
	private DefaultTreeModel tree_model;
	private DefaultMutableTreeNode root;
	private DataModel model = null;
	private JList<Object> listSortField;
	private JList<Object> listTraceIdFields;
	
	private List<Object> orderNamesSelected = new  ArrayList<>();
	private List<Object> traceIdNamesSelected = new  ArrayList<>();

	private List<GraphNode> listTraceIdNodes = new  ArrayList<>();
	
	private Object selectedRoot = null;
	private JTextField txtRootelement;
	private TableInfo common_table = null;
	private JLabel processedEventsLabel;
	private JLabel generatedTracesLabel;
	//private JLabel dagTasksLabel;
	private JLabel removedTracesLabel;
	private HistogramPanel histogramPanel;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
	
	private JLabel lblStartdatevalue = null;
	private JLabel lblEnddatevalue = null;
	private SLEXEventCollection eventCollection;
	private SLEXStorageCollection storage;
	
	private void setDataModel(DataModel model) {
		if (model != this.model) {
			DefaultListModel list2Model = (DefaultListModel) listTraceIdFields.getModel();
			list2Model.removeAllElements();
			DefaultListModel list3Model = (DefaultListModel) listSortField.getModel();
			list3Model.removeAllElements();
			traceIdNamesSelected = new Vector<>();
			orderNamesSelected = new Vector<>();
		}
		this.model = model;
		root.setUserObject("Fields: "+model.getName());
		root.removeAllChildren();
		
		/**/
		common_table = new TableInfo();
		common_table.columns = new Vector<>();
		common_table.name = "COMMON";
		
		EventAttributeColumn ec = new EventAttributeColumn();
		ec.c = new Column();
		ec.c.name = OracleLogMinerExtractor.COLUMN_CHANGES;
		ec.c.table = common_table;
		common_table.columns.add(ec.c);
		DefaultMutableTreeNode nodeC = new DefaultMutableTreeNode(ec);
		root.add(nodeC);
		
		EventAttributeColumn ec2 = new EventAttributeColumn();
		ec2.c = new Column();
		ec2.c.name = OracleLogMinerExtractor.COLUMN_ORDER;
		ec2.c.table = common_table;
		common_table.columns.add(ec2.c);
		DefaultMutableTreeNode nodeC2 = new DefaultMutableTreeNode(ec2);
		root.add(nodeC2);
		
		
		for (String c: OracleLogMinerExtractor.LOG_MINER_BASIC_FIELDS) {
			EventAttributeColumn eac = new EventAttributeColumn();
			eac.c = new Column();
			eac.c.name = c;
			eac.c.table = common_table;
			common_table.columns.add(eac.c);
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
				EventAttributeColumn ect = new EventAttributeColumn();
				ect.c = c;
				ect.type = 0;
				
				tableNode.add(new DefaultMutableTreeNode(ect));
			}
			
		}
		
		//tree.expandPath(tree.getPathForRow(0));
		tree.repaint();
	}	
	
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
		public static final int ORDER_HANDLER = 3;
		
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
		        } else if (type == ORDER_HANDLER) {
		        	orderNamesSelected.add(data);
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
			
	        field.setText(data.toString()); //FIXME proper field name transformation
			return true;
		}
	}
	
	public FrameLogSplitter() {
		super("Log Splitter");
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		setBounds(715, 30, 820, 670);
		setResizable(true);
		setMaximizable(true);
		setIconifiable(true);
		
		tree = new JTree();
		tree.setVisibleRowCount(15);
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
		
		final JButton btnLoad = new JButton("Load Data Model & Event Collection");
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
								
				final List<Column> sortFields = new Vector<>();
				for (int i = 0; i < orderNamesSelected.size(); i++) {
					if (orderNamesSelected.get(i) instanceof EventAttributeColumn) {
						sortFields.add(((EventAttributeColumn) orderNamesSelected.get(i)).c);
					} else if (orderNamesSelected.get(i) instanceof Column) {
						sortFields.add((Column) orderNamesSelected.get(i));
					}
				}
				
				AskNameDialog askDiag = new AskNameDialog(FrameLogSplitter.this);
				final String outputLogName = askDiag.showDialog();
				
				final SLEXEventCollection evCol = eventCollection;
				
				List<TableInfo> tables = new Vector<>();
				tables.addAll(model.getTables());
				tables.add(common_table);
				
				final SLEXAttributeMapper mapper = LogTraceSplitter.computeMapping(evCol, tables);
				final TraceIDPattern tp = new TraceIDPattern(model);
				
				if (selectedRoot instanceof EventAttributeColumn) {
					tp.setRoot(((EventAttributeColumn) selectedRoot).c);
				} else if (selectedRoot instanceof Column) {
					tp.setRoot((Column) selectedRoot);
				} else if (selectedRoot instanceof Key) {
					tp.setRoot((Key) selectedRoot);
				}
				
				for (GraphNode n: getTraceIdHierarchy()) {
					if (n instanceof Column) {
						tp.add((Column) n);
					} else if (n instanceof Key) {
						tp.add((Key) n);
					}
				}
				
				final ProgressHandler progressHandler = new ProgressHandler() {
					public void refreshValue(final String key, final String value) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {

								if (key != null) {
									if (key.compareTo("Traces") == 0) {
										generatedTracesLabel.setText(value);
									} else if (key.compareTo("Events") == 0) {
										processedEventsLabel.setText(value);
									} else if (key.compareTo("RemovedTraces") == 0) {
										removedTracesLabel.setText(value);
									} else if (key.compareTo("DAGTasks") == 0) {
										//dagTasksLabel.setText(value);
									}
								}
							}
						});
					}
				};
				
				final String startDate = lblStartdatevalue.getText();
				final String endDate = lblEnddatevalue.getText();
				
				Thread logSplitThread = new Thread(new Runnable() {
					@Override
					public void run() {
						progressBar.setIndeterminate(true);
						progressBar.setStringPainted(true);
						progressBar.setString("Splitting...");
						
						SLEXPerspective perspective = LogTraceSplitter.splitLog(outputLogName, model, evCol, mapper, tp, sortFields, startDate, endDate, progressHandler);
						
						FramePerspectives.getInstance().addPerspective(perspective);
						
						progressBar.setIndeterminate(false);
						progressBar.setString("Log Split");
					}
				},"LogSplitThread");
				
				logSplitThread.start();
			}
		});
		
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final DataModel model = FrameDataModels.getInstance().getSelectedDataModel();
				final SLEXEventCollection evcol = FrameEventCollections.getInstance().getEventCollectionFromSelector();
				
				if (model != null && evcol != null) {
					Thread loadThread = new Thread(new Runnable() {
						
						@Override
						public void run() {
							
							try {
								storage = new SLEXStorageImpl(evcol.getStorage().getPath(),evcol.getStorage().getFilename(),evcol.getStorage().getType());
							} catch (Exception e1) {
								e1.printStackTrace();
							}
							eventCollection = storage.getEventCollection(evcol.getId());
							progressBar.setStringPainted(true);
							progressBar.setString("Loading...");
							progressBar.setIndeterminate(true);
							String title = "Log Splitter - Event Collection: "+eventCollection.getName()+" - Data Model: "+model.getName();
							FrameLogSplitter.this.setTitle(title);
							FrameLogSplitter.this.setDataModel(model);
							histogramPanel.setData(eventCollection,dateFormat);
							progressBar.setIndeterminate(false);
							progressBar.setString("Loaded");
						}
					});
					loadThread.start();
					
				} else {
					JOptionPane.showMessageDialog(FrameLogSplitter.this, "Data Model or Event Collection not selected");
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
		gbl_panel_side.rowHeights = new int[] {60, 100, 40, 30, 30, 30, 30, 30, 30, 0};
		gbl_panel_side.columnWeights = new double[]{1.0, 1.0};
		gbl_panel_side.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
		panel_side.setLayout(gbl_panel_side);
		
		listSortField = new JList<>();
		listSortField.setVisibleRowCount(3);
		listSortField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_DELETE) {
					int selected = listSortField.getSelectedIndex();
					if (selected > -1) {
						DefaultListModel model = (DefaultListModel) listSortField.getModel();
						Object selectedObj = model.get(selected);
						orderNamesSelected.remove(selectedObj);
						model.remove(selected);
					}
				}
			}
		});
		
		JLabel lblNewLabel_1 = new JLabel("Order");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_1.insets = new Insets(0, 5, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 0;
		panel_side.add(lblNewLabel_1, gbc_lblNewLabel_1);
		listSortField.setModel(new DefaultListModel<>());
		listSortField.setDropMode(DropMode.INSERT);
		listSortField.setTransferHandler(new ListTransferHandler(ListTransferHandler.ORDER_HANDLER));
		
		JScrollPane scrollPane_0 = new JScrollPane(listSortField);
		GridBagConstraints gbc_scrollPane_0 = new GridBagConstraints();
		gbc_scrollPane_0.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_0.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_0.gridx = 1;
		gbc_scrollPane_0.gridy = 0;
		panel_side.add(scrollPane_0, gbc_scrollPane_0);
		GridBagConstraints gbc_listActivityNameFields = new GridBagConstraints();
		gbc_listActivityNameFields.insets = new Insets(0, 0, 5, 0);
		gbc_listActivityNameFields.gridx = 1;
		gbc_listActivityNameFields.gridy = 2;
		
		listTraceIdFields = new JList<>();
		listTraceIdFields.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
		
		JLabel lblNewLabel_2 = new JLabel("TraceId");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_2.insets = new Insets(0, 5, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 1;
		panel_side.add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		JScrollPane scrollPane_2 = new JScrollPane(listTraceIdFields);
		GridBagConstraints gbc_textFieldTraceId = new GridBagConstraints();
		gbc_textFieldTraceId.fill = GridBagConstraints.BOTH;
		gbc_textFieldTraceId.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldTraceId.gridx = 1;
		gbc_textFieldTraceId.gridy = 1;
		panel_side.add(scrollPane_2, gbc_textFieldTraceId);		
		
		JButton btnSelectRootTraceid = new JButton("Select Root");
		btnSelectRootTraceid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedRoot = getSelectedTraceIdElement();
				txtRootelement.setText(selectedRoot.toString());
			}
		});
		GridBagConstraints gbc_btnSelectRootTraceid = new GridBagConstraints();
		gbc_btnSelectRootTraceid.insets = new Insets(0, 0, 5, 5);
		gbc_btnSelectRootTraceid.gridx = 0;
		gbc_btnSelectRootTraceid.gridy = 2;
		panel_side.add(btnSelectRootTraceid, gbc_btnSelectRootTraceid);
		
		txtRootelement = new JTextField();
		txtRootelement.setEditable(false);
		GridBagConstraints gbc_txtRootelement = new GridBagConstraints();
		gbc_txtRootelement.insets = new Insets(0, 0, 5, 0);
		gbc_txtRootelement.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtRootelement.gridx = 1;
		gbc_txtRootelement.gridy = 2;
		panel_side.add(txtRootelement, gbc_txtRootelement);
		txtRootelement.setColumns(10);
		
		JLabel lblProcessedEvents = new JLabel("Processed Events");
		GridBagConstraints gbc_lblProcessedEvents = new GridBagConstraints();
		gbc_lblProcessedEvents.insets = new Insets(0, 0, 5, 5);
		gbc_lblProcessedEvents.gridx = 0;
		gbc_lblProcessedEvents.gridy = 3;
		panel_side.add(lblProcessedEvents, gbc_lblProcessedEvents);
		
		processedEventsLabel = new JLabel("0");
		GridBagConstraints gbc_processedEventsLabel = new GridBagConstraints();
		gbc_processedEventsLabel.insets = new Insets(0, 0, 5, 0);
		gbc_processedEventsLabel.gridx = 1;
		gbc_processedEventsLabel.gridy = 3;
		panel_side.add(processedEventsLabel, gbc_processedEventsLabel);
		
		JLabel lblGeneratedTraces = new JLabel("Generated Traces");
		GridBagConstraints gbc_lblGeneratedTraces = new GridBagConstraints();
		gbc_lblGeneratedTraces.insets = new Insets(0, 0, 5, 5);
		gbc_lblGeneratedTraces.gridx = 0;
		gbc_lblGeneratedTraces.gridy = 4;
		panel_side.add(lblGeneratedTraces, gbc_lblGeneratedTraces);
		
		generatedTracesLabel = new JLabel("0");
		GridBagConstraints gbc_generatedTracesLabel = new GridBagConstraints();
		gbc_generatedTracesLabel.insets = new Insets(0, 0, 5, 0);
		gbc_generatedTracesLabel.gridx = 1;
		gbc_generatedTracesLabel.gridy = 4;
		panel_side.add(generatedTracesLabel, gbc_generatedTracesLabel);
		
		JLabel lblRemovedTraces = new JLabel("Removed Traces");
		GridBagConstraints gbc_lblRemovedTraces = new GridBagConstraints();
		gbc_lblRemovedTraces.insets = new Insets(0, 0, 5, 5);
		gbc_lblRemovedTraces.gridx = 0;
		gbc_lblRemovedTraces.gridy = 5;
		panel_side.add(lblRemovedTraces, gbc_lblRemovedTraces);
		
		removedTracesLabel = new JLabel("0");
		GridBagConstraints gbc_removedTracesLabel = new GridBagConstraints();
		gbc_removedTracesLabel.insets = new Insets(0, 0, 5, 0);
		gbc_removedTracesLabel.gridx = 1;
		gbc_removedTracesLabel.gridy = 5;
		panel_side.add(removedTracesLabel, gbc_removedTracesLabel);
		
//		JLabel lbldagTasks = new JLabel("DAG Tasks");
//		GridBagConstraints gbc_lbldagTasks = new GridBagConstraints();
//		gbc_lbldagTasks.insets = new Insets(0, 0, 5, 5);
//		gbc_lbldagTasks.gridx = 0;
//		gbc_lbldagTasks.gridy = 5;
//		panel_side.add(lbldagTasks, gbc_lbldagTasks);
		
//		dagTasksLabel = new JLabel("0");
//		GridBagConstraints gbc_dagTasksLabel = new GridBagConstraints();
//		gbc_dagTasksLabel.insets = new Insets(0, 0, 5, 0);
//		gbc_dagTasksLabel.gridx = 1;
//		gbc_dagTasksLabel.gridy = 5;
//		panel_side.add(dagTasksLabel, gbc_dagTasksLabel);
		
		JLabel lblStartDate = new JLabel("Start Date");
		GridBagConstraints gbc_lblStartDate = new GridBagConstraints();
		gbc_lblStartDate.insets = new Insets(0, 0, 5, 5);
		gbc_lblStartDate.gridx = 0;
		gbc_lblStartDate.gridy = 6;
		panel_side.add(lblStartDate, gbc_lblStartDate);
		
		lblStartdatevalue = new JLabel("");
		GridBagConstraints gbc_lblStartdatevalue = new GridBagConstraints();
		gbc_lblStartdatevalue.insets = new Insets(0, 0, 5, 0);
		gbc_lblStartdatevalue.gridx = 1;
		gbc_lblStartdatevalue.gridy = 6;
		panel_side.add(lblStartdatevalue, gbc_lblStartdatevalue);
		
		JLabel lblEndDate = new JLabel("End Date");
		GridBagConstraints gbc_lblEndDate = new GridBagConstraints();
		gbc_lblEndDate.insets = new Insets(0, 0, 5, 5);
		gbc_lblEndDate.gridx = 0;
		gbc_lblEndDate.gridy = 7;
		panel_side.add(lblEndDate, gbc_lblEndDate);
		
		lblEnddatevalue = new JLabel("");
		GridBagConstraints gbc_lblEnddatevalue = new GridBagConstraints();
		gbc_lblEnddatevalue.insets = new Insets(0, 0, 5, 0);
		gbc_lblEnddatevalue.gridx = 1;
		gbc_lblEnddatevalue.gridy = 7;
		panel_side.add(lblEnddatevalue, gbc_lblEnddatevalue);
		
		JPanel panel_4 = new JPanel();
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[] {0};
		gbl_panel_4.rowHeights = new int[] {5, 60, 0};
		gbl_panel_4.columnWeights = new double[]{1.0};
		gbl_panel_4.rowWeights = new double[]{0.0, 0.0, 0.0};
		panel_4.setLayout(gbl_panel_4);
		getContentPane().add(panel_4, BorderLayout.SOUTH);
		histogramPanel = new HistogramPanel();
		GridBagLayout gridBagLayout = (GridBagLayout) histogramPanel.getLayout();
		gridBagLayout.rowHeights = new int[] {0, 0, 0};
		GridBagConstraints gbc_histogramPanel = new GridBagConstraints();
		gbc_histogramPanel.anchor = GridBagConstraints.NORTH;
		gbc_histogramPanel.fill = GridBagConstraints.BOTH;
		gbc_histogramPanel.gridx = 0;
		gbc_histogramPanel.gridy = 1;
		panel_4.add(histogramPanel, gbc_histogramPanel);
		
		histogramPanel.addDateRangeChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				Date[] dates = histogramPanel.getSelectedRange();
				lblStartdatevalue.setText(dateFormat.format(dates[0]));
				lblEnddatevalue.setText(dateFormat.format(dates[1]));
			}
			
		});
		
	}

	private Object getSelectedTraceIdElement() {
		return listTraceIdFields.getSelectedValue();
	}
	
	public void setTraceIdHierarchy(List<GraphNode> listSelectedNodes) {
		DefaultListModel model = (DefaultListModel) listTraceIdFields.getModel();
		model.removeAllElements();
		listTraceIdNodes = new Vector<>();
		for (GraphNode g: listSelectedNodes) {
			if ((g instanceof Key) || (g instanceof Column)) {
				model.addElement(g);
				listTraceIdNodes.add(g);
			}			
		}
	}

	public List<GraphNode> getTraceIdHierarchy() {
		return listTraceIdNodes;
	}	
}
