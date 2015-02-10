package soot.jimple.infoflow.methodSummary.postProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SourceContextAndPath;
import soot.jimple.infoflow.data.pathBuilders.ContextSensitivePathBuilder;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.IInfoflowCFG;
import soot.jimple.infoflow.source.SourceInfo;

/**
 * Extended path reconstruction algorithm for StubDroid
 * 
 * @author Steven Arzt
 */
public class SummaryPathBuilder extends ContextSensitivePathBuilder {
	
	private Set<SummaryResultInfo> resultInfos = new HashSet<SummaryResultInfo>();
	
	/**
	 * Extended version of the {@link SourceInfo} class that also allows to
	 * store the abstractions along the path.
	 * 
	 * @author Steven Arzt
	 */
	public class SummarySourceInfo extends ResultSourceInfo {
		
		private final List<Abstraction> abstractionPath;
		
		public SummarySourceInfo(AccessPath source, Stmt context, Object userData,
				List<Stmt> path, List<Abstraction> abstractionPath) {
			super(source, context, userData, path);
			this.abstractionPath = abstractionPath;
		}
		
		/**
		 * Gets the sequence of abstractions along the propagation path
		 * @return The sequence of abstractions along the propagation path
		 */
		public List<Abstraction> getAbstractionPath() {
			return this.abstractionPath;
		}
		
	}
	
	/**
	 * Data class containing a single source-to-sink connection produced by
	 * FlowDroid
	 * 
	 * @author Steven Arzt
	 */
	public class SummaryResultInfo {
		private final SummarySourceInfo sourceInfo;
		private final ResultSinkInfo sinkInfo;
		
		/**
		 * Creates a new instance of the {@link SummaryResultInfo} class
		 * @param sourceInfo The source information object
		 * @param sinkInfo The sink information object
		 */
		public SummaryResultInfo(SummarySourceInfo sourceInfo,
				ResultSinkInfo sinkInfo) {
			this.sourceInfo = sourceInfo;
			this.sinkInfo = sinkInfo;
		}
		
		/**
		 * Gets the source information for this source-to-sink connection
		 * @return The source information for this source-to-sink connection
		 */
		public SummarySourceInfo getSourceInfo() {
			return this.sourceInfo;
		}
		
		/**
		 * Gets the sink information for this source-to-sink connection
		 * @return The sink information for this source-to-sink connection
		 */
		public ResultSinkInfo getSinkInfo() {
			return this.sinkInfo;
		}
		
	}
	
	/**
	 * Creates a new instance of the SummaryPathBuilder class 
	 * @param icfg The interprocedural control-flow graph to use
	 * @param maxThreadNum The maximum number of threads to use
	 */
	public SummaryPathBuilder(IInfoflowCFG icfg, int maxThreadNum) {
		super(icfg, maxThreadNum, true);
	}
	
	@Override
	protected boolean checkForSource(Abstraction abs, SourceContextAndPath scap) {
		if (!super.checkForSource(abs, scap))
			return false;
		
		// Save the abstraction path
		SummarySourceInfo ssi = new SummarySourceInfo(
				abs.getSourceContext().getAccessPath(),
				abs.getSourceContext().getStmt(),
				abs.getSourceContext().getUserData(),
				scap.getPath(),
				scap.getAbstractionPath());
		ResultSinkInfo rsi = new ResultSinkInfo(
				scap.getAccessPath(),
				scap.getStmt());
		this.resultInfos.add(new SummaryResultInfo(ssi, rsi));
		return true;
	}
	
	/**
	 * Clears all results computed by this path reconstruction algorithm so far
	 */
	public void clear() {
		getResults().clear();
		resultInfos.clear();
	}
	
	/**
	 * Gets the source information and the reconstructed paths
	 * @return The found source-to-sink connections and the respective
	 * propagation paths
	 */
	public Set<SummaryResultInfo> getResultInfos() {
		return this.resultInfos;
	}

}