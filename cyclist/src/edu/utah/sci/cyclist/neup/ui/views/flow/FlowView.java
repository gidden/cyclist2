package edu.utah.sci.cyclist.neup.ui.views.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import edu.utah.sci.cyclist.core.event.Pair;
import edu.utah.sci.cyclist.core.event.dnd.DnD;
import edu.utah.sci.cyclist.core.model.Field;
import edu.utah.sci.cyclist.core.model.Simulation;
import edu.utah.sci.cyclist.core.ui.components.CyclistViewBase;
import edu.utah.sci.cyclist.core.ui.components.IntegerField;
import edu.utah.sci.cyclist.core.util.AwesomeIcon;
import edu.utah.sci.cyclist.core.util.GlyphRegistry;
import edu.utah.sci.cyclist.neup.model.Facility;
import edu.utah.sci.cyclist.neup.model.Inventory;
import edu.utah.sci.cyclist.neup.model.Transaction;
import edu.utah.sci.cyclist.neup.model.proxy.SimulationProxy;

public class FlowView extends CyclistViewBase {
	public static final String ID = "flow-view";
	public static final String TITLE = "Material Flow";
	
	public static final int Y_OFFSET_TOP = 30;
	public static final int Y_OFFSET_BOTTOM = 20;
	public static final int X_CHART_OFFSET = 10;
	
	public static final String NATURAL_U = "natl_u";
	public static final String ENRICHED_U = "enriched_u";
	public static final String WASTE = "waste";
	
	public static final int SRC = 0;
	public static final int DEST = 1;
	
	public static final int INIT_TIMESTEP = 0;
	public static final int MIN_TIMESTEP = 0;

	
	private FlowLine _line[]; 
	private FlowChart _chart;
	
	private Map<String, Function<Facility, Object>> kindFactory = new HashMap<>();
	private Map<Integer, Facility> _facilities = new HashMap<>();
	private List<Connector> _connectors = new ArrayList<>();
	private Map<FlowNode, ObservableList<Inventory>> _selectedNodes = new HashMap<>();

	private Simulation _currentSim = null;
	private SimulationProxy _simProxy = null;
	private int _targetLine = -1;
	private boolean _isChangingKind = false;
	
	/*
	 * Properties
	 */
	private ObjectProperty<Predicate<Transaction>> _transactionsPredicateProperty = new SimpleObjectProperty<>(t->true);
	
	private ObjectProperty<Function<Transaction, Object>> _aggregateFuncProperty = new SimpleObjectProperty<>();
	private IntegerProperty _chartModeProperty = new SimpleIntegerProperty(0);
		
	private Predicate<Transaction> _commodityPredicate = t->true;
	private Predicate<Transaction> _isoPredicate = t->true;

	
	// UI Components
	private Pane _pane;
	private IntegerField _timestepField;	
	private Text _total;
	
	/**
	 * Constructor
	 */
	
	public FlowView() {
		super();
		
		init();
		build();
	}
	
	
	@Override
	public void selectSimulation(Simulation sim, boolean active) {
		super.selectSimulation(sim, active);
		
		if (!active && sim != _currentSim) {
			return; // ignore
		}
		
		_currentSim = active? sim : null;
		update();
	}
	
	private int getTime() {
		return _timestepField.getValue();
	}
	
	private void init() {
		setSupportsTables(false);
		
		kindFactory.put("Implementation", f->f.implementation);
		kindFactory.put("Prototype", f->f.prototype);
		kindFactory.put("AgentID", f->f.id);
		kindFactory.put("InstitutionID", f->f.intitution);
	}
	
	private void update() {
		if (_currentSim == null) {
			_simProxy = null;
		}
		_simProxy = new SimulationProxy(_currentSim);
		
		fetchFacilities();
	}
	
	private void fetchFacilities() {
		// fetch facilities from the simulation
		Task<ObservableList<Facility>> task = new Task<ObservableList<Facility>>() {
			@Override
			protected ObservableList<Facility> call() throws Exception {
				return _simProxy.getFacilities();
			}
		};
		
		task.valueProperty().addListener( new ChangeListener<ObservableList<Facility>>() {

			@Override
			public void changed(
					ObservableValue<? extends ObservableList<Facility>> observable,
					ObservableList<Facility> oldValue,
					ObservableList<Facility> newValue) 
			{
				if (newValue != null) {
					_facilities = new HashMap<>();
					for (Facility f : newValue) {
						_facilities.put(f.getId(), f);
					}
					System.out.println("Flow: received facilities: "+newValue.size());
				}
			}
		});
		
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
	}
	
	private void updateTransactionsPredicate() {
		_transactionsPredicateProperty.set(_commodityPredicate.and(_isoPredicate));
		
		updateTotal();
	}
	
	private void updateTotal() {
		if (_line == null || _line[SRC] == null) return;
		
		double total = 0;
		for (FlowNode node : _line[SRC].getNodes()) {
			for (Transaction t : node.getActiveTransactions()) {
				total += t.amount;
			}
		}
		

		if (total == 0) {
			_total.setText("");
		} else {
			_total.setText(String.format("%.2e kg", total));
		}
	}
	
	private void timeChanged(int time) {
		// connect explicit nodes
		List<FlowNode> list = new ArrayList<>();
		for (FlowNode node : _line[SRC].getNodes()) {
			if (node.isExplicit())
				list.add(node);
		}
		
		for (FlowNode node : _line[DEST].getNodes()) {
			if (node.isExplicit())
				list.add(node);
		}
		
		queryTransactions(list);	
	}
	
	private void removeAllConnectors() {
		for (Connector c : _connectors) {
			c.release();
			c.getFrom().removeConnector(c);
			c.getTo().removeConnector(c);
			_pane.getChildren().remove(c);
		}
		_connectors.clear();
	}
	
	private FlowNode createNode(String kind, Object value, int direction, boolean explicit) {
		FlowNode node = new FlowNode(kind, value, direction, explicit);
		node.setOnOpen(n->openNode(n));
		node.setOnClose(n->closeNode(n));
		node.setOnSelect(n->selectNode(n));
		node.getActiveTransactions().predicateProperty().bind(_transactionsPredicateProperty);
		node.getActiveTransactions().addListener((Observable o)->{transactionsChanged(node);});
		
		return node;
	}
	

	private void addNode(Field field, Object value, int direction, double y, boolean explicit) {	
		FlowLine line = _line[direction];
		
		if (line.getKind() == null) {
			line.setKind(field.getName());
		} else if (!line.getKind().equals(field.getName())) {
			System.out.println("Error: REJECT node of this kind");
			return;
		}
		
		FlowNode node = line.findNode(value);
		if (node == null) {
			node = createNode(field.getName(), value, direction, explicit);
			line.addNode(node);
		} else {
			if (node.isExplicit() || !explicit) {
				// nothing to do here
				return;
			}
		}
		
		node.setExplicit(explicit);
	
		if (explicit) {
			queryMaterialFlow(node);
		}
	} 
	
	private void closeNode(FlowNode node) {
		List<FlowNode> closeNodes = new ArrayList<>();
		
		Iterator<Connector> i = node.getConnectors().iterator();
		while (i.hasNext()) {
			Connector c = i.next();
			FlowNode other = c.getFrom() == node ? c.getTo() : c.getFrom();
			if (other.isImplicit()) {
				c.release();
				other.removeConnector(c);
				if (other.isImplicit() && other.getConnectors().isEmpty()) {
					closeNodes.add(other);
				}
				i.remove();
				_pane.getChildren().remove(c);
			}
		}
		
		if (node.getConnectors().isEmpty()) {
			removeNode(node);
		} else
			node.setExplicit(false);
		
		for (FlowNode n : closeNodes) {
			closeNode(n);
		}
	}
	
	private void removeNode(FlowNode node) {
		_line[node.getDirection()].removeNode(node);
	}
	
	public void removeNodes(List<FlowNode> nodes) {
		for (FlowNode node : nodes) {
			removeNode(node);
		}
	}
	
	private void openNode(FlowNode node) {
		queryMaterialFlow(node);
		node.setExplicit(true);
	}
	
	@SuppressWarnings("unchecked")
	private void selectNode(final FlowNode node) {
		if (node.isSelected()) {
			node.setSelected(false);
			_chart.remove(node);
		} else {
			node.setSelected(true);
			ObservableList<Inventory> values = _selectedNodes.get(node);
			if (values == null) {
				queryInventory(node).addListener((Observable o)->{
					ObjectProperty<ObservableList<Inventory>> p = (ObjectProperty<ObservableList<Inventory>>) o;

					_selectedNodes.put(node, p.get());
					addToChart(node, p.get());
				});
				
			} else {
				addToChart(node, values);
			}
		}
	}
	
	private void addToChart(FlowNode node, ObservableList<Inventory> values) {
		Map<Integer, Pair<Integer, Double>> map = new HashMap<>();
		for (Inventory i : values) {
			Pair<Integer, Double> p = map.get(i.time);
			if (p == null) {
				p = new Pair<Integer, Double>();
				p.v1 = i.time;
				p.v2 = 0.0;
				map.put(i.time, p);
			}
			p.v2 += i.amount;
		}
		_chart.add(node, node.getName(), map.values());
	}
	
	private ReadOnlyObjectProperty<ObservableList<Inventory>> queryInventory(FlowNode node) {
		Task<ObservableList<Inventory>> task = new Task<ObservableList<Inventory>>() {
			@Override
			protected ObservableList<Inventory> call() throws Exception {
				ObservableList<Inventory> list = _simProxy.getInventory(node.getType(), node.getValue().toString());
				return list;
			}	
		};
		
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
		
		node.setTask(task);
		
		return task.valueProperty();
	}
	
	private void queryMaterialFlow(final FlowNode node) {
		final int timestep = getTime();
		Task<ObservableList<Transaction>> task = new Task<ObservableList<Transaction>>() {
			@Override
			protected ObservableList<Transaction> call() throws Exception {
				return _simProxy.getTransactions(node.getType(), node.getValue().toString(), timestep, node.isSRC());
			}	
		};
		
		task.valueProperty().addListener((o, p, n)->{
				if (n != null) {
					addRelatedNodes(node, n, timestep);
				}
		});

		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
		
		setCurrentTask(task);
	}
	
	private void queryTransactions(final List<FlowNode> list) {
		final int timestep = getTime();
	
		Task<ObservableMap<FlowNode, ObservableList<Transaction>>> task = new Task<ObservableMap<FlowNode, ObservableList<Transaction>>>() {
			@Override
			protected ObservableMap<FlowNode,ObservableList<Transaction>> call() throws Exception {
				Map<FlowNode, ObservableList<Transaction>> map = new HashMap<>();
				for (FlowNode node : list) {
					ObservableList<Transaction> list = _simProxy.getTransactions(node.getType(), node.getValue().toString(), timestep, node.isSRC());
					map.put(node, list);
				}
				
				return FXCollections.observableMap(map);
			}	
		};
		
		task.valueProperty().addListener((o, p, n)->{
			if (n != null) {
				removeAllConnectors();
				for (FlowNode node : n.keySet()) {
					addRelatedNodes(node, n.get(node), timestep);
				}			
				removeEmptyImplicitNodes();
			}
		});

		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
		
		setCurrentTask(task);
	}
	
	private void addRelatedNodes(FlowNode node, ObservableList<Transaction> list, int time) {
		if (time == getTime()) {
			node.setTransactions(list);
		}
	}
	
	private void removeEmptyImplicitNodes() {
		// remove empty implicit nodes
		for(Iterator<FlowNode> i = _line[SRC].getNodes().iterator(); i.hasNext();) {
			FlowNode node = i.next();
			if (node.isImplicit() && node.getConnectors().isEmpty()) {
				i.remove();
				removeNode(node);
			}
		}
		
		for(Iterator<FlowNode> i = _line[DEST].getNodes().iterator(); i.hasNext();) {
			FlowNode node = i.next();
			if (node.isImplicit() && node.getConnectors().isEmpty()) {
				i.remove();
				removeNode(node);
			}
		}
		
	}
	
	private void lineKindChanged(int direction) {
		FlowLine changed = _line[direction];
		FlowLine src = _line[1-direction];
		

		removeAllConnectors();
		
		String kind = changed.getKind();
		removeNodes(changed.selectNodes(n->true));
		changed.setKind(kind);
		
		// remove implicit nodes
		removeNodes(src.selectNodes(n->n.isImplicit()));
		
		// update remaining nodes
		src.getNodes()
			.stream()
			.forEach(n->transactionsChanged(n));
	}
	
	private void transactionsChanged(FlowNode node) {		
		int from = node.getDirection();
		int to = 1-from;
		
		FlowLine line = _line[to];
		
		if (line.getKind() == null) {
			line.setKind(_line[from].getKind());			
		}
		
		Set<Object> set = groupSet(node.getActiveTransactions(), node.isSRC() ? t->t.receiver : t->t.sender, kindFactory.get(line.getKind()));
		
		for (final Object value : set) {
			FlowNode target = line.findNode(value);
			if (target == null) {
				target = createNode(line.getKind(), value, to, false);
				line.addNode(target);
			}
			
			final Function<Facility, Object> kind = kindFactory.get(line.getKind());
			Function<Transaction, Facility> f = node.isSRC() ? t->_facilities.get(t.receiver) : t->_facilities.get(t.sender);
			
			Connector c = new Connector(node, target, node.getActiveTransactions().filtered(t->kind.apply(f.apply(t)) == value));		
			node.addConnector(c);
			target.addConnector(c);
			_connectors.add(c);
			_pane.getChildren().add(c);
		}

	}
	
	private Set<Object> groupSet(ObservableList<Transaction> transactions, Function<Transaction, Integer> targetFunc, Function<Facility, Object> func) {
		Set<Object> set = new HashSet<>();
		for (Transaction t : transactions) {
			Facility f = _facilities.get(targetFunc.apply(t));
			set.add(func.apply(f));
		}
		return set;
	}
	
	private void isoFilterChanged(String value) {
		if (value == null || value.equals("")) {
			_isoPredicate = t->true;
			updateTransactionsPredicate();
		} else {
			try {
				final int n = Integer.parseInt(value);	
				_isoPredicate =  
					n< 200 ? 
						t->Math.floorDiv(t.nucid, 1000*10000) == n:
					n < 200000 ?
						t->Math.floorDiv(t.nucid, 10000) == n:
						t->t.nucid == n  ;
				updateTransactionsPredicate();
			} catch (Exception e)  {
				System.out.println("*** TODO: Indicate to user iso was invalid number");
			}
		}
	}
	
	private void build() {
		setTitle(TITLE);
		getStyleClass().add("flow");
		this.setPrefWidth(400);
		this.setPrefHeight(300);
		
		BorderPane pane = new BorderPane();
		pane.setLeft(buildControlls());
		pane.setCenter(buildMainArea());
		pane.setBottom(buildInventoryChart());
	
		_line[SRC].charModeProperty().bind(_chartModeProperty);
		_line[SRC].aggregationFunProperty().bind(_aggregateFuncProperty);
		_line[DEST].charModeProperty().bind(_chartModeProperty);
		_line[DEST].aggregationFunProperty().bind(_aggregateFuncProperty);
		
		_aggregateFuncProperty.set(t->t.commodity);
		
		setContent(pane);
	}

	private Node buildControlls() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("ctrl");
		
		vbox.getChildren().addAll(
			buildTimestepCtrl(),
			buildSelectionCtrl(),
			buildNuclideCtrl(),
			new Separator(),
//			buildChartMode(),
			buildChartAggr()
		);
		
		return vbox;
	}
	
	private Node buildTimestepCtrl() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("infobar");
		
		Label title = new Label("Time");
		title.getStyleClass().add("title");

		_timestepField = new IntegerField(INIT_TIMESTEP);
		_timestepField.getStyleClass().add("timestep");
		_timestepField.setMinValue(MIN_TIMESTEP);	
				
		Label forward = GlyphRegistry.get(AwesomeIcon.CARET_RIGHT, "16px");
		Label backward = GlyphRegistry.get(AwesomeIcon.CARET_LEFT, "16px");

		forward.getStyleClass().add("flat-button");
		backward.getStyleClass().add("flat-button");
		
		forward.setOnMouseClicked(e->_timestepField.setValue(_timestepField.getValue()+1));			
		backward.setOnMouseClicked(e->_timestepField.setValue(_timestepField.getValue()-1));			
		
		HBox hbox = new HBox();
		hbox.setStyle("-fx-padding: 0");
		hbox.getStyleClass().add("infobar");
		hbox.getChildren().addAll( _timestepField, backward, forward);
		
		vbox.getChildren().addAll(
			title,
			hbox
		);
		
		_timestepField.valueProperty().addListener(o->timeChanged(_timestepField.getValue()));
		
		_timestepField.setOnDragOver(e->{
			DnD.LocalClipboard clipboard = getLocalClipboard();
			
			if (clipboard.hasContent(DnD.VALUE_FORMAT)) {
				Field field = clipboard.get(DnD.FIELD_FORMAT, Field.class);
				if (field.getName().equals("EnterTime") || field.getName().equals("ExitTime")) {
					e.acceptTransferModes(TransferMode.COPY);
					e.consume();
				}
			}
		});
		
		_timestepField.setOnDragDropped(e-> {
			DnD.LocalClipboard clipboard = getLocalClipboard();	
			if (clipboard.hasContent(DnD.VALUE_FORMAT)) {
				Integer i = clipboard.get(DnD.VALUE_FORMAT, Integer.class);					
				_timestepField.setValue(i);
				e.consume();
			}
		});
			
		return vbox;
	}
	
	private Node buildSelectionCtrl() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("infobar");
		
		Text title = new Text("Commodity");
		title.getStyleClass().add("title");
		
		CheckBox naturalU = new CheckBox("Natual U");
		CheckBox enrichedU = new CheckBox("Enriched U");
		CheckBox waste = new CheckBox("Waste");
		
		InvalidationListener listener = o->{
			removeAllConnectors();

			final boolean n = naturalU.isSelected();
			final boolean e = enrichedU.isSelected();
			final boolean w = waste.isSelected();

			_commodityPredicate = t->
					(n && t.commodity.equals(NATURAL_U))
					|| (e && t.commodity.equals(ENRICHED_U))
					|| (w && t.commodity.equals(WASTE));
					
			updateTransactionsPredicate();
		};
		
		naturalU.selectedProperty().addListener(listener);
		enrichedU.selectedProperty().addListener(listener);
		waste.selectedProperty().addListener(listener);

		naturalU.setSelected(true);
		enrichedU.setSelected(true);
		waste.setSelected(true);

		vbox.getChildren().addAll(
			title,
			naturalU,
			enrichedU,
			waste
		);
		
		return vbox;	
	}
	
	private Node buildNuclideCtrl() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("infobar");

		Text title = new Text("Nuclide");
		title.getStyleClass().add("title");
		
		TextField entry = new TextField();
		entry.getStyleClass().add("nuclide");

		vbox.getChildren().addAll(
			title,
			entry
		);
		
		
		entry.setOnAction(e->isoFilterChanged(entry.getText()));
		return vbox;
	}

//	private Node buildChartMode() {
//		VBox vbox = new VBox();
//		vbox.getStyleClass().add("infobar");
//
//		Text title = new Text("Chart");
//		title.getStyleClass().add("title");
//		
//		ToggleGroup group = new ToggleGroup();
//		RadioButton listMode= new RadioButton("List");
//		listMode.setToggleGroup(group);
//		
//		RadioButton chartMode = new RadioButton("Chart");
//		chartMode.setToggleGroup(group);
//		
//		vbox.getChildren().addAll(
//			title,
//			listMode,
//			chartMode
//		);
//		
//		listMode.setOnAction(e->_chartModeProperty.set(0));
//		chartMode.setOnAction(e->_chartModeProperty.set(1));
//		listMode.setSelected(true);
//		
//		return vbox;
//	}
	
	private Node buildChartAggr() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("infobar");

		Text title = new Text("Info");
		title.getStyleClass().add("title");
		
		ToggleGroup group = new ToggleGroup();
		
		RadioButton commodity= new RadioButton("Commodity");
		commodity.getStyleClass().add("flat-button");
		commodity.setToggleGroup(group);
		
		RadioButton elem = new RadioButton("Element");
		elem.getStyleClass().add("flat-button");
		elem.setToggleGroup(group);

		RadioButton iso = new RadioButton("Iso");
		iso.getStyleClass().add("flat-button");
		iso.setToggleGroup(group);
		
		vbox.getChildren().addAll(
			title,
			commodity,
			elem,
			iso
		);
		
		commodity.setOnAction(e->_aggregateFuncProperty.set(t->t.commodity));
		elem.setOnAction(e->_aggregateFuncProperty.set(t->Math.floorDiv(t.nucid, 1000)));
		iso.setOnAction(e->_aggregateFuncProperty.set(t->t.nucid));
		
		commodity.setSelected(true);

		return vbox;
	}
	
	private Node buildInventoryChart() {
		VBox vbox = new VBox();
		_chart = new FlowChart();
		
		_timestepField.valueProperty().bindBidirectional(_chart.timeProperty());
		
		vbox.getChildren().addAll(
			new Separator(),
			_chart
		);
		return vbox;
	}
	
	private Node buildMainArea() {
		_pane = new Pane();
		_pane.getStyleClass().add("pane");
		
		Rectangle clip = new Rectangle(0, 0, 100, 100);
		clip.widthProperty().bind(_pane.widthProperty());
		clip.heightProperty().bind(_pane.heightProperty());
		_pane.setClip(clip);
		
		_line = new FlowLine[2];
		_line[SRC] = new FlowLine(SRC, _pane);
		_line[SRC].setKindItems(kindFactory.keySet());
		
		_line[DEST] = new FlowLine(DEST, _pane);
		_line[DEST].setKindItems(kindFactory.keySet());

		Text totalLabel = new Text("Total: ");
		totalLabel.getStyleClass().add("total");
		_total = new Text();
		TextFlow totalLine = new TextFlow();
		totalLine.getChildren().addAll(totalLabel, _total);
		
		_pane.getChildren().addAll(/* _line[0], _line[1],*/ totalLine);		
		
		DoubleProperty w = new SimpleDoubleProperty();
		w.bind(_pane.widthProperty().divide(5)); 
		
		_line[SRC].centerXProperty().bind(w);
		_line[SRC].infoXProperty().set(10);
		_line[SRC].startYProperty().bind(_pane.translateYProperty().add(Y_OFFSET_TOP));
		_line[SRC].endYProperty().bind(_pane.heightProperty().subtract(Y_OFFSET_TOP+Y_OFFSET_BOTTOM));
		
		_line[DEST].centerXProperty().bind(w.multiply(4));
		_line[DEST].infoXProperty().bind(
				_line[DEST].centerXProperty().add(
						(_line[DEST].widthProperty().divide(2)).add(10)));
		_line[DEST].startYProperty().bind(_pane.translateYProperty().add(Y_OFFSET_TOP));
		_line[DEST].endYProperty().bind(_pane.heightProperty().subtract(Y_OFFSET_TOP+Y_OFFSET_BOTTOM));

		totalLine.translateXProperty().bind((_pane.widthProperty().subtract(totalLine.widthProperty()).divide(2)));
		totalLine.setTranslateY(Y_OFFSET_TOP/2);
		setPaneListeners();
		
		return _pane;
	}
	
	private void setPaneListeners() {
		_pane.setOnDragOver(e->{
			boolean accept = false;
			DnD.LocalClipboard clipboard = getLocalClipboard();
			
			if (clipboard.hasContent(DnD.VALUE_FORMAT)) {
				Field field = clipboard.get(DnD.FIELD_FORMAT, Field.class);
				if (kindFactory.containsKey(field.getName())) {
					// accept if it near one of the lines
					double x = e.getX();
					if (Math.abs(x - _line[SRC].getCenter()) < 10) {
						if (_line[SRC].getKind() == null 
								|| _line[SRC].getKind().equals(field.getName())) 
						{
							_targetLine = SRC;
							accept = true;
						}
					} else if (Math.abs(x - _line[DEST].getCenter()) < 10) {
						if (_line[DEST].getKind() == null 
								|| _line[DEST].getKind().equals(field.getName())) 
						{
							_targetLine = DEST;
							accept = true;
						}
					} else {
						_targetLine = -1;
					}
				}
				if (accept) {				
					e.acceptTransferModes(TransferMode.COPY);
					e.consume();
				}
			} 
		});
		
		_pane.setOnDragDropped(e-> {
			DnD.LocalClipboard clipboard = getLocalClipboard();
			
			Object value = clipboard.get(DnD.VALUE_FORMAT, Object.class);
			Field field = clipboard.get(DnD.FIELD_FORMAT, Field.class);
//			Table table = clipboard.get(DnD.TABLE_FORMAT, Table.class);
			
			addNode(field, value, _targetLine, e.getY(), true);
			
			_targetLine = -1;
			
			e.setDropCompleted(true);
			e.consume();
		});
		
		_line[SRC].getNodes().addListener((Observable o)-> {
			updateTotal();
		});
		
		_line[SRC].kindProperty().addListener((o, p, n)->{
			if (p != null && !_isChangingKind)	{
				_isChangingKind = true;
				lineKindChanged(SRC);
				_isChangingKind = false;
			}
		});
		
		_line[DEST].kindProperty().addListener((o, p, n)->{
			if (p != null && !_isChangingKind)	{
				_isChangingKind = true;
				lineKindChanged(DEST);
				_isChangingKind = false;
			}
		});
	}
}