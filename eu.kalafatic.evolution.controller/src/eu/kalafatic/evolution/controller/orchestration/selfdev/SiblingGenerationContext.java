package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;

/**
 * Context for generating the next sibling in an evolutionary generation.
 * Aligns with the constraint-solving model where each sibling is aware of its older siblings.
 */
public class SiblingGenerationContext {

    private GoalModel goal;
    private TrajectoryBlueprint parent;
    private EvolutionDimension dimension;
    private List<TrajectoryBlueprint> olderSiblings = new ArrayList<>();
    private int siblingIndex;
    private int targetPopulation;
    

    private SemanticGenome genome;
    private EvolutionTree tree;
    private String currentParentId;
    private int generation;
    private IntentExpansionResult expansion;
    
    public SiblingGenerationContext() {
		// TODO Auto-generated constructor stub
	}
  

    public SiblingGenerationContext(GoalModel goal, TrajectoryBlueprint parent, EvolutionDimension dimension, int targetPopulation) {
        this.goal = goal;
        this.parent = parent;
        this.dimension = dimension;
        this.targetPopulation = targetPopulation;
    }

   

	public GoalModel getGoal() {
        return goal;
    }

    public void setGoal(GoalModel goal) {
        this.goal = goal;
    }

    public TrajectoryBlueprint getParent() {
        return parent;
    }

    public void setParent(TrajectoryBlueprint parent) {
        this.parent = parent;
    }

    public EvolutionDimension getDimension() {
        return dimension;
    }

    public void setDimension(EvolutionDimension dimension) {
        this.dimension = dimension;
    }

    public List<TrajectoryBlueprint> getOlderSiblings() {
        return olderSiblings;
    }

    public void setOlderSiblings(List<TrajectoryBlueprint> olderSiblings) {
        this.olderSiblings = olderSiblings;
    }

    public int getSiblingIndex() {
        return siblingIndex;
    }

    public void setSiblingIndex(int siblingIndex) {
        this.siblingIndex = siblingIndex;
    }

    public int getTargetPopulation() {
        return targetPopulation;
    }

    public void setTargetPopulation(int targetPopulation) {
        this.targetPopulation = targetPopulation;
    }

	public SemanticGenome getGenome() {
		return genome;
	}

	public void setGenome(SemanticGenome genome) {
		this.genome = genome;
	}

	public EvolutionTree getTree() {
		return tree;
	}

	public void setTree(EvolutionTree tree) {
		this.tree = tree;
	}

	public String getCurrentParentId() {
		return currentParentId;
	}

	public void setCurrentParentId(String currentParentId) {
		this.currentParentId = currentParentId;
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = generation;
	}


	public IntentExpansionResult getExpansion() {
		return expansion;
	}


	public void setExpansion(IntentExpansionResult expansion) {
		this.expansion = expansion;
	}


}
