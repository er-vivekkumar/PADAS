package org.processmining.database.metamodel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.HTreeMap;
import org.processmining.openslex.SLEXAttribute;
import org.processmining.openslex.SLEXAttributeValue;
import org.processmining.openslex.SLEXEvent;
import org.processmining.openslex.SLEXEventCollection;
import org.processmining.openslex.SLEXEventResultSet;
import org.processmining.openslex.SLEXPerspective;
import org.processmining.openslex.SLEXTrace;
import org.processmining.openslex.SLEXTraceResultSet;
import org.processmining.openslex.metamodel.*;
import org.processmining.redologs.common.Column;
import org.processmining.redologs.common.DataModel;
import org.processmining.redologs.common.Key;
import org.processmining.redologs.common.LogTraceSplitter;
import org.processmining.redologs.common.SLEXAttributeMapper;
import org.processmining.redologs.common.TableInfo;
import org.processmining.redologs.common.TraceID;
import org.processmining.redologs.common.TraceIDPattern;
import org.processmining.redologs.oracle.OracleLogMinerExtractor;

public class MetaModelPopulator {

	private DataModel dm;
	private SLEXEventCollection evCol;
	private MetaModel mm;
	private List<SLEXAttribute> orderAttributes;
	private List<SLEXAttribute> activityAttributes;
	private SLEXAttributeMapper mapper;
	private TableInfo commonTable;
	private SLEXPerspective perspective;
	
	public MetaModelPopulator(TableInfo common, DataModel dm,
			SLEXEventCollection evCol, List<Column> orderColumns,
			SLEXAttributeMapper mapper, List<Column> activityColumns,
			SLEXPerspective perspective) {
		
		this.evCol = evCol;
		this.mm = new MetaModel();
		this.mapper = mapper;
		this.dm = dm;
		this.commonTable = common;
		this.perspective = perspective;
		
		orderAttributes = new ArrayList<>();
		for (Column ordC: orderColumns) {
			orderAttributes.add(mapper.map(ordC));
		}
		
		activityAttributes = new ArrayList<>();
		for (Column actC: activityColumns) {
			activityAttributes.add(mapper.map(actC));
		}
	}
	
	public TableInfo getTableFromEvent(SLEXEvent e, SLEXAttributeMapper m) {
		TableInfo t = null;
		
		for (SLEXAttribute a: e.getAttributeValues().keySet()) {
			Column c = m.map(a);
			if (c != null) {
				if (c.table != null && c.table != commonTable) {
					return c.table;
				}
			}
		}
		
		return t;
	}
	
	public long getEventTimestamp(SLEXEvent e, SLEXAttributeMapper m) {
		
		try {
			for (Column c : commonTable.columns) {
				if (c.name == OracleLogMinerExtractor.COLUMN_TIMESTAMP) {
					SLEXAttributeValue vAttr = e.getAttributeValues().get(
							m.map(c));
					if (vAttr != null) {
						String v = vAttr.getValue();
						// 2014-11-27 15:55:34.0
						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss.S");
						Date date = sdf.parse(v);
						return date.getTime();
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return -1L;
	}
	
	public String getEventLabel(SLEXEvent e, SLEXAttributeMapper m) {
		
		try {
			for (Column c : commonTable.columns) {
				if (c.name == OracleLogMinerExtractor.COLUMN_OPERATION) {
					SLEXAttributeValue vAttr = e.getAttributeValues().get(
							m.map(c));
					if (vAttr != null) {
						return vAttr.getValue();
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public String getEventResource(SLEXEvent e, SLEXAttributeMapper m) {
		
		try {
			for (Column c : commonTable.columns) {
				if (c.name == OracleLogMinerExtractor.COLUMN_SEG_OWNER) {
					SLEXAttributeValue vAttr = e.getAttributeValues().get(
							m.map(c));
					if (vAttr != null) {
						return vAttr.getValue();
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public String getEventLifecycle(SLEXEvent e, SLEXAttributeMapper m) {
		
		try {
			for (Column c : commonTable.columns) {
				if (c.name == OracleLogMinerExtractor.COLUMN_OPERATION) {
					SLEXAttributeValue vAttr = e.getAttributeValues().get(
							m.map(c));
					if (vAttr != null) {
						return vAttr.getValue();
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public DataModel getDataModel() {
		return this.dm;
	}
		
	public void computeMetaModel() {
		
		DB db = DBMaker.newTempFileDB().transactionDisable().mmapFileEnableIfSupported().make();
		
		SLEXEventResultSet evrset = evCol.getEventsResultSetOrderedBy(orderAttributes);
		
		HashMap<TableInfo,CompactTableInfo> tablesCompactMap = new HashMap<>();
		HashMap<Column,CompactColumn> columnsCompactMap = new HashMap<>();
		HashMap<CompactColumn,Column> columnsCompactReverseMap = new HashMap<>();
		
		HashSet<String> activitySet = new HashSet<>();
		
		// import org.mapdb.*;
		HTreeMap<Integer,CompactActivityInstance> eventActivityInstanceMap =
				db.createHashMap("eventActivityInstanceMap"+System.currentTimeMillis()).make();
		Set<CompactRelation> relationsSet = db.createHashSet("relationsSet"+System.currentTimeMillis()).make();
		HashMap<TableInfo,Set<CompactObjectID>> objects = new HashMap<>();
		HTreeMap<SLEXTrace,HashSet<CompactActivityInstance>> caseToActivityInstancesMap =
				db.createHashMap("caseToActivityInstancesMap"+System.currentTimeMillis()).make();
		
		HashMap<TableInfo,NavigableSet<Fun.Tuple2<CompactObjectID, CompactObjectVersion>>> objectVersions = new HashMap<>();
		
		HashMap<Integer,Key> keyIndexMap = new HashMap<>();
		HashMap<Key,Integer> keyIndexReverseMap = new HashMap<>();
				
		this.mm.columnsCompactMap = columnsCompactMap;
		this.mm.columnsCompactReverseMap = columnsCompactReverseMap;
		this.mm.objects = objects;
		this.mm.relations = relationsSet;
		this.mm.objectVersions = objectVersions;
		this.mm.dm = dm;
		this.mm.evCol = evCol;
		this.mm.perspective = perspective;
		this.mm.mapper = mapper;
		this.mm.activitySet = activitySet;
		this.mm.caseToActivityInstancesMap = caseToActivityInstancesMap;
		this.mm.eventActivityInstanceMap = eventActivityInstanceMap;
		this.mm.keyIndexMap = keyIndexMap;
		this.mm.keyIndexReverseMap = keyIndexReverseMap;
		
		int kindex = 0;
		int ctid = 0;
		for (TableInfo t: dm.getTables()) {
			CompactTableInfo ct = new CompactTableInfo();
			ct.db = t.db;
			ct.name = t.name;
			ct.id = ctid++;
			tablesCompactMap.put(t,ct);
			for (Column c: t.columns) {
				CompactColumn cc = new CompactColumn();
				cc.tableId = ct.id;
				cc.name = c.name;
				columnsCompactMap.put(c, cc);
				columnsCompactReverseMap.put(cc, c);
			}
			
			for (Key k: dm.getKeysPerTable(t)) {
				if (k.type == Key.FOREIGN_KEY) {
					keyIndexMap.put(kindex,k);
					keyIndexReverseMap.put(k,kindex);
					kindex++;
				}
			}
		}
		
		SLEXEvent e = null;
		
		int order = 0;
		
		while ((e = evrset.getNext()) != null) {
			// Get the class for the event			
			TableInfo t = getTableFromEvent(e,mapper);
			CompactTableInfo ct = tablesCompactMap.get(t);

			// Find event Activity and create ActivityInstance
			String activityName = getActivityNameFromEvent(e,activityAttributes);
			if (!activitySet.contains(activityName)) {
				activitySet.add(activityName);
			}
			CompactActivityInstance actIns = new CompactActivityInstance();
			actIns.activityName = activityName; 
			eventActivityInstanceMap.put(e.getId(),actIns);
			
			List<Key> fks = new ArrayList<>();
			// Get the PK of such class
			List<Key> keys = dm.getKeysPerTable(t);
			Key pk = null;
			for (Key k: keys) {
				if (k.type == Key.PRIMARY_KEY) {
					pk = k;
				} else if (k.type == Key.FOREIGN_KEY) {
					fks.add(k);
				}
			}
			
			if (pk == null) {
				continue;
			}
			
			// Get the Object ID
			TraceIDPattern tp = new TraceIDPattern(dm);
			tp.add(pk);
			TraceID eID = LogTraceSplitter.generateTraceID(tp,mapper,e);
			
			// If object does not exist
			if (!objects.containsKey(t)) {
				//objects.put(t, new HashSet<TraceID>());
				Set<CompactObjectID> set = DBMaker.newTempHashSet();
				objects.put(t, set);
			}
			Set<CompactObjectID> objs = objects.get(t);
			
			CompactObjectID oID = new CompactObjectID();
			oID.tableId = ct.id;
			oID.valuesId = new HashMap<>();
			for (Column c: eID.getPA()) {
				String v = eID.getValue(c);
				CompactColumn cc = columnsCompactMap.get(c);
				oID.valuesId.put(cc, v);
			}
			
			if (!objs.contains(oID)) {
				// Create Object
				objs.add(oID);
			}
			
			// Create version for object
			// Link event to version
			CompactObjectVersion cobjVersion = new CompactObjectVersion();
			cobjVersion.id = order;
			cobjVersion.eventId = e.getId();
			cobjVersion.label = getEventLabel(e,mapper);
			cobjVersion.startDate = getEventTimestamp(e,mapper);
			cobjVersion.endDate = -1L;
			cobjVersion.order = order;
			
			/**/
			CompactObjectVersion prevObjVersion = getLastObjectVersion(t, oID, objectVersions);
			if (prevObjVersion != null) {
				objectVersions.get(t).remove(new Fun.Tuple2<CompactObjectID, CompactObjectVersion>(oID,prevObjVersion));
				prevObjVersion.endDate = cobjVersion.startDate;
				objectVersions.get(t).add(new Fun.Tuple2<CompactObjectID, CompactObjectVersion>(oID, prevObjVersion));
			}
			/**/
			
			order++;
			if (!objectVersions.containsKey(t)) {
				NavigableSet<Fun.Tuple2<CompactObjectID, CompactObjectVersion>> navigableSet =
						db.createTreeSet("obj-v-for-"+t.toString()+System.currentTimeMillis()).make();
				objectVersions.put(t, navigableSet);
			}
			NavigableSet<Fun.Tuple2<CompactObjectID, CompactObjectVersion>> objectsForTable = objectVersions.get(t);

			for (SLEXAttribute a: e.getAttributeValues().keySet()) {
				Column c = mapper.map(a);
				if (c != null && c.table == t) {
					SLEXAttributeValue v = e.getAttributeValues().get(a);
					String vStr = v.getValue();
					CompactColumn cc = columnsCompactMap.get(c);
					cobjVersion.attributeValues.put(cc, vStr);
				}				
			}
			
			objectsForTable.add(new Fun.Tuple2<CompactObjectID, CompactObjectVersion>(oID, cobjVersion));
			
			// Get FKs of class
			// Get relations. For each:
			for (Key fk: fks) {
				// Find relation and referred object
				TraceIDPattern tpfk = new TraceIDPattern(dm);
				tpfk.add(fk);
				tpfk.add(fk.refers_to);
				TraceID fkeID = LogTraceSplitter.generateTraceID(tpfk, mapper, e);
				
				Key referred_pk = fk.refers_to;
				
				CompactObjectID creferredID = new CompactObjectID();
				CompactTableInfo creferredTable = tablesCompactMap.get(referred_pk.table);
				creferredID.tableId = creferredTable.id;
				creferredID.valuesId = new HashMap<>();
				for (Column c: fkeID.getPA()) {
					String v = fkeID.getValue(c);
					CompactColumn cc = columnsCompactMap.get(c);
					creferredID.valuesId.put(cc, v);
				}
				
				/**/
				if (!objects.containsKey(referred_pk.table)) {
					Set<CompactObjectID> set = db.createHashSet(
							"objects-set-for-table-"+creferredTable.db+"-"+creferredTable.name+System.currentTimeMillis()
							).make();
					objects.put(referred_pk.table, set);
				}
				if (objects.get(referred_pk.table).contains(creferredID)) {
					//System.out.println("Contained");
				} else {
					//System.out.println("Not Contained");
					objects.get(referred_pk.table).add(creferredID);
				}
				/**/
				
				CompactRelation r = new CompactRelation();
				r.sourceObjectVersionId = cobjVersion.id;
				CompactObjectVersion targetObjectVersion = getLastObjectVersion(referred_pk.table,creferredID,objectVersions);
				if (targetObjectVersion != null) {
					r.targetObjectVersionId = targetObjectVersion.id;
					r.startTimestamp = cobjVersion.startDate;
					r.endTimestamp = -1L; // FIXME
					r.relationshipId = keyIndexReverseMap.get(fk);
					relationsSet.add(r);
				}
			}
			
		}
		
		// Get cases and find relation to Activity Instances
		if (perspective != null) {
			SLEXTraceResultSet trset = perspective.getTracesResultSet();
			SLEXTrace tr = null;
			while ((tr = trset.getNext()) != null) {
				HashSet<CompactActivityInstance> aiSet = new HashSet<>();
			
				SLEXEventResultSet erset = tr.getEventsResultSet();
				SLEXEvent ev = null;
				while ((ev = erset.getNext()) != null) {
					CompactActivityInstance ai = eventActivityInstanceMap.get(ev.getId());
					if (ai != null) {
						aiSet.add(ai);
					}
				}
				
				caseToActivityInstancesMap.put(tr, aiSet);
			}
		}
	}
	
	private String getActivityNameFromEvent(SLEXEvent e,
			List<SLEXAttribute> activityAttributes) {
		StringBuffer name = new StringBuffer();
		
		Hashtable<SLEXAttribute, SLEXAttributeValue> attValues = e.getAttributeValues();
		
		int i = 1;
		for (SLEXAttribute at: activityAttributes) {
			SLEXAttributeValue atV = attValues.get(at);
			name.append(atV.getValue());
			if (i < activityAttributes.size()) {
				name.append("+");
			}
			i++;
		}
		
		return name.toString();
	}

	public CompactObjectVersion getLastObjectVersion(TableInfo t, CompactObjectID objId,
			HashMap<TableInfo,NavigableSet<Fun.Tuple2<CompactObjectID, CompactObjectVersion>>> objectVersions) {
		CompactObjectVersion objV = null;
		
		if (objectVersions.containsKey(t)) {
			NavigableSet<Fun.Tuple2<CompactObjectID, CompactObjectVersion>> objsT = objectVersions.get(t);

			for (CompactObjectVersion v: Fun.filter(objsT,objId)) {
				if (objV == null || v.order > objV.order) {
					objV = v;
				}
			}
			
		}
		
		return objV;
	}
	
	public MetaModel getMetaModel() {
		return mm;
	}
	
	public SLEXMMStorageMetaModel saveMetaModelToDisk(String name) {
		try {
			SLEXMMStorageMetaModel strg = new SLEXMMStorageMetaModelImpl("data",name+"-"
					+System.currentTimeMillis()+SLEXMMStorageMetaModel.METAMODEL_FILE_EXTENSION);
			
			HashMap<TableInfo,SLEXMMClass> tableToClassMap = new HashMap<>();
			HashMap<Column,SLEXMMAttribute> columnToAttributeMap = new HashMap<>();
			HashMap<TableInfo,Key> primaryKeysPerTable = new HashMap<>();
			HashMap<SLEXAttribute,SLEXMMEventAttribute> eventAttrMap = new HashMap<>();
			
			HashMap<Integer,Integer> eventToMMEventMap = new HashMap<>();
			
			HashMap<Integer,Integer> objVToMMObjVersionMap = new HashMap<>();
			
			HashMap<Key,SLEXMMRelationship> fkeyToRelationshipMap = new HashMap<>();
			
			HashMap<String,SLEXMMActivity> activityMap = new HashMap<>();
			
			HashMap<Integer,Integer> activityInstancesMap = new HashMap<>();
			
			// Save DataModel
			
			SLEXMMDataModel slxmmdm = strg.createDataModel(dm.getName());
			
			for (TableInfo t: dm.getTables()) {
				SLEXMMClass cl = strg.createClass(slxmmdm.getId(), t.name);
				tableToClassMap.put(t, cl);
				for (Column c: t.columns) {
					SLEXMMAttribute at = strg.createAttribute(cl.getId(), c.name);
					columnToAttributeMap.put(c,at);
				}
			}
			
			for (TableInfo t: dm.getTables()) {
				for (Key k: dm.getKeysPerTable(t)) {
					if (k.type == Key.PRIMARY_KEY) {
						primaryKeysPerTable.put(t,k);
					} else if (k.type == Key.FOREIGN_KEY) {
						SLEXMMClass sourceClass = tableToClassMap.get(t);
						SLEXMMClass targetClass = tableToClassMap.get(k.refers_to.table);
						SLEXMMRelationship rl = strg.createRelationship(k.name,
								sourceClass.getId(), targetClass.getId());
						fkeyToRelationshipMap.put(k,rl);
					}
				}
			}
			
			// Create process
			SLEXMMProcess proc = strg.createProcess("proc01");
			
			// Create Activities
			for (String actName: mm.activitySet) {
				SLEXMMActivity act = strg.createActivity(proc.getId(), actName);
				activityMap.put(actName,act);
			}			

			strg.setAutoCommit(false);

			// Save Cases and Activity Instances			
			for (SLEXTrace tr : mm.caseToActivityInstancesMap.keySet()) {
				SLEXMMCase pcase = strg.createCase(tr.getCaseId());
				for (CompactActivityInstance ai : mm.caseToActivityInstancesMap
						.get(tr)) {
					if (!activityInstancesMap.containsKey(ai.hashCode())) {
						SLEXMMActivity act = activityMap.get(ai.activityName);
						SLEXMMActivityInstance mmai = strg
								.createActivityInstance(act);
						activityInstancesMap.put(ai.hashCode(), mmai.getId());
					}
					Integer mmaiId = activityInstancesMap.get(ai.hashCode());

					pcase.add(mmaiId);
				}
			}

			strg.commit();
				
			// Save Event Collection
						
			SLEXEventResultSet evrset = evCol.getEventsResultSetOrderedBy(orderAttributes);
			SLEXEvent e = null;
			
			int i = 1;
			while ((e = evrset.getNext()) != null) {
				
				CompactActivityInstance ai = mm.eventActivityInstanceMap.get(e.getId());
				if (!activityInstancesMap.containsKey(ai.hashCode())) {
					SLEXMMActivity act = activityMap.get(ai.activityName);
					SLEXMMActivityInstance mmai = strg.createActivityInstance(act);
					activityInstancesMap.put(ai.hashCode(), mmai.getId());
				}
				Integer mmaiId = activityInstancesMap.get(ai.hashCode());
				
				String lifecycle = getEventLifecycle(e,mm.mapper);
				String resource = getEventResource(e,mm.mapper);
				long timestamp = getEventTimestamp(e,mm.mapper);
				SLEXMMEvent mme = strg.createEvent(i,mmaiId,lifecycle,resource,timestamp);
				eventToMMEventMap.put(e.getId(),mme.getId());
				
				i++;
				Hashtable<SLEXAttribute, SLEXAttributeValue> attrVals = e.getAttributeValues();
				for (SLEXAttribute at: attrVals.keySet()) {
					SLEXMMEventAttribute mmatt = null;
					if (eventAttrMap.containsKey(at)) {
						mmatt = eventAttrMap.get(at);
					} else {
						mmatt = strg.createEventAttribute(at.getName());
						eventAttrMap.put(at, mmatt);
					}
					SLEXAttributeValue attVal = attrVals.get(at);
					String value = attVal.getValue();
					String type = "STRING";
					SLEXMMEventAttributeValue mmattVal = strg.createEventAttributeValue(mmatt.getId(),
							mme.getId(), value, type);
				}
				
			}
		
			strg.commit();
			// Save Objects
			
			for (TableInfo t: mm.objects.keySet()) {
				SLEXMMClass c = tableToClassMap.get(t);
				for (CompactObjectID objId: mm.objects.get(t)) {
					SLEXMMObject obj = strg.createObject(c.getId());
					
					// Save ObjectVersions
					if (mm.objectVersions.containsKey(t)) {
						
						NavigableSet<Tuple2<CompactObjectID, CompactObjectVersion>> objectVersionsPerObject = mm.objectVersions.get(t);
						
						for (CompactObjectVersion objV: Fun.filter(objectVersionsPerObject, objId)) {
							Integer evId = eventToMMEventMap.get(objV.eventId);
							SLEXMMObjectVersion mmObjV = strg.createObjectVersion(obj.getId(),evId, objV.label,objV.startDate,objV.endDate); // FIXME
							objVToMMObjVersionMap.put(objV.id,mmObjV.getId());
							
							// Save Attribute values for Object version
							for (CompactColumn cc: objV.attributeValues.keySet()) {
								Column ck = mm.columnsCompactReverseMap.get(cc);
								SLEXMMAttribute mmAttr = columnToAttributeMap.get(ck);
								String value = objV.attributeValues.get(cc);
								String type = "STRING";
								SLEXMMAttributeValue attVal =
										strg.createAttributeValue(mmAttr.getId(), mmObjV.getId(), value, type);
							}
						}
					}
				}
			}
			
			strg.commit();
			
			// Save Relations			
			for (CompactRelation r: mm.relations) {								
				
				if (r.sourceObjectVersionId >= 0 && r.targetObjectVersionId >= 0) {
									
					Integer sourceObjectVersionId = objVToMMObjVersionMap.get(r.sourceObjectVersionId);
					Integer targetObjectVersionId = objVToMMObjVersionMap.get(r.targetObjectVersionId);
				
					if (sourceObjectVersionId != null && targetObjectVersionId != null) {
						int relationshipId = -1;
						Key k = mm.keyIndexMap.get(r.relationshipId);
						if (k != null) {
							SLEXMMRelationship relshp = fkeyToRelationshipMap.get(k);
							if (relshp != null) {
								relationshipId = relshp.getId();
							}
						}
						SLEXMMRelation mmRel = strg.createRelation(
								sourceObjectVersionId, targetObjectVersionId, relationshipId, r.startTimestamp, r.endTimestamp);
					}
				}
			}
			strg.commit();
			strg.setAutoCommit(true);
			
			return strg;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}