package soot.jimple.infoflow.methodSummary.postProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.methodSummary.data.factory.SourceSinkFactory;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.generator.GapManager;
import soot.jimple.infoflow.methodSummary.generator.SummaryGeneratorConfiguration;
import soot.jimple.infoflow.methodSummary.postProcessor.SummaryPathBuilder.SummaryResultInfo;
import soot.jimple.infoflow.methodSummary.postProcessor.SummaryPathBuilder.SummarySourceInfo;
import soot.jimple.infoflow.methodSummary.util.AliasUtils;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.util.MultiMap;

public class InfoflowResultPostProcessor {
	private final boolean DEBUG = true;
	private static final Logger logger = LoggerFactory.getLogger(InfoflowResultPostProcessor.class);

	protected final IInfoflowCFG cfg;
	private final MultiMap<Abstraction, Stmt> collectedAbstractions;
	private final String method;
	protected final SourceSinkFactory sourceSinkFactory;
	private final GapManager gapManager;
	private final SummaryGeneratorConfiguration config;
	
	public InfoflowResultPostProcessor(MultiMap<Abstraction, Stmt> collectedAbstractions,
			IInfoflowCFG cfg, String m, SourceSinkFactory sourceSinkFactory,
			GapManager gapManager, SummaryGeneratorConfiguration config) {
		this.collectedAbstractions = collectedAbstractions;
		this.cfg = cfg;
		this.method = m;
		this.sourceSinkFactory = sourceSinkFactory;
		this.gapManager = gapManager;
		this.config = config;
	}
	
	/**
	 * Post process the information collected during a Infoflow analysis.
	 * Extract all summary flow from collectedAbstractions.
	 * @return The generated method summaries
	 */
	public MethodSummaries postProcess() {
		MethodSummaries summaries = new MethodSummaries();
		postProcess(summaries);
		return summaries;
	}
	
	/**
	 * Post process the information collected during a Infoflow analysis.
	 * Extract all summary flow from collectedAbstractions.
	 * @param flos The method summary object in which to store the detected
	 * flows
	 */
	public MethodSummaries postProcess(MethodSummaries flows) {
		logger.info("start processing {} infoflow abstractions for method {}", 
				collectedAbstractions.size(), method);
		final SootMethod m = Scene.v().getMethod(method);
		
		// Create a context-sensitive path builder. Without context-sensitivity,
		// we get quite some false positives here.
		SummaryPathBuilder pathBuilder = new SummaryPathBuilder(cfg,
				Runtime.getRuntime().availableProcessors());
		
		int analyzedPaths = 0;
		int abstractionCount = 0;
		for (Abstraction a : collectedAbstractions.keySet()) {
			// If this abstraction is directly the source abstraction, we do not
			// need to construct paths
			if (a.getSourceContext() != null) {
				for (Stmt stmt : collectedAbstractions.get(a)) {
					processFlowSource(flows, m, a.getAccessPath(), stmt,
							pathBuilder.new SummarySourceInfo(a.getAccessPath(), a.getCurrentStmt(),
									a.getSourceContext().getUserData(),
									a.getAccessPath(),
									isAliasedField(a.getAccessPath(),
											a.getSourceContext().getAccessPath(),
											a.getSourceContext().getStmt()),
									false));
				}
			}
			else {
				// In case we have the same abstraction in multiple places and we
				// extend it with external sink information regardless of the
				// original propagation, we need to clean up first
				a.clearPathCache();
				
				// Get the source info and process the flow
				pathBuilder.clear();
				pathBuilder.computeTaintPaths(Collections.singleton(
						new AbstractionAtSink(a, a.getCurrentStmt())));
				logger.info("Obtained {} source-to-sink connections.",
						pathBuilder.getResultInfos().size());
				
				// Reconstruct the sources
				for (Stmt stmt : collectedAbstractions.get(a)) {
					abstractionCount++;
					
					// If this abstraction is directly the source abstraction, we do not
					// need to construct paths
					if (a.getSourceContext() != null) {
						continue;
					}
					
					for (SummaryResultInfo si : pathBuilder.getResultInfos()) {
						final AccessPath sourceAP = si.getSourceInfo().getAccessPath();
						final AccessPath sinkAP = si.getSinkInfo().getAccessPath();
						final Stmt sourceStmt = si.getSourceInfo().getSource();
						
						// Check that we don't get any weird results
						if (sourceAP == null || sinkAP == null)
							throw new RuntimeException("Invalid access path");
						
						// We only take flows which are not identity flows.
						// If we have a flow from a gap parameter to the original
						// method parameter, the access paths are equal, but that's
						// ok in the case of aliasing.
						boolean isAliasedField = gapManager.getGapForCall(sourceStmt) != null
								&& isAliasedField(sinkAP, sourceAP, sourceStmt)
								&& si.getSourceInfo().getIsAlias();
						if (!sinkAP.equals(sourceAP) || isAliasedField) {
							// Process the flow from this source
							processFlowSource(flows, m, sinkAP, stmt, si.getSourceInfo());
							analyzedPaths++;
						}
					}
				}
				
				// Free some memory
				pathBuilder.clear();
			}
		}
		
		pathBuilder.shutdown();
		
		// Compact the flow set to remove paths that are over-approximations of
		// other flows
		new SummaryFlowCompactor(flows).compact();
		
		// Check the generated summaries for validity
		if (config.getValidateResults())
			flows.validate();
		
		logger.info("Result processing finished, analyzed {} paths from {} stored "
				+ "abstractions", analyzedPaths, abstractionCount);
		return flows;
	}
	
	/**
	 * Checks whether the two given access paths may alias at the given
	 * statement
	 * @param apAtSink The first access path
	 * @param apAtSource The second access path
	 * @param sourceStmt The statement at which to check for may-alias
	 * @return True if the two given access paths may alias at the given
	 * statement, otherwise false
	 */
	private boolean isAliasedField(AccessPath apAtSink,
			AccessPath apAtSource, Stmt sourceStmt) {
		// Strings and primitives do not alias
		if (!AliasUtils.canAccessPathHaveAliases(apAtSink))
			return false;
		
		return true;
	}
	
	/**
	 * Processes data from a given flow source that has arrived at a given
	 * statement
	 * @param flows The flows object to which to add the newly found flow
	 * @param ap The access path that has reached the given statement
	 * @param m The method in which the flow has been found
	 * @param stmt The statement at which the flow has arrived
	 * @param source The source from which the flow originated
	 */
	private void processFlowSource(MethodSummaries flows, final SootMethod m,
			AccessPath ap, Stmt stmt, SummarySourceInfo sourceInfo) {
		// Get the source information for this abstraction
		@SuppressWarnings("unchecked")
		Collection<FlowSource> sources = (Collection<FlowSource>) sourceInfo.getUserData();
		if (sources == null || sources.size() == 0)
			throw new RuntimeException("Link to source missing");
		
		// We can have multiple sources from a gap a.foo(b,b) on access path b
		for (FlowSource flowSource : sources) {
			if (flowSource == null)
				continue;
			
			// Get the source access path
			AccessPath sourceAP = sourceInfo.getSourceAP();
			boolean isAlias = sourceInfo.getIsAlias();
			boolean isInCallee = sourceInfo.getIsInCallee();
			
			// Create the flow source data object
			flowSource = sourceSinkFactory.createSource(flowSource.getType(),
					flowSource.getParameterIndex(), sourceAP, flowSource.getGap());
			
			// Depending on the statement at which the flow ended, we need to create
			// a different type of summary
			if (cfg.isExitStmt(stmt))
				processAbstractionAtReturn(flows, ap, m, flowSource, stmt,
						sourceAP, isAlias, isInCallee);
			else if (cfg.isCallStmt(stmt))
				processAbstractionAtCall(flows, ap, flowSource, stmt, sourceAP, isAlias);
			else
				throw new RuntimeException("Invalid statement for flow "
						+ "termination: " + stmt);
		}
	}
	
	/**
	 * Processes an abstraction at a method call. This is a partial summary that
	 * ends at a gap which can for instance be a callback into unknown code.
	 * @param flows The flows object to which to add the newly found flow
	 * @param apAtCall The access path that has reached the method call
	 * @param source The source at which the data flow started
	 * @param stmt The statement at which the call happened
	 * @param sourceAP The access path of the flow source
	 * @param isAlias True if source and sink alias, otherwise false
	 */
	protected void processAbstractionAtCall(MethodSummaries flows, AccessPath apAtCall,
			FlowSource source, Stmt stmt, AccessPath sourceAP,
			boolean isAlias) {
		// Create a gap
		GapDefinition gd = gapManager.getGapForCall(stmt);
		if (gd == null)
			return;
		
		// Create the flow sink
		FlowSink sink = createFlowSinkAtCall(apAtCall, gd, stmt);
		if (sink != null)
			addFlow(source, sink, isAlias, flows);
	}
	
	/**
	 * Creates a flow sink at the given call site
	 * @param apAtCall The access path that arives at the given call site
	 * @param gd The gap created at the given call site
	 * @param stmt The statement containing the call site
	 * @return The flow sink created for the given access path at the given
	 * statement if it matches, otherwise false
	 */
	protected FlowSink createFlowSinkAtCall(AccessPath apAtCall, GapDefinition gd,
			Stmt stmt) {
		// Check whether we have the base object
		if (apAtCall.isLocal())
			if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
				Local baseLocal = (Local) iinv.getBase();
				if (baseLocal == apAtCall.getPlainValue()) {
					return sourceSinkFactory.createGapBaseObjectSink(gd,
							apAtCall.getBaseType());
				}
			}
		
		// The sink may be a parameter in the call to the gap method
		for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
			Value p = stmt.getInvokeExpr().getArg(i);
			if (apAtCall.getPlainValue() == p) {
				return sourceSinkFactory.createParameterSink(i, apAtCall, gd);
			}
		}

		// The sink may be a local field on the base object
		if (apAtCall.getFieldCount() > 0 && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
			if (apAtCall.getPlainValue() == iinv.getBase()) {
				return sourceSinkFactory.createFieldSink(apAtCall);
			}
		}
		
		// Nothing matched
		return null;
	}
	
	/**
	 * Processes an abstraction at the end of a method. This gives full
	 * summaries for the whole method
	 * @param flows The flows object to which to add the newly found flow
	 * @param apAtReturn The access path that has reached the end of the method
	 * @param m The method in which the flow has been found
	 * @param source The source at which the data flow started
	 * @param stmt The statement at which the flow left the method
	 * @param sourceAP The access path of the flow source
	 * @param isAlias True if source and sink alias, otherwise false
	 * @param isInCallee True if the abstraction was recorded in a callee, false
	 * if it was recorded in the original method without any recursive calls or
	 * the like
	 */
	protected void processAbstractionAtReturn(MethodSummaries flows, AccessPath apAtReturn,
			SootMethod m, FlowSource source, Stmt stmt, AccessPath sourceAP,
			boolean isAlias, boolean isInCallee) {
		// Was this the value returned by the method?
		if (stmt instanceof ReturnStmt) {
			ReturnStmt retStmt = (ReturnStmt) stmt;
			if (apAtReturn.getPlainValue() == retStmt.getOp()) {
				FlowSink sink = sourceSinkFactory.createReturnSink(apAtReturn);
				addFlow(source, sink, isAlias, flows);
			}
		}
		
		// The sink may be a parameter
		if (!isInCallee) {
			if (!apAtReturn.isLocal()
					|| apAtReturn.getTaintSubFields()
					|| apAtReturn.getBaseType() instanceof ArrayType)
				for (int i = 0; i < m.getParameterCount(); i++) {
					Local p = m.getActiveBody().getParameterLocal(i);
					if (apAtReturn.getPlainValue() == p) {
						FlowSink sink = sourceSinkFactory.createParameterSink(i, apAtReturn);
						addFlow(source, sink, isAlias, flows);
					}
				}
		}
		
		// The sink may be a local field
		if (!m.isStatic() && apAtReturn.getPlainValue() ==
				m.getActiveBody().getThisLocal()) {
			FlowSink sink = sourceSinkFactory.createFieldSink(apAtReturn);
			addFlow(source, sink, isAlias, flows);
		}
		
		// The sink may be a field on a value obtained from a gap
		if (apAtReturn.isInstanceFieldRef()) {
			Set<GapDefinition> referencedGaps = gapManager.getGapDefinitionsForLocalUse(
					apAtReturn.getPlainValue());
			if (referencedGaps != null && !referencedGaps.isEmpty())
				for (GapDefinition gap : referencedGaps) {
					FlowSink sink = sourceSinkFactory.createFieldSink(apAtReturn, gap);
					addFlow(source, sink, isAlias, flows);
				}
			
			referencedGaps = gapManager.getGapDefinitionsForLocalDef(
					apAtReturn.getPlainValue());
			if (referencedGaps != null && !referencedGaps.isEmpty())
				for (GapDefinition gap : referencedGaps) {
					FlowSink sink = sourceSinkFactory.createReturnSink(apAtReturn, gap);
					addFlow(source, sink, isAlias, flows);
				}
		}
	}
	
	/**
	 * Checks whether this flow has equal sources and sinks, i.e. is propagated
	 * through the method as-is.
	 * @param source The source to check
	 * @param sink The sink to check
	 * @return True if the source is equivalent to the sink, otherwise false
	 */
	private boolean isIdentityFlow(FlowSource source, FlowSink sink) {
		if (sink.isReturn())
			return false;
		if (sink.isField() && source.isParameter())
			return false;
		if (sink.isParameter() && (source.isField() || source.isThis()))
			return false;
		if (source.getGap() != sink.getGap())
			return false;
		
		if (sink.getParameterIndex() != source.getParameterIndex())
			return false;
		
		// If the sink has an access path, but not the source, or vice versa,
		// this cannot be an identity flow
		if ((sink.getAccessPath() == null && source.getAccessPath() != null)
				|| (sink.getAccessPath() != null && source.getAccessPath() == null))
			return false;
		
		// Compare the access paths
		if (sink.getAccessPath() != null) {
			if (sink.getAccessPath().length != source.getAccessPath().length)
				return false;
			for (int i = 0; i < sink.getAccessPath().length; i++) {
				if (!source.getAccessPath()[i].equals(sink.getAccessPath()[i]))
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Adds a flow from the given source to the given sink to the given method
	 * summary
	 * @param source The source at which the data flow starts
	 * @param sink The sink at which the data flow ends
	 * @param isAlias True if the source and sink alias, otherwise false
	 * @param summaries The method summary to which to add the data flow
	 */
	protected void addFlow(FlowSource source, FlowSink sink, boolean isAlias,
			MethodSummaries summaries) {
		// Convert the method signature into a subsignature
		String methodSubSig = SootMethodRepresentationParser.v()
				.parseSootMethodString(method).getSubSignature();
		
		if (source.toString().equals("source: Field [<soot.jimple.infoflow.test.methodSummary.ContextSensitivity: java.lang.String x>]"))
			System.out.println("x");
		
		// Ignore identity flows
		if (isIdentityFlow(source, sink))
			return;
		
		MethodFlow mFlow = new MethodFlow(methodSubSig, source, sink, isAlias);
		if (summaries.addFlow(mFlow))
			debugMSG(source, sink, isAlias);
	}
	
	private void debugMSG(FlowSource source, FlowSink sink, boolean isAlias) {
		if (DEBUG) {
			System.out.println("\nmethod: " + method);
			System.out.println("source: " + source.toString());
			System.out.println("sink  : " + sink.toString());
			System.out.println("alias : " + isAlias);

			System.out.println("------------------------------------");
		}
	}
	
}
