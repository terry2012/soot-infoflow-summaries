package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static soot.jimple.infoflow.methodSummary.data.SourceSinkType.Field;
import static soot.jimple.infoflow.methodSummary.data.SourceSinkType.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.MethodFlow;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public class ParaToFieldTests extends TestHelper {
	protected static Map<String, Set<MethodFlow>> flows;
	static boolean executeSummary = true;
	static final String className = "soot.jimple.infoflow.test.methodSummary.ParaToField";
	static final String LIST_ITEM[] = new String[]{"<soot.jimple.infoflow.test.methodSummary.ParaToField: java.util.List listField>","<java.util.LinkedList: java.util.LinkedList$Node first>","<java.util.LinkedList$Node: java.lang.Object item>"} ;
	static final String DATA_FIELD = "<soot.jimple.infoflow.test.methodSummary.ParaToField: soot.jimple.infoflow.test.methodSummary.Data dataField>";
	private static final String INT_FIELD = "<soot.jimple.infoflow.test.methodSummary.ParaToField: int intField>";
	private static final String IARRAY_FIELD = "<soot.jimple.infoflow.test.methodSummary.ParaToField: int[] intArray>";
	private static final String OBJECT_FIELD = "<soot.jimple.infoflow.test.methodSummary.ParaToField: java.lang.Object obField>";
	private static final String OBJ_ARRAY_FIELD = "<soot.jimple.infoflow.test.methodSummary.ParaToField: java.lang.Object[] arrayField>";
	private static final String LIST_FIELD = "<soot.jimple.infoflow.test.methodSummary.ParaToField: java.util.List listField>";

	@Test(timeout = 100000)
	public void intParameter() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void intPara(int)>";
		Set<MethodFlow> flow = createSummaries(mSig);

		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {DATA_FIELD, DATACLASS_INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {IARRAY_FIELD}));
		
		assertEquals(3,flow.size());
	}

	@Test(timeout = 100000)
	public void intParameterRec() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void intParaRec(int,int)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {DATA_FIELD,DATACLASS_INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {IARRAY_FIELD}));
		
		assertEquals(3,flow.size());
	}

	@Test(timeout = 100000)
	public void objectParameter() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void objPara(java.lang.Object)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {OBJ_ARRAY_FIELD}));
	}

	@Test(timeout = 100000)
	public void objectParameter2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void objPara(java.lang.Object)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {DATA_FIELD,DATACLASS_OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,LIST_ITEM));
		
		assertEquals(6,flow.size());
	}

	@Test(timeout = 100000)
	public void intAndObjectParameter() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void intAndObj(int,java.lang.Object)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJ_ARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {IARRAY_FIELD}));
		
		assertEquals(9,flow.size());
	}

	@Test(timeout = 100000)
	public void intAndObjectParameter2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void intAndObj(int,java.lang.Object)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {DATA_FIELD,DATACLASS_OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,LIST_ITEM));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {DATA_FIELD,DATACLASS_INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJ_ARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {IARRAY_FIELD}));
		
		assertEquals(9,flow.size());
	}

	@Test(timeout = 100000)
	public void arrayParas() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void arrayParas(int[],java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJ_ARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {IARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {LIST_FIELD,LINKEDLIST_FIRST,LINKEDLIST_ITEM}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {DATA_FIELD,DATACLASS_OBJECT_FIELD}));	
		assertEquals(9,flow.size());
	}

	@Test(timeout = 300000)
	public void arrayParas2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void arrayParas(int[],java.lang.Object[])>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {DATA_FIELD,DATACLASS_INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {DATA_FIELD,DATACLASS_OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,LIST_ITEM));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {}, Field,new String[] {OBJ_ARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {}, Field,new String[] {IARRAY_FIELD}));
		assertEquals(9,flow.size());
	}

	@Test(timeout = 300000)
	public void dataAndListParameterNotCompled() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void dataAndList(soot.jimple.infoflow.test.methodSummary.Data,java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {DATA_FIELD,DATACLASS_OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {DATA_FIELD,DATACLASS_INT_FIELD}));
		//assertTrue(containsFlow(flow, Parameter,1,new String[] {LINKEDLIST_FIRST}, Field,new String[] {OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {OBJ_ARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {IARRAY_FIELD}));	
		assertEquals(6,flow.size());
	}
	
	@Ignore //We dont identify list.first.item as a source. that is because first = null
	@Test(timeout = 300000)
	public void dataAndListParameter() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void dataAndList(soot.jimple.infoflow.test.methodSummary.Data,java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {DATA_FIELD,DATACLASS_OBJECT_FIELD}));
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {OBJ_ARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {IARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {LINKEDLIST_FIRST}, Field,new String[] {OBJECT_FIELD}));
		assertEquals(6,flow.size());
	}

	@Test(timeout = 400000)
	public void dataAndListParameter2NotComplet() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void dataAndList(soot.jimple.infoflow.test.methodSummary.Data,java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {DATA_FIELD,DATACLASS_INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {IARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {DATA_FIELD, DATACLASS_OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {OBJ_ARRAY_FIELD}));
		
		assertEquals(6,flow.size());
	}
	
	@Ignore //LinkedList works that as parameter source because we have list.null.null and then the points to doesnt work correctly
	@Test(timeout = 400000)
	public void dataAndListParameter2() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ParaToField: void dataAndList(soot.jimple.infoflow.test.methodSummary.Data,java.util.List)>";
		Set<MethodFlow> flow = createSummaries(mSig);
		
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {DATA_FIELD,DATACLASS_INT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_INT_FIELD}, Field,new String[] {IARRAY_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {DATA_FIELD, DATACLASS_OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,0,new String[] {DATACLASS_OBJECT_FIELD}, Field,new String[] {OBJ_ARRAY_FIELD}));
		
		assertTrue(containsFlow(flow, Parameter,1,new String[] {LINKEDLIST_FIRST,LINKEDLIST_ITEM}, Field,new String[] {OBJECT_FIELD}));
		assertTrue(containsFlow(flow, Parameter,1,new String[] {LINKEDLIST_FIRST,LINKEDLIST_ITEM}, Field,new String[] {LIST_FIELD}));
		assertEquals(6,flow.size());
	}
	
	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = new SummaryGenerator();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.LinkedList");
		sg.setSubstitutedWith(sub);
		sg.setUseRecursiveAccessPaths(true);
		sg.setAnalyseMethodsTogether(false);
		sg.setAccessPathLength(3);
		sg.setIgnoreFlowsInSystemPackages(false);
		return sg;
	}

}
